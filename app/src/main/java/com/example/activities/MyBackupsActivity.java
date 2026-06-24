package com.example.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.BackupAdapter;
import com.example.backup.BackupManager;
import com.example.models.BackupFile;
import com.example.service.WifiDirectService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyBackupsActivity extends AppCompatActivity {

    private TextView txtFileCount;
    private View emptyState;
    private RecyclerView rv;

    // WiFi Direct Service
    private WifiDirectService wifiService;
    private boolean isServiceBound = false;

    // Service Connection
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

        // Bind to WiFi Direct Service
        Intent intent = new Intent(this, WifiDirectService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialize views
        rv = findViewById(R.id.recyclerBackups);
        txtFileCount = findViewById(R.id.txtFileCount);
        emptyState = findViewById(R.id.emptyState);

        rv.setLayoutManager(new LinearLayoutManager(this));

        List<BackupFile> list = loadFiles();

        // Update file count
        if (txtFileCount != null) {
            txtFileCount.setText(String.valueOf(list.size()));
        }

        // Show empty state if no files
        if (emptyState != null) {
            if (list.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        }

        BackupAdapter adapter = new BackupAdapter(list, new BackupAdapter.OnBackupClickListener() {
            @Override
            public void onClick(BackupFile f) {
                handleFileClick(f.getFile());
            }

            @Override
            public void onLongClick(BackupFile f) {
                showOptions(f.getFile());
            }
        });
        rv.setAdapter(adapter);
    }

    // ── ✅ NEW: Handle file click (check if encrypted) ──────────────
    private void handleFileClick(File file) {
        if (BackupManager.isEncryptedFile(file)) {
            showDecryptDialog(file);
        } else {
            openFile(file);
        }
    }

    // ── ✅ NEW: Show decrypt dialog for encrypted files ──────────
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

    // ── ✅ NEW: Decrypt and open file ──────────────────────────────
    private void decryptAndOpenFile(File file, String password) {
        try {
            String jsonContent = BackupManager.decryptEncryptedFile(this, file, password);

            // Show decrypted content
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

    // ── Load My Backup Files (Supports .json and .enc) ────────────
    private List<BackupFile> loadFiles() {
        List<BackupFile> list = new ArrayList<>();

        File base = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "MilkDelivery"
        );

        if (!base.exists()) {
            base.mkdirs();
        }

        // ✅ Load BOTH .json AND .enc files
        File[] ownFiles = base.listFiles(file ->
                file.isFile() &&
                        (file.getName().endsWith(".json") || file.getName().endsWith(".enc")) &&
                        !file.getParentFile().getName().equals("received")
        );

        if (ownFiles != null) {
            for (File f : ownFiles) {
                String type = "";
                if (f.getName().endsWith(".enc")) {
                    type = "🔒 ";
                }
                list.add(new BackupFile(f, type));
            }
        }

        return list;
    }

    // ── Options dialog (Supports encrypted files) ──────────────────
    private void showOptions(File file) {
        String[] opts;

        // ✅ Check if file is encrypted
        if (BackupManager.isEncryptedFile(file)) {
            opts = new String[]{"🔓 Decrypt & Open", "📤 Share", "🗑️ Delete"};
        } else {
            opts = new String[]{"📄 Open", "📤 Share", "🗑️ Delete"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(opts, (d, i) -> {
                    if (BackupManager.isEncryptedFile(file)) {
                        switch (i) {
                            case 0: showDecryptDialog(file); break;
                            case 1: shareFile(file); break;
                            case 2: confirmDelete(file); break;
                        }
                    } else {
                        switch (i) {
                            case 0: openFile(file); break;
                            case 1: shareFile(file); break;
                            case 2: confirmDelete(file); break;
                        }
                    }
                }).show();
    }

    // ── Send via WiFi Direct ──────────────────────────────────────
    private void sendViaWifi(File file) {
        // Check if WiFi Direct is connected
        if (!WifiDirectService.isConnected) {
            Toast.makeText(this, "Not connected to any device.\nOpen WiFi Direct first.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, WifiDirectActivity.class));
            return;
        }

        // Check if service is bound
        if (!isServiceBound || wifiService == null) {
            Toast.makeText(this, "Service not available. Please reconnect.", Toast.LENGTH_LONG).show();
            // Try to bind again
            Intent intent = new Intent(this, WifiDirectService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            return;
        }

        // Send the file using service
        wifiService.sendFile(file);
        Toast.makeText(this, "Sending: " + file.getName(), Toast.LENGTH_SHORT).show();
    }

    // ── Open file (show JSON content) ─────────────────────────────
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

    // ── Share file ───────────────────────────────────────────────
    private void shareFile(File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider
                    .getUriForFile(this, getPackageName() + ".provider", file);

            if (uri != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_STREAM, uri);

                // Grant permission via ClipData
                intent.setClipData(android.content.ClipData.newRawUri(null, uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(intent, "Share Backup"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Delete file ──────────────────────────────────────────────
    private void confirmDelete(File file) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Backup")
                .setMessage("Delete \"" + file.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (file.delete()) {
                        Toast.makeText(this, "✅ Deleted", Toast.LENGTH_SHORT).show();
                        recreate();
                    } else {
                        Toast.makeText(this, "❌ Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind service when activity is destroyed
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}