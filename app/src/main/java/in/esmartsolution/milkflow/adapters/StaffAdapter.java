package in.esmartsolution.milkflow.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.activities.EditStaff_Activity;
import in.esmartsolution.milkflow.databinding.ItemStaffBinding;
import in.esmartsolution.milkflow.models.Staff;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    public interface Listener {
        void onCall(Staff staff);
        void onDetails(Staff staff);
    }

    private final Listener listener;
    Context mcontext;
    private List<Staff> list = new ArrayList<>();
    private List<Staff> fullList = new ArrayList<>();

    public StaffAdapter(Listener listener) {
        this.listener = listener;
    }

    public StaffAdapter(Listener listener, Context context) {
        this.listener = listener;
        this.mcontext = context;
    }

    public void setData(List<Staff> staffList) {
        this.list = new ArrayList<>(staffList);
        this.fullList = new ArrayList<>(staffList);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        list.clear();

        if (text.isEmpty()) {
            list.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (Staff s : fullList) {
                if (s.getName().toLowerCase().contains(text) ||
                        s.getMobile().contains(text)) {
                    list.add(s);
                }
            }
        }

        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffBinding binding = ItemStaffBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Staff staff = list.get(position);

        // Set data
        holder.binding.txtNumber.setText(String.valueOf(position + 1));
        holder.binding.tvStaffName.setText(staff.getName());
        holder.binding.tvStaffMobile.setText("📱 " + staff.getMobile());

        // Set Password
        if (staff.getPassword() != null && !staff.getPassword().isEmpty()) {
            holder.binding.tvStaffPassword.setText("🔑 " + staff.getPassword());
            holder.binding.tvStaffPassword.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvStaffPassword.setVisibility(View.GONE);
        }

        // Set status
        if (staff.getIsactive() == 1) {
            holder.binding.tvStaffStatus.setText("Active");
            holder.binding.tvStaffStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.binding.tvStaffStatus.setTextColor(0xFF10B981);
        } else {
            holder.binding.tvStaffStatus.setText("Inactive");
            holder.binding.tvStaffStatus.setBackgroundResource(R.drawable.bg_status_inactive);
            holder.binding.tvStaffStatus.setTextColor(0xFFEF4444);
        }

        // Profile image - placeholder
        holder.binding.imgStaff.setImageResource(R.drawable.ic_person);

        //  FIXED: Edit button - Open EditStaff_Activity
        holder.binding.btnEdit.setOnClickListener(v -> {
            if (mcontext != null) {
                Gson gson = new Gson();
                String staffJson = gson.toJson(staff);

                Intent intent = new Intent(mcontext, EditStaff_Activity.class);
                intent.putExtra("staff_data_json", staffJson);
                mcontext.startActivity(intent);
            }
        });

        // Call button
        holder.binding.btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + staff.getMobile()));
            if (mcontext != null) {
                mcontext.startActivity(intent);
            } else {
                v.getContext().startActivity(intent);
            }
        });

        // Click for details
        holder.itemView.setOnClickListener(v -> {
            listener.onDetails(staff);
        });

        // Long press for details
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDetails(staff);
            return true;
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemStaffBinding binding;

        ViewHolder(ItemStaffBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}