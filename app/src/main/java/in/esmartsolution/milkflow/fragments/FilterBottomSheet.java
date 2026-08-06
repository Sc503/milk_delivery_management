package in.esmartsolution.milkflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import in.esmartsolution.milkflow.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    private Spinner spinMonthFilter, spinYearFilter;
    private TextInputEditText etCustomerName;
    private MaterialButton btnClear, btnApply;

    private OnFilterApplyListener listener;

    private int selectedMonth;
    private String selectedYear;
    private String customerFilter;

    public interface OnFilterApplyListener {
        void onApply(int month, String year, String customerName, int deliveries, int pending);
    }

    public FilterBottomSheet(
            int selectedMonth,
            String selectedYear,
            String customerFilter,
            int minDeliveries,
            int minPending,
            OnFilterApplyListener listener) {
        this.selectedMonth = selectedMonth;
        this.selectedYear = selectedYear;
        this.customerFilter = customerFilter;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // XML name - bottom_sheet_filter (tumcha exact file name)
        View view = inflater.inflate(R.layout.bottom_sheet_filter, container, false);

        initViews(view);
        setupSpinners();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        spinMonthFilter = view.findViewById(R.id.spinMonthFilter);
        spinYearFilter = view.findViewById(R.id.spinYearFilter);
        etCustomerName = view.findViewById(R.id.etCustomerName);
        btnClear = view.findViewById(R.id.btnClear);
        btnApply = view.findViewById(R.id.btnApply);
    }

    private void setupSpinners() {
        // Months
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinMonthFilter.setAdapter(monthAdapter);
        spinMonthFilter.setSelection(selectedMonth);

        // Years
        String[] years = {"2025", "2026", "2027", "2028"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinYearFilter.setAdapter(yearAdapter);

        int yearPosition = 0;
        for (int i = 0; i < years.length; i++) {
            if (years[i].equals(selectedYear)) {
                yearPosition = i;
                break;
            }
        }
        spinYearFilter.setSelection(yearPosition);

        // Set customer name
        etCustomerName.setText(customerFilter);
    }

    private void setupListeners() {
        btnApply.setOnClickListener(v -> {
            int month = spinMonthFilter.getSelectedItemPosition();
            String year = spinYearFilter.getSelectedItem().toString();
            String customerName = etCustomerName.getText().toString().trim();

            if (listener != null) {
                listener.onApply(month, year, customerName, 0, 0);
            }
            dismiss();
        });

        btnClear.setOnClickListener(v -> {
            etCustomerName.setText("");
            spinMonthFilter.setSelection(Calendar.getInstance().get(Calendar.MONTH));

            String[] years = {"2025", "2026", "2027", "2028"};
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int yearPosition = 0;
            for (int i = 0; i < years.length; i++) {
                if (years[i].equals(String.valueOf(currentYear))) {
                    yearPosition = i;
                    break;
                }
            }
            spinYearFilter.setSelection(yearPosition);
        });
    }
}