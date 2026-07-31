package com.example.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.BackupAdapter1;
import com.example.backup.BackupManager;
import com.example.models.BackupFile1;
import com.example.services.WifiDirectService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MyBackupsActivity extends AppCompatActivity {

    private TextView txtFileCount;
    private View emptyState;
    private RecyclerView rv;
    private BackupAdapter1 adapter;
    private List<BackupFile1> backupFileList = new ArrayList<>();

    private WifiDirectService wifiService;
    private boolean isServiceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            wifiService = ((WifiDirectService.LocalBinder) service).getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_backups);

        Intent intent = new Intent(this, WifiDirectService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        rv = findViewById(R.id.recyclerBackups);
        txtFileCount = findViewById(R.id.txtFileCount);
        emptyState = findViewById(R.id.emptyState);

        rv.setLayoutManager(new LinearLayoutManager(this));

        refreshFileList();

        adapter = new BackupAdapter1(backupFileList, new BackupAdapter1.OnBackupClickListener() {
            @Override
            public void onClick(BackupFile1 f) {
                showFileOptionsDialog(f.getFile());
            }

            @Override
            public void onLongClick(BackupFile1 f) {
                showOptions(f.getFile());
            }
        });
        rv.setAdapter(adapter);
    }

    private void refreshFileList() {
        backupFileList.clear();
        backupFileList.addAll(loadFiles());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUI();
    }

    private void updateUI() {
        if (txtFileCount != null) {
            txtFileCount.setText(String.valueOf(backupFileList.size()));
        }

        if (emptyState != null) {
            if (backupFileList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showFileOptionsDialog(File file) {
        String[] options;

        if (BackupManager.isEncryptedFile(file)) {
            options = new String[]{"🔓 Decrypt & Open", "📤 Share", "📶 Send via WiFi Direct", "♻️ Restore", "🗑️ Delete"};
        } else {
            options = new String[]{"📄 Open", "📤 Share", "📶 Send via WiFi Direct", "♻️ Restore", "🗑️ Delete"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(options, (d, i) -> {
                    if (BackupManager.isEncryptedFile(file)) {
                        switch (i) {
                            case 0: showDecryptDialog(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: restoreBackup(file); break;
                            case 4: confirmDelete(file); break;
                        }
                    } else {
                        switch (i) {
                            case 0: openFile(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: restoreBackup(file); break;
                            case 4: confirmDelete(file); break;
                        }
                    }
                })
                .show();
    }

    private void showDecryptDialog(File file) {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔓 Decrypt Backup")
                .setMessage("Enter the password to decrypt: " + file.getName())
                .setView(passwordInput)
                .setPositiveButton("Decrypt", (d, w) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    decryptAndOpenFile(file, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void decryptAndOpenFile(File file, String password) {
        try {
            String jsonContent = BackupManager.decryptEncryptedFile(this, file, password);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("📄 " + file.getName().replace(".enc", ".json"))
                    .setMessage(jsonContent)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Share", (d, w) -> shareFile(file))
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Wrong password or corrupted file!", Toast.LENGTH_LONG).show();
        }
    }

    private List<BackupFile1> loadFiles() {
        List<BackupFile1> list = new ArrayList<>();

        File base = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "MilkDelivery"
        );

        if (!base.exists()) {
            base.mkdirs();
        }

        File[] ownFiles = base.listFiles(file ->
                file.isFile() &&
                        (file.getName().endsWith(".json") || file.getName().endsWith(".enc")) &&
                        !file.getParentFile().getName().equals("received")
        );

        if (ownFiles != null) {
            java.util.Arrays.sort(ownFiles, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified())
            );

            for (File f : ownFiles) {
                String type = "";
                if (f.getName().endsWith(".enc")) {
                    type = "🔒 ";
                }
                list.add(new BackupFile1(f, type));
            }
        }

        return list;
    }

    private void showOptions(File file) {
        String[] opts;

        if (BackupManager.isEncryptedFile(file)) {
            opts = new String[]{"🔓 Decrypt & Open", "📤 Share", "📶 Send via WiFi Direct", "♻️ Restore", "🗑️ Delete"};
        } else {
            opts = new String[]{"📄 Open", "📤 Share", "📶 Send via WiFi Direct", "♻️ Restore", "🗑️ Delete"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(opts, (d, i) -> {
                    if (BackupManager.isEncryptedFile(file)) {
                        switch (i) {
                            case 0: showDecryptDialog(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: restoreBackup(file); break;
                            case 4: confirmDelete(file); break;
                        }
                    } else {
                        switch (i) {
                            case 0: openFile(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: restoreBackup(file); break;
                            case 4: confirmDelete(file); break;
                        }
                    }
                }).show();
    }

    private void sendViaWifi(File file) {
        if (!WifiDirectService.isConnected) {
            Toast.makeText(this, "Not connected to any device.\nOpen WiFi Direct first.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, WifiDirectActivity.class));
            return;
        }

        if (!isServiceBound || wifiService == null) {
            Toast.makeText(this, "Service not available. Please reconnect.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, WifiDirectService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            return;
        }

        wifiService.sendFile(file);
        Toast.makeText(this, "Sending: " + file.getName() + " via WiFi Direct...", Toast.LENGTH_SHORT).show();
    }

    private void openFile(File file) {
        try {
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();

            String content = sb.toString();
            if (content.length() > 2000) {
                content = content.substring(0, 2000) + "\n\n... (truncated)";
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("📄 " + file.getName())
                    .setMessage(content)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Share", (d, w) -> shareFile(file))
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider
                    .getUriForFile(this, getPackageName() + ".provider", file);

            if (uri != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(android.content.ClipData.newRawUri(null, uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Backup"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ──  RESTORE WITH MERGE/REPLACE ──────────────────────────────
    private void restoreBackup(File file) {
        // Create custom dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("♻️ Restore/Merge Backup");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 30);

        TextView fileNameText = new TextView(this);
        fileNameText.setText("📄 " + file.getName());
        fileNameText.setTextSize(16);
        fileNameText.setPadding(0, 0, 0, 20);
        layout.addView(fileNameText);

        // Merge Button
        Button mergeButton = new Button(this);
        mergeButton.setText("🔄 Merge (Keep existing + Add backup)");
        mergeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#10B981")
        ));
        mergeButton.setTextColor(android.graphics.Color.WHITE);
        mergeButton.setPadding(20, 20, 20, 20);
        mergeButton.setOnClickListener(v -> {
            confirmMergeRestore(file);
        });
        layout.addView(mergeButton);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 20));
        layout.addView(spacer);

        // Replace Button
        Button replaceButton = new Button(this);
        replaceButton.setText("🔁 Replace (Delete existing + Restore)");
        replaceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#EF4444")
        ));
        replaceButton.setTextColor(android.graphics.Color.WHITE);
        replaceButton.setPadding(20, 20, 20, 20);
        replaceButton.setOnClickListener(v -> {
            confirmReplaceRestore(file);
        });
        layout.addView(replaceButton);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmMergeRestore(File file) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔄 Merge Backup")
                .setMessage(
                        "This will:\n\n" +
                                "✅ KEEP your existing data\n" +
                                "✅ ADD data from the backup\n" +
                                "⚠️ Duplicate customers will be SKIPPED\n\n" +
                                "✅ No data will be lost!\n\n" +
                                "Continue with MERGE?"
                )
                .setPositiveButton("Merge", (d, w) -> {
                    if (BackupManager.isEncryptedFile(file)) {
                        showRestorePasswordDialog(file, "merge");
                    } else {
                        performMergeRestore(file);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReplaceRestore(File file) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔁 Replace Backup")
                .setMessage(
                        "⚠️ WARNING!\n\n" +
                                "This will:\n\n" +
                                "❌ DELETE all your current data\n" +
                                "✅ RESTORE data from the backup\n\n" +
                                "This action CANNOT be undone!\n\n" +
                                "Continue with REPLACE?"
                )
                .setPositiveButton("Replace", (d, w) -> {
                    if (BackupManager.isEncryptedFile(file)) {
                        showRestorePasswordDialog(file, "replace");
                    } else {
                        performReplaceRestore(file);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestorePasswordDialog(File file, String mode) {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String title = mode.equals("merge") ? "🔓 Decrypt & Merge" : "🔓 Decrypt & Replace";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Enter the password to decrypt:\n" + file.getName())
                .setView(passwordInput)
                .setPositiveButton(mode.equals("merge") ? "Merge" : "Replace", (d, w) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mode.equals("merge")) {
                        performMergeRestore(file, password);
                    } else {
                        performReplaceRestore(file, password);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ──  Perform Merge Restore ──────────────────────────────────────
    private void performMergeRestore(File file) {
        performMergeRestore(file, null);
    }


    private void performMergeRestore(File file, String password) {
        String fileType = password != null ? "encrypted" : "regular";
        Toast.makeText(this, "Merging " + fileType + " from: " + file.getName(), Toast.LENGTH_LONG).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String errorMsg = "";

            try {
                if (password != null) {
                    //  Encrypted merge
                    success = BackupManager.mergeEncryptedBackup(this, file, password);
                    if (!success) {
                        errorMsg = "Encrypted merge failed - wrong password or corrupted file";
                    }
                } else {
                    //  Regular merge
                    success = BackupManager.mergeBackupFromFile(this, file);
                    if (!success) {
                        errorMsg = "Regular merge failed";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
                errorMsg = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    Toast.makeText(MyBackupsActivity.this, " Merge Successful! Data added.", Toast.LENGTH_LONG).show();
                    refreshFileList();
                } else {
                    Toast.makeText(MyBackupsActivity.this, " Merge Failed! " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ── Perform Replace Restore ──────────────────────────────────────
    private void performReplaceRestore(File file) {
        performReplaceRestore(file, null);
    }

    // ──  Perform Replace Restore ──────────────────────────────────────
    private void performReplaceRestore(File file, String password) {
        String fileType = password != null ? "encrypted" : "regular";
        Toast.makeText(this, "Restoring " + fileType + " from: " + file.getName(), Toast.LENGTH_LONG).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String errorMsg = "";

            try {
                if (password != null) {
                    //  Encrypted restore
                    success = BackupManager.restoreEncryptedBackup(this, file, password);
                    if (!success) {
                        errorMsg = "Encrypted restore failed - wrong password or corrupted file";
                    }
                } else {
                    //  Regular restore
                    success = BackupManager.restoreBackupFromFile(this, file);
                    if (!success) {
                        errorMsg = "Regular restore failed";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
                errorMsg = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    Toast.makeText(MyBackupsActivity.this, " Restore Successful! Data replaced.", Toast.LENGTH_LONG).show();
                    refreshFileList();
                } else {
                    Toast.makeText(MyBackupsActivity.this, " Restore Failed! " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    private void confirmDelete(File file) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Backup")
                .setMessage("Delete \"" + file.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (file.delete()) {
                        Toast.makeText(this, " Deleted", Toast.LENGTH_SHORT).show();
                        refreshFileList();
                    } else {
                        Toast.makeText(this, " Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}