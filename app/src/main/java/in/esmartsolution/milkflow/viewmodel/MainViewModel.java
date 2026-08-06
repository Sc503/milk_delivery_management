package in.esmartsolution.milkflow.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import in.esmartsolution.milkflow.models.DeviceModel;
import in.esmartsolution.milkflow.models.FileModel;
import in.esmartsolution.milkflow.models.TransferState;
import in.esmartsolution.milkflow.repository.WifiRepository;
import in.esmartsolution.milkflow.services.FileTransferService;
import in.esmartsolution.milkflow.utils.FileUtils;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final WifiRepository wifiRepository;
    private FileTransferService fileTransferService;

    private final MutableLiveData<FileModel> selectedFile = new MutableLiveData<>(null);
    private final MediatorLiveData<TransferState> transferStateMediator = new MediatorLiveData<>();
    private final MediatorLiveData<String> peerIpMediator = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.wifiRepository = WifiRepository.getInstance(application);

        // Setup initial default idle transfer state
        transferStateMediator.setValue(TransferState.idle());
    }

    public void setFileTransferService(FileTransferService service) {
        this.fileTransferService = service;
        if (service != null) {
            // Unify Service's live transfer updates with our ViewModel LiveData
            transferStateMediator.addSource(service.getTransferState(), transferStateMediator::setValue);
            peerIpMediator.addSource(service.getPeerIpLiveData(), peerIpMediator::setValue);
        }
    }

    public LiveData<List<DeviceModel>> getDiscoveredDevices() {
        return wifiRepository.getDiscoveredDevices();
    }

    public LiveData<Boolean> getWifiEnabled() {
        return wifiRepository.getWifiEnabled();
    }

    public LiveData<Boolean> getIsConnected() {
        return wifiRepository.getIsConnected();
    }

    public LiveData<String> getConnectionStatusText() {
        return wifiRepository.getConnectionStatusText();
    }

    public LiveData<WifiP2pInfo> getConnectionInfo() {
        return wifiRepository.getConnectionInfo();
    }

    public LiveData<String> getLocalDeviceName() {
        return wifiRepository.getLocalDeviceName();
    }

    public LiveData<FileModel> getSelectedFile() {
        return selectedFile;
    }

    public LiveData<TransferState> getTransferState() {
        return transferStateMediator;
    }

    public LiveData<String> getPeerIp() {
        return peerIpMediator;
    }

    // WiFi actions
    public void discoverPeers(WifiP2pManager.ActionListener listener) {
        wifiRepository.discoverPeers(listener);
    }

    public void connectToDevice(DeviceModel device, WifiP2pManager.ActionListener listener) {
        wifiRepository.connect(device, listener);
    }

    public void disconnectDevice(WifiP2pManager.ActionListener listener) {
        wifiRepository.disconnect(listener);
    }

    // File selection
    public void selectFile(Uri uri) {
        if (uri == null) {
            selectedFile.setValue(null);
            return;
        }
        FileModel model = FileUtils.getFileModelFromUri(getApplication(), uri);
        selectedFile.setValue(model);
    }

    public void clearSelectedFile() {
        selectedFile.setValue(null);
    }

    // Transfer actions
    public void sendSelectedFile() {
        FileModel file = selectedFile.getValue();
        if (file == null || fileTransferService == null) {
            Log.e("MainViewModel", "File or service is null");
            return;
        }

        WifiP2pInfo info = wifiRepository.getConnectionInfo().getValue();
        if (info == null || !info.groupFormed) {
            Log.e("MainViewModel", "Not connected to any device");
            return;
        }

        String ipAddress;
        if (info.isGroupOwner) {
            ipAddress = fileTransferService.getConnectedPeerIp();
            if (ipAddress == null) {
                Log.e("MainViewModel", "Peer IP not found. Handshake may have failed.");
                return;
            }
        } else {
            ipAddress = info.groupOwnerAddress.getHostAddress();
        }

        if (ipAddress != null && !ipAddress.isEmpty()) {
            fileTransferService.sendFile(file.getUri(), ipAddress);
            Log.d("MainViewModel", "Sending file to: " + ipAddress);
        } else {
            Log.e("MainViewModel", "Invalid IP address");
        }
    }

    public void cancelTransfer() {
        if (fileTransferService != null) {
            fileTransferService.cancelTransfer();
        }
    }

    public void performHandshakeIfClient() {
        WifiP2pInfo info = wifiRepository.getConnectionInfo().getValue();
        if (info != null && info.groupFormed && !info.isGroupOwner) {
            if (fileTransferService != null) {
                fileTransferService.sendHandshakeToGroupOwner();
                Log.d("MainViewModel", "Handshake sent to Group Owner");
            } else {
                Log.w("MainViewModel", "FileTransferService is null, cannot send handshake");
            }
        }
    }

    @Override
    protected void onCleared() {
        if (fileTransferService != null) {
            transferStateMediator.removeSource(fileTransferService.getTransferState());
            peerIpMediator.removeSource(fileTransferService.getPeerIpLiveData());
        }
        super.onCleared();
    }

    // ✅ Clear transfer state to prevent old dialog from showing
    public void clearTransferState() {
        // ✅ fileTransferService null असला तरी state clear करा
        transferStateMediator.setValue(TransferState.idle());
        Log.d("MainViewModel", "Transfer state cleared");
    }
}
