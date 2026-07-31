package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.Delivery;
import com.example.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DeliveryHistoryAdapter extends RecyclerView.Adapter<DeliveryHistoryAdapter.HistoryViewHolder> {

    private List<Delivery> deliveries = new ArrayList<>();

    public void setData(List<Delivery> list) {
        this.deliveries = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Delivery item = deliveries.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return deliveries.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView txtDate;
        private final TextView txtStatus;
        private final TextView txtTime;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.history_item_icon);
            txtDate = itemView.findViewById(R.id.history_item_date);
            txtStatus = itemView.findViewById(R.id.history_item_status);
            txtTime = itemView.findViewById(R.id.history_item_time);
        }

        public void bind(final Delivery delivery) {
            // Friendly format date
            txtDate.setText(DateUtils.getFriendlyDateString(delivery.getDeliveryDate()));

            String status = delivery.getStatus();
            txtStatus.setText(status);

            if ("Delivered".equalsIgnoreCase(status)) {
                txtStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.delivered_green));
                iconView.setImageResource(R.drawable.ic_location);
                iconView.setColorFilter(itemView.getContext().getResources().getColor(R.color.delivered_green));
                txtTime.setText(delivery.getDeliveredTime());
                txtTime.setVisibility(View.VISIBLE);
            } else {
                txtStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.pending_red));
                iconView.setImageResource(R.drawable.ic_calendar);
                iconView.setColorFilter(itemView.getContext().getResources().getColor(R.color.pending_red));
                txtTime.setText("--");
                txtTime.setVisibility(View.VISIBLE);
            }
        }
    }
}
