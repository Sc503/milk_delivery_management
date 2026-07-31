package com.example.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.models.DeviceModel;

import java.util.ArrayList;
import java.util.List;

public class WifiRepository {
    private static final String TAG = "WifiRepository";
    private static WifiRepository instance;

    private final Context context;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;

    private final MutableLiveData<List<DeviceModel>> discoveredDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> wifiEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> connectionStatusText = new MutableLiveData<>("Disconnected");
    private final MutableLiveData<WifiP2pInfo> connectionInfo = new MutableLiveData<>(null);
    private final MutableLiveData<String> localDeviceName = new MutableLiveData<>("Unknown Device");

    public static synchronized WifiRepository getInstance(Context context) {
        if (instance == null) {
            instance = new WifiRepository(context.getApplicationContext());
        }
        return instance;
    }

    private WifiRepository(Context context) {
        this.context = context;
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            this.channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);
        } else {
            this.channel = null;
        }
    }

    public LiveData<List<DeviceModel>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public LiveData<Boolean> getWifiEnabled() {
        return wifiEnabled;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public LiveData<String> getConnectionStatusText() {
        return connectionStatusText;
    }

    public LiveData<WifiP2pInfo> getConnectionInfo() {
        return connectionInfo;
    }

    public LiveData<String> getLocalDeviceName() {
        return localDeviceName;
    }

    public void setWifiEnabled(boolean enabled) {
        wifiEnabled.postValue(enabled);
        if (!enabled) {
            connectionStatusText.postValue("WiFi Disabled");
            isConnected.postValue(false);
            discoveredDevices.postValue(new ArrayList<>());
        } else {
            connectionStatusText.postValue("Disconnected");
        }
    }

    public void setLocalDevice(WifiP2pDevice device) {
        if (device != null && device.deviceName != null) {
            localDeviceName.postValue(device.deviceName + " (" + device.deviceAddress + ")");
        }
    }

    @SuppressLint("MissingPermission")
    public void discoverPeers(WifiP2pManager.ActionListener listener) {
        if (wifiP2pManager == null || channel == null) {
            if (listener != null) listener.onFailure(WifiP2pManager.ERROR);
            return;
        }

        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discover peers started successfully.");
                if (listener != null) listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Discover peers failed: " + reason);
                if (listener != null) listener.onFailure(reason);
            }
        });
    }

    public void updatePeerList(WifiP2pDeviceList peerList) {
        List<DeviceModel> devices = new ArrayList<>();
        if (peerList != null) {
            for (WifiP2pDevice device : peerList.getDeviceList()) {
                devices.add(new DeviceModel(device));
            }
        }
        discoveredDevices.postValue(devices);
    }

    @SuppressLint("MissingPermission")
    public void connect(DeviceModel device, WifiP2pManager.ActionListener listener) {
        if (wifiP2pManager == null || channel == null || device == null) {
            if (listener != null) listener.onFailure(WifiP2pManager.ERROR);
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.getMacAddress();
        config.groupOwnerIntent = 15; // Prefers to be the sender or let system decide

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connecting to device " + device.getName());
                connectionStatusText.postValue("Connecting to " + device.getName() + "...");
                if (listener != null) listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connect failed: " + reason);
                connectionStatusText.postValue("Failed to connect: " + reason);
                if (listener != null) listener.onFailure(reason);
            }
        });
    }

    public void disconnect(WifiP2pManager.ActionListener listener) {
        if (wifiP2pManager == null || channel == null) {
            if (listener != null) listener.onFailure(WifiP2pManager.ERROR);
            return;
        }

        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnected successfully.");
                isConnected.postValue(false);
                connectionStatusText.postValue("Disconnected");
                connectionInfo.postValue(null);
                discoveredDevices.postValue(new ArrayList<>()); // ✅ Devices list clear करा
                if (listener != null) listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Disconnect failed: " + reason);
                // ✅ तरीही status update करा
                isConnected.postValue(false);
                connectionStatusText.postValue("Disconnected");
                connectionInfo.postValue(null);
                if (listener != null) listener.onFailure(reason);
            }
        });
    }

    public void requestConnectionInfo() {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestConnectionInfo(channel, info -> {
                if (info != null && info.groupFormed) {
                    connectionInfo.postValue(info);
                    isConnected.postValue(true);
                    connectionStatusText.postValue("Connected");
                } else {
                    isConnected.postValue(false);
                    connectionInfo.postValue(null);
                    connectionStatusText.postValue("Disconnected"); // ✅ हे add करा
                }
            });
        }
    }
    public void requestPeers() {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestPeers(channel, this::updatePeerList);
        }
    }
}
