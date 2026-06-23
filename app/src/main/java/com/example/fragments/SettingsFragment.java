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

import com.example.activities.LoginActivity;
import com.example.R;
import com.example.backup.BackupManager;
import com.example.backup.RestoreManager;
import com.example.databinding.FragmentSettingsBinding;
import com.example.utils.PermissionManager;
import com.example.viewmodel.MilkViewModel;

import android.content.Intent;
import android.net.Uri;
import android.content.ClipData;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MilkViewModel viewModel;
    private ActivityResultLauncher<String>
            restoreLauncher;

    private String currentUserType;

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

        viewModel =
                new ViewModelProvider(requireActivity())
                        .get(MilkViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        currentUserType =
                requireContext()
                        .getSharedPreferences(
                                "UserSession",
                                android.content.Context.MODE_PRIVATE
                        )
                        .getString(
                                "userType",
                                ""
                        );

        if (!PermissionManager.canResetDatabase(currentUserType)) {

            binding.btnClearCache.setVisibility(
                    View.GONE
            );

        }

        else if (currentUserType.equals("Customer")) {

            // Customer ला settings screen उघडताच काही दिसू नये
            binding.btnClearCache.setVisibility(View.GONE);

            binding.btnBackupNow.setVisibility(View.GONE);

            binding.btnRestoreBackup.setVisibility(View.GONE);

            binding.btnShareBackup.setVisibility(View.GONE);



        }


        // Clear Database
        binding.btnClearCache.setOnClickListener(v -> {

            viewModel.getRepository()
                    .getExecutor()
                    .execute(() -> {

                        com.example.database.AppDatabase
                                .getInstance(requireContext())
                                .clearAllTables();

                        if (getActivity() != null) {

                            getActivity().runOnUiThread(() ->

                                    Toast.makeText(
                                            getContext(),
                                            "Local database reset successfully!",
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                        }
                    });

        });


        binding.btnBackupNow.setOnClickListener(v -> {

            viewModel.getRepository()
                    .getExecutor()
                    .execute(() -> {

                        boolean success =
                                BackupManager.createBackup(
                                        requireContext()
                                );

                        if (getActivity() != null) {

                            getActivity().runOnUiThread(() -> {

                                Toast.makeText(
                                        getContext(),
                                        success
                                                ? "Backup Created"
                                                : "Backup Failed",
                                        Toast.LENGTH_LONG
                                ).show();

                            });
                        }
                    });
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE);

        String lastBackup =
                prefs.getString(
                        "last_backup_time",
                        "Never"
                );


        restoreLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            if (uri != null) {

                                Executors.newSingleThreadExecutor()
                                        .execute(() -> {

                                            boolean success =
                                                    BackupManager.restoreBackup(
                                                            requireContext(),
                                                            uri
                                                    );

                                            requireActivity()
                                                    .runOnUiThread(() -> {

                                                        if (success) {

                                                            Toast.makeText(
                                                                    requireContext(),
                                                                    "Restore Successful",
                                                                    Toast.LENGTH_LONG
                                                            ).show();

                                                        } else {

                                                            Toast.makeText(
                                                                    requireContext(),
                                                                    "Restore Failed",
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                        }
                                                    });
                                        });
                            }
                        }
                );

        binding.txtLastBackup.setText(
                "Last Backup: " + lastBackup
        );

        binding.btnShareBackup.setOnClickListener(v -> {
            shareBackupFile();
        });

        binding.btnRestoreBackup.setOnClickListener(v -> {

            restoreLauncher.launch("*/*");

        });
    }

    private void shareBackupFile() {

        File backupFolder =
                new File(
                        android.os.Environment
                                .getExternalStoragePublicDirectory(
                                        android.os.Environment.DIRECTORY_DOWNLOADS
                                ),
                        "MilkDelivery"
                );

        if (!backupFolder.exists()) {

            Toast.makeText(
                    getContext(),
                    "Backup folder not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File[] files = backupFolder.listFiles();

        if (files == null || files.length == 0) {

            Toast.makeText(
                    getContext(),
                    "No backup files found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File latestFile = files[0];

        for (File file : files) {

            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }

        Uri uri =
                FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".provider",
                        latestFile
                );

        Intent shareIntent =
                new Intent(Intent.ACTION_SEND);

        shareIntent.setType("application/json");

        shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
        );

        // Grant permission via ClipData
        shareIntent.setClipData(ClipData.newRawUri(null, uri));

        shareIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        Intent chooser = Intent.createChooser(
                shareIntent,
                "Share Backup"
        );
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(chooser);
    }
    private void showRestoreDialog() {

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restore Backup")
                .setMessage(
                        "Your current data may contain changes that are not saved in the backup file.\n\n" +
                                "Do you want to create a backup first?"
                )

                .setPositiveButton(
                        "Backup & Restore",
                        (dialog, which) -> {

                            viewModel.getRepository()
                                    .getExecutor()
                                    .execute(() -> {

                                        boolean success =
                                                BackupManager.createBackup(
                                                        requireContext()
                                                );

                                        if (getActivity() != null) {

                                            getActivity().runOnUiThread(() -> {

                                                if (success) {

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Backup Created",
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    startRestore();

                                                } else {

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Backup Failed",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                            });
                                        }
                                    });
                        })

                .setNeutralButton(
                        "Restore Anyway",
                        (dialog, which) -> {
                            startRestore();
                        })

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }

    private void startRestore() {

        viewModel.getRepository()
                .getExecutor()
                .execute(() -> {

                    boolean success =
                            RestoreManager.restoreBackup(
                                    requireContext()
                            );

                    if (getActivity() != null) {

                        getActivity().runOnUiThread(() -> {

                            Toast.makeText(
                                    getContext(),
                                    success
                                            ? "Restore Completed"
                                            : "Restore Failed",
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