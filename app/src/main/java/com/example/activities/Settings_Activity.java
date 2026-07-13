package com.example.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        // Theme Switch
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

        // Auto Backup Switch
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

        // Last Backup Time
        updateLastBackupTime();

        // Backup Now - Original Dialog
        binding.btnBackupNow.setOnClickListener(v -> {
            showBackupOptionsDialog();
        });

        // Share Backup - Open MyBackupsActivity
        binding.btnShareBackup.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBackupsActivity.class);
            startActivity(intent);
        });

        // Restore Backup - Open MyBackupsActivity
        binding.btnRestoreBackup.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBackupsActivity.class);
            startActivity(intent);
        });

        // Backup Center
        binding.btnBackupCenter.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackupCenterActivity.class);
            startActivity(intent);
        });

        // WiFi Direct
        binding.btnWifiDirect.setOnClickListener(v -> {
            Intent intent = new Intent(this, WifiDirectActivity.class);
            startActivity(intent);
        });

        // Reset Database
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

    // ✅ Original Backup Options Dialog
    private void showBackupOptionsDialog() {
        String[] options = {"📄 Normal Backup (.json)", "🔒 Encrypted Backup (.enc)"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Create Backup")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        performNormalBackup();
                    } else {
                        showEncryptionPasswordDialog();
                    }
                })
                .show();
    }

    // ✅ Original Password Dialog
    private void showEncryptionPasswordDialog() {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final EditText confirmInput = new EditText(this);
        confirmInput.setHint("Confirm password");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 30);
        layout.addView(passwordInput);
        layout.addView(confirmInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔒 Encrypted Backup")
                .setMessage("Enter a password to encrypt your backup file")
                .setView(layout)
                .setPositiveButton("Create Encrypted Backup", (d, w) -> {
                    String password = passwordInput.getText().toString();
                    String confirm = confirmInput.getText().toString();

                    if (password.isEmpty() || confirm.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!password.equals(confirm)) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performEncryptedBackup(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performNormalBackup() {
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
    }

    private void performEncryptedBackup(String password) {
        Toast.makeText(this, "🔒 Creating encrypted backup...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean success = BackupManager.createEncryptedBackup(this, password);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "✅ Encrypted backup created successfully!", Toast.LENGTH_LONG).show();
                    updateLastBackupTime();
                } else {
                    Toast.makeText(this, "❌ Encrypted backup failed!", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
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