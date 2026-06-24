package com.example.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.service.AutoBackupManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Check for boot completed actions
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            AutoBackupManager autoBackupManager = new AutoBackupManager(context);

            // Re-schedule auto backup after device restart
            if (autoBackupManager.isAutoBackupEnabled()) {
                autoBackupManager.setAutoBackupEnabled(true);
            }
        }
    }
}