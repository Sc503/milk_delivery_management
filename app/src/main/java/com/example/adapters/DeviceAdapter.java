package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.WifiDevice;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<WifiDevice> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(WifiDevice device);
    }

    public DeviceAdapter(List<WifiDevice> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        WifiDevice device = deviceList.get(position);

        String deviceName = device.name != null ? device.name : "Unknown Device";
        holder.tvDeviceName.setText(deviceName);
        holder.tvDeviceStatus.setText("Tap to connect");
        holder.tvDeviceStatus.setTextColor(android.graphics.Color.parseColor("#3B82F6"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceStatus;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
        }
    }
}
