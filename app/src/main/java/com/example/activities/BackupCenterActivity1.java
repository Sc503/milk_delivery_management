package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;

public class BackupCenterActivity1 extends AppCompatActivity {

    private Button btnMyBackups;
    private Button btnReceivedBackups;
    private Button btnWifiDirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_center1);

        // Initialize all buttons
        btnMyBackups = findViewById(R.id.btnMyBackups);
        btnReceivedBackups = findViewById(R.id.btnReceivedBackups);
        btnWifiDirect = findViewById(R.id.btnWifiDirect);

        // My Backups - Open MyBackupsActivity
        btnMyBackups.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBackupsActivity.class);
            startActivity(intent);
        });

        // Received Backups - Open ReceivedBackupsActivity
        btnReceivedBackups.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReceivedBackupsActivity.class);
            startActivity(intent);
        });

        // WiFi Direct - DISABLED (just shows a toast)
        btnWifiDirect.setOnClickListener(v -> {
            // WiFi Direct is disabled
        });
    }
}