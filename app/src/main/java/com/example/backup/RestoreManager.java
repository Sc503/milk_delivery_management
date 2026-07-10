package com.example.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class RestoreManager {

    private static final String TAG = "RestoreManager";

    // ── RESTORE FROM LATEST FILE (JSON ONLY) ──────────────────────
    public static boolean restoreBackup(Context context) {
        try {
            File backupFolder = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                    ),
                    "MilkDelivery"
            );

            if (!backupFolder.exists()) {
                Log.e(TAG, "Backup folder not found");
                return false;
            }

            File[] files = backupFolder.listFiles();

            if (files == null || files.length == 0) {
                Log.e(TAG, "No backup files found");
                return false;
            }

            //  Find latest file (only .json, not .enc)
            File latestFile = null;
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    if (latestFile == null || file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }
            }

            if (latestFile == null) {
                Log.e(TAG, "No JSON backup file found");
                return false;
            }

            Log.d(TAG, "Restoring from: " + latestFile.getName());

            Gson gson = new Gson();
            FileReader reader = new FileReader(latestFile);
            BackupData backupData = gson.fromJson(reader, BackupData.class);
            reader.close();

            AppDatabase db = AppDatabase.getInstance(context);
            db.clearAllTables();

            if (backupData.getCustomers() != null) {
                db.customerDao().insertAll(backupData.getCustomers());
            }

            if (backupData.getDeliveries() != null) {
                db.deliveryDao().insertAll(backupData.getDeliveries());
            }

            Log.d(TAG, " Restore successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            return false;
        }
    }

    // ── RESTORE FROM URI (FOR JSON FILES) ──────────────────────────────
    public static boolean restoreFromUri(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream");
                return false;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();

            String jsonData = builder.toString();

            Gson gson = new Gson();
            BackupData backupData = gson.fromJson(jsonData, BackupData.class);

            AppDatabase db = AppDatabase.getInstance(context);
            db.runInTransaction(() -> {
                db.deliveryDao().deleteAll();
                db.customerDao().deleteAll();
                if (backupData.getCustomers() != null) {
                    db.customerDao().insertAll(backupData.getCustomers());
                }
                if (backupData.getDeliveries() != null) {
                    db.deliveryDao().insertAll(backupData.getDeliveries());
                }
            });

            Log.d(TAG, " Restore from URI successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore from URI failed: " + e.getMessage(), e);
            return false;
        }
    }
    // ── Helper: Get file name from Uri ──────────────────────────────
    private static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;
        try {
            if (uri.getScheme().equals("content")) {
                android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }
            } else {
                File file = new File(uri.getPath());
                fileName = file.getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name: " + e.getMessage());
        }
        return fileName;
    }
}