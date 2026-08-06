package in.esmartsolution.milkflow.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import in.esmartsolution.milkflow.repository.WifiRepository;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiDirectReceiver";

    private  WifiRepository repository;


    // Default constructor जोडा
    public WifiDirectBroadcastReceiver() {
        // Required for Android system
    }

    public WifiDirectBroadcastReceiver(WifiRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "onReceive action: " + action);


        if (repository == null) {
            repository = WifiRepository.getInstance(context);
        }

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean isEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                repository.setWifiEnabled(isEnabled);
                if (!isEnabled) {
                    Toast.makeText(context, "Wi-Fi P2P is disabled. Please enable Wi-Fi.", Toast.LENGTH_SHORT).show();
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                Log.d(TAG, "Peers list changed. Requesting peers...");
                repository.requestPeers();
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.d(TAG, "Connected. Requesting connection info...");
                    repository.requestConnectionInfo();
                } else {
                    Log.d(TAG, "Disconnected.");
                    repository.disconnect(null);
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                repository.setLocalDevice(device);
                break;
        }
    }
}
