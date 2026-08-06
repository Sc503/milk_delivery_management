package in.esmartsolution.milkflow.wifi;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import in.esmartsolution.milkflow.models.FileModel;
import in.esmartsolution.milkflow.utils.FileUtils;
import in.esmartsolution.milkflow.utils.NetworkUtils;
import in.esmartsolution.milkflow.utils.ProgressListener;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient implements Runnable {
    private final Context context;
    private final String receiverIp;
    private final Uri fileUri;
    private final ProgressListener listener;
    private volatile boolean isCancelled = false;

    public SocketClient(Context context, String receiverIp, Uri fileUri, ProgressListener listener) {
        this.context = context.getApplicationContext();
        this.receiverIp = receiverIp;
        this.fileUri = fileUri;
        this.listener = listener;
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        InputStream is = null;
        OutputStream os = null;
        DataOutputStream dos = null;

        try {
            // Retrieve file details
            FileModel fileModel = FileUtils.getFileModelFromUri(context, fileUri);
            if (fileModel == null) {
                if (listener != null) listener.onTransferFailed("File details could not be retrieved.");
                return;
            }

            if (listener != null) {
                listener.onTransferStarted(fileModel.getName(), fileModel.getSize(), true);
            }

            // Connect to receiver
            socket.bind(null);
            socket.connect(new InetSocketAddress(receiverIp, NetworkUtils.PORT), 10000);

            os = socket.getOutputStream();
            dos = new DataOutputStream(new BufferedOutputStream(os));
            is = context.getContentResolver().openInputStream(fileUri);

            if (is == null) {
                if (listener != null) listener.onTransferFailed("Failed to open file input stream.");
                return;
            }

            // 1. Send metadata
            dos.writeUTF(fileModel.getName());
            dos.writeLong(fileModel.getSize());
            dos.writeUTF(fileModel.getMimeType() != null ? fileModel.getMimeType() : "application/octet-stream");
            dos.flush();

            // 2. Stream file bytes
            byte[] buffer = new byte[8192];
            int len;
            long totalBytes = fileModel.getSize();
            long bytesSent = 0;

            long startTime = SystemClock.elapsedRealtime();
            long lastUpdateTime = startTime;
            long lastBytesSent = 0;

            while ((len = is.read(buffer)) != -1) {
                if (isCancelled) {
                    if (listener != null) listener.onTransferFailed("Transfer cancelled by user.");
                    return;
                }

                dos.write(buffer, 0, len);
                bytesSent += len;

                // Update progress periodically (every 500ms)
                long now = SystemClock.elapsedRealtime();
                if (now - lastUpdateTime >= 500 || bytesSent == totalBytes) {
                    double timeElapsedSec = (now - startTime) / 1000.0;
                    double intervalTimeSec = (now - lastUpdateTime) / 1000.0;

                    // Speed in MB/s
                    double speedMbps = 0;
                    if (intervalTimeSec > 0) {
                        long intervalBytes = bytesSent - lastBytesSent;
                        speedMbps = (intervalBytes / 1024.0 / 1024.0) / intervalTimeSec;
                    }

                    long remainingBytes = totalBytes - bytesSent;
                    long remainingSeconds = 0;
                    if (speedMbps > 0) {
                        remainingSeconds = (long) ((remainingBytes / 1024.0 / 1024.0) / speedMbps);
                    }

                    int progressPercent = (int) ((bytesSent * 100) / totalBytes);

                    if (listener != null) {
                        listener.onTransferProgress(bytesSent, totalBytes, speedMbps, remainingSeconds);
                    }

                    lastUpdateTime = now;
                    lastBytesSent = bytesSent;
                }
            }

            dos.flush();
            if (listener != null) {
                listener.onTransferSuccess("File sent successfully!", fileModel.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null && !isCancelled) {
                listener.onTransferFailed("Transfer error: " + e.getLocalizedMessage());
            }
        } finally {
            try {
                if (is != null) is.close();
                if (dos != null) dos.close();
                if (os != null) os.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
