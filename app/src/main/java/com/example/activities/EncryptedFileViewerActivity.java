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
import androidx.core.content.FileProvider;

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
    private ScrollView scrollView;
    private LinearLayout layoutContent;
    private LinearLayout layoutPassword;

    private String filePath = "";
    private String fileName = "";
    private long fileSize = 0;

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
        scrollView = findViewById(R.id.scrollView);
        layoutContent = findViewById(R.id.layoutContent);
        layoutPassword = findViewById(R.id.layoutPassword);

        // Handle the intent
        handleIntent();

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
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        // ✅ Check for both VIEW and SEND actions
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                loadFileFromUri(uri);
                return;
            }
        }

        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                loadFileFromUri(uri);
                return;
            }
        }

        // If no file, show error
        tvFileName.setText("No file selected");
        tvFileInfo.setText("");
        layoutPassword.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
    }

    private void loadFileFromUri(Uri uri) {
        try {
            // Get file name
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

            // Show password input
            layoutPassword.setVisibility(View.VISIBLE);
            layoutContent.setVisibility(View.GONE);

        } catch (SecurityException e) {
            e.printStackTrace();
            tvFileName.setText("Access Denied");
            tvFileInfo.setText("Permission error: The app cannot access this file. Please try sharing it again or opening it from a different file manager.");
            layoutPassword.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
            tvFileName.setText("Error loading file");
            tvFileInfo.setText(e.getMessage());
            layoutPassword.setVisibility(View.GONE);
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

            Toast.makeText(this, "✅ Decrypted successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Wrong password or corrupted file!", Toast.LENGTH_LONG).show();
            tvContent.setText("❌ Decryption failed.\n\n" + e.getMessage());
            layoutContent.setVisibility(View.VISIBLE);
        }
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