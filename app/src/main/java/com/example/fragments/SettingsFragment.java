package com.example.fragments;

import android.content.ContentUris;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.activities.BackupCenterActivity;
import com.example.activities.WifiDirectActivity;
import com.example.backup.BackupManager;
import com.example.backup.RestoreManager;
import com.example.databinding.FragmentSettingsBinding;
import com.example.viewmodel.MilkViewModel;
import com.google.android.material.button.MaterialButton;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MilkViewModel viewModel;
    private ActivityResultLauncher<String> restoreLauncher;
    private ActivityResultLauncher<String> mergeBackupLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnClearCache.setOnClickListener(v -> {
            viewModel.getRepository().getExecutor().execute(() -> {
                // Clear the Room Database tables
                com.example.database.AppDatabase.getInstance(requireContext()).clearAllTables();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Local database reset successfully!", Toast.LENGTH_LONG).show());
                }
            });
        });

        binding.btnBackupNow.setOnClickListener(v -> {
            viewModel.getRepository()
                    .getExecutor()
                    .execute(() -> {
                        boolean success = BackupManager.createBackup(requireContext());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(
                                        getContext(),
                                        success ? "Backup Created" : "Backup Failed",
                                        Toast.LENGTH_LONG
                                ).show();

                                // ✅ UPDATE last backup time after successful backup
                                if (success) {
                                    saveLastBackupTime();
                                    updateLastBackupDisplay();
                                }
                            });
                        }
                    });
        });

        binding.btnBackupCenter.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BackupCenterActivity.class);
            startActivity(intent);
        });

        binding.btnWifiDirect.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WifiDirectActivity.class);
            startActivity(intent);
        });

        // ✅ UPDATE: Load and display last backup time
        updateLastBackupDisplay();

        restoreLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            boolean success = BackupManager.restoreBackup(requireContext(), uri);
                            requireActivity().runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(requireContext(), "Restore Successful", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(), "Restore Failed", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                    }
                });

        mergeBackupLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri == null) {
                                return;
                            }
                            Executors.newSingleThreadExecutor()
                                    .execute(() -> {
                                        boolean success = BackupManager.importAndMergeBackup(requireContext(), uri);
                                        requireActivity().runOnUiThread(() -> {
                                            if (success) {
                                                Toast.makeText(requireContext(), "Backup merged successfully", Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(requireContext(), "Merge failed", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    });
                        }
                );

        binding.btnShareBackup.setOnClickListener(v -> {
            shareBackupFile();
        });

        binding.btnRestoreBackup.setOnClickListener(v -> {
            mergeBackupLauncher.launch("application/json");
        });
    }

    // ── ✅ NEW: Save last backup time ──────────────────────────────
    private void saveLastBackupTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        prefs.edit().putString("last_backup_time", currentTime).apply();
    }

    // ── ✅ NEW: Update last backup display ──────────────────────────
    private void updateLastBackupDisplay() {
        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);
        String lastBackup = prefs.getString("last_backup_time", null);

        if (lastBackup != null && !lastBackup.equals("Never") && !lastBackup.isEmpty()) {
            try {
                // Parse the stored time
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(lastBackup);

                // Format for display
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault());
                String formattedTime = outputFormat.format(date);

                binding.txtLastBackup.setText("Last Backup: " + formattedTime);
            } catch (Exception e) {
                // If parsing fails, show as is
                binding.txtLastBackup.setText("Last Backup: " + lastBackup);
            }
        } else {
            binding.txtLastBackup.setText("Last Backup: Never");
        }
    }

    private void shareBackupFile() {
        File backupFolder = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                ),
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
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Backup"));
    }

    private void showRestoreDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restore Backup")
                .setMessage("Your current data may contain changes that are not saved in the backup file.\n\n" +
                        "Do you want to create a backup first?")
                .setPositiveButton("Backup & Restore", (dialog, which) -> {
                    viewModel.getRepository()
                            .getExecutor()
                            .execute(() -> {
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
                    Toast.makeText(
                            getContext(),
                            success ? "Restore Completed" : "Restore Failed",
                            Toast.LENGTH_LONG
                    ).show();
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