package com.example.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.R;
import com.example.backup.BackupManager;
import com.example.databinding.ActivitySettingsBinding;
import com.example.service.AutoBackupManager;

// ✅ Media3 Imports
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;

public class Settings_Activity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SwitchCompat switchTheme;
    private SwitchCompat switchAutoBackup;
    private AutoBackupManager autoBackupManager;

    // ✅ Media3 ExoPlayer instance
    private ExoPlayer exoPlayer;

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

        // Backup Now - With Professional Dialog
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

        // Reset Database - Only Owner
        String currentUserType = getSharedPreferences("UserSession", MODE_PRIVATE)
                .getString("userType", "");

        if (currentUserType.equals("Owner")) {
            binding.btnClearCache.setVisibility(View.VISIBLE);

            binding.btnClearCache.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
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
        } else {
            binding.btnClearCache.setVisibility(View.GONE);
        }

        // ✅ Info Button - Show Video Dialog
        binding.btnInfo.setOnClickListener(v -> {
            showVideoDialog();
        });
    }

    // ✅ Video Dialog Function with Media3 ExoPlayer
    private void showVideoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_video_player);
        dialog.setCancelable(false);

        // Set dialog width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        PlayerView playerView = dialog.findViewById(R.id.playerView);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        Button btnOk = dialog.findViewById(R.id.btnOk);

        // ✅ Create Media3 ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        // ✅ Play video from res/raw/milkflowtutorial.mp4
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.milkflowtutorial);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);

        // ✅ Close listener
        View.OnClickListener closeListener = v -> {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
            dialog.dismiss();
        };

        btnClose.setOnClickListener(closeListener);
        btnOk.setOnClickListener(closeListener);

        dialog.setOnDismissListener(d -> {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        });

        dialog.show();
    }

    //  Professional Backup Options Dialog
    private void showBackupOptionsDialog() {
        String[] options = {"📄 Normal Backup (.json)", "🔒 Encrypted Backup (.enc)"};

        new AlertDialog.Builder(this)
                .setTitle("Create Backup")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        performNormalBackup();
                    } else {
                        showProfessionalEncryptionDialog();
                    }
                })
                .show();
    }

    //  Professional Encryption Password Setup Dialog
    private void showProfessionalEncryptionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView fileInfo = new TextView(this);
        fileInfo.setText("📁 Encrypted Backup File");
        fileInfo.setTextSize(16);
        fileInfo.setTextColor(getResources().getColor(android.R.color.black));
        fileInfo.setTypeface(Typeface.DEFAULT_BOLD);
        fileInfo.setPadding(0, 0, 0, 10);
        layout.addView(fileInfo);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        divider.setPadding(0, 0, 0, 20);
        layout.addView(divider);

        TextView infoText = new TextView(this);
        infoText.setText("This file will be encrypted. Enter a password to protect your backup.");
        infoText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        infoText.setTextSize(14);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);

        LinearLayout passwordLayout = new LinearLayout(this);
        passwordLayout.setOrientation(LinearLayout.HORIZONTAL);
        passwordLayout.setGravity(Gravity.CENTER_VERTICAL);
        passwordLayout.setBackgroundResource(android.R.drawable.editbox_background);
        passwordLayout.setPadding(15, 5, 15, 5);

        TextView lockIcon = new TextView(this);
        lockIcon.setText("🔒");
        lockIcon.setTextSize(18);
        lockIcon.setPadding(0, 0, 10, 0);
        passwordLayout.addView(lockIcon);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setBackground(null);
        passwordInput.setPadding(0, 10, 0, 10);
        passwordInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        passwordLayout.addView(passwordInput);

        layout.addView(passwordLayout);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 15));
        layout.addView(spacer);

        LinearLayout confirmLayout = new LinearLayout(this);
        confirmLayout.setOrientation(LinearLayout.HORIZONTAL);
        confirmLayout.setGravity(Gravity.CENTER_VERTICAL);
        confirmLayout.setBackgroundResource(android.R.drawable.editbox_background);
        confirmLayout.setPadding(15, 5, 15, 5);

        TextView lockIcon2 = new TextView(this);
        lockIcon2.setText("🔒");
        lockIcon2.setTextSize(18);
        lockIcon2.setPadding(0, 0, 10, 0);
        confirmLayout.addView(lockIcon2);

        final EditText confirmInput = new EditText(this);
        confirmInput.setHint("Confirm password");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmInput.setBackground(null);
        confirmInput.setPadding(0, 10, 0, 10);
        confirmInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        confirmLayout.addView(confirmInput);

        layout.addView(confirmLayout);

        TextView strengthHint = new TextView(this);
        strengthHint.setText("💡 Use at least 4 characters for security");
        strengthHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
        strengthHint.setTextSize(12);
        strengthHint.setPadding(0, 15, 0, 0);
        layout.addView(strengthHint);

        new AlertDialog.Builder(this)
                .setTitle("🔐 Set Encryption Password")
                .setView(layout)
                .setPositiveButton("Create Encrypted Backup", (d, w) -> {
                    String password = passwordInput.getText().toString();
                    String confirm = confirmInput.getText().toString();

                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (password.length() < 4) {
                        Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
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
//            Toast.makeText(this, "dad", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        binding = null;
    }
}
