package com.example.models;

import java.io.File;

public class BackupFile {
    private File file;
    private String type; // "My Backups" or "Received"

    // Constructor with just File (for My Backups)
    public BackupFile(File file) {
        this.file = file;
        this.type = "My Backups";
    }

    // Constructor with File and type (for Received Backups)
    public BackupFile(File file, String type) {
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