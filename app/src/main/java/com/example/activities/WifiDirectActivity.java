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
    private static final String TAG = "WiFiDirect";

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

    private WifiDirectService service;
    private boolean isBound = false;

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

        // Start and Bind Service
        Intent svc = new Intent(this, WifiDirectService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        bindService(svc, connection, BIND_AUTO_CREATE);
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
            startActivity(new Intent(this, BackupCenterActivity.class));
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
                    if (manager != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            manager.requestPeers(channel, WifiDirectActivity.this);
                        }
                    }
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    if (manager == null) return;
                    android.net.NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                    if (networkInfo != null && networkInfo.isConnected()) {
                        manager.requestConnectionInfo(channel, info -> {
                            if (info.groupFormed) {
                                if (info.isGroupOwner) {
                                    service.onBecomeHost(info.groupOwnerAddress.getHostAddress());
                                } else {
                                    service.onBecomeClient(info.groupOwnerAddress.getHostAddress(), "");
                                }
                            }
                        });
                    } else {
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
                    updateUIState();
                    String role = asHost ? "HOST (Receiver)" : "CLIENT (Sender)";
                    Toast.makeText(WifiDirectActivity.this, "Connected as " + role, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    isConnecting = false;
                    updateUIState();
                    Toast.makeText(WifiDirectActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFileReceived(String filePath) {
                runOnUiThread(() -> {
                    Toast.makeText(WifiDirectActivity.this, "File Received: " + filePath, Toast.LENGTH_LONG).show();
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
        if (WifiDirectService.isConnected) {
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
                setStatus("● Scanning for devices...", "#1565C0");
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

        isScanning = true;
        updateUIState();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Scanning for devices...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                isScanning = false;
                updateUIState();
                Toast.makeText(WifiDirectActivity.this, "Scan failed: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    WifiDirectService.isConnected = false;
                    WifiDirectService.isGroupOwner = false;
                    WifiDirectService.connectedHostAddress = null;
                    updateUIState();
                });
            }

            @Override
            public void onFailure(int reason) {}
        });
    }

    private void connectToDevice(WifiDevice device) {
        if (!checkPermissions()) return;

        isConnecting = true;
        isScanning = false;
        updateUIState();

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.address;
        config.wps.setup = WpsInfo.PBC;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                WifiDirectService.connectedDeviceName = device.name;
            }

            @Override
            public void onFailure(int reason) {
                isConnecting = false;
                updateUIState();
                Toast.makeText(WifiDirectActivity.this, "Connection failed: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        deviceList.clear();
        for (WifiP2pDevice device : peers.getDeviceList()) {
            deviceList.add(new WifiDevice(device.deviceName, device.deviceAddress, device.status));
        }
        adapter.notifyDataSetChanged();

        if (isScanning) {
            isScanning = false;
            updateUIState();
        }
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
}
