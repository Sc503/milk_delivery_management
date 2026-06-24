package com.example.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogStaffDetailsBinding;
import com.example.models.Staff;

public class StaffDetailsDialog extends DialogFragment {

    private final Staff staff;

    public StaffDetailsDialog(Staff staff) {
        this.staff = staff;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        DialogStaffDetailsBinding binding =
                DialogStaffDetailsBinding.inflate(getLayoutInflater());

        binding.txtName.setText(staff.getName());
        binding.txtMobile1.setText(staff.getMobile1());
        binding.txtMobile2.setText(staff.getMobile2());
        binding.txtAddress.setText(staff.getAddress());
        binding.txtDocumentType.setText(staff.getDocumentType());

        if (staff.getDocumentPath() != null &&
                !staff.getDocumentPath().isEmpty()) {

            binding.imgDocument.setImageURI(
                    Uri.parse(staff.getDocumentPath()));
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        builder.setView(binding.getRoot());

        builder.setPositiveButton("Close", null);

        return builder.create();
    }
}