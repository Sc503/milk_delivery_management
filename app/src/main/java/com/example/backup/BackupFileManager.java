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

    // ✅ CHANGED: Use same folder name as BackupManager
    private static final String BACKUP_FOLDER_NAME = "MilkDelivery";
    private static final String RECEIVED_FOLDER_NAME = "Received";

    private final File myBackupFolder;
    private final File receivedBackupFolder;

    private BackupFileManager(Context context) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // ✅ CHANGED: Now uses "MilkDelivery" instead of "MilkFlow"
        myBackupFolder = new File(downloadsDir, BACKUP_FOLDER_NAME);
        if (!myBackupFolder.exists()) {
            boolean created = myBackupFolder.mkdirs();
            if (created) {
                Log.d(TAG, "Created MilkDelivery folder: " + myBackupFolder.getAbsolutePath());
            }
        }

        // ✅ CHANGED: Received folder now inside MilkDelivery
        receivedBackupFolder = new File(myBackupFolder, RECEIVED_FOLDER_NAME);
        if (!receivedBackupFolder.exists()) {
            boolean created = receivedBackupFolder.mkdirs();
            if (created) {
                Log.d(TAG, "Created Received folder: " + receivedBackupFolder.getAbsolutePath());
            }
        }
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
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }
        }
        return files;
    }

    public List<File> getReceivedBackupFiles() {
        List<File> files = new ArrayList<>();
        if (receivedBackupFolder != null && receivedBackupFolder.exists()) {
            File[] fileList = receivedBackupFolder.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }
        }
        return files;
    }

    public void saveReceivedFile(InputStream inputStream, String fileName) throws IOException {
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

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        }
    }

    public File getUniqueFileForReceived(String fileName) {
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
}