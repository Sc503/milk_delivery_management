package in.esmartsolution.milkflow.models;

import android.net.Uri;

public class FileModel {
    private final String name;
    private final Uri uri;
    private final long size;
    private final String extension;
    private final String mimeType;

    public FileModel(String name, Uri uri, long size, String extension, String mimeType) {
        this.name = name;
        this.uri = uri;
        this.size = size;
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public long getSize() {
        return size;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFormattedSize() {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
