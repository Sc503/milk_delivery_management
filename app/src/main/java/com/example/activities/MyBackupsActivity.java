package com.example.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.BackupAdapter;
import com.example.models.BackupFile;
import com.example.service.WifiDirectService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyBackupsActivity extends AppCompatActivity {

    private TextView txtFileCount;
    private View emptyState;
    private RecyclerView rv;

    // ✅ WiFi Direct Service
    private WifiDirectService wifiService;
    private boolean isServiceBound = false;

    // ✅ Service Connection
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

        // ✅ Bind to WiFi Direct Service
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
                openFile(f.getFile());
            }

            @Override
            public void onLongClick(BackupFile f) {
                showOptions(f.getFile());
            }
        });
        rv.setAdapter(adapter);
    }

    // ── Load My Backup Files ─────────────────────────────────────
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

        // Load JSON files (excluding received folder)
        File[] ownFiles = base.listFiles(file ->
                file.isFile() &&
                        file.getName().endsWith(".json") &&
                        !file.getParentFile().getName().equals("received")
        );

        if (ownFiles != null) {
            for (File f : ownFiles) {
                list.add(new BackupFile(f));
            }
        }

        return list;
    }

    // ── Options dialog ───────────────────────────────────────────
    private void showOptions(File file) {
        String[] opts = {"Open", "Send via WiFi Direct", "Share", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(opts, (d, i) -> {
                    switch (i) {
                        case 0: openFile(file);      break;
                        case 1: sendViaWifi(file);   break;
                        case 2: shareFile(file);     break;
                        case 3: confirmDelete(file); break;
                    }
                }).show();
    }

    // ── ✅ FIXED: Send via WiFi Direct ──────────────────────────────
    private void sendViaWifi(File file) {
        // Check if WiFi Direct is connected
        if (!WifiDirectService.isConnected) {
            Toast.makeText(this, "Not connected to any device.\nOpen WiFi Direct first.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, WifiDirectActivity.class));
            return;
        }

        // ✅ Check if service is bound
        if (!isServiceBound || wifiService == null) {
            Toast.makeText(this, "Service not available. Please reconnect.", Toast.LENGTH_LONG).show();
            // Try to bind again
            Intent intent = new Intent(this, WifiDirectService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            return;
        }

        // ✅ Send the file using service
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
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Backup"));
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
        // ✅ Unbind service when activity is destroyed
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}