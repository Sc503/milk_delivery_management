package in.esmartsolution.milkflow.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.Receivers.WifiDirectBroadcastReceiver;
import in.esmartsolution.milkflow.adapters.DeviceAdapter;
import in.esmartsolution.milkflow.databinding.ActivityWifiDirectBinding;
import in.esmartsolution.milkflow.models.DeviceModel;
import in.esmartsolution.milkflow.models.TransferState;
import in.esmartsolution.milkflow.services.FileTransferService;
import in.esmartsolution.milkflow.utils.PermissionHelper;
import in.esmartsolution.milkflow.viewmodel.MainViewModel;

import java.io.File;

import in.esmartsolution.milkflow.repository.WifiRepository;

public class TempWifiDirectActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 2002;
    private static final String TAG = "TempWifiDirectActivity";
    private static final int REQUEST_CODE = 3333;

    private ActivityWifiDirectBinding binding;
    private MainViewModel viewModel;

    private DeviceAdapter deviceAdapter;
    private WifiDirectBroadcastReceiver broadcastReceiver;
    private final IntentFilter intentFilter = new IntentFilter();

    private FileTransferService fileTransferService;
    private boolean isServiceBound = false;

    private boolean isPermissionPermanentlyDenied = false;

    private boolean isPermissionRequestInProgress = false;


    // ActivityResultLauncher for Backup Center
    private final ActivityResultLauncher<Intent> backupCenterLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            //  FileProvider URI already has grant permissions
                            // No need to call takePersistableUriPermission
                            viewModel.selectFile(uri);
                            Log.d(TAG, "✅ File selected: " + uri.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error selecting file", e);
                            Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    // ServiceConnection to bind Foreground FileTransferService
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.LocalBinder binder = (FileTransferService.LocalBinder) service;
            fileTransferService = binder.getService();
            isServiceBound = true;
            viewModel.setFileTransferService(fileTransferService);
            viewModel.performHandshakeIfClient();
            Log.d(TAG, "Service connected successfully");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fileTransferService = null;
            isServiceBound = false;
            viewModel.setFileTransferService(null);
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWifiDirectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        //  Check if file was passed from MyBackupsActivity
        checkAndRequestPermissions();
        handleIncomingFileFromMyBackups();

        setupRecyclerView();
        setupClickListeners();
        setupBroadcastFilters();
        observeViewModel();

        binding.btnDisconnect.setVisibility(View.GONE);
        binding.layoutConnectedDetails.setVisibility(View.GONE);

        // Start and bind the foreground service
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceAdapter(this);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDevices.setAdapter(deviceAdapter);
    }

    private void setupClickListeners() {
        // Discover/Scan for peers
        binding.btnScan.setOnClickListener(v -> triggerDeviceDiscovery());
        binding.btnRefresh.setOnClickListener(v -> triggerDeviceDiscovery());

        // File Selection Button - Opens Backup Center
        binding.btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(TempWifiDirectActivity.this, BackupCenterActivity1.class);
            backupCenterLauncher.launch(intent);
        });

        // Clear/Delete selected file
        binding.btnDeleteFile.setOnClickListener(v -> viewModel.clearSelectedFile());

        // Disconnect from current peer group
        binding.btnDisconnect.setOnClickListener(v -> {
            viewModel.disconnectDevice(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TempWifiDirectActivity.this, "Disconnected from peer group.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(TempWifiDirectActivity.this, "Failed to disconnect: " + reason, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Send Selected File Button
        binding.btnSendFile.setOnClickListener(v -> viewModel.sendSelectedFile());

        // Cancel Active Transfer
        binding.btnCancelTransfer.setOnClickListener(v -> viewModel.cancelTransfer());
    }

    //  Handle file from MyBackupsActivity
    private void handleIncomingFileFromMyBackups() {
        Intent intent = getIntent();
        if (intent == null) return;


        String filePath = intent.getStringExtra("file_path");
        String fileName = intent.getStringExtra("file_name");
        Uri uri = intent.getData();

        if (uri != null) {
            Log.d(TAG, "📥 File received from MyBackups: " + uri.toString());
            viewModel.selectFile(uri);
            Toast.makeText(this, "📁 File loaded: " + fileName, Toast.LENGTH_SHORT).show();
        } else if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                viewModel.selectFile(fileUri);
                Toast.makeText(this, "📁 File loaded: " + fileName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ File not found: " + filePath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBroadcastFilters() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        broadcastReceiver = new WifiDirectBroadcastReceiver(WifiRepository.getInstance(this));
    }

    private void observeViewModel() {
        // 1. Wifi State Observer
        viewModel.getWifiEnabled().observe(this, isEnabled -> {
            if (!isEnabled) {
                binding.tvConnectionStatus.setText("Wi-Fi Disabled");
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi_off);
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorError));
            }
        });

        // 2. Discovered Devices Observer
        viewModel.getDiscoveredDevices().observe(this, devices -> {
            deviceAdapter.updateDevices(devices);
            if (devices.isEmpty()) {
                binding.layoutDevicesEmpty.setVisibility(View.VISIBLE);
                binding.rvDevices.setVisibility(View.GONE);
            } else {
                binding.layoutDevicesEmpty.setVisibility(View.GONE);
                binding.rvDevices.setVisibility(View.VISIBLE);
            }
        });

        // 3. Local Device Info Observer
        viewModel.getLocalDeviceName().observe(this, localName -> {
            binding.tvLocalDeviceInfo.setText("Local Device: " + localName);
        });

        // 4. Group Connection Status Observer
        viewModel.getConnectionStatusText().observe(this, status -> {
            if (status == null) return;

            binding.tvConnectionStatus.setText(status);

            if ("Connected".equalsIgnoreCase(status) || "CONNECTED".equalsIgnoreCase(status)) {
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi);
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorSuccess));
                binding.btnDisconnect.setVisibility(View.VISIBLE);
                binding.layoutConnectedDetails.setVisibility(View.VISIBLE);
            } else if (status.toLowerCase().contains("connecting")) {
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                binding.ivStatusIcon.setImageResource(R.drawable.ic_info);
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
                binding.btnDisconnect.setVisibility(View.GONE);
                binding.layoutConnectedDetails.setVisibility(View.GONE);
            } else {
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi_off);
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorError));
                binding.btnDisconnect.setVisibility(View.GONE);
                binding.layoutConnectedDetails.setVisibility(View.GONE);
            }
        });

        // 5. Connection Details Info Observer
        viewModel.getConnectionInfo().observe(this, info -> {
            if (info != null && info.groupFormed) {
                binding.tvConnectedIp.setText(info.isGroupOwner ? "192.168.49.1 (This GO)" : info.groupOwnerAddress.getHostAddress());
                binding.tvConnectedRole.setText(info.isGroupOwner ? "Group Owner" : "Client");

                if (!info.isGroupOwner) {
                    viewModel.performHandshakeIfClient();
                }
            } else {
                binding.tvConnectedIp.setText("N/A");
                binding.tvConnectedRole.setText("N/A");
                binding.tvConnectedDeviceName.setText("N/A");
                binding.tvConnectedTime.setText("00:00:00");
            }
        });

        // 6. Connected Peer IP Observer
        viewModel.getPeerIp().observe(this, peerIp -> {
            if (peerIp != null) {
                binding.tvConnectedDeviceName.setText("Remote Peer: " + peerIp);
            }
        });

        // 7. Selected File Observer
        viewModel.getSelectedFile().observe(this, fileModel -> {
            if (fileModel != null) {
                binding.cardSelectedFileDetails.setVisibility(View.VISIBLE);
                binding.tvSelectedFileName.setText(fileModel.getName());
                binding.tvSelectedFileSizeType.setText(fileModel.getFormattedSize() + " • " + fileModel.getExtension().toUpperCase() + " File");

                Boolean isConnectedValue = viewModel.getIsConnected().getValue();
                binding.btnSendFile.setEnabled(isConnectedValue != null && isConnectedValue);
            } else {
                binding.cardSelectedFileDetails.setVisibility(View.GONE);
                binding.btnSendFile.setEnabled(false);
            }
        });

        // 8. Connection Status check to toggle send button state
        viewModel.getIsConnected().observe(this, isConnected -> {
            Boolean hasFile = viewModel.getSelectedFile().getValue() != null;
            binding.btnSendFile.setEnabled(isConnected != null && isConnected && hasFile);
        });

        // 9. Active Transfer Progress State Observer
        viewModel.getTransferState().observe(this, state -> {
            if (state == null) return;

            if (state.getStatus() == TransferState.Status.IDLE) {
                binding.cardTransferProgress.setVisibility(View.GONE);
            } else {
                binding.cardTransferProgress.setVisibility(View.VISIBLE);
                binding.transferProgressBar.setProgress(state.getProgressPercent());
                binding.tvTransferPercentage.setText(state.getProgressPercent() + "%");
                binding.tvTransferSpeed.setText(state.getFormattedSpeed());
                binding.tvTransferredSize.setText(state.getFormattedTransferredSize());
                binding.tvRemainingTime.setText(state.getFormattedRemainingTime());
                binding.tvTransferStatus.setText(state.getMessage());


                if (state.getStatus() == TransferState.Status.SUCCESS) {

                    if (fileTransferService != null) {
                        showSuccessDialog(state.getMessage());

                        viewModel.clearTransferState();
                    }
                } else if (state.getStatus() == TransferState.Status.FAILED) {
                    showFailureDialog(state.getMessage());
                }
            }
        });
    }

    //  UPDATED: Check permissions with Samsung-specific handling
    private void checkAndRequestPermissions() {
        //  If user already permanently denied, don't ask again
        if (isPermissionPermanentlyDenied) {
            Toast.makeText(this, "⚠️ Permissions permanently denied. Please enable in Settings.", Toast.LENGTH_LONG).show();
            showSamsungPermissionDialog();
            return;
        }

        //  Prevent multiple simultaneous requests
        if (isPermissionRequestInProgress) {
            Log.d(TAG, "Permission request already in progress, skipping...");
            return;
        }

        //  Check if WiFi is enabled
        android.net.wifi.WifiManager wifiManager =
                (android.net.wifi.WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("📶 Enable WiFi")
                    .setMessage("WiFi Direct requires WiFi to be enabled.\n\n" +
                            "Would you like to enable WiFi now?")
                    .setPositiveButton("Enable WiFi", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        //  Check location permission (Critical for Samsung)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            isPermissionRequestInProgress = true;

            new AlertDialog.Builder(this)
                    .setTitle("📍 Location Permission Required")
                    .setMessage("WiFi Direct scanning requires LOCATION permission on your device.\n\n" +
                            "Please grant location permission when prompted.\n\n" +
                            "If denied, go to:\n" +
                            "Settings → Apps → MilkFlow → Permissions\n\n" +
                            "Enable LOCATION.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                },
                                PERMISSIONS_REQUEST_CODE
                        );
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        isPermissionRequestInProgress = false;
                        Toast.makeText(this, "Location permission required for WiFi Direct.", Toast.LENGTH_LONG).show();
                        finish(); //  Close activity if user cancels
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        //  Check all other permissions
        if (!PermissionHelper.hasAllPermissions(this)) {
            isPermissionRequestInProgress = true;
            ActivityCompat.requestPermissions(
                    this,
                    PermissionHelper.getRequiredPermissions(),
                    PERMISSIONS_REQUEST_CODE
            );
        } else {
            //  All permissions already granted
            Log.d(TAG, "All permissions already granted");
        }
    }

    //  UPDATED: Handle permission results with detailed logging

    boolean allGranted = true;
    boolean locationGranted = false;
    boolean wifiGranted = false;
    boolean anyDenied = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //  Reset the flag
        isPermissionRequestInProgress = false;

        if (requestCode == PERMISSIONS_REQUEST_CODE) {


            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int result = grantResults[i];

                if (result == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "✅ Permission GRANTED: " + permission);
                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        locationGranted = true;
                    }
                    if (permission.equals(Manifest.permission.ACCESS_WIFI_STATE) ||
                            permission.equals(Manifest.permission.CHANGE_WIFI_STATE)) {
                        wifiGranted = true;
                    }
                } else {
                    Log.d(TAG, "❌ Permission DENIED: " + permission);
                    allGranted = false;
                    anyDenied = true;
                    if (permission.toLowerCase().contains("external"))
                    {
                        // This ONLY works on Android 10 and below
// On Android 11+, it will NOT show a popup
                        // This ONLY works on Android 10 and below
// On Android 11+, it will NOT show a popup
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {  // Android 10 or lower
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        REQUEST_CODE);
                            }
                        }
                    }
                }
            }

            if (allGranted) {
                //  All permissions granted
//                Toast.makeText(this, "✅ Permissions granted! You can now scan for peers.",
//                        Toast.LENGTH_SHORT).show();
                triggerDeviceDiscovery();

            } else if (locationGranted && wifiGranted) {
                allGranted = true;
                //  Location and WiFi permissions granted
                Toast.makeText(this, "✅ Required permissions granted.", Toast.LENGTH_SHORT).show();
               // triggerDeviceDiscovery();

            } else {
                // ❌ Some permissions denied

                //  Check if user checked "Never ask again"
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (!shouldShowRationale) {
                    //  User checked "Never ask again" - permanently denied
                    isPermissionPermanentlyDenied = true;
                    Toast.makeText(this,
                            "⚠️ Permissions permanently denied. Please enable in Settings.",
                            Toast.LENGTH_LONG).show();
                } else {
                    //  User denied but we can ask again
                    Toast.makeText(this,
                            "⚠️ Permissions denied. WiFi Direct features will be limited.",
                            Toast.LENGTH_LONG).show();
                }

                // Show Samsung dialog
                showSamsungPermissionDialog();
            }
        }
    }
    //  New method: Show Samsung-specific permission dialog
    private void showSamsungPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📍 Permission Required")
                .setMessage("On Samsung devices, WiFi Direct requires:\n\n" +
                        "1. ✅ WiFi ON\n" +
                        "2. ✅ Location Permission\n\n" +
                        "Please go to:\n" +
                        "Settings → Apps → MilkFlow → Permissions\n\n" +
                        "Enable LOCATION permission.\n\n" +
                        "Also enable:\n" +
                        "Settings → Location → Improve Accuracy → WiFi scanning")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    //  Close activity - user needs to enable permissions in Settings
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    //  Close activity - user doesn't want to grant permissions
                    Toast.makeText(this,
                            "WiFi Direct feature disabled. Enable permissions in Settings to use it.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    //  UPDATED: Device discovery with WiFi and permission checks
    private void triggerDeviceDiscovery() {
        //  Check if permissions are permanently denied
        if (isPermissionPermanentlyDenied) {
            Toast.makeText(this,
                    "⚠️ Permissions permanently denied. Please enable in Settings.",
                    Toast.LENGTH_LONG).show();

            if (!allGranted)
            {
                showSamsungPermissionDialog();
            }

            return;
        }

        //  Check if WiFi is enabled
        android.net.wifi.WifiManager wifiManager =
                (android.net.wifi.WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "📶 Please enable WiFi first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }

        //  Check location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "📍 Location permission required for scanning", Toast.LENGTH_LONG).show();
                checkAndRequestPermissions();
                return;
            }
        }

        //  Check all permissions
        if (!PermissionHelper.hasAllPermissions(this)) {
            checkAndRequestPermissions();
            return;
        }

        //  If permission request is in progress, don't scan
        if (isPermissionRequestInProgress) {
            Toast.makeText(this, "⏳ Permission request in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        //  All good - start discovery
        viewModel.discoverPeers(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TempWifiDirectActivity.this, "🔍 Scanning for nearby devices...",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                String errorMsg = "Scan failed: ";
                switch (reason) {
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        errorMsg += "Wi-Fi Direct not supported.";
                        break;
                    case WifiP2pManager.BUSY:
                        errorMsg += "System busy. Retry shortly.";
                        break;
                    case WifiP2pManager.ERROR:
                        errorMsg += "Internal error.";
                        break;
                    default:
                        errorMsg += "Unknown error (" + reason + ")";
                }
                Toast.makeText(TempWifiDirectActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectClick(DeviceModel device) {
        viewModel.connectToDevice(device, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TempWifiDirectActivity.this, "Initiating connection to " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(TempWifiDirectActivity.this, "Failed to connect: " + reason, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Transfer Successful")
                .setMessage(message)
                .setIcon(R.drawable.ic_check_circle)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showFailureDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Transfer Failed")
                .setMessage(message)
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver != null) {
            registerReceiver(broadcastReceiver, intentFilter);
        }
        refreshConnectionStatus();


        if (viewModel != null) {
            viewModel.clearTransferState();
        }
    }

    private void refreshConnectionStatus() {
        if (viewModel != null) {
            // Request fresh connection info
            WifiRepository.getInstance(this).requestConnectionInfo();

            // Get current connection status
            Boolean isConnected = viewModel.getIsConnected().getValue();
            String status = viewModel.getConnectionStatusText().getValue();

            Log.d(TAG, "Refresh connection status - isConnected: " + isConnected + ", status: " + status);

            // Manually update UI based on current status
            if (isConnected != null && isConnected) {
                binding.btnDisconnect.setVisibility(View.VISIBLE);
                binding.layoutConnectedDetails.setVisibility(View.VISIBLE);
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi);
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorSuccess));
            } else {
                binding.btnDisconnect.setVisibility(View.GONE);
                binding.layoutConnectedDetails.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        //  Close all activities and go to Settings_Activity
        Intent intent = new Intent(this, Settings_Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}