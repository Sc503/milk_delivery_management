package com.example.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.R;
import com.example.backup.BackupManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EncryptedFileViewerActivity extends AppCompatActivity {

    private TextView tvFileName;
    private TextView tvFileInfo;
    private TextView tvContent;
    private EditText etPassword;
    private Button btnDecrypt;
    private Button btnShare;
    private Button btnClose;
    private Button btnRestore;
    private ScrollView scrollView;
    private LinearLayout layoutContent;
    private LinearLayout layoutPassword;
    private LinearLayout layoutRestoreButton;

    private String filePath = "";
    private String fileName = "";
    private long fileSize = 0;
    private Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypted_file_viewer);

        // Initialize views
        tvFileName = findViewById(R.id.tvFileName);
        tvFileInfo = findViewById(R.id.tvFileInfo);
        tvContent = findViewById(R.id.tvContent);
        etPassword = findViewById(R.id.etPassword);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnShare = findViewById(R.id.btnShare);
        btnClose = findViewById(R.id.btnClose);
        btnRestore = findViewById(R.id.btnRestore);
        scrollView = findViewById(R.id.scrollView);
        layoutContent = findViewById(R.id.layoutContent);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutRestoreButton = findViewById(R.id.layoutRestoreButton);

        // Handle the intent
        handleIntent();

        //  Decrypt button
        btnDecrypt.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }
            decryptFile(password);
        });

        btnShare.setOnClickListener(v -> shareDecryptedContent());
        btnClose.setOnClickListener(v -> finish());

        //   Restore button click listener
        btnRestore.setOnClickListener(v -> {
            if (filePath.isEmpty()) {
                Toast.makeText(this, "No file to restore", Toast.LENGTH_SHORT).show();
                return;
            }
            showRestoreOptions();
        });
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                fileUri = uri;
                loadFileFromUri(uri);
                return;
            }
        }

        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                fileUri = uri;
                loadFileFromUri(uri);
                return;
            }
        }

        // If no file, show error
        tvFileName.setText("No file selected");
        tvFileInfo.setText("");
        layoutPassword.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        btnRestore.setVisibility(View.GONE);
        if (layoutRestoreButton != null) {
            layoutRestoreButton.setVisibility(View.GONE);
        }
    }

    private void loadFileFromUri(Uri uri) {
        try {
            fileName = getFileNameFromUri(uri);
            filePath = uri.toString();

            tvFileName.setText("🔒 " + fileName);

            // Get file size
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    fileSize = inputStream.available();
                    tvFileInfo.setText("Size: " + getFileSize(fileSize) + " | Encrypted backup file");
                }
            }

            // Save the file to cache for processing
            saveFileToCache(uri);

            // Show password input and restore button
            layoutPassword.setVisibility(View.VISIBLE);
            layoutContent.setVisibility(View.GONE);
            btnRestore.setVisibility(View.VISIBLE);

        } catch (SecurityException e) {
            e.printStackTrace();
            tvFileName.setText("Access Denied");
            tvFileInfo.setText("Permission error");
            layoutPassword.setVisibility(View.GONE);
            btnRestore.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
            tvFileName.setText("Error loading file");
            tvFileInfo.setText(e.getMessage());
            layoutPassword.setVisibility(View.GONE);
            btnRestore.setVisibility(View.GONE);
        }
    }

    private void saveFileToCache(Uri uri) {
        try {
            File cacheDir = getCacheDir();
            File tempFile = new File(cacheDir, "temp_" + System.currentTimeMillis() + ".enc");

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            filePath = tempFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptFile(String password) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String jsonContent = BackupManager.decryptEncryptedFile(this, file, password);

            tvContent.setText(jsonContent);
            layoutContent.setVisibility(View.VISIBLE);
            layoutPassword.setVisibility(View.GONE);
            btnShare.setVisibility(View.VISIBLE);
            btnRestore.setVisibility(View.GONE);

            //  Show restore button at bottom
            if (layoutRestoreButton != null) {
                layoutRestoreButton.setVisibility(View.VISIBLE);
                Button btnRestoreContent = findViewById(R.id.btnRestoreContent);
                btnRestoreContent.setOnClickListener(v -> {
                    showRestoreOptions();
                });
                Button btnCloseContent = findViewById(R.id.btnCloseContent);
                btnCloseContent.setOnClickListener(v -> finish());
            }

            Toast.makeText(this, "✅ Decrypted successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Wrong password or corrupted file!", Toast.LENGTH_LONG).show();
            tvContent.setText("Decryption failed.\n\n" + e.getMessage());
            layoutContent.setVisibility(View.VISIBLE);
            layoutPassword.setVisibility(View.GONE);
        }
    }

    //  NEW: Show restore options dialog
    private void showRestoreOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("♻️ Restore Backup");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 30);

        TextView fileNameText = new TextView(this);
        fileNameText.setText("📄 " + fileName);
        fileNameText.setTextSize(16);
        fileNameText.setPadding(0, 0, 0, 20);
        layout.addView(fileNameText);

        // Merge Button
        Button mergeButton = new Button(this);
        mergeButton.setText("🔄 Merge (Keep existing + Add backup)");
        mergeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#10B981")));
        mergeButton.setTextColor(android.graphics.Color.WHITE);
        mergeButton.setPadding(20, 20, 20, 20);
        mergeButton.setOnClickListener(v -> {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if encrypted
            if (file.getName().endsWith(".enc")) {
                showPasswordDialog(file, "merge");
            } else {
                performRestore(file, "merge", null);
            }
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
                android.graphics.Color.parseColor("#EF4444")));
        replaceButton.setTextColor(android.graphics.Color.WHITE);
        replaceButton.setPadding(20, 20, 20, 20);
        replaceButton.setOnClickListener(v -> {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            if (file.getName().endsWith(".enc")) {
                showPasswordDialog(file, "replace");
            } else {
                performRestore(file, "replace", null);
            }
        });
        layout.addView(replaceButton);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // NEW: Password dialog for encrypted files
    private void showPasswordDialog(File file, String mode) {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter encryption password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String title = mode.equals("merge") ? "🔓 Decrypt & Merge" : "🔓 Decrypt & Replace";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Enter the password to decrypt:\n" + file.getName())
                .setView(passwordInput)
                .setPositiveButton(mode.equals("merge") ? "Merge" : "Replace", (d, w) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performRestore(file, mode, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //  NEW: Perform restore in background
    private void performRestore(File file, String mode, String password) {
        Toast.makeText(this, mode.equals("merge") ? "🔄 Merging..." : "🔁 Restoring...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            boolean success = false;
            String errorMsg = "";

            try {
                if (password != null) {
                    // Encrypted restore
                    if (mode.equals("merge")) {
                        success = BackupManager.mergeEncryptedBackup(this, file, password);
                    } else {
                        success = BackupManager.restoreEncryptedBackup(this, file, password);
                    }
                } else {
                    // Regular restore
                    if (mode.equals("merge")) {
                        success = BackupManager.mergeBackupFromFile(this, file);
                    } else {
                        success = BackupManager.restoreBackupFromFile(this, file);
                    }
                }

                if (!success) {
                    errorMsg = "Restore operation failed";
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
                    Toast.makeText(this, "✅ Restore Successful!", Toast.LENGTH_LONG).show();
                    // Close activity after success
                    finish();
                } else {
                    Toast.makeText(this, "❌ Restore Failed: " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void shareDecryptedContent() {
        String content = tvContent.getText().toString();
        if (content.isEmpty()) {
            Toast.makeText(this, "No content to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Share Decrypted Backup"));
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null && uri != null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "Unknown";
    }

    private String getFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        } else {
            return (size / (1024 * 1024)) + " MB";
        }
    }
}