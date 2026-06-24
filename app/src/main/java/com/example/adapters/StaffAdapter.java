package com.example.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemStaffBinding;
import com.example.dialogs.ImagePreviewDialog;
import com.example.models.Staff;
import com.example.utils.StaffUtils;

import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder>{

    public interface Listener{
        void onEdit(Staff staff);
        void onDelete(Staff staff);

        void onCall(Staff staff);
        void onDetails(Staff staff);
    }

    private final Listener listener;
    private List<Staff> list = new ArrayList<>();

    private List<Staff> fullList = new ArrayList<>();

    public StaffAdapter(Listener listener){
        this.listener = listener;
    }

    public void setData(List<Staff> staffList){

        list = new ArrayList<>(staffList);

        fullList = new ArrayList<>(staffList);

        notifyDataSetChanged();
    }

    public void filter(String text){

        list.clear();

        if(text.isEmpty()){

            list.addAll(fullList);

        }else{

            text = text.toLowerCase();

            for(Staff s : fullList){

                if(s.getName()
                        .toLowerCase()
                        .contains(text)){

                    list.add(s);
                }
            }
        }

        notifyDataSetChanged();
    }
    class ViewHolder extends RecyclerView.ViewHolder{

        ItemStaffBinding binding;

        ViewHolder(ItemStaffBinding binding){
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        ItemStaffBinding binding =
                ItemStaffBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Staff staff = list.get(position);

        holder.binding.txtName.setText(staff.getName());

        holder.binding.txtMobile1.setText(
                staff.getMobile1());

        holder.binding.txtMobile2.setText(
                staff.getMobile2());

        holder.binding.txtDocument.setText(
                staff.getDocumentType());

        if(staff.getDocumentPath()!=null &&
                !staff.getDocumentPath().isEmpty()){

            holder.binding.imgStaff.setImageURI(
                    Uri.parse(staff.getDocumentPath()));
        }

        holder.binding.imgStaff.setOnClickListener(v -> {
            if (staff.getDocumentPath() != null && !staff.getDocumentPath().isEmpty()) {
                new ImagePreviewDialog(
                        staff.getDocumentPath()
                ).show(
                        ((FragmentActivity) v.getContext())
                                .getSupportFragmentManager(),
                        "doc"
                );
            }
        });

        holder.binding.btnEdit.setOnClickListener(v ->
                listener.onEdit(staff));

        holder.binding.btnCall.setOnClickListener(v ->
                listener.onCall(staff));

        holder.binding.btnWhatsapp.setOnClickListener(v -> {
            StaffUtils.shareOnWhatsapp(
                    v.getContext(),
                    staff
            );
        });

        holder.binding.btnDelete.setOnClickListener(v ->
                listener.onDelete(staff));

        holder.itemView.setOnLongClickListener(v -> {

            listener.onDetails(staff);

            return true;
        });
    }

    public Staff getItem(int position) {

        return list.get(position);

    }

    public List<Staff> getCurrentList() {
        return list;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}