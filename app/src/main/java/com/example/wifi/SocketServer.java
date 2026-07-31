package com.example.wifi;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.example.backup.BackupFileManager;
import com.example.utils.NetworkUtils;
import com.example.utils.ProgressListener;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer implements Runnable {
    private static final String TAG = "SocketServer";
    private final Context context;
    private final ProgressListener listener;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private volatile boolean isRunning = true;
    private volatile boolean isStopped = false;

    public SocketServer(Context context, ProgressListener listener) {
        this.context = context.getApplicationContext(); // ✅ Application context
        this.listener = listener;
    }

    /**
     * Stop the server and release all resources
     */
    public void stop() {
        isRunning = false;
        isStopped = true;
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                clientSocket = null;
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            Log.d(TAG, "Server stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server", e);
        }
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning && !isStopped;
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(NetworkUtils.PORT));
            Log.d(TAG, "✅ Server started successfully on port: " + NetworkUtils.PORT);

            while (isRunning && !isStopped && !Thread.currentThread().isInterrupted()) {
                try {
                    clientSocket = serverSocket.accept();
                    Log.d(TAG, "📱 Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    handleIncomingConnection(clientSocket);
                } catch (Exception e) {
                    if (!isRunning || isStopped || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    Log.e(TAG, "Connection error", e);
                    if (listener != null) {
                        listener.onTransferFailed("Connection error: " + e.getLocalizedMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (!isStopped) {
                Log.e(TAG, "❌ Failed to start server", e);
                if (listener != null) {
                    listener.onTransferFailed("Failed to start server: " + e.getLocalizedMessage());
                }
            }
        } finally {
            cleanup(); // ✅ cleanup() method call करा
        }
    }

    // ✅ नवीन cleanup() method add करा
    private void cleanup() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing server socket", e);
        }
    }

    private void handleIncomingConnection(Socket socket) {
        InputStream is = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;

        try {
            is = socket.getInputStream();
            dis = new DataInputStream(new BufferedInputStream(is));

            // 1. Read metadata
            String fileName = dis.readUTF();
            long totalBytes = dis.readLong();
            String mimeType = dis.readUTF(); // mimeType is received but not used
            Log.d(TAG, "📥 Receiving file: " + fileName + " (" + totalBytes + " bytes)");

            if (listener != null) {
                listener.onTransferStarted(fileName, totalBytes, false);
            }

            // Save to Received Backups folder using BackupFileManager
            // Save to Received Backups folder using BackupFileManager
            BackupFileManager backupManager = BackupFileManager.getInstance(context);
            File destFile = backupManager.getUniqueFileForReceived(fileName);

// ✅ ADD: जर फाइल अजूनही exists असेल तर force delete करा
            if (destFile.exists()) {
                Log.w(TAG, "⚠️ File still exists, deleting: " + destFile.getName());
                boolean deleted = destFile.delete();
                if (deleted) {
                    Log.d(TAG, "✅ Old file deleted");
                    // नवीन FileOutputStream साठी नवीन File object
                    destFile = new File(destFile.getParent(), fileName);
                }
            }

            fos = new FileOutputStream(destFile);

            // 2. Stream file bytes
            byte[] buffer = new byte[8192];
            int len;
            long bytesReceived = 0;

            long startTime = SystemClock.elapsedRealtime();
            long lastUpdateTime = startTime;
            long lastBytesReceived = 0;

            while (bytesReceived < totalBytes && (len = dis.read(buffer, 0, (int) Math.min(buffer.length, totalBytes - bytesReceived))) != -1) {
                if (!isRunning || isStopped) {
                    if (listener != null) {
                        listener.onTransferFailed("Transfer stopped by user.");
                    }
                    return;
                }

                fos.write(buffer, 0, len);
                bytesReceived += len;

                // Update progress periodically (every 500ms)
                long now = SystemClock.elapsedRealtime();
                if (now - lastUpdateTime >= 500 || bytesReceived == totalBytes) {
                    double intervalTimeSec = (now - lastUpdateTime) / 1000.0;

                    // Speed in MB/s
                    double speedMbps = 0;
                    if (intervalTimeSec > 0) {
                        long intervalBytes = bytesReceived - lastBytesReceived;
                        speedMbps = (intervalBytes / 1024.0 / 1024.0) / intervalTimeSec;
                    }

                    long remainingBytes = totalBytes - bytesReceived;
                    long remainingSeconds = 0;
                    if (speedMbps > 0) {
                        remainingSeconds = (long) ((remainingBytes / 1024.0 / 1024.0) / speedMbps);
                    }

                    if (listener != null) {
                        listener.onTransferProgress(bytesReceived, totalBytes, speedMbps, remainingSeconds);
                    }

                    lastUpdateTime = now;
                    lastBytesReceived = bytesReceived;
                }
            }

            fos.flush();
            Log.d(TAG, "✅ File received successfully: " + destFile.getName());
            if (listener != null) {
                listener.onTransferSuccess("File saved to Received folder: " + destFile.getName(), destFile.getAbsolutePath());
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error receiving file", e);
            if (listener != null && isRunning && !isStopped) {
                listener.onTransferFailed("Error receiving file: " + e.getLocalizedMessage());
            }
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (dis != null) {
                    dis.close();
                }
                if (is != null) {
                    is.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error closing resources", ex);
            }
        }
    }
}