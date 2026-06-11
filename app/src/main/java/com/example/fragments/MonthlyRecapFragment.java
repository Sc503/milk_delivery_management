package com.example.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.FragmentMonthlyRecapBinding;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.activities.CustomerRecapDetailsActivity;
import com.example.adapters.MonthlyRecapAdapter;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyRecapFragment extends Fragment {

    private FragmentMonthlyRecapBinding binding;
    private MilkViewModel viewModel;
    private MonthlyRecapAdapter adapter;

    private final String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final String[] years = {"2025", "2026", "2027", "2028"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMonthlyRecapBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSpinners();
        setupRecyclerView();

        binding.btnFilter.setOnClickListener(v -> runMonthlyCalculation());

        // Run calculation once initially for the current month and year
        runMonthlyCalculation();
    }

    private void setupSpinners() {
        // Populating Month
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinMonth.setAdapter(monthAdapter);

        // Populating Year
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinYear.setAdapter(yearAdapter);

        // Set selections to current date
        Calendar c = Calendar.getInstance();
        binding.spinMonth.setSelection(c.get(Calendar.MONTH));
        
        String currentYearStr = String.valueOf(c.get(Calendar.YEAR));
        for (int i = 0; i < years.length; i++) {
            if (years[i].equals(currentYearStr)) {
                binding.spinYear.setSelection(i);
                break;
            }
        }
    }

    private void setupRecyclerView() {
        binding.rvMonthlyRecap.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MonthlyRecapAdapter(item -> {
            // Jump to Screen 6 (Customer Recap Details Screen) on row tap!
            Intent intent = new Intent(getContext(), CustomerRecapDetailsActivity.class);
            intent.putExtra("CUSTOMER_ID", item.customerId);
            
            // Send selected month and year filters
            intent.putExtra("FILTER_MONTH_INDEX", binding.spinMonth.getSelectedItemPosition());
            intent.putExtra("FILTER_YEAR_STRING", (String) binding.spinYear.getSelectedItem());
            startActivity(intent);
        });
        binding.rvMonthlyRecap.setAdapter(adapter);
    }

    private void runMonthlyCalculation() {
        final int selectedMonthIdx = binding.spinMonth.getSelectedItemPosition(); // 0-based
        final String selectedYear = (String) binding.spinYear.getSelectedItem();
        final int yearInt = Integer.parseInt(selectedYear);

        final int totalDaysInMonth = DateUtils.getDaysInMonth(selectedMonthIdx, yearInt);
        final String yearMonthPrefix = DateUtils.getYearMonthPrefix(selectedMonthIdx, yearInt); // e.g. "2026-06%"

        // Run in repository thread pool to keep UI responsive
        viewModel.getRepository().getExecutor().execute(() -> {

            List<Customer> allCustomers =
                    viewModel.getRepository().getAllCustomersSync();

            if (allCustomers == null) {
                allCustomers = new ArrayList<>();
            }

            final List<Customer> customersList = allCustomers;

            // Step B: Fetch all deliveries matching this month
            List<Delivery> monthDeliveries = viewModel.getRepository().getDeliveriesForMonthSync(yearMonthPrefix);
            if (monthDeliveries == null) {
                monthDeliveries = new ArrayList<>();
            }

            // Index deliveries by Customer ID
            Map<Long, List<Delivery>> customerDeliveriesMap = new HashMap<>();
            for (Delivery d : monthDeliveries) {
                List<Delivery> list = customerDeliveriesMap.get(d.getCustomerId());
                if (list == null) {
                    list = new ArrayList<>();
                    customerDeliveriesMap.put(d.getCustomerId(), list);
                }
                list.add(d);
            }

            // Step C: Aggregate customer stats
            final List<MonthlyRecapAdapter.RecapItem> recapItems = new ArrayList<>();
            int totalDeliveriesSum = 0;
            int totalPendingSum = 0;

            for (Customer customer : customersList) {
                List<Delivery> list = customerDeliveriesMap.get(customer.getId());
                if (list == null) {
                    list = new ArrayList<>();
                }

                int deliveredCount = 0;
                for (Delivery d : list) {
                    if ("Delivered".equalsIgnoreCase(d.getStatus())) {
                        deliveredCount++;
                    }
                }

                int pendingCount = totalDaysInMonth - deliveredCount;
                if (pendingCount < 0) pendingCount = 0;

                double pct = totalDaysInMonth > 0 ? ((double) deliveredCount / totalDaysInMonth) * 100.0 : 0.0;
                
                recapItems.add(new MonthlyRecapAdapter.RecapItem(
                        customer.getId(),
                        customer.getName(),
                        totalDaysInMonth,
                        deliveredCount,
                        pendingCount,
                        pct
                ));

                totalDeliveriesSum += deliveredCount;
                totalPendingSum += pendingCount;
            }

            final int finalTotalCustomers = customersList.size();
            final int finalTotalDeliveries = totalDeliveriesSum;
            final int finalTotalPending = totalPendingSum;
            final double finalAveragePercentage = (finalTotalDeliveries + finalTotalPending) > 0 
                    ? ((double) finalTotalDeliveries / (finalTotalDeliveries + finalTotalPending)) * 100.0 
                    : 0.0;

            // Step D: Publish results to main UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setData(recapItems);
                    
                    binding.recapTotalCustomers.setText(String.valueOf(finalTotalCustomers));
                    binding.recapTotalDeliveries.setText(String.valueOf(finalTotalDeliveries));
                    binding.recapTotalPending.setText(String.valueOf(finalTotalPending));
                    binding.recapAveragePercentage.setText(String.format(Locale.getDefault(), "%.2f%%", finalAveragePercentage));
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
