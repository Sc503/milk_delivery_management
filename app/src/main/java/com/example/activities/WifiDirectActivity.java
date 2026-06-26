package com.example.activities;

import static android.content.Context.WIFI_P2P_SERVICE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.DeviceAdapter;
import com.example.models.WifiDevice;
import com.example.service.WifiDirectService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiDirectActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener {

    private static final int PERM_CODE = 1001;
    private static final int REQUEST_CODE_BACKUP_CENTER = 1002;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private DeviceAdapter adapter;
    private List<WifiDevice> deviceList = new ArrayList<>();

    private TextView statusText;
    private Button btnScan, btnDisconnect, btnSendFile;
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private String ownDeviceAddress = "";

    private WifiDirectService service;
    private boolean isBound = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder b) {
            service = ((WifiDirectService.LocalBinder) b).getService();
            isBound = true;
            setupListener();
            updateUIState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);

        initViews();
        initWifiP2p();

        Intent svc = new Intent(this, WifiDirectService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        bindService(svc, connection, BIND_AUTO_CREATE);

        getOwnDeviceAddress();
    }

    private void getOwnDeviceAddress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkLocationPermission()) {
                manager.requestDeviceInfo(channel, device -> {
                    if (device != null) {
                        ownDeviceAddress = device.deviceAddress;
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BACKUP_CENTER && resultCode == RESULT_OK && data != null) {
            String filePath = data.getStringExtra("BACKUP_FILE_PATH");
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists() && isBound && service != null) {
                    service.sendFile(file);
                    Toast.makeText(this, "Sending: " + file.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initViews() {
        statusText = findViewById(R.id.txtConnectionStatus);
        btnScan = findViewById(R.id.btnScan);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSendFile = findViewById(R.id.btnSendFile);
        RecyclerView rv = findViewById(R.id.recyclerDevices);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(deviceList, this::connectToDevice);
        rv.setAdapter(adapter);

        btnScan.setOnClickListener(v -> startScan());
        btnDisconnect.setOnClickListener(v -> disconnect());

        btnSendFile.setOnClickListener(v -> {
            if (!WifiDirectService.isConnected) {
                Toast.makeText(this, "Connect to a device first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, BackupCenterActivity.class);
            intent.putExtra("MODE", "SELECT_TO_SEND");
            startActivityForResult(intent, REQUEST_CODE_BACKUP_CENTER);
        });
    }

    private void initWifiP2p() {
        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        setStatus("● Wi-Fi Direct is OFF", "#B71C1C");
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    if (manager != null && checkLocationPermission()) {
                        manager.requestPeers(channel, WifiDirectActivity.this);
                    }
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    if (manager == null) return;
                    android.net.NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                    if (networkInfo != null && networkInfo.isConnected()) {
                        manager.requestConnectionInfo(channel, info -> {
                            if (info.groupFormed) {
                                isConnected = true;
                                if (info.isGroupOwner) {
                                    service.onBecomeHost(info.groupOwnerAddress.getHostAddress());
                                } else {
                                    service.onBecomeClient(info.groupOwnerAddress.getHostAddress(), "");
                                }
                            }
                        });
                    } else {
                        isConnected = false;
                        service.onConnectionLost();
                    }
                }
            }
        };

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void setupListener() {
        WifiDirectService.setListener(new WifiDirectService.StateListener() {
            @Override
            public void onConnected(boolean asHost, String hostAddress, String deviceName) {
                runOnUiThread(() -> {
                    isConnecting = false;
                    isConnected = true;
                    updateUIState();
                    Toast.makeText(WifiDirectActivity.this, "✅ Connected!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    isConnecting = false;
                    isConnected = false;
                    updateUIState();
                    Toast.makeText(WifiDirectActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFileReceived(String filePath) {
                runOnUiThread(() -> {
                    Toast.makeText(WifiDirectActivity.this, "File Received!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(WifiDirectActivity.this, ReceivedBackupsActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onFileSent(String fileName) {
                runOnUiThread(() -> Toast.makeText(WifiDirectActivity.this, "Sent: " + fileName, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(WifiDirectActivity.this, message, Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
        updateUIState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiDirectService.clearListener();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private void updateUIState() {
        if (isConnected || WifiDirectService.isConnected) {
            setStatus("● Connected to: " + WifiDirectService.connectedDeviceName, "#2E7D32");
            btnScan.setEnabled(false);
            btnDisconnect.setVisibility(View.VISIBLE);
            btnSendFile.setEnabled(true);
            isScanning = false;
        } else {
            btnDisconnect.setVisibility(View.GONE);
            btnSendFile.setEnabled(false);
            btnScan.setEnabled(true);
            if (isScanning) {
                setStatus("● Scanning...", "#1565C0");
                btnScan.setText("SCANNING...");
            } else if (isConnecting) {
                setStatus("● Connecting...", "#F9A825");
                btnScan.setText("CONNECTING...");
            } else {
                setStatus("● Ready to connect", "#757575");
                btnScan.setText("SCAN DEVICES");
            }
        }
    }

    private void setStatus(String text, String colorHex) {
        statusText.setText(text);
        statusText.setTextColor(Color.parseColor(colorHex));
    }

    private void startScan() {
        if (!checkPermissions()) return;

        android.net.wifi.WifiManager wifiManager =
                (android.net.wifi.WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Please enable WiFi first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }

        if (isConnected || WifiDirectService.isConnected) {
            Toast.makeText(this, "Already connected. Disconnect first.", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        updateUIState();

        if (!checkLocationPermission()) return;

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Scanning...", Toast.LENGTH_SHORT).show();
                handler.postDelayed(() -> {
                    isScanning = false;
                    updateUIState();
                }, 15000);
            }

            @Override
            public void onFailure(int reason) {
                isScanning = false;
                updateUIState();
                handler.postDelayed(() -> {
                    if (!isConnected && !WifiDirectService.isConnected) {
                        startScan();
                    }
                }, 2000);
            }
        });
    }

    private void disconnect() {
        if (manager == null || channel == null) return;

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    isConnected = false;
                    WifiDirectService.isConnected = false;
                    WifiDirectService.isGroupOwner = false;
                    WifiDirectService.connectedHostAddress = null;
                    updateUIState();
                    Toast.makeText(WifiDirectActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(int reason) {
                runOnUiThread(() -> {
                    isConnected = false;
                    WifiDirectService.isConnected = false;
                    WifiDirectService.isGroupOwner = false;
                    WifiDirectService.connectedHostAddress = null;
                    updateUIState();
                    Toast.makeText(WifiDirectActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void connectToDevice(WifiDevice device) {
        if (!checkPermissions()) return;
        if (isConnected || WifiDirectService.isConnected) {
            Toast.makeText(this, "Already connected. Disconnect first.", Toast.LENGTH_SHORT).show();
            return;
        }

        manager.cancelConnect(channel, null);

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                performConnect(device);
            }

            @Override
            public void onFailure(int reason) {
                performConnect(device);
            }
        });
    }

    private void performConnect(WifiDevice device) {
        isConnecting = true;
        isScanning = false;
        updateUIState();

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.address;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 7;

        if (!checkLocationPermission()) return;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                WifiDirectService.connectedDeviceName = device.name;
                runOnUiThread(() -> {
                    Toast.makeText(WifiDirectActivity.this, "Connecting to " + device.name + "...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(int reason) {
                isConnecting = false;
                updateUIState();
                handler.postDelayed(() -> {
                    if (!isConnected && !WifiDirectService.isConnected) {
                        performConnect(device);
                    }
                }, 2000);
            }
        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        deviceList.clear();

        for (WifiP2pDevice device : peers.getDeviceList()) {
            String deviceName = device.deviceName;
            if (deviceName == null || deviceName.isEmpty()) {
                continue;
            }

            if (deviceName.equalsIgnoreCase("Android")) {
                continue;
            }

            deviceList.add(new WifiDevice(device.deviceName, device.deviceAddress, device.status));
        }
        adapter.notifyDataSetChanged();

        if (isScanning && !deviceList.isEmpty()) {
            isScanning = false;
            updateUIState();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_CODE) {
            if (checkLocationPermission()) {
                startScan();
            } else {
                Toast.makeText(this, "Permissions required for WiFi Direct", Toast.LENGTH_LONG).show();
            }
        }
    }
}