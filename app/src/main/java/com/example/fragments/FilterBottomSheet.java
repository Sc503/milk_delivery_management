package com.example.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterCallback {
        void onApply(
                int month,
                String year,
                String customer,
                int deliveries,
                int pending
        );
    }

    private final int selectedMonth;
    private final String selectedYear;
    private final String customerName;
    private final int deliveries;
    private final int pending;
    private final FilterCallback callback;

    public FilterBottomSheet(
            int selectedMonth,
            String selectedYear,
            String customerName,
            int deliveries,
            int pending,
            FilterCallback callback
    ) {
        this.selectedMonth = selectedMonth;
        this.selectedYear = selectedYear;
        this.customerName = customerName;
        this.deliveries = deliveries;
        this.pending = pending;
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                (com.google.android.material.bottomsheet.BottomSheetDialog)
                        super.onCreateDialog(savedInstanceState);

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_filter, null);

        dialog.setContentView(view);

        Spinner spinMonth =
                view.findViewById(R.id.spinMonthFilter);

        Spinner spinYear =
                view.findViewById(R.id.spinYearFilter);

        EditText etCustomer =
                view.findViewById(R.id.etCustomerName);

        EditText etDeliveries =
                view.findViewById(R.id.etDeliveries);

        EditText etPending =
                view.findViewById(R.id.etPending);

        Button btnApply =
                view.findViewById(R.id.btnApply);

        Button btnClear =
                view.findViewById(R.id.btnClear);

        String[] months = {
                "January","February","March","April",
                "May","June","July","August",
                "September","October","November","December"
        };

        String[] years = {
                "2025","2026","2027","2028"
        };

        ArrayAdapter<String> monthAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        months
                );

        monthAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinMonth.setAdapter(monthAdapter);

        ArrayAdapter<String> yearAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        years
                );

        yearAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinYear.setAdapter(yearAdapter);

        spinMonth.setSelection(selectedMonth);

        for (int i = 0; i < years.length; i++) {
            if (years[i].equals(selectedYear)) {
                spinYear.setSelection(i);
                break;
            }
        }

        etCustomer.setText(customerName);

        if (deliveries > 0) {
            etDeliveries.setText(String.valueOf(deliveries));
        }

        if (pending > 0) {
            etPending.setText(String.valueOf(pending));
        }

        btnApply.setOnClickListener(v -> {

            int month =
                    spinMonth.getSelectedItemPosition();

            String year =
                    spinYear.getSelectedItem().toString();

            String customer =
                    etCustomer.getText().toString().trim();

            int deliveryCount = 0;
            int pendingCount = 0;

            if (!TextUtils.isEmpty(
                    etDeliveries.getText().toString())) {

                deliveryCount =
                        Integer.parseInt(
                                etDeliveries.getText().toString());
            }

            if (!TextUtils.isEmpty(
                    etPending.getText().toString())) {

                pendingCount =
                        Integer.parseInt(
                                etPending.getText().toString());
            }

            callback.onApply(
                    month,
                    year,
                    customer,
                    deliveryCount,
                    pendingCount
            );

            dismiss();
        });

        btnClear.setOnClickListener(v -> {

            callback.onApply(
                    selectedMonth,
                    selectedYear,
                    "",
                    0,
                    0
            );

            dismiss();
        });

        return dialog;
    }
}