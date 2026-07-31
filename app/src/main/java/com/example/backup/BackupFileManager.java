package com.example.backup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BackupFileManager {
    private static final String TAG = "BackupFileManager";
    private static BackupFileManager instance;

    // ✅ CORRECT: Same as BackupManager
    private static final String BACKUP_FOLDER_NAME = "MilkDelivery";
    private static final String RECEIVED_FOLDER_NAME = "received";

    private final File myBackupFolder;
    private final File receivedBackupFolder;

    private BackupFileManager(Context context) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // ✅ My Backups folder: /Downloads/MilkDelivery/
        myBackupFolder = new File(downloadsDir, BACKUP_FOLDER_NAME);
        if (!myBackupFolder.exists()) {
            boolean created = myBackupFolder.mkdirs();
            if (created) {
                Log.d(TAG, "Created MilkDelivery folder: " + myBackupFolder.getAbsolutePath());
            }
        }

        // ✅ Received folder: /Downloads/MilkDelivery/Received/
        receivedBackupFolder = new File(myBackupFolder, RECEIVED_FOLDER_NAME);
        if (!receivedBackupFolder.exists()) {
            boolean created = receivedBackupFolder.mkdirs();
            if (created) {
                Log.d(TAG, "Created Received folder: " + receivedBackupFolder.getAbsolutePath());
            }
        }

        Log.d(TAG, "📁 My Backups folder: " + myBackupFolder.getAbsolutePath());
        Log.d(TAG, "📁 Received folder: " + receivedBackupFolder.getAbsolutePath());
    }

    public static synchronized BackupFileManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackupFileManager(context);
        }
        return instance;
    }

    public File getMyBackupFolder() {
        return myBackupFolder;
    }

    public File getReceivedBackupFolder() {
        return receivedBackupFolder;
    }

    public List<File> getMyBackupFiles() {
        List<File> files = new ArrayList<>();
        if (myBackupFolder != null && myBackupFolder.exists()) {
            File[] fileList = myBackupFolder.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile() && !file.getName().startsWith(".")) {
                        files.add(file);
                    }
                }
            }
        }
        // ✅ Sort newest first
        files.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return files;
    }

    public List<File> getReceivedBackupFiles() {
        List<File> files = new ArrayList<>();
        if (receivedBackupFolder != null && receivedBackupFolder.exists()) {
            File[] fileList = receivedBackupFolder.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile() && !file.getName().startsWith(".")) {
                        files.add(file);
                    }
                }
            }
        }
        // ✅ Sort newest first
        files.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return files;
    }

    public void saveReceivedFile(InputStream inputStream, String fileName) throws IOException {
        if (!receivedBackupFolder.exists()) {
            receivedBackupFolder.mkdirs();
        }

        File destFile = new File(receivedBackupFolder, fileName);

        // ✅ Handle duplicate file names
        int count = 1;
        String name = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        while (destFile.exists()) {
            destFile = new File(receivedBackupFolder, name + "_" + count + extension);
            count++;
        }

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        }

        Log.d(TAG, "✅ File saved: " + destFile.getAbsolutePath());
    }

    public File getUniqueFileForReceived(String fileName) {
        if (!receivedBackupFolder.exists()) {
            receivedBackupFolder.mkdirs();
        }

        File destFile = new File(receivedBackupFolder, fileName);
        int count = 1;
        String name = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        while (destFile.exists()) {
            destFile = new File(receivedBackupFolder, name + "_" + count + extension);
            count++;
        }
        return destFile;
    }

    // ✅ Helper to delete a backup file
    public boolean deleteBackupFile(String fileName) {
        File file = new File(myBackupFolder, fileName);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    // ✅ Helper to delete a received backup file
    public boolean deleteReceivedFile(String fileName) {
        File file = new File(receivedBackupFolder, fileName);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}