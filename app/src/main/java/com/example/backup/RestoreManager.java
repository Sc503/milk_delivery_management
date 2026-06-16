package com.example.backup;

import android.content.Context;
import android.os.Environment;

import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;

public class RestoreManager {

     public static boolean restoreBackup(Context context) {

        try {

            File backupFolder =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                            ),
                            "MilkDelivery"
                    );

            if (!backupFolder.exists()) {
                return false;
            }

            File[] files = backupFolder.listFiles();

            if (files == null || files.length == 0) {
                return false;
            }

            File latestFile = files[0];

            for (File file : files) {

                if (file.lastModified() > latestFile.lastModified()) {
                    latestFile = file;
                }
            }

            Gson gson = new Gson();

            FileReader reader =
                    new FileReader(latestFile);

            BackupData backupData =
                    gson.fromJson(
                            reader,
                            BackupData.class
                    );

            reader.close();

            AppDatabase db =
                    AppDatabase.getInstance(context);

            db.clearAllTables();

            if (backupData.getCustomers() != null) {

                db.customerDao()
                        .insertAll(
                                backupData.getCustomers()
                        );
            }

            if (backupData.getDeliveries() != null) {

                db.deliveryDao()
                        .insertAll(
                                backupData.getDeliveries()
                        );
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}