package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.widget.ImageButton;

public class MonthlyRecapAdapter extends RecyclerView.Adapter<MonthlyRecapAdapter.RecapViewHolder> {

    private List<RecapItem> itemsList = new ArrayList<>();
    private final OnItemClickListener listener;

    private boolean isOwner = false;

    public interface OnItemClickListener {

        void onItemClick(RecapItem item);

        void onEditClick(RecapItem item);

    }

    public static class RecapItem {
        public long customerId;
        public String customerName;
        public int totalDays;
        public int deliveredCount;
        public int pendingCount;
        public double percentage;

        public RecapItem(long customerId, String customerName, int totalDays, int deliveredCount, int pendingCount, double percentage) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.totalDays = totalDays;
            this.deliveredCount = deliveredCount;
            this.pendingCount = pendingCount;
            this.percentage = percentage;
        }
    }

    public MonthlyRecapAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public void setData(List<RecapItem> items) {
        this.itemsList = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_recap_customer, parent, false);
        return new RecapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecapViewHolder holder, int position) {
        RecapItem item = itemsList.get(position);
        holder.bind(
                item,
                listener,
                isOwner
        );
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    static class RecapViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final TextView txtTotalDays;
        private final TextView txtDelivered;
        private final TextView txtPending;
        private final TextView txtPercentage;

        private final ImageButton btnEditCustomer;



        public RecapViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_recap_customer_name);
            txtTotalDays = itemView.findViewById(R.id.txt_recap_total_days);
            txtDelivered = itemView.findViewById(R.id.txt_recap_delivered_count);
            txtPending = itemView.findViewById(R.id.txt_recap_pending_count);
            txtPercentage = itemView.findViewById(R.id.txt_recap_percentage);
            btnEditCustomer = itemView.findViewById(R.id.btnEditCustomer);
        }

        public void bind(
                final RecapItem item,
                final OnItemClickListener listener,
                boolean isOwner){

            txtName.setText(item.customerName);

            txtTotalDays.setText(
                    String.valueOf(item.totalDays));

            txtDelivered.setText(
                    String.valueOf(item.deliveredCount));

            txtPending.setText(
                    String.valueOf(item.pendingCount));

            txtPercentage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            item.percentage));

            itemView.setOnClickListener(v -> {

                if(listener!=null){

                    listener.onItemClick(item);

                }

            });

            if(isOwner){

                btnEditCustomer.setVisibility(
                        View.VISIBLE);

            }else{

                btnEditCustomer.setVisibility(
                        View.GONE);

            }

            btnEditCustomer.setOnClickListener(v->{

                if(listener!=null){

                    listener.onEditClick(item);

                }

            });

        }
    }
}
