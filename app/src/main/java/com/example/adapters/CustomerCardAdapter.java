package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.Customer;

import java.util.List;
import java.util.Map;

public class CustomerCardAdapter extends RecyclerView.Adapter<CustomerCardAdapter.CustomerViewHolder> {

    private List<Customer> customerList;
    private Map<Long, String> statusMap;
    private Map<Long, String> staffNameMap;  // ✅ NEW
    private final CustomerCardListener listener;

    public interface CustomerCardListener {
        void onCall(Customer customer);
        void onNavigate(Customer customer);
        void onDeliver(Customer customer);
        void onEdit(Customer customer);
    }

    public CustomerCardAdapter(List<Customer> customerList, CustomerCardListener listener) {
        this.customerList = customerList;
        this.listener = listener;
    }

    public void updateData(List<Customer> newList, Map<Long, String> newStatusMap, Map<Long, String> newStaffNameMap) {
        this.customerList = newList;
        this.statusMap = newStatusMap;
        this.staffNameMap = newStaffNameMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_card, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customerList.get(position);

        holder.tvName.setText(customer.getName());
        holder.tvMobile.setText("📱 " + customer.getMobile());
        holder.tvAddress.setText("📍 " + customer.getAddress());

        // Set status
        String status = statusMap != null && statusMap.containsKey(customer.getId())
                ? statusMap.get(customer.getId())
                : "Pending";

        if ("Delivered".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("✅ Delivered");
            holder.tvStatus.setTextColor(0xFF10B981);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        } else {
            holder.tvStatus.setText("⏳ Pending");
            holder.tvStatus.setTextColor(0xFFEF4444);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
        }

        // ✅ SET STAFF NAME
        String staffName = staffNameMap != null && staffNameMap.containsKey(customer.getId())
                ? staffNameMap.get(customer.getId())
                : "Not assigned";
        holder.tvStaffName.setText("👨‍💼 " + staffName);

        // Set click listeners
        holder.btnCall.setOnClickListener(v -> listener.onCall(customer));
        holder.btnNavigate.setOnClickListener(v -> listener.onNavigate(customer));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(customer));
        holder.btnDeliver.setOnClickListener(v -> listener.onDeliver(customer));
    }

    @Override
    public int getItemCount() {
        return customerList != null ? customerList.size() : 0;
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMobile, tvAddress, tvStatus, tvStaffName;
        Button btnCall, btnNavigate, btnEdit, btnDeliver;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.txt_customer_name);
            tvMobile = itemView.findViewById(R.id.txt_customer_mobile);
            tvAddress = itemView.findViewById(R.id.txt_customer_address);
            tvStatus = itemView.findViewById(R.id.txt_customer_status);
            tvStaffName = itemView.findViewById(R.id.txt_customer_staff_name);  // ✅ NEW
            btnCall = itemView.findViewById(R.id.btn_call);
            btnNavigate = itemView.findViewById(R.id.btn_navigate);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDeliver = itemView.findViewById(R.id.btn_deliver);
        }
    }
}