package in.esmartsolution.milkflow.models;

import java.io.File;

public class BackupFile1 {
    private File file;
    private String type; // "My Backups" or "Received"

    // Constructor with just File (for My Backups)
    public BackupFile1(File file) {
        this.file = file;
        this.type = "My Backups";
    }

    // Constructor with File and type (for Received Backups)
    public BackupFile1(File file, String type) {
        this.file = file;
        this.type = type;
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}