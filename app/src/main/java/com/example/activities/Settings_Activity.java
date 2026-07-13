package com.example.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.R;
import com.example.backup.BackupManager;
import com.example.databinding.ActivitySettingsBinding;
import com.example.service.AutoBackupManager;

public class Settings_Activity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SwitchCompat switchTheme;
    private SwitchCompat switchAutoBackup;
    private AutoBackupManager autoBackupManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        autoBackupManager = new AutoBackupManager(this);

        // ============================================================
        // 1. THEME SWITCH
        // ============================================================
        switchTheme = binding.switchTheme;
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);
        switchTheme.setChecked(isDarkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences("ThemePrefs", MODE_PRIVATE).edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate();
        });

        // ============================================================
        // 2. AUTO BACKUP SWITCH
        // ============================================================
        switchAutoBackup = binding.switchAutoBackup;
        boolean isAutoBackupEnabled = autoBackupManager.isAutoBackupEnabled();
        switchAutoBackup.setChecked(isAutoBackupEnabled);

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoBackupManager.setAutoBackupEnabled(isChecked);
            if (isChecked) {
                Toast.makeText(this, "✅ Auto Backup Enabled (Every 24 hours)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Auto Backup Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // ============================================================
        // 3. LAST BACKUP TIME
        // ============================================================
        updateLastBackupTime();

        // ============================================================
        // 4. BACKUP NOW
        // ============================================================
        binding.btnBackupNow.setOnClickListener(v -> {
            Toast.makeText(this, "Creating backup...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean success = BackupManager.createBackup(this);
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "✅ Backup created successfully!", Toast.LENGTH_LONG).show();
                        updateLastBackupTime();
                    } else {
                        Toast.makeText(this, "❌ Backup failed!", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        // ============================================================
        // 5. SHARE BACKUP
        // ============================================================
        binding.btnShareBackup.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBackupsActivity.class);
            startActivity(intent);
        });

        // ============================================================
        // 6. RESTORE BACKUP
        // ============================================================
        binding.btnRestoreBackup.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBackupsActivity.class);
            startActivity(intent);
        });

        // ============================================================
        // 7. BACKUP CENTER
        // ============================================================
        binding.btnBackupCenter.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackupCenterActivity.class);
            startActivity(intent);
        });

        // ============================================================
        // 8. WiFi DIRECT
        // ============================================================
        binding.btnWifiDirect.setOnClickListener(v -> {
            Intent intent = new Intent(this, WifiDirectActivity.class);
            startActivity(intent);
        });

        // ============================================================
        // 9. RESET DATABASE
        // ============================================================
        binding.btnClearCache.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Reset Local Database")
                    .setMessage("This will delete ALL local data. This action cannot be undone. Continue?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                com.example.database.AppDatabase db =
                                        com.example.database.AppDatabase.getInstance(this);
                                db.customerDao().deleteAll();
                                db.deliveryDao().deleteAll();
                                db.staffDao().deleteAll();


                                runOnUiThread(() -> {
                                    Toast.makeText(this, "✅ Database reset successfully!", Toast.LENGTH_LONG).show();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "❌ Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void updateLastBackupTime() {
        String lastBackup = autoBackupManager.getLastBackupTime();
        binding.txtLastBackup.setText("Last Backup: " + lastBackup);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}