package com.example.utils;

public interface ProgressListener {
    void onTransferStarted(String fileName, long totalBytes, boolean isSending);
    void onTransferProgress(long bytesTransferred, long totalBytes, double speedMbps, long remainingSeconds);
    void onTransferSuccess(String message, String filePath);
    void onTransferFailed(String error);
}
