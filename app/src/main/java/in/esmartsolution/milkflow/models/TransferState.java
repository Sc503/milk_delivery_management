package in.esmartsolution.milkflow.models;

public class TransferState {
    public enum Status {
        IDLE,
        PREPARING,
        TRANSFERRING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private final Status status;
    private final long bytesTransferred;
    private final long totalBytes;
    private final int progressPercent;
    private final double speedMbps; // Speed in MB/s
    private final long remainingTimeSeconds;
    private final String message;
    private final String fileName;

    public TransferState(Status status, long bytesTransferred, long totalBytes, int progressPercent, double speedMbps, long remainingTimeSeconds, String message, String fileName) {
        this.status = status;
        this.bytesTransferred = bytesTransferred;
        this.totalBytes = totalBytes;
        this.progressPercent = progressPercent;
        this.speedMbps = speedMbps;
        this.remainingTimeSeconds = remainingTimeSeconds;
        this.message = message;
        this.fileName = fileName;
    }

    // TransferState.java
    public static TransferState idle() {
        return new TransferState(Status.IDLE, 0, 0, 0, 0.0, 0, "Idle", "");
    }

    public Status getStatus() {
        return status;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public double getSpeedMbps() {
        return speedMbps;
    }

    public long getRemainingTimeSeconds() {
        return remainingTimeSeconds;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFormattedSpeed() {
        if (speedMbps < 1) {
            return String.format(java.util.Locale.US, "%.1f KB/s", speedMbps * 1024);
        }
        return String.format(java.util.Locale.US, "%.2f MB/s", speedMbps);
    }

    public String getFormattedRemainingTime() {
        if (remainingTimeSeconds <= 0) return "00:00:00";
        long h = remainingTimeSeconds / 3600;
        long m = (remainingTimeSeconds % 3600) / 60;
        long s = remainingTimeSeconds % 60;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    public String getFormattedTransferredSize() {
        return getFormattedSize(bytesTransferred) + " / " + getFormattedSize(totalBytes);
    }

    private String getFormattedSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
