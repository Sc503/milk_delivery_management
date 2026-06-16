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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupManager {

    private static final String TAG = "BACKUP_TEST";

    public static boolean createBackup(Context context) {

        try {

            AppDatabase db = AppDatabase.getInstance(context);

            List<Customer> customers =
                    db.customerDao().getAllCustomersForBackup();

            List<Delivery> deliveries =
                    db.deliveryDao().getAllDeliveriesForBackup();

            BackupData backupData =
                    new BackupData(customers, deliveries);

            Gson gson =
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create();

            String json = gson.toJson(backupData);

            String time = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
            ).format(new Date());

            String fileName =
                    "milk_backup_" + time + ".json";

            ContentResolver resolver =
                    context.getContentResolver();

            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    fileName
            );

            values.put(
                    MediaStore.Downloads.MIME_TYPE,
                    "application/json"
            );

            values.put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/MilkDelivery"
            );

            Uri uri =
                    null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );
            }

            if (uri == null) {
                return false;
            }

            OutputStream out =
                    resolver.openOutputStream(uri);

            if (out == null) {
                return false;
            }

            out.write(json.getBytes());
            out.flush();
            out.close();

            return true;

        } catch (Exception e) {

            Log.e(TAG, "BACKUP ERROR", e);

            return false;
        }
    }

    public static boolean restoreBackup(
            Context context,
            Uri uri
    ) {

        try {

            ContentResolver resolver =
                    context.getContentResolver();

            InputStream inputStream =
                    resolver.openInputStream(uri);

            if (inputStream == null) {
                return false;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(inputStream)
                    );

            StringBuilder builder =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            reader.close();

            Gson gson = new Gson();

            BackupData backupData =
                    gson.fromJson(
                            builder.toString(),
                            BackupData.class
                    );

            AppDatabase db =
                    AppDatabase.getInstance(context);

            db.runInTransaction(() -> {

                db.deliveryDao().deleteAll();
                db.customerDao().deleteAll();

                if (backupData.getCustomers() != null) {

                    db.customerDao().insertAll(
                            backupData.getCustomers()
                    );
                }

                if (backupData.getDeliveries() != null) {

                    db.deliveryDao().insertAll(
                            backupData.getDeliveries()
                    );
                }
            });

            return true;

        } catch (Exception e) {

            Log.e(
                    "RESTORE",
                    "Restore failed",
                    e
            );

            return false;
        }
    }
}