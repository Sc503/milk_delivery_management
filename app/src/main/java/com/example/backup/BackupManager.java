package com.example.backup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.ByteArrayOutputStream;

public class BackupManager {

    private static final String TAG = "BACKUP_TEST";

    // ── CREATE REGULAR BACKUP (JSON) ─────────────────────────────
    public static boolean createBackup(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Customer> customers = db.customerDao().getAllCustomersForBackup();
            List<Delivery> deliveries = db.deliveryDao().getAllDeliveriesForBackup();
            BackupData backupData = new BackupData(customers, deliveries);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(backupData);

            String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "milk_backup_" + time + ".json";

            return saveBackupFile(context, json, fileName);

        } catch (Exception e) {
            Log.e(TAG, "BACKUP ERROR", e);
            return false;
        }
    }

    // ── CREATE ENCRYPTED BACKUP ──────────────────────────────────
    public static boolean createEncryptedBackup(Context context, String password) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Customer> customers = db.customerDao().getAllCustomersForBackup();
            List<Delivery> deliveries = db.deliveryDao().getAllDeliveriesForBackup();
            BackupData backupData = new BackupData(customers, deliveries);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(backupData);

            EncryptionHelper encryption = new EncryptionHelper(context);
            byte[] encryptedData = encryption.encrypt(json.getBytes("UTF-8"), password);

            String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "milk_backup_" + time + ".enc";

            return saveEncryptedFile(context, encryptedData, fileName);

        } catch (Exception e) {
            Log.e(TAG, "ENCRYPTED BACKUP ERROR", e);
            return false;
        }
    }

    // ── SAVE REGULAR JSON FILE ────────────────────────────────────
    private static boolean saveBackupFile(Context context, String json, String fileName) {
        try {
            // For Android 10 and above, use MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MilkDelivery");

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;

                OutputStream out = resolver.openOutputStream(uri);
                if (out == null) return false;

                out.write(json.getBytes());
                out.flush();
                out.close();
                return true;
            } else {
                // For older Android versions, use traditional file system
                File dir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "MilkDelivery"
                );
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(json.getBytes());
                fos.flush();
                fos.close();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Save backup error", e);
            return false;
        }
    }

    // ── SAVE ENCRYPTED FILE ──────────────────────────────────────
    private static boolean saveEncryptedFile(Context context, byte[] encryptedData, String fileName) {
        try {
            // For Android 10 and above, use MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MilkDelivery");

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;

                OutputStream out = resolver.openOutputStream(uri);
                if (out == null) return false;

                out.write(encryptedData);
                out.flush();
                out.close();
                return true;
            } else {
                // For older Android versions, use traditional file system
                File dir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "MilkDelivery"
                );
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(encryptedData);
                fos.flush();
                fos.close();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Save encrypted backup error", e);
            return false;
        }
    }

    // ── CHECK IF FILE IS ENCRYPTED ──────────────────────────────
    public static boolean isEncryptedFile(File file) {
        return file.getName().endsWith(".enc");
    }

    // ── DECRYPT AND OPEN ENCRYPTED FILE ──────────────────────────
    public static String decryptEncryptedFile(Context context, File file, String password) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] encryptedData = new byte[(int) file.length()];
        fis.read(encryptedData);
        fis.close();

        EncryptionHelper encryption = new EncryptionHelper(context);
        byte[] decryptedData = encryption.decrypt(encryptedData, password);

        return new String(decryptedData, "UTF-8");
    }

    // ── RESTORE ENCRYPTED BACKUP ──────────────────────────────────
    public static boolean restoreEncryptedBackup(Context context, File file, String password) {
        try {
            String jsonData = decryptEncryptedFile(context, file, password);

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

            return true;

        } catch (Exception e) {
            Log.e(TAG, "RESTORE ENCRYPTED ERROR", e);
            return false;
        }
    }

    // ── RESTORE REGULAR BACKUP ────────────────────────────────────
    public static boolean restoreBackup(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            return restoreBackup(context, inputStream);
        } catch (Exception e) {
            Log.e("RESTORE", "Restore failed", e);
            return false;
        }
    }

    public static boolean restoreBackup(Context context, InputStream inputStream) {
        try {
            if (inputStream == null) return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            reader.close();

            Gson gson = new Gson();
            BackupData backupData = gson.fromJson(builder.toString(), BackupData.class);

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

            return true;

        } catch (Exception e) {
            Log.e("RESTORE", "Restore failed", e);
            return false;
        }
    }

    // ── IMPORT AND MERGE BACKUP ──────────────────────────────────
    public static boolean importAndMergeBackup(Context context, Uri fileUri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri)) {
            return importAndMergeBackup(context, inputStream);
        } catch (Exception e) {
            Log.e("IMPORT", "Import failed", e);
            return false;
        }
    }

    public static boolean importAndMergeBackup(Context context, InputStream inputStream) {
        try {
            if (inputStream == null) return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            reader.close();

            Gson gson = new Gson();
            BackupData backupData = gson.fromJson(builder.toString(), BackupData.class);

            AppDatabase db = AppDatabase.getInstance(context);
            Map<Long, Long> customerIdMap = new HashMap<>();

            db.runInTransaction(() -> {
                if (backupData.getCustomers() != null) {
                    for (Customer backupCustomer : backupData.getCustomers()) {
                        Customer existingCustomer = db.customerDao().getCustomerByMobile(backupCustomer.getMobile());
                        long localCustomerId;
                        if (existingCustomer == null) {
                            long oldBackupId = backupCustomer.getId();
                            backupCustomer.setId(0);
                            localCustomerId = db.customerDao().insert(backupCustomer);
                            customerIdMap.put(oldBackupId, localCustomerId);
                        } else {
                            customerIdMap.put(backupCustomer.getId(), existingCustomer.getId());
                        }
                    }
                }

                if (backupData.getDeliveries() != null) {
                    for (Delivery backupDelivery : backupData.getDeliveries()) {
                        Long localCustomerId = customerIdMap.get(backupDelivery.getCustomerId());
                        if (localCustomerId == null) continue;

                        Delivery existingDelivery = db.deliveryDao().findDelivery(localCustomerId, backupDelivery.getDeliveryDate());

                        if (existingDelivery == null) {
                            backupDelivery.setId(0);
                            backupDelivery.setCustomerId(localCustomerId);
                            db.deliveryDao().insert(backupDelivery);
                        } else {
                            if ("Delivered".equalsIgnoreCase(backupDelivery.getStatus()) &&
                                    !"Delivered".equalsIgnoreCase(existingDelivery.getStatus())) {
                                existingDelivery.setStatus("Delivered");
                                existingDelivery.setDeliveredTime(backupDelivery.getDeliveredTime());
                                db.deliveryDao().update(existingDelivery);
                            }
                        }
                    }
                }
            });

            return true;

        } catch (Exception e) {
            Log.e("MERGE_BACKUP", "Merge failed", e);
            return false;
        }
    }

    // ── RESTORE ENCRYPTED BACKUP FROM URI ──────────────────────────────
    public static boolean restoreEncryptedBackupFromUri(Context context, Uri uri, String password) {
        try {
            // 1. Read encrypted data from Uri
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream");
                return false;
            }

            byte[] encryptedData = readAllBytes(inputStream);
            inputStream.close();

            // 2. Decrypt the data
            EncryptionHelper encryption = new EncryptionHelper(context);
            byte[] decryptedData = encryption.decrypt(encryptedData, password);
            String jsonData = new String(decryptedData, "UTF-8");

            // 3. Parse JSON and restore to database
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

            Log.d(TAG, " Encrypted restore successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore encrypted from URI failed: " + e.getMessage(), e);
            return false;
        }
    }

    // ── MERGE ENCRYPTED BACKUP ──────────────────────────────────────


    public static boolean mergeEncryptedBackup(Context context, File file, String password) {
        try {
            // Decrypt the file
            String jsonData = decryptEncryptedFile(context, file, password);

            // Parse JSON
            Gson gson = new Gson();
            BackupData backupData = gson.fromJson(jsonData, BackupData.class);

            // Merge into database
            AppDatabase db = AppDatabase.getInstance(context);
            Map<Long, Long> customerIdMap = new HashMap<>();

            db.runInTransaction(() -> {
                // MERGE CUSTOMERS
                if (backupData.getCustomers() != null) {
                    for (Customer backupCustomer : backupData.getCustomers()) {
                        Customer existingCustomer = db.customerDao().getCustomerByMobile(backupCustomer.getMobile());

                        if (existingCustomer == null) {
                            long oldBackupId = backupCustomer.getId();

                            backupCustomer.setId(0);
                            long newId = db.customerDao().insert(backupCustomer);
                            customerIdMap.put(oldBackupId, newId);
                        } else {
                            customerIdMap.put(backupCustomer.getId(), existingCustomer.getId());
                        }
                    }
                }

                // MERGE DELIVERIES
                if (backupData.getDeliveries() != null) {
                    for (Delivery backupDelivery : backupData.getDeliveries()) {
                        Long localCustomerId = customerIdMap.get(backupDelivery.getCustomerId());
                        if (localCustomerId == null) continue;

                        Delivery existingDelivery = db.deliveryDao().findDelivery(
                                localCustomerId,
                                backupDelivery.getDeliveryDate()
                        );

                        if (existingDelivery == null) {
                            backupDelivery.setId(0);
                            backupDelivery.setCustomerId(localCustomerId);
                            db.deliveryDao().insert(backupDelivery);
                        } else {
                            if ("Delivered".equalsIgnoreCase(backupDelivery.getStatus()) &&
                                    !"Delivered".equalsIgnoreCase(existingDelivery.getStatus())) {
                                existingDelivery.setStatus("Delivered");
                                existingDelivery.setDeliveredTime(backupDelivery.getDeliveredTime());
                                db.deliveryDao().update(existingDelivery);
                            }
                        }
                    }
                }
            });

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Merge encrypted failed: " + e.getMessage(), e);
            return false;
        }
    }

    //  MERGE BACKUP FROM URI ──────────────────────────────────────────
    public static boolean mergeBackupFromUri(Context context, Uri uri) {
        try {
            // Read the file content
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

            // Parse JSON
            Gson gson = new Gson();
            BackupData backupData = gson.fromJson(jsonData, BackupData.class);

            // Merge into database
            AppDatabase db = AppDatabase.getInstance(context);
            Map<Long, Long> customerIdMap = new HashMap<>();

            db.runInTransaction(() -> {
                // ── MERGE CUSTOMERS ──
                if (backupData.getCustomers() != null) {
                    for (Customer backupCustomer : backupData.getCustomers()) {
                        Customer existingCustomer = db.customerDao().getCustomerByMobile(backupCustomer.getMobile());

                        if (existingCustomer == null) {
                            long oldBackupId = backupCustomer.getId();
                            backupCustomer.setId(0);
                            long newId = db.customerDao().insert(backupCustomer);
                            customerIdMap.put(oldBackupId, newId);
                        } else {
                            customerIdMap.put(backupCustomer.getId(), existingCustomer.getId());
                        }
                    }
                }

                // ── MERGE DELIVERIES ──
                if (backupData.getDeliveries() != null) {
                    for (Delivery backupDelivery : backupData.getDeliveries()) {
                        Long localCustomerId = customerIdMap.get(backupDelivery.getCustomerId());
                        if (localCustomerId == null) continue;

                        Delivery existingDelivery = db.deliveryDao().findDelivery(
                                localCustomerId,
                                backupDelivery.getDeliveryDate()
                        );

                        if (existingDelivery == null) {
                            backupDelivery.setId(0);
                            backupDelivery.setCustomerId(localCustomerId);
                            db.deliveryDao().insert(backupDelivery);
                        } else {
                            if ("Delivered".equalsIgnoreCase(backupDelivery.getStatus()) &&
                                    !"Delivered".equalsIgnoreCase(existingDelivery.getStatus())) {
                                existingDelivery.setStatus("Delivered");
                                existingDelivery.setDeliveredTime(backupDelivery.getDeliveredTime());
                                db.deliveryDao().update(existingDelivery);
                            }
                        }
                    }
                }
            });

            Log.d(TAG, " Merge successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Merge from URI failed: " + e.getMessage(), e);
            return false;
        }
    }

    //  MERGE BACKUP FROM FILE (DIRECT) ──────────────────────────────
    public static boolean mergeBackupFromFile(Context context, File file) {
        try {
            Log.d(TAG, "========== MERGE FROM FILE ==========");
            Log.d(TAG, "File path: " + file.getAbsolutePath());
            Log.d(TAG, "File exists: " + file.exists());
            Log.d(TAG, "File size: " + file.length() + " bytes");

            if (!file.exists()) {
                Log.e(TAG, " File does not exist");
                return false;
            }

            if (file.length() == 0) {
                Log.e(TAG, " File is empty");
                return false;
            }

            // Read file content directly using FileReader
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            fileReader.close();

            String jsonData = builder.toString();
            Log.d(TAG, " JSON data length: " + jsonData.length() + " characters");

            if (jsonData.isEmpty()) {
                Log.e(TAG, " JSON data is empty");
                return false;
            }

            // Parse JSON
            Gson gson = new Gson();
            BackupData backupData;
            try {
                backupData = gson.fromJson(jsonData, BackupData.class);
                Log.d(TAG, " JSON parsed successfully");
            } catch (Exception e) {
                Log.e(TAG, " JSON parse error: " + e.getMessage());
                return false;
            }

            // Check if data exists
            int customerCount = backupData.getCustomers() != null ? backupData.getCustomers().size() : 0;
            int deliveryCount = backupData.getDeliveries() != null ? backupData.getDeliveries().size() : 0;
            Log.d(TAG, " Found " + customerCount + " customers and " + deliveryCount + " deliveries");

            if (customerCount == 0 && deliveryCount == 0) {
                Log.e(TAG, " No data found in backup file");
                return false;
            }

            // Merge into database
            AppDatabase db = AppDatabase.getInstance(context);
            Map<Long, Long> customerIdMap = new HashMap<>();

            db.runInTransaction(() -> {
                // MERGE CUSTOMERS
                if (backupData.getCustomers() != null) {
                    for (Customer backupCustomer : backupData.getCustomers()) {
                        Customer existingCustomer = db.customerDao().getCustomerByMobile(backupCustomer.getMobile());

                        if (existingCustomer == null) {
                            long oldBackupId = backupCustomer.getId();
                            backupCustomer.setId(0);
                            long newId = db.customerDao().insert(backupCustomer);
                            customerIdMap.put(oldBackupId, newId);
                            Log.d(TAG, " Added customer: " + backupCustomer.getName());
                        } else {
                            customerIdMap.put(backupCustomer.getId(), existingCustomer.getId());
                            Log.d(TAG, "⏭ Skipped duplicate: " + backupCustomer.getName());
                        }
                    }
                }

                // MERGE DELIVERIES
                if (backupData.getDeliveries() != null) {
                    for (Delivery backupDelivery : backupData.getDeliveries()) {
                        Long localCustomerId = customerIdMap.get(backupDelivery.getCustomerId());
                        if (localCustomerId == null) continue;

                        Delivery existingDelivery = db.deliveryDao().findDelivery(
                                localCustomerId,
                                backupDelivery.getDeliveryDate()
                        );

                        if (existingDelivery == null) {
                            backupDelivery.setId(0);
                            backupDelivery.setCustomerId(localCustomerId);
                            db.deliveryDao().insert(backupDelivery);
                            Log.d(TAG, " Added delivery");
                        } else {
                            if ("Delivered".equalsIgnoreCase(backupDelivery.getStatus()) &&
                                    !"Delivered".equalsIgnoreCase(existingDelivery.getStatus())) {
                                existingDelivery.setStatus("Delivered");
                                existingDelivery.setDeliveredTime(backupDelivery.getDeliveredTime());
                                db.deliveryDao().update(existingDelivery);
                                Log.d(TAG, " Updated delivery to Delivered");
                            }
                        }
                    }
                }
            });

            Log.d(TAG, " Merge successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, " Merge from file failed: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
    }

    //  RESTORE BACKUP FROM FILE ──────────────────────────────────────
    public static boolean restoreBackupFromFile(Context context, File file) {
        try {
            Log.d(TAG, "========== RESTORE FROM FILE ==========");
            Log.d(TAG, "File path: " + file.getAbsolutePath());

            if (!file.exists()) {
                Log.e(TAG, " File does not exist");
                return false;
            }

            // Read file content directly
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            fileReader.close();

            String jsonData = builder.toString();

            // Parse JSON
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

            Log.d(TAG, " Restore successful!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, " Restore from file failed: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
    }



    // ── Helper: Read all bytes from InputStream ──────────────────────────
    private static byte[] readAllBytes(InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }
}