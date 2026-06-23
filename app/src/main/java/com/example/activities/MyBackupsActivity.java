package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
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
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyBackupsActivity extends AppCompatActivity {

    private TextView txtFileCount;
    private View emptyState;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_backups);

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

    // ── Send via WiFi Direct ─────────────────────────────────────
    private void sendViaWifi(File file) {
        if (!WifiDirectService.isConnected) {
            Toast.makeText(this, "Connect devices first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, WifiDirectActivity.class));
            return;
        }

        String targetAddress;
        if (WifiDirectService.isGroupOwner) {
            targetAddress = WifiDirectService.clientAddress;
            if (targetAddress == null || targetAddress.isEmpty()) {
                String[] possibleIps = {"192.168.49.1", "192.168.49.2", "192.168.1.1"};
                for (String ip : possibleIps) {
                    try {
                        java.net.InetAddress address = java.net.InetAddress.getByName(ip);
                        if (address.isReachable(2000)) {
                            targetAddress = ip;
                            break;
                        }
                    } catch (Exception e) {}
                }
            }
        } else {
            targetAddress = WifiDirectService.connectedHostAddress;
        }

        if (targetAddress == null || targetAddress.isEmpty()) {
            Toast.makeText(this, "Could not find target device", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Sending: " + file.getName(), Toast.LENGTH_LONG).show();

        final String finalTargetAddress = targetAddress;
        new Thread(() -> {
            try {
                Socket socket = new Socket(finalTargetAddress, WifiDirectService.PORT);
                FileInputStream fis = new FileInputStream(file);
                OutputStream out = socket.getOutputStream();

                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

                out.flush();
                socket.shutdownOutput();
                fis.close();
                socket.close();

                runOnUiThread(() ->
                        Toast.makeText(MyBackupsActivity.this, "✅ Sent Successfully", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MyBackupsActivity.this, "❌ Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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
}