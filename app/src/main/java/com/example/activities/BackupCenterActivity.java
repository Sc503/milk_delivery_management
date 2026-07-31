package com.example.activities;



import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapters.BackupFileAdapter;
import com.example.backup.BackupFileManager;
import com.example.databinding.ActivityBackupCenterBinding;
import com.example.models.BackupFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackupCenterActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 3001;

    private ActivityBackupCenterBinding binding;
    private BackupFileAdapter myBackupsAdapter;
    private BackupFileAdapter receivedBackupsAdapter;

    private final List<BackupFile> myBackupFiles = new ArrayList<>();
    private final List<BackupFile> receivedBackupFiles = new ArrayList<>();

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        Intent resultIntent = new Intent();
                        resultIntent.setData(uri);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();
        checkPermissions();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Backup Center");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        myBackupsAdapter = new BackupFileAdapter(this, myBackupFiles, file -> {
            selectFileAndReturn(file);
        });
        binding.rvMyBackups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyBackups.setAdapter(myBackupsAdapter);

        receivedBackupsAdapter = new BackupFileAdapter(this, receivedBackupFiles, file -> {
            selectFileAndReturn(file);
        });
        binding.rvReceivedBackups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReceivedBackups.setAdapter(receivedBackupsAdapter);
    }

    private void setupClickListeners() {
        // ✅ FIXED: Open My Backups - Opens MilkFlow folder directly
        binding.btnOpenMyBackups.setOnClickListener(v -> {
            File myBackupFolder = BackupFileManager.getInstance(this).getMyBackupFolder();
            if (myBackupFolder != null && myBackupFolder.exists()) {
                openFolderInFileManager(myBackupFolder);
            } else {
                Toast.makeText(this, "MilkFlow folder not found", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ FIXED: Open Received Backups - Opens MilkFlow/ReceivedBackups folder
        binding.btnOpenReceivedBackups.setOnClickListener(v -> {
            File receivedFolder = BackupFileManager.getInstance(this).getReceivedBackupFolder();
            if (receivedFolder != null && receivedFolder.exists()) {
                openFolderInFileManager(receivedFolder);
            } else {
                Toast.makeText(this, "ReceivedBackups folder not found", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ FIXED: WiFi Direct button - Opens TempWifiDirectActivity
        binding.btnWifiDirect.setOnClickListener(v -> {
            Intent intent = new Intent(BackupCenterActivity.this, TempWifiDirectActivity.class);
            startActivity(intent);
        });
    }

    private void openFolderInFileManager(File folder) {
        if (folder != null && folder.exists()) {
            try {
                // ✅ Best method for Android 5.0+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(folder));
                    startActivity(intent);
                } else {
                    // For older Android
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(folder), "resource/folder");
                    startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // ✅ Fallback: Show Toast with path
                Toast.makeText(this, "📁 " + folder.getAbsolutePath(), Toast.LENGTH_LONG).show();

                // ✅ Also show in dialog with copy option
                showFolderPathDialog(folder);
            }
        } else {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ NEW METHOD: Show folder path in dialog with copy option
    private void showFolderPathDialog(File folder) {
        String folderPath = folder.getAbsolutePath();

        new AlertDialog.Builder(this)
                .setTitle("📁 Folder Location")
                .setMessage("Folder: " + folderPath + "\n\n" +
                        "You can manually navigate to this folder using any file manager app.\n\n" +
                        "Tap 'Copy Path' to copy the folder location to clipboard.")
                .setPositiveButton("Copy Path", (dialog, which) -> {
                    // Copy path to clipboard
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip =
                            android.content.ClipData.newPlainText("folder_path", folderPath);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "📋 Path copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("OK", null)
                .show();
    }

    private void selectFileAndReturn(File file) {
        if (file != null && file.exists()) {
            try {
                // ✅ Method 1: Try with FileProvider
                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );

                Intent resultIntent = new Intent();
                resultIntent.setData(uri);
                resultIntent.putExtra("file_path", file.getAbsolutePath());
                resultIntent.putExtra("file_name", file.getName());
                resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                setResult(RESULT_OK, resultIntent);
                finish();

            } catch (Exception e) {
                Log.e("BackupCenter", "Error with FileProvider", e);

                // ✅ Method 2: Fallback to Uri.fromFile()
                try {
                    Uri uri = Uri.fromFile(file);
                    Intent resultIntent = new Intent();
                    resultIntent.setData(uri);
                    resultIntent.putExtra("file_path", file.getAbsolutePath());
                    resultIntent.putExtra("file_name", file.getName());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception ex) {
                    Log.e("BackupCenter", "Error selecting file", ex);
                    Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }
    }
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
                return;
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } else {
                loadBackups();
            }
        } else {
            loadBackups();
        }
    }

    private void requestManageStoragePermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Storage Permission Required")
                .setMessage("This app needs access to storage to manage backup files.")
                .setPositiveButton("Grant", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show())
                .show();
    }

    private void loadBackups() {
        BackupFileManager manager = BackupFileManager.getInstance(this);

        // ✅ My Backups
        myBackupFiles.clear();
        List<File> myBackups = manager.getMyBackupFiles();
        if (myBackups != null) {
            for (File file : myBackups) {
                if (file != null && file.exists()) {
                    myBackupFiles.add(new BackupFile(file, BackupFile.Type.MY_BACKUP));
                }
            }
        }
        if (myBackupsAdapter != null) {
            myBackupsAdapter.notifyDataSetChanged();
        }
        updateMyBackupsVisibility();

        // ✅ Received Backups
        receivedBackupFiles.clear();
        List<File> receivedBackups = manager.getReceivedBackupFiles();
        if (receivedBackups != null) {
            for (File file : receivedBackups) {
                if (file != null && file.exists()) {
                    receivedBackupFiles.add(new BackupFile(file, BackupFile.Type.RECEIVED));
                }
            }
        }
        if (receivedBackupsAdapter != null) {
            receivedBackupsAdapter.notifyDataSetChanged();
        }
        updateReceivedBackupsVisibility();
    }
    private void updateMyBackupsVisibility() {
        if (myBackupFiles.isEmpty()) {
            binding.tvMyBackupsEmpty.setVisibility(View.VISIBLE);
            binding.rvMyBackups.setVisibility(View.GONE);
        } else {
            binding.tvMyBackupsEmpty.setVisibility(View.GONE);
            binding.rvMyBackups.setVisibility(View.VISIBLE);
        }
    }

    private void updateReceivedBackupsVisibility() {
        if (receivedBackupFiles.isEmpty()) {
            binding.tvReceivedBackupsEmpty.setVisibility(View.VISIBLE);
            binding.rvReceivedBackups.setVisibility(View.GONE);
        } else {
            binding.tvReceivedBackupsEmpty.setVisibility(View.GONE);
            binding.rvReceivedBackups.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadBackups();
                Log.d("BackupCenter", "📋 Refreshed backup lists");
            }
        } else {
            loadBackups();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadBackups();
            } else {
                Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
}






