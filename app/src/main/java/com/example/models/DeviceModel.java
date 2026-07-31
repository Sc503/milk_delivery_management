package com.example.models;

import android.net.wifi.p2p.WifiP2pDevice;

public class DeviceModel {
    private final WifiP2pDevice originalDevice;
    private final String name;
    private final String macAddress;
    private String statusText;
    private int status; // WifiP2pDevice status (AVAILABLE, CONNECTED, INVITED, etc.)

    public DeviceModel(WifiP2pDevice device) {
        this.originalDevice = device;
        this.name = (device.deviceName == null || device.deviceName.isEmpty()) ? "Unnamed Device" : device.deviceName;
        this.macAddress = device.deviceAddress != null ? device.deviceAddress : "Unknown MAC";
        this.status = device.status;
        this.statusText = getStatusString(device.status);
    }

    public WifiP2pDevice getOriginalDevice() {
        return originalDevice;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getStatusText() {
        return statusText;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        this.statusText = getStatusString(status);
    }

    private String getStatusString(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}
