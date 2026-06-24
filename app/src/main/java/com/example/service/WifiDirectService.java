package com.example.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WifiDirectService extends Service {

    public static final int    PORT            = 8988;
    public static final String CHANNEL_ID      = "wifi_direct_channel";

    // ── Shared state ─────────────────────────────────────────────
    public static boolean isConnected          = false;
    public static boolean isGroupOwner         = false;
    public static String  connectedHostAddress = null;
    public static String  connectedDeviceName  = "";
    public static String  pendingFilePath      = null;
    public static String  myHostAddress        = null;
    public static String  clientAddress        = null; // ✅ Store client IP

    // ── Callbacks ─────────────────────────────────────────────────
    public interface StateListener {
        void onConnected(boolean asHost, String hostAddress, String deviceName);
        void onDisconnected();
        void onFileReceived(String filePath);
        void onFileSent(String fileName);
        void onError(String message);
    }
    private static StateListener listener;
    public static void setListener(StateListener l) { listener = l; }
    public static void clearListener()              { listener = null; }

    // ── Binder ───────────────────────────────────────────────────
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public WifiDirectService getService() { return WifiDirectService.this; }
    }
    @Override public IBinder onBind(Intent intent) { return binder; }

    private boolean serverRunning      = false;
    private ServerSocket activeSocket  = null;
    private List<String> connectedClients = new ArrayList<>();

    // ────────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification("Wi-Fi Direct active"));
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    // ── Start Server for Receiving ──────────────────────────────
    private void startServer() {
        if (!serverRunning) {
            serverRunning = true;
            new ReceiveThread().start();
        }
    }

    // ── Called by WifiDirectActivity when connection state changes
    public void onBecomeHost(String hostAddress) {
        isConnected          = true;
        isGroupOwner         = true;
        connectedHostAddress = hostAddress;
        myHostAddress        = hostAddress;
        clientAddress        = null;

        updateNotification("HOST – waiting for file…");

        if (!serverRunning) {
            serverRunning = true;
            new ReceiveThread().start();
        }

        if (listener != null)
            listener.onConnected(true, hostAddress, connectedDeviceName);
    }

    public void onBecomeClient(String hostAddress, String deviceName) {
        isConnected          = true;
        isGroupOwner         = false;
        connectedHostAddress = hostAddress;
        connectedDeviceName  = deviceName;
        myHostAddress        = null;
        clientAddress        = null;

        updateNotification("Connected to " + deviceName);

        if (listener != null)
            listener.onConnected(false, hostAddress, deviceName);
    }

    public void onConnectionLost() {
        isConnected          = false;
        isGroupOwner         = false;
        connectedHostAddress = null;
        myHostAddress        = null;
        clientAddress        = null;
        serverRunning        = false;
        connectedClients.clear();

        updateNotification("Wi-Fi Direct – not connected");

        stopServer();

        if (listener != null)
            listener.onDisconnected();
    }

    // ── ✅ FIXED: Send a file (BOTH HOST AND CLIENT CAN SEND) ──
    public void sendFile(File file) {
        if (!isConnected) {
            if (listener != null)
                listener.onError("Not connected to any device");
            return;
        }

        // ✅ REMOVED the isGroupOwner check - BOTH can send!
        new SendThread(file).start();
    }

    // ── Stop server socket ───────────────────────────────────────
    private void stopServer() {
        serverRunning = false;
        try { if (activeSocket != null) activeSocket.close(); }
        catch (Exception ignored) {}
        activeSocket  = null;
    }

    // ── ✅ RECEIVE (BOTH HOST AND CLIENT) ────────────────────────
    private class ReceiveThread extends Thread {
        @Override public void run() {
            try {
                activeSocket = new ServerSocket(PORT);

                while (serverRunning) {
                    Socket client = activeSocket.accept();

                    // ✅ Get client IP address
                    String clientIp = client.getInetAddress().getHostAddress();
                    if (!connectedClients.contains(clientIp)) {
                        connectedClients.add(clientIp);
                        clientAddress = clientIp;
                    }

                    File dir = new File(
                            android.os.Environment
                                    .getExternalStoragePublicDirectory(
                                            android.os.Environment.DIRECTORY_DOWNLOADS),
                            "MilkDelivery/received"
                    );
                    if (!dir.exists()) dir.mkdirs();

                    File outFile = new File(
                            dir,
                            "received_backup_" + System.currentTimeMillis() + ".json"
                    );

                    InputStream in = client.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outFile);

                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }

                    fos.flush();
                    fos.close();
                    in.close();
                    client.close();

                    updateNotification("File received ✅");

                    if (listener != null)
                        listener.onFileReceived(outFile.getAbsolutePath());
                }

            } catch (Exception e) {
                if (serverRunning && listener != null) {
                    listener.onError("Receive error: " + e.getMessage());
                }
            }
        }
    }

    // ── ✅ SEND (BOTH HOST AND CLIENT) ────────────────────────────
    private class SendThread extends Thread {
        private final File file;
        SendThread(File file) { this.file = file; }

        @Override public void run() {
            Socket socket = null;
            FileInputStream fis = null;
            String targetAddress = null;

            try {
                // ✅ Determine target address based on role
                if (isGroupOwner) {
                    // HOST sends to client - find client IP
                    targetAddress = findClientAddress();
                } else {
                    // CLIENT sends to host
                    targetAddress = connectedHostAddress;
                }

                if (targetAddress == null) {
                    if (listener != null) {
                        listener.onError("Could not find target device. Try reconnecting.");
                    }
                    return;
                }

                socket = new Socket(targetAddress, PORT);
                OutputStream out = socket.getOutputStream();
                fis = new FileInputStream(file);

                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }

                out.flush();
                socket.shutdownOutput();

                updateNotification("File sent ✅");

                if (listener != null)
                    listener.onFileSent(file.getName());

            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Send error: " + e.getMessage() + "\nTry reconnecting.");
                }
            } finally {
                try { if (fis != null) fis.close(); } catch (Exception ignored) {}
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            }
        }

        // ── Find client IP when HOST wants to send ──────────────────
        private String findClientAddress() {
            // Try stored client address
            if (clientAddress != null && !clientAddress.isEmpty()) {
                if (isReachable(clientAddress, 2000)) {
                    return clientAddress;
                }
            }

            // Try connected clients list
            for (String ip : connectedClients) {
                if (isReachable(ip, 2000)) {
                    return ip;
                }
            }

            // Try common WiFi Direct client IPs
            String[] possibleIps = {
                    "192.168.49.1",
                    "192.168.49.2",
                    "192.168.1.1",
                    "192.168.0.1",
                    "192.168.43.1",
                    "10.0.0.1"
            };

            for (String ip : possibleIps) {
                if (isReachable(ip, 2000)) {
                    return ip;
                }
            }

            // Try to find using host IP (scan subnet)
            if (myHostAddress != null && !myHostAddress.isEmpty()) {
                try {
                    String[] parts = myHostAddress.split("\\.");
                    if (parts.length == 4) {
                        String base = parts[0] + "." + parts[1] + "." + parts[2] + ".";
                        for (int i = 1; i <= 254; i++) {
                            if (i != Integer.parseInt(parts[3])) {
                                String testIp = base + i;
                                if (isReachable(testIp, 500)) {
                                    return testIp;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue with default IPs
                }
            }

            return null;
        }

        private boolean isReachable(String ip, int timeoutMs) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                return address.isReachable(timeoutMs);
            } catch (Exception e) {
                return false;
            }
        }
    }

    // ── Notification ─────────────────────────────────────────────
    private Notification buildNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "WiFi Direct", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi Direct Transfer")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(1, buildNotification(text));
    }
}