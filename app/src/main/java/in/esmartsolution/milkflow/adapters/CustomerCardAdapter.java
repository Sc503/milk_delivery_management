package in.esmartsolution.milkflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;  // ✅ नवीन import
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.models.Customer;

import java.util.List;
import java.util.Map;

public class CustomerCardAdapter extends RecyclerView.Adapter<CustomerCardAdapter.CustomerViewHolder> {

    private List<Customer> customerList;
    private Map<Long, String> statusMap;
    private Map<Long, String> staffNameMap;
    private final CustomerCardListener listener;

    public interface CustomerCardListener {
        void onCall(Customer customer);      // ✅ Call Icon साठी
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
        // ✅ 📱 काढा (कारण आता icon आहे)
        holder.tvMobile.setText(customer.getMobile());
        holder.tvAddress.setText("📍 " + customer.getAddress());

        // Set status
        String status = statusMap != null && statusMap.containsKey(customer.getId())
                ? statusMap.get(customer.getId())
                : "Pending";

        if ("Delivered".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("✅ Delivered");
            holder.tvStatus.setTextColor(0xFF10B981);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.btnDeliver.setText("Undeliver");
            holder.btnDeliver.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444));
        } else {
            holder.tvStatus.setText("⏳ Pending");
            holder.tvStatus.setTextColor(0xFFEF4444);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
            holder.btnDeliver.setText("Deliver");
            holder.btnDeliver.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
        }

        // Set staff name
        String staffName = staffNameMap != null && staffNameMap.containsKey(customer.getId())
                ? staffNameMap.get(customer.getId())
                : "Not assigned";
        holder.tvStaffName.setText("👨‍💼 " + staffName);

        // ✅ Set click listeners - बदललं
        holder.btnCallIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCall(customer);
            }
        });

        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNavigate(customer);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(customer);
            }
        });

        holder.btnDeliver.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeliver(customer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customerList != null ? customerList.size() : 0;
    }

    // ✅ ViewHolder - बदललं
    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMobile, tvAddress, tvStatus, tvStaffName;
        ImageButton btnCallIcon;  // ✅ ImageButton (Button नाही)
        Button btnNavigate, btnEdit, btnDeliver;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.txt_customer_name);
            tvMobile = itemView.findViewById(R.id.txt_customer_mobile);
            tvAddress = itemView.findViewById(R.id.txt_customer_address);
            tvStatus = itemView.findViewById(R.id.txt_customer_status);
            tvStaffName = itemView.findViewById(R.id.txt_customer_staff_name);
            btnCallIcon = itemView.findViewById(R.id.btn_call_icon);  // ✅ नवीन ID
            btnNavigate = itemView.findViewById(R.id.btn_navigate);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDeliver = itemView.findViewById(R.id.btn_deliver);
        }
    }
}