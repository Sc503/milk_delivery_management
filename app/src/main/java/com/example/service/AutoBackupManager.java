package com.example.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import com.example.backup.BackupManager;
import com.example.Receivers.AutoBackupReceiver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AutoBackupManager {

    private static final String PREFS_NAME = "auto_backup_prefs";
    private static final String KEY_ENABLED = "auto_backup_enabled";
    private static final String KEY_LAST_BACKUP = "auto_backup_last_time";
    private static final String KEY_BACKUP_RUNNING = "backup_running";

    // ============================================================
    // 🧪 TESTING: 2 minutes for testing
    // ============================================================
//    private static final int BACKUP_INTERVAL = 2 * 60 * 1000; // 2 minutes

    // ============================================================
    // ✅ PRODUCTION: 24 HOURS (FIXED)
    // ============================================================
    private static final int BACKUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

    private Context context;

    public AutoBackupManager(Context context) {
        this.context = context;
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Check if Auto Backup is Enabled ──────────────────────────
    public boolean isAutoBackupEnabled() {
        return getPrefs().getBoolean(KEY_ENABLED, false);
    }

    // ── Enable/Disable Auto Backup ──────────────────────────────
    public void setAutoBackupEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_ENABLED, enabled).apply();

        if (enabled) {
            scheduleAutoBackup();
            performAutoBackup(); // Run immediately when enabled
        } else {
            cancelAutoBackup();
        }
    }

    // ── Get Last Backup Time ─────────────────────────────────────
    public String getLastBackupTime() {
        long lastBackup = getPrefs().getLong(KEY_LAST_BACKUP, 0);
        if (lastBackup == 0) {
            return "Never";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        return sdf.format(new Date(lastBackup));
    }

    // ── Get Last Backup Time (Raw Timestamp) ─────────────────────
    public long getLastBackupTimeMillis() {
        return getPrefs().getLong(KEY_LAST_BACKUP, 0);
    }

    // ── ✅ FIXED: Update Last Backup Time ──────────────────────────
    private void updateLastBackupTime() {
        // ✅ Update auto backup preference
        getPrefs().edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply();

        // ✅ Also update the main backup preference for consistency
        SharedPreferences mainPrefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE);
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        mainPrefs.edit().putString("last_backup_time", currentTime).apply();
    }

    // ── Schedule Auto Backup ─────────────────────────────────────
    private void scheduleAutoBackup() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AutoBackupReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel existing alarm
        alarmManager.cancel(pendingIntent);


        long triggerTime = System.currentTimeMillis() + BACKUP_INTERVAL;

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }

    // ── Cancel Auto Backup ──────────────────────────────────────
    private void cancelAutoBackup() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AutoBackupReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    // ── ✅ FIXED: Perform Auto Backup ──────────────────────────────
    public void performAutoBackup() {
        // Prevent multiple backups running simultaneously
        if (isBackupRunning()) {
            return;
        }

        setBackupRunning(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // ✅ Create backup (creates .json file)
                boolean success = BackupManager.createBackup(context);

                if (success) {
                    // ✅ Update last backup time
                    updateLastBackupTime();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                setBackupRunning(false);
            }
        });
    }

    // ── Prevent Multiple Backups ─────────────────────────────────
    private boolean isBackupRunning() {
        return getPrefs().getBoolean(KEY_BACKUP_RUNNING, false);
    }

    private void setBackupRunning(boolean running) {
        getPrefs().edit().putBoolean(KEY_BACKUP_RUNNING, running).apply();
    }

    // ── Get Total Backup Files Count ────────────────────────────
    public int getBackupFileCount() {
        File backupDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MilkDelivery"
        );

        if (!backupDir.exists()) {
            return 0;
        }

        File[] files = backupDir.listFiles();
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                count++;
            }
        }
        return count;
    }
}