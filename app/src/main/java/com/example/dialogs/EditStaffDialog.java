package com.example.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogEditStaffBinding;
import com.example.models.Staff;

public class EditStaffDialog extends DialogFragment {

    public interface Listener {
        void onSave(Staff staff);
    }

    private final Staff staff;
    private final Listener listener;

    public EditStaffDialog(
            Staff staff,
            Listener listener) {

        this.staff = staff;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        DialogEditStaffBinding binding =
                DialogEditStaffBinding.inflate(
                        LayoutInflater.from(requireContext()));

        binding.etName.setText(
                staff.getName());

        binding.etMobile1.setText(
                staff.getMobile1());

        binding.etMobile2.setText(
                staff.getMobile2());

        binding.etAddress.setText(
                staff.getAddress());

        return new AlertDialog.Builder(requireContext())

                .setTitle("Edit Staff")

                .setView(binding.getRoot())

                .setPositiveButton(
                        "Save",
                        (dialog, which) -> {

                            staff.setName(
                                    binding.etName
                                            .getText()
                                            .toString());

                            staff.setMobile1(
                                    binding.etMobile1
                                            .getText()
                                            .toString());

                            staff.setMobile2(
                                    binding.etMobile2
                                            .getText()
                                            .toString());

                            staff.setAddress(
                                    binding.etAddress
                                            .getText()
                                            .toString());

                            listener.onSave(staff);

                        })

                .setNegativeButton(
                        "Cancel",
                        null)

                .create();
    }
}