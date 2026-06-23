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
import java.net.ServerSocket;
import java.net.Socket;

public class WifiDirectService extends Service {

    public static final int    PORT            = 8988;
    public static final String CHANNEL_ID      = "wifi_direct_channel";

    // ── Shared state (read by any Activity) ─────────────────────
    public static boolean isConnected          = false;
    public static boolean isGroupOwner         = false;
    public static String  connectedHostAddress = null;
    public static String  connectedDeviceName  = "";
    public static String  pendingFilePath      = null;
    public static String  myHostAddress        = null;
    public static String  clientAddress        = null;

    // ── Callbacks to update UI ───────────────────────────────────
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

    // ────────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification("Wi-Fi Direct active"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // restart if killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    // ── Called by WifiDirectActivity when connection state changes
    public void onBecomeHost(String hostAddress) {
        isConnected          = true;
        isGroupOwner         = true;
        connectedHostAddress = hostAddress;

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

        updateNotification("Connected to " + deviceName);

        if (listener != null)
            listener.onConnected(false, hostAddress, deviceName);
    }

    public void onConnectionLost() {
        isConnected          = false;
        isGroupOwner         = false;
        connectedHostAddress = null;
        serverRunning        = false;

        updateNotification("Wi-Fi Direct – not connected");

        stopServer();

        if (listener != null)
            listener.onDisconnected();
    }

    // ── Send a file (called from any Activity) ───────────────────
    public void sendFile(File file) {
        if (!isConnected || isGroupOwner || connectedHostAddress == null) {
            if (listener != null)
                listener.onError("Not connected as client");
            return;
        }
        new SendThread(file).start();
    }

    // ── Stop server socket ───────────────────────────────────────
    private void stopServer() {
        try { if (activeSocket != null) activeSocket.close(); }
        catch (Exception ignored) {}
        activeSocket  = null;
        serverRunning = false;
    }

    // ── RECEIVE (HOST) ───────────────────────────────────────────
    private class ReceiveThread extends Thread {
        @Override public void run() {
            Socket client = null;
            try {
                activeSocket = new ServerSocket(PORT);
                client       = activeSocket.accept();

                File dir = new File(
                        android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS),
                        "MilkDelivery/received");
                if (!dir.exists()) dir.mkdirs();

                File         out = new File(dir, "backup_" + System.currentTimeMillis() + ".json");
                InputStream  in  = client.getInputStream();
                FileOutputStream fos = new FileOutputStream(out);

                byte[] buf = new byte[4096];
                int    n;
                while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);

                fos.flush(); fos.close(); in.close();

                updateNotification("File received ✅");

                if (listener != null)
                    listener.onFileReceived(out.getAbsolutePath());

            } catch (Exception e) {
                if (listener != null) listener.onError("Receive error: " + e.getMessage());
            } finally {
                serverRunning = false;
                try { if (client       != null) client.close();       } catch (Exception ignored) {}
                try { if (activeSocket != null) activeSocket.close(); } catch (Exception ignored) {}
                activeSocket = null;
            }
        }
    }

    // ── SEND (CLIENT) ────────────────────────────────────────────
    private class SendThread extends Thread {
        private final File file;
        SendThread(File file) { this.file = file; }

        @Override public void run() {
            Socket          socket = null;
            FileInputStream fis    = null;
            try {
                socket = new Socket(connectedHostAddress, PORT);
                OutputStream out = socket.getOutputStream();
                fis = new FileInputStream(file);

                byte[] buf = new byte[4096];
                int    n;
                while ((n = fis.read(buf)) != -1) out.write(buf, 0, n);

                out.flush();
                socket.shutdownOutput(); // sends EOF to HOST

                updateNotification("File sent ✅");

                if (listener != null)
                    listener.onFileSent(file.getName());

            } catch (Exception e) {
                if (listener != null) listener.onError("Send error: " + e.getMessage());
            } finally {
                try { if (fis    != null) fis.close();    } catch (Exception ignored) {}
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
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