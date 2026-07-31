package com.example.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.R;
import com.example.activities.TempWifiDirectActivity;
import com.example.models.TransferState;
import com.example.utils.NetworkUtils;
import com.example.utils.ProgressListener;
import com.example.wifi.SocketClient;
import com.example.wifi.SocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileTransferService extends Service implements ProgressListener {

    private static final String TAG = "FileTransferService";
    private static final String CHANNEL_ID = "WiFiDirectShare_Channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int CONTROL_PORT = 8889;

    private final IBinder binder = new LocalBinder();
    private final MutableLiveData<TransferState> transferState = new MutableLiveData<>(TransferState.idle());
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private SocketServer socketServer;
    private SocketClient socketClient;
    private ServerSocket controlServerSocket;
    private boolean isControlServerRunning = false;
    private Thread controlServerThread;

    private String connectedPeerIp = null;
    private final MutableLiveData<String> peerIpLiveData = new MutableLiveData<>(null);

    public class LocalBinder extends Binder {
        public FileTransferService getService() {
            return FileTransferService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Ready for offline transfer", 0));

        // Automatically start socket server to accept incoming transfers
        startListeningForFiles();
        // Automatically start control server to capture peer IP addresses
        startControlServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    public LiveData<TransferState> getTransferState() {
        return transferState;
    }

    public LiveData<String> getPeerIpLiveData() {
        return peerIpLiveData;
    }

    public void setConnectedPeerIp(String ip) {
        this.connectedPeerIp = ip;
        peerIpLiveData.postValue(ip);
        Log.d(TAG, "Connected peer IP set: " + ip);
    }

    public String getConnectedPeerIp() {
        return connectedPeerIp;
    }

    // ✅ Server operations - Fixed
    public void startListeningForFiles() {
        Log.d(TAG, "Starting file listening server...");

        if (socketServer != null) {
            socketServer.stop();
            socketServer = null;
        }

        // ✅ Wait for port to be released - 200ms वरून 500ms केले
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        socketServer = new SocketServer(getApplicationContext(), this);
        executorService.execute(socketServer);

        Log.d(TAG, "File listening server started");
    }

    // ✅ Control server to discover client IP address - Fixed
    private void startControlServer() {
        if (isControlServerRunning) {
            Log.d(TAG, "Control server already running");
            return;
        }

        // ✅ Stop existing control server if any
        stopControlServer();

        isControlServerRunning = true;
        Log.d(TAG, "Starting control server on port: " + CONTROL_PORT);

        controlServerThread = new Thread(() -> {
            try {
                controlServerSocket = new ServerSocket();
                controlServerSocket.setReuseAddress(true);
                controlServerSocket.bind(new InetSocketAddress(CONTROL_PORT));
                Log.d(TAG, "✅ Control server started successfully on port: " + CONTROL_PORT);

                while (isControlServerRunning) {
                    try {
                        Socket socket = controlServerSocket.accept();
                        String peerIp = socket.getInetAddress().getHostAddress();
                        Log.d(TAG, "📱 Control client connected from: " + peerIp);
                        setConnectedPeerIp(peerIp);
                        socket.close();
                    } catch (IOException e) {
                        if (!isControlServerRunning) {
                            break;
                        }
                        Log.e(TAG, "Control server accept error", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start control server", e);
                isControlServerRunning = false;
            } finally {
                try {
                    if (controlServerSocket != null && !controlServerSocket.isClosed()) {
                        controlServerSocket.close();
                        controlServerSocket = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error closing control server", e);
                }
            }
        });
        controlServerThread.start();
    }

    // ✅ नवीन method - Control server stop करण्यासाठी
    private void stopControlServer() {
        isControlServerRunning = false;
        try {
            if (controlServerSocket != null && !controlServerSocket.isClosed()) {
                controlServerSocket.close();
                controlServerSocket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping control server", e);
        }
        if (controlServerThread != null && controlServerThread.isAlive()) {
            controlServerThread.interrupt();
            controlServerThread = null;
        }
    }

    // ✅ Control client to notify Group Owner - Fixed
    public void sendHandshakeToGroupOwner() {
        Log.d(TAG, "Sending handshake to Group Owner...");

        executorService.execute(() -> {
            boolean connected = false;
            // Try several times as the server might be starting up
            for (int i = 0; i < 5; i++) {
                try {
                    Log.d(TAG, "Handshake attempt " + (i + 1) + " to " + NetworkUtils.GROUP_OWNER_IP + ":" + CONTROL_PORT);
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(NetworkUtils.GROUP_OWNER_IP, CONTROL_PORT), 2000);
                    socket.close();
                    connected = true;
                    Log.d(TAG, "✅ Handshake sent successfully to Group Owner");
                    break;
                } catch (Exception e) {
                    Log.d(TAG, "Handshake attempt " + (i + 1) + " failed: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (!connected) {
                Log.w(TAG, "⚠️ Handshake failed after 5 attempts");
            }
        });
    }

    // ✅ Client operations - Fixed
    public void sendFile(Uri fileUri, String receiverIp) {
        Log.d(TAG, "Sending file to: " + receiverIp);

        if (socketClient != null) {
            socketClient.cancel();
            socketClient = null;
        }

        socketClient = new SocketClient(getApplicationContext(), receiverIp, fileUri, this);
        executorService.execute(socketClient);
    }

    public void cancelTransfer() {
        Log.d(TAG, "Cancelling transfer...");

        if (socketClient != null) {
            socketClient.cancel();
            socketClient = null;
        }

        if (socketServer != null) {
            // Stop and restart server
            socketServer.stop();
            socketServer = null;
            startListeningForFiles();
        }

        transferState.postValue(new TransferState(
                TransferState.Status.CANCELLED,
                0, 0, 0, 0.0, 0,
                "Transfer cancelled",
                ""
        ));
        updateNotification("Transfer cancelled", 0);
    }

    // ✅ ProgressListener implementations
    @Override
    public void onTransferStarted(String fileName, long totalBytes, boolean isSending) {
        Log.d(TAG, "Transfer started: " + fileName + " (" + totalBytes + " bytes)");

        transferState.postValue(new TransferState(
                TransferState.Status.PREPARING,
                0, totalBytes, 0, 0.0, 0,
                isSending ? "Preparing to send..." : "Preparing to receive...",
                fileName
        ));
        updateNotification(isSending ? "Sending: " + fileName : "Receiving: " + fileName, 0);
    }

    @Override
    public void onTransferProgress(long bytesTransferred, long totalBytes, double speedMbps, long remainingSeconds) {
        int progressPercent = (int) ((bytesTransferred * 100) / totalBytes);
        String formattedSpeed = String.format(java.util.Locale.US, "%.1f MB/s", speedMbps);
        String msg = "Transferring (" + progressPercent + "%) • " + formattedSpeed;

        transferState.postValue(new TransferState(
                TransferState.Status.TRANSFERRING,
                bytesTransferred, totalBytes, progressPercent, speedMbps, remainingSeconds,
                msg,
                transferState.getValue() != null ? transferState.getValue().getFileName() : "File"
        ));
        updateNotification(msg, progressPercent);
    }

    @Override
    public void onTransferSuccess(String message, String filePath) {
        Log.d(TAG, "✅ Transfer successful: " + message);

        transferState.postValue(new TransferState(
                TransferState.Status.SUCCESS,
                0, 0, 100, 0.0, 0,
                message,
                filePath
        ));
        updateNotification("Transfer completed successfully", 100);

        // ✅ SUCCESS state नंतर IDLE state ला reset करा
        // जेणेकरून पुन्हा activity open केल्यावर dialog दिसणार नाही
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            transferState.postValue(TransferState.idle());
            Log.d(TAG, "Transfer state reset to IDLE after success");
        }, 1000); // 1 सेकंदानंतर reset

        // ✅ Restart Server automatically to accept subsequent transfers
        startListeningForFiles();
    }

    @Override
    public void onTransferFailed(String error) {
        Log.e(TAG, "❌ Transfer failed: " + error);

        transferState.postValue(new TransferState(
                TransferState.Status.FAILED,
                0, 0, 0, 0.0, 0,
                error,
                ""
        ));
        updateNotification("Transfer failed: " + error, 0);

        // ✅ Restart Server automatically to accept subsequent transfers
        startListeningForFiles();
    }

    // ✅ Notification methods
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WiFi Direct Share Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows file transfer progress");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text, int progress) {
        Intent notificationIntent = new Intent(this, TempWifiDirectActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi Direct Share")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_refresh)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false);
        } else if (progress >= 100) {
            builder.setProgress(0, 0, false);
            builder.setOngoing(false);
        }

        return builder.build();
    }

    private void updateNotification(String text, int progress) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text, progress));
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");

        // ✅ Stop control server - नवीन method call करा
        stopControlServer();

        // ✅ Stop socket server
        if (socketServer != null) {
            socketServer.stop();
            socketServer = null;
        }

        // ✅ Cancel socket client
        if (socketClient != null) {
            socketClient.cancel();
            socketClient = null;
        }

        // ✅ Shutdown executor service properly
        executorService.shutdownNow(); // shutdown() ऐवजी shutdownNow()
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                // Force shutdown
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        super.onDestroy();
    }
}