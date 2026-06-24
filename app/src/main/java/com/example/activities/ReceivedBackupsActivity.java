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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceivedBackupsActivity extends AppCompatActivity {

    private TextView txtFileCount;
    private View emptyState;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_backups);

        // Initialize views
        rv = findViewById(R.id.recyclerReceivedBackups);
        txtFileCount = findViewById(R.id.txtFileCount);
        emptyState = findViewById(R.id.emptyState);

        rv.setLayoutManager(new LinearLayoutManager(this));

        List<BackupFile> list = loadReceivedFiles();

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

    // ── Load Received Backup Files ──────────────────────────────
    private List<BackupFile> loadReceivedFiles() {
        List<BackupFile> list = new ArrayList<>();

        File base = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "MilkDelivery/received"
        );

        if (!base.exists()) {
            base.mkdirs();
            return list;
        }

        // Load JSON files from received folder
        File[] receivedFiles = base.listFiles(file ->
                file.isFile() && file.getName().endsWith(".json")
        );

        if (receivedFiles != null) {
            for (File f : receivedFiles) {
                list.add(new BackupFile(f, "Received"));
            }
        }

        return list;
    }

    // ── Options dialog ───────────────────────────────────────────
    private void showOptions(File file) {
        String[] opts = {"Open", "Share", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(opts, (d, i) -> {
                    switch (i) {
                        case 0: openFile(file);      break;
                        case 1: shareFile(file);     break;
                        case 2: confirmDelete(file); break;
                    }
                }).show();
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

                // Grant permission via ClipData (recommended for modern Android versions)
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
}