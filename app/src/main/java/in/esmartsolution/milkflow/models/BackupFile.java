package in.esmartsolution.milkflow.models;



import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupFile {
    public enum Type {
        MY_BACKUP,
        RECEIVED
    }

    private final File file;
    private final Type type;

    public BackupFile(File file, Type type) {
        this.file = file;
        this.type = type;
    }

    public File getFile() {
        return file;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return file.getName();
    }

    public long getSize() {
        return file.length();
    }

    public String getFormattedSize() {
        long size = file.length();
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(file.lastModified()));
    }

    public String getExtension() {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < name.length() - 1) {
            return name.substring(dotIndex + 1).toUpperCase();
        }
        return "FILE";
    }
}