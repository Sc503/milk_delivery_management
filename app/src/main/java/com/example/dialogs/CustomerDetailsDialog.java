package com.example.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogCustomerDetailsBinding;
import com.example.models.Customer;

public class CustomerDetailsDialog extends DialogFragment {

    private final Customer customer;
    private final DialogCallback callback;
    private DialogCustomerDetailsBinding binding;

    public interface DialogCallback {
        void onDeliver(Customer customer);
        void onCancel();
    }

    public CustomerDetailsDialog(Customer customer, DialogCallback callback) {
        this.customer = customer;
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCustomerDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind data values
        binding.dialogTxtName.setText(customer.getName());
        binding.dialogTxtMobile.setText(customer.getMobile());
        binding.dialogTxtAddress.setText(customer.getAddress());


        // Cancel click hook
        binding.dialogBtnCancel.setOnClickListener(v -> {
            if (callback != null) {
                callback.onCancel();
            }
            dismiss();
        });

        // Deliver click hook
        binding.dialogBtnDeliver.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDeliver(customer);
            }
            dismiss();
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Remove background frame padding so card corners can be drawn smoothly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
