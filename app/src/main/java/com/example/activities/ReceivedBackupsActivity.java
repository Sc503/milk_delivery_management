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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceivedBackupsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_received_backups);

        // Bind to WiFi Direct Service
        Intent intent = new Intent(this, WifiDirectService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

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
                showFileOptionsDialog(f.getFile());
            }

            @Override
            public void onLongClick(BackupFile f) {
                showOptions(f.getFile());
            }
        });
        rv.setAdapter(adapter);
    }

    // ── Show file options dialog ──────────────────────────────────────
    private void showFileOptionsDialog(File file) {
        String[] options;

        // ✅ Check both extension AND content
        boolean isEncrypted = BackupManager.isEncryptedFile(file) || isFileEncrypted(file);

        if (isEncrypted) {
            options = new String[]{"🔓 Decrypt & Open", "📤 Share", "📶 Send via WiFi Direct", "🗑️ Delete"};
        } else {
            options = new String[]{"📄 Open", "📤 Share", "📶 Send via WiFi Direct", "🗑️ Delete"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(options, (d, i) -> {
                    if (isEncrypted) {
                        switch (i) {
                            case 0: showDecryptDialog(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: confirmDelete(file); break;
                        }
                    } else {
                        switch (i) {
                            case 0: openFile(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: confirmDelete(file); break;
                        }
                    }
                })
                .show();
    }

    // ── Show decrypt dialog for encrypted files ──────────────────
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

    // ── Decrypt and open file ──────────────────────────────────────
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

        // Load both .json and .enc files
        File[] receivedFiles = base.listFiles(file ->
                file.isFile() &&
                        (file.getName().endsWith(".json") || file.getName().endsWith(".enc"))
        );

        if (receivedFiles != null) {
            for (File f : receivedFiles) {
                String type = "";
                if (f.getName().endsWith(".enc")) {
                    type = "🔒 ";
                } else if (isFileEncrypted(f)) {
                    // ✅ Check if .json file is actually encrypted content
                    type = "🔒 ";
                }
                list.add(new BackupFile(f, type));
            }
        }

        return list;
    }

    // ── Check if file content is encrypted ──────────────────────────────
    private boolean isFileEncrypted(File file) {
        try {
            String content = readFileContent(file);
            return isContentEncrypted(content);
        } catch (Exception e) {
            return true; // If we can't read it, assume it's encrypted
        }
    }

    // ── Read file content ───────────────────────────────────────────────
    private String readFileContent(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    // ── Check if content is encrypted (not valid JSON) ────────────────
    private boolean isContentEncrypted(String content) {
        if (content == null || content.isEmpty()) {
            return true;
        }

        String trimmed = content.trim();

        // Check if it starts with JSON characters
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                new JSONObject(trimmed);
                return false; // Valid JSON
            } catch (Exception e) {
                try {
                    new JSONArray(trimmed);
                    return false; // Valid JSON Array
                } catch (Exception e2) {
                    return true; // Not valid JSON → encrypted
                }
            }
        }

        // Check for encrypted indicators
        int printableCount = 0;
        for (char c : trimmed.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) ||
                    c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',' || c == '"') {
                printableCount++;
            }
        }

        double printableRatio = (double) printableCount / trimmed.length();
        return printableRatio < 0.5;
    }

    // ── Options dialog ──────────────────────────────────────────
    private void showOptions(File file) {
        String[] opts;

        boolean isEncrypted = BackupManager.isEncryptedFile(file) || isFileEncrypted(file);

        if (isEncrypted) {
            opts = new String[]{"🔓 Decrypt & Open", "📤 Share", "📶 Send via WiFi Direct", "🗑️ Delete"};
        } else {
            opts = new String[]{"📄 Open", "📤 Share", "📶 Send via WiFi Direct", "🗑️ Delete"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(opts, (d, i) -> {
                    if (isEncrypted) {
                        switch (i) {
                            case 0: showDecryptDialog(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: confirmDelete(file); break;
                        }
                    } else {
                        switch (i) {
                            case 0: openFile(file); break;
                            case 1: shareFile(file); break;
                            case 2: sendViaWifi(file); break;
                            case 3: confirmDelete(file); break;
                        }
                    }
                }).show();
    }

    // ── Send via WiFi Direct ──────────────────────────────────────
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

    // ── Open file (smart detection) ─────────────────────────────────────
    private void openFile(File file) {
        try {
            String content = readFileContent(file);

            // Check if content is encrypted
            if (isContentEncrypted(content)) {
                Toast.makeText(this, "This file appears to be encrypted. Please decrypt it.", Toast.LENGTH_LONG).show();
                showDecryptDialog(file);
                return;
            }

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
            Toast.makeText(this, "Cannot read file. It may be encrypted.", Toast.LENGTH_LONG).show();
            showDecryptDialog(file);
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
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}