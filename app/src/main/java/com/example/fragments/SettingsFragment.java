package com.example.fragments;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.activities.BackupCenterActivity;
import com.example.activities.WifiDirectActivity;
import com.example.backup.BackupManager;
import com.example.backup.RestoreManager;
import com.example.databinding.FragmentSettingsBinding;
import com.example.service.AutoBackupManager;
import com.example.utils.PermissionManager;
import com.example.viewmodel.MilkViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MilkViewModel viewModel;
    private ActivityResultLauncher<String> restoreLauncher;
    private String currentUserType;
    private AutoBackupManager autoBackupManager;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(
                inflater,
                container,
                false);

        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        autoBackupManager = new AutoBackupManager(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        currentUserType = requireContext()
                .getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
                .getString("userType", "");

        if (!PermissionManager.canResetDatabase(currentUserType)) {
            binding.btnClearCache.setVisibility(View.GONE);
        } else if (currentUserType.equals("Customer")) {
            binding.btnClearCache.setVisibility(View.GONE);
            binding.btnBackupNow.setVisibility(View.GONE);
            binding.btnRestoreBackup.setVisibility(View.GONE);
            binding.btnShareBackup.setVisibility(View.GONE);
        }

        // ── Clear Database ─────────────────────────────────────────
        binding.btnClearCache.setOnClickListener(v -> {
            viewModel.getRepository().getExecutor().execute(() -> {
                com.example.database.AppDatabase.getInstance(requireContext()).clearAllTables();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Local database reset successfully!", Toast.LENGTH_LONG).show()
                    );
                }
            });
        });

        // ── Backup Now with Encryption ────────────────────────────
        binding.btnBackupNow.setOnClickListener(v -> {
            showEncryptionPasswordDialog();
        });

        // ── Backup Center ──────────────────────────────────────────
        binding.btnBackupCenter.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BackupCenterActivity.class);
            startActivity(intent);
        });

        // ── WiFi Direct ────────────────────────────────────────────
        binding.btnWifiDirect.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WifiDirectActivity.class);
            startActivity(intent);
        });

        // ── Setup Auto Backup ──────────────────────────────────────
        setupAutoBackup();

        // ── Restore Launcher ──────────────────────────────────────
        restoreLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri != null) {
                                // Check if file is encrypted
                                boolean isEncrypted = uri.toString().endsWith(".enc");

                                if (isEncrypted) {
                                    showRestorePasswordDialog(uri);
                                } else {
                                    performRestore(uri, null);
                                }
                            }
                        }
                );

        // ── Last Backup Display ─────────────────────────────────────
        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);
        String lastBackup = prefs.getString("last_backup_time", "Never");
        binding.txtLastBackup.setText("Last Backup: " + lastBackup);

        // ── Share Backup ────────────────────────────────────────────
        binding.btnShareBackup.setOnClickListener(v -> {
            shareBackupFile();
        });

        // ── Restore Backup ──────────────────────────────────────────
        binding.btnRestoreBackup.setOnClickListener(v -> {
            restoreLauncher.launch("*/*");
        });
    }

    // ── Setup Auto Backup ──────────────────────────────────────────
    private void setupAutoBackup() {
        SwitchCompat switchAutoBackup = binding.switchAutoBackup;


        boolean isEnabled = autoBackupManager.isAutoBackupEnabled();
        switchAutoBackup.setChecked(isEnabled);


        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoBackupManager.setAutoBackupEnabled(isChecked);
            if (isChecked) {
                Toast.makeText(getContext(), "✅ Auto backup enabled (every 24 hours)", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(getContext(), "❌ Auto backup disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Show Encryption Password Dialog ──────────────────────────
    private void showEncryptionPasswordDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 20);

        TextView passwordLabel = new TextView(getContext());
        passwordLabel.setText("🔐 Password");
        passwordLabel.setTextSize(14);
        passwordLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.on_surface));
        passwordLabel.setPadding(0, 0, 0, 8);
        layout.addView(passwordLabel);

        final EditText passwordInput = new EditText(getContext());
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setBackgroundResource(R.drawable.edit_text_background);
        passwordInput.setPadding(40, 16, 40, 16);
        passwordInput.setTextSize(14);
        layout.addView(passwordInput);

        View spacer1 = new View(getContext());
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        layout.addView(spacer1);

        TextView confirmLabel = new TextView(getContext());
        confirmLabel.setText("🔑 Confirm Password");
        confirmLabel.setTextSize(14);
        confirmLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.on_surface));
        confirmLabel.setPadding(0, 0, 0, 8);
        layout.addView(confirmLabel);

        final EditText confirmInput = new EditText(getContext());
        confirmInput.setHint("Confirm your password");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmInput.setBackgroundResource(R.drawable.edit_text_background);
        confirmInput.setPadding(40, 16, 40, 16);
        confirmInput.setTextSize(14);
        layout.addView(confirmInput);

        TextView hintText = new TextView(getContext());
        hintText.setText("💡 Use at least 6 characters for better security");
        hintText.setTextSize(12);
        hintText.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_dark));
        hintText.setPadding(0, 16, 0, 0);
        layout.addView(hintText);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🔐 Set Encryption Password")
                .setMessage("This password will be required to decrypt this backup")
                .setView(layout)
                .setPositiveButton("🔐 Create Encrypted Backup", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    String confirm = confirmInput.getText().toString();

                    if (password.isEmpty()) {
                        Toast.makeText(getContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (password.length() < 6) {
                        Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!password.equals(confirm)) {
                        Toast.makeText(getContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createEncryptedBackup(password);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ── Create encrypted backup ──────────────────────────────────────
    private void createEncryptedBackup(String password) {
        viewModel.getRepository().getExecutor().execute(() -> {
            boolean success = BackupManager.createEncryptedBackup(requireContext(), password);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "✅ Encrypted backup created!\nPassword: " + password, Toast.LENGTH_LONG).show();
                    saveLastBackupTime();
                    updateLastBackupDisplay();
                } else {
                    Toast.makeText(getContext(), "❌ Backup failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ── Show restore password dialog ─────────────────────────────
    private void showRestorePasswordDialog(Uri uri) {
        final EditText passwordInput = new EditText(getContext());
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🔓 Restore Encrypted Backup")
                .setMessage("Enter the password to decrypt and restore this backup")
                .setView(passwordInput)
                .setPositiveButton("Restore", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(getContext(), "Password required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performRestore(uri, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


   // ── Perform restore with or without password ──────────────────────────
    private void performRestore(Uri uri, String password) {
        AtomicBoolean success = new AtomicBoolean(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean result = false;

            try {
                if (password != null) {
                    //  ENCRYPTED FILE: Read from Uri and decrypt
                    result = BackupManager.restoreEncryptedBackupFromUri(requireContext(), uri, password);
                } else {
                    //  REGULAR JSON FILE: Restore directly
                    result = RestoreManager.restoreFromUri(requireContext(), uri);
                }
            } catch (Exception e) {
                Log.e("RESTORE", "Error: " + e.getMessage(), e);
                result = false;
            }

            success.set(result);

            requireActivity().runOnUiThread(() -> {
                if (success.get()) {
                    Toast.makeText(requireContext(), "✅ Restore Successful!", Toast.LENGTH_LONG).show();
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                } else {
                    Toast.makeText(requireContext(), "❌ Restore Failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ── Save last backup time ──────────────────────────────────────
    private void saveLastBackupTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        prefs.edit().putString("last_backup_time", currentTime).apply();
    }

    // ── Update last backup display ──────────────────────────────────
    private void updateLastBackupDisplay() {
        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);
        String lastBackup = prefs.getString("last_backup_time", null);

        if (lastBackup != null && !lastBackup.equals("Never") && !lastBackup.isEmpty()) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(lastBackup);

                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault());
                String formattedTime = outputFormat.format(date);

                binding.txtLastBackup.setText("Last Backup: " + formattedTime);
            } catch (Exception e) {
                binding.txtLastBackup.setText("Last Backup: " + lastBackup);
            }
        } else {
            binding.txtLastBackup.setText("Last Backup: Never");
        }
    }

    private void shareBackupFile() {
        File backupFolder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MilkDelivery"
        );

        if (!backupFolder.exists()) {
            Toast.makeText(getContext(), "Backup folder not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = backupFolder.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(getContext(), "No backup files found", Toast.LENGTH_SHORT).show();
            return;
        }

        File latestFile = files[0];
        for (File file : files) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }

        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                latestFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setClipData(ClipData.newRawUri(null, uri));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, "Share Backup");
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(chooser);
    }

    private void showRestoreDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restore Backup")
                .setMessage("Your current data may contain changes that are not saved in the backup file.\n\n" +
                        "Do you want to create a backup first?")
                .setPositiveButton("Backup & Restore", (dialog, which) -> {
                    viewModel.getRepository().getExecutor().execute(() -> {
                        boolean success = BackupManager.createBackup(requireContext());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(getContext(), "Backup Created", Toast.LENGTH_SHORT).show();
                                    startRestore();
                                } else {
                                    Toast.makeText(getContext(), "Backup Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNeutralButton("Restore Anyway", (dialog, which) -> {
                    startRestore();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startRestore() {
        viewModel.getRepository().getExecutor().execute(() -> {
            boolean success = RestoreManager.restoreBackup(requireContext());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), success ? "Restore Completed" : "Restore Failed", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}