package com.example.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.service.AutoBackupManager;

public class AutoBackupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AutoBackupManager autoBackupManager = new AutoBackupManager(context);

        // Check if auto backup is enabled
        if (autoBackupManager.isAutoBackupEnabled()) {
            // Perform auto backup
            autoBackupManager.performAutoBackup();

            // Reschedule for next backup (24 hours later)
            autoBackupManager.setAutoBackupEnabled(true);
        }
    }
}