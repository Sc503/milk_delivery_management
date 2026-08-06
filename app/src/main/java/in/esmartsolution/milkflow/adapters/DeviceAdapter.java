package in.esmartsolution.milkflow.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.databinding.ItemDeviceBinding;
import in.esmartsolution.milkflow.models.DeviceModel;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onConnectClick(DeviceModel device);
    }

    private final List<DeviceModel> devices = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void updateDevices(List<DeviceModel> newDevices) {
        // Use DiffUtil for smooth visual transitions and minimum updates
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return devices.size();
            }

            @Override
            public int getNewListSize() {
                return newDevices.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return devices.get(oldItemPosition).getMacAddress().equals(newDevices.get(newItemPosition).getMacAddress());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                DeviceModel oldDev = devices.get(oldItemPosition);
                DeviceModel newDev = newDevices.get(newItemPosition);
                return oldDev.getStatus() == newDev.getStatus() &&
                        oldDev.getName().equals(newDev.getName());
            }
        });

        devices.clear();
        devices.addAll(newDevices);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceBinding binding = ItemDeviceBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new DeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeviceBinding binding;

        public DeviceViewHolder(@NonNull ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DeviceModel device) {
            binding.tvDeviceName.setText(device.getName());
            binding.tvDeviceAddress.setText(device.getMacAddress());
            binding.tvDeviceStatus.setText(device.getStatusText());

            // Color status and buttons dynamically based on availability
            if (device.getStatus() == WifiP2pDevice.CONNECTED) {
                binding.tvDeviceStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.colorSuccess));
                binding.ivSignalStrength.setImageResource(R.drawable.ic_check_circle);
                binding.ivSignalStrength.setColorFilter(itemView.getContext().getResources().getColor(R.color.colorSuccess));
                binding.btnConnect.setVisibility(View.GONE);
            } else if (device.getStatus() == WifiP2pDevice.INVITED) {
                binding.tvDeviceStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.colorAccent));
                binding.ivSignalStrength.setImageResource(R.drawable.ic_info);
                binding.ivSignalStrength.setColorFilter(itemView.getContext().getResources().getColor(R.color.colorAccent));
                binding.btnConnect.setVisibility(View.GONE);
            } else {
                binding.tvDeviceStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.colorTextSecondary));
                binding.ivSignalStrength.setImageResource(R.drawable.ic_wifi);
                binding.ivSignalStrength.setColorFilter(itemView.getContext().getResources().getColor(R.color.colorPrimary));
                binding.btnConnect.setVisibility(View.VISIBLE);
                binding.btnConnect.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onConnectClick(device);
                    }
                });
            }
        }
    }
}
