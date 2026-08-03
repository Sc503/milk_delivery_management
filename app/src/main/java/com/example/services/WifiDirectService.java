package com.example.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class WifiDirectService extends Service {

    public static final int PORT = 8988;
    public static final String CHANNEL_ID = "wifi_direct_channel";

    // ALL PUBLIC STATIC - Easy access from anywhere
    public static boolean isConnected = false;
    public static boolean isGroupOwner = false;
    public static String connectedHostAddress = null;
    public static String connectedDeviceName = "";
    public static String myHostAddress = null;
    public static String hostIpAddress = null;
    public static String clientIpAddress = null;

    // Callbacks
    public interface StateListener {
        void onConnected(boolean asHost, String hostAddress, String deviceName);
        void onDisconnected();
        void onFileReceived(String filePath);
        void onFileSent(String fileName);
        void onError(String message);
    }
    private static StateListener listener;
    public static void setListener(StateListener l) { listener = l; }
    public static void clearListener() { listener = null; }

    // Binder
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public WifiDirectService getService() { return WifiDirectService.this; }
    }
    @Override public IBinder onBind(Intent intent) { return binder; }

    private boolean serverRunning = false;
    private ServerSocket serverSocket = null;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification("Wi-Fi Direct active"));
        Log.d("WifiDirectService", "✅ Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
        Log.d("WifiDirectService", "❌ Service destroyed");
    }

    // ── Connection State Methods ──────────────────────────────────
    public void onBecomeHost(String hostAddress) {
        isConnected = true;
        isGroupOwner = true;
        connectedHostAddress = hostAddress;
        myHostAddress = hostAddress;
        hostIpAddress = hostAddress;
        clientIpAddress = null;

        Log.d("WifiDirectService", "✅ Became HOST: " + hostAddress);
        Log.d("WifiDirectService", "✅ Host IP stored: " + hostIpAddress);
        startServer();

        if (listener != null)
            listener.onConnected(true, hostAddress, connectedDeviceName);
    }

    public void onBecomeClient(String hostAddress, String deviceName) {
        isConnected = true;
        isGroupOwner = false;
        connectedHostAddress = hostAddress;
        connectedDeviceName = deviceName;
        myHostAddress = null;
        hostIpAddress = hostAddress;
        clientIpAddress = null;

        Log.d("WifiDirectService", "✅ Became CLIENT. Host: " + hostAddress);
        Log.d("WifiDirectService", "✅ Device Name: " + deviceName);
        Log.d("WifiDirectService", "✅ Host IP stored: " + hostIpAddress);
        startServer();

        if (listener != null)
            listener.onConnected(false, hostAddress, deviceName);
    }

    public void onConnectionLost() {
        isConnected = false;
        isGroupOwner = false;
        connectedHostAddress = null;
        myHostAddress = null;
        hostIpAddress = null;
        clientIpAddress = null;

        Log.d("WifiDirectService", "❌ Connection lost");
        stopServer();

        if (listener != null)
            listener.onDisconnected();
    }

    // ── Start Server for Receiving ──────────────────────────────
    private void startServer() {
        if (serverRunning) {
            Log.d("WifiDirectService", "⚠️ Server already running");
            return;
        }
        serverRunning = true;
        new Thread(new ReceiveRunnable()).start();
        Log.d("WifiDirectService", "📥 Server started on port " + PORT);
    }

    private void stopServer() {
        serverRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception ignored) {}
        Log.d("WifiDirectService", "📥 Server stopped");
    }

    // ── Receive Runnable ─────────────────────────────────────────
    private class ReceiveRunnable implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                Log.d("WifiDirectService", "📥 Server listening on port " + PORT);

                while (serverRunning) {
                    try {
                        Log.d("WifiDirectService", "📥 Waiting for incoming connection...");
                        Socket client = serverSocket.accept();
                        client.setSoTimeout(30000);

                        // Get client IP
                        String clientIp = client.getInetAddress().getHostAddress();

                        //  FIX 3: Don't accept connections from our own device
                        if (clientIp.equals(myHostAddress) || clientIp.equals("127.0.0.1")) {
                            Log.d("WifiDirectService", "⚠️ Ignoring own connection from: " + clientIp);
                            client.close();
                            continue;
                        }

                        // Store client IP for sending back
                        clientIpAddress = clientIp;
                        Log.d("WifiDirectService", "✅ CLIENT CONNECTED!");
                        Log.d("WifiDirectService", "✅ Client IP: " + clientIp);

                        // Receive file
                        receiveFile(client);

                    } catch (Exception e) {
                        if (serverRunning) {
                            Log.e("WifiDirectService", "Receive error: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("WifiDirectService", "Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ── Receive File (SAVES TO DOWNLOADS FOLDER) ──────────────
    private void receiveFile(Socket client) {
        InputStream in = null;
        FileOutputStream fos = null;

        try {
            in = client.getInputStream();

            // Get received folder (Downloads/MilkDelivery/received)
            File receivedFolder = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                    ),
                    "MilkDelivery/received"
            );

            if (!receivedFolder.exists()) {
                receivedFolder.mkdirs();
                Log.d("WifiDirectService", "📁 Created folder: " + receivedFolder.getAbsolutePath());
            }

            // Create unique filename
            String timestamp = new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
            ).format(new java.util.Date());
            File outFile = new File(receivedFolder, "received_backup_" + timestamp + ".enc");

            fos = new FileOutputStream(outFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            fos.flush();

            Log.d("WifiDirectService", "✅ File received: " + outFile.getAbsolutePath());
            Log.d("WifiDirectService", "✅ File size: " + totalBytes + " bytes");

            //  FIX 2: Notify with file path so UI can refresh
            if (listener != null) {
                listener.onFileReceived(outFile.getAbsolutePath());
            }

        } catch (Exception e) {
            Log.e("WifiDirectService", "Error receiving file: " + e.getMessage());
            e.printStackTrace();
            if (listener != null) {
                listener.onError("Receive failed: " + e.getMessage());
            }
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (client != null) client.close(); } catch (Exception ignored) {}
        }
    }

    // ── Send File ─────────────────────────────────────────────────
    public void sendFile(File file) {
        if (!isConnected) {
            if (listener != null)
                listener.onError("Not connected to any device");
            return;
        }

        Log.d("WifiDirectService", "📤 Preparing to send: " + file.getName());
        Log.d("WifiDirectService", "📤 isGroupOwner: " + isGroupOwner);
        Log.d("WifiDirectService", "📤 clientIpAddress: " + clientIpAddress);
        Log.d("WifiDirectService", "📤 hostIpAddress: " + hostIpAddress);

        new Thread(new SendRunnable(file)).start();
    }

    private class SendRunnable implements Runnable {
        private final File file;

        SendRunnable(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            Socket socket = null;
            OutputStream out = null;
            FileInputStream fis = null;

            try {
                // Determine target address
                String targetAddress = getTargetAddress();

                if (targetAddress == null || targetAddress.isEmpty()) {
                    Log.e("WifiDirectService", "❌ No target address found");
                    if (listener != null) {
                        listener.onError("Could not find target device IP. Make sure both devices are connected.");
                    }
                    return;
                }

                Log.d("WifiDirectService", "📤 Target address: " + targetAddress);
                Log.d("WifiDirectService", "📤 Connecting to: " + targetAddress + ":" + PORT);

                // Connect to target
                socket = new Socket();
                socket.connect(new InetSocketAddress(targetAddress, PORT), 30000);
                socket.setSoTimeout(30000);

                Log.d("WifiDirectService", "📤 Connected to target!");

                out = socket.getOutputStream();
                fis = new FileInputStream(file);

                // Send file data
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                out.flush();
                socket.shutdownOutput();

                Log.d("WifiDirectService", "✅ File sent! Total: " + totalBytes + " bytes");

                if (listener != null) {
                    listener.onFileSent(file.getName());
                }

            } catch (Exception e) {
                Log.e("WifiDirectService", "❌ Send error: " + e.getMessage());
                e.printStackTrace();
                if (listener != null) {
                    listener.onError("Send failed: " + e.getMessage());
                }
            } finally {
                try { if (fis != null) fis.close(); } catch (Exception ignored) {}
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ── Get Target Address ──────────────────────────────────────
    private String getTargetAddress() {
        if (isGroupOwner) {
            // HOST sends to CLIENT
            Log.d("WifiDirectService", "🔍 HOST looking for CLIENT...");
            if (clientIpAddress != null && !clientIpAddress.isEmpty()) {
                Log.d("WifiDirectService", "✅ Using stored client IP: " + clientIpAddress);
                return clientIpAddress;
            }
            Log.d("WifiDirectService", "⚠️ No stored client IP, searching...");
            return findClientAddress();
        } else {
            // CLIENT sends to HOST
            Log.d("WifiDirectService", "🔍 CLIENT looking for HOST...");
            if (hostIpAddress != null && !hostIpAddress.isEmpty()) {
                Log.d("WifiDirectService", "✅ Using stored host IP: " + hostIpAddress);
                return hostIpAddress;
            }
            Log.d("WifiDirectService", "⚠️ No stored host IP, using connectedHostAddress: " + connectedHostAddress);
            return connectedHostAddress;
        }
    }

    // ── Find Client Address ──────────────────────────────────────
    private String findClientAddress() {
        Log.d("WifiDirectService", "🔍 Searching for client IP...");

        String[] possibleIps = {
                "192.168.49.2",
                "192.168.49.1", //host
                "192.168.1.2",
                "192.168.0.2",
                "192.168.43.2",
                "10.0.0.2",
                "192.168.49.97" //client
        };

        for (String ip : possibleIps) {
            if (isReachable(ip)) {
                Log.d("WifiDirectService", "✅ Found client at: " + ip);
                return ip;
            }
        }

        if (myHostAddress != null && !myHostAddress.isEmpty()) {
            try {
                String[] parts = myHostAddress.split("\\.");
                if (parts.length == 4) {
                    String base = parts[0] + "." + parts[1] + "." + parts[2] + ".";
                    for (int i = 1; i <= 254; i++) {
                        if (i == Integer.parseInt(parts[3])) continue;
                        String testIp = base + i;
                        if (isReachable(testIp)) {
                            Log.d("WifiDirectService", "✅ Found client at: " + testIp);
                            return testIp;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("WifiDirectService", "Error scanning subnet: " + e.getMessage());
            }
        }

        Log.e("WifiDirectService", "❌ No client IP found!");
        return null;
    }

    private boolean isReachable(String ip) {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(ip);
            return address.isReachable(2000);
        } catch (Exception e) {
            return false;
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
}