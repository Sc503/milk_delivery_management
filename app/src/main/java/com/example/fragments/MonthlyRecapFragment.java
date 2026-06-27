package com.example.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.FragmentMonthlyRecapBinding;
import com.example.dialogs.EditCustomerDialog;
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

    private String currentUserType;
    private String currentMobile;

    private final String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final String[] years = {"2025", "2026", "2027", "2028"};

    private int selectedMonth =
            Calendar.getInstance().get(Calendar.MONTH);

    private String selectedYear =
            String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    private String customerFilter = "";
    private int minDeliveriesFilter = 0;
    private int minPendingFilter = 0;

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

        currentUserType =
                requireContext()
                        .getSharedPreferences(
                                "UserSession",
                                android.content.Context.MODE_PRIVATE
                        )
                        .getString(
                                "userType",
                                ""
                        );

        currentMobile =
                requireContext()
                        .getSharedPreferences(
                                "UserSession",
                                android.content.Context.MODE_PRIVATE
                        )
                        .getString(
                                "mobile",
                                ""
                        );


        setupRecyclerView();

        // ROLE BASED UI CONTROL
        if (currentUserType.equals("Customer")) {

            binding.fabFilter.setVisibility(View.GONE);
        }

        binding.fabFilter.setOnClickListener(v -> {
            showFilterBottomSheet();
        });




        runMonthlyCalculation();
    }

    private void showFilterBottomSheet() {

        if (currentUserType.equals("Customer")) {
            return; // Customer ला filter allowed नाही
        }

        FilterBottomSheet sheet =
                new FilterBottomSheet(
                        selectedMonth,
                        selectedYear,
                        customerFilter,
                        minDeliveriesFilter,
                        minPendingFilter,
                        (month, year, customer, deliveries, pending) -> {

                            selectedMonth = month;
                            selectedYear = year;
                            customerFilter = customer;
                            minDeliveriesFilter = deliveries;
                            minPendingFilter = pending;

                            runMonthlyCalculation();
                        });

        sheet.show(getChildFragmentManager(), "FILTER_SHEET");
    }



    private void setupRecyclerView() {

        binding.rvMonthlyRecap.setLayoutManager(
                new LinearLayoutManager(getContext()));

        adapter =
                new MonthlyRecapAdapter(

                        new MonthlyRecapAdapter.OnItemClickListener() {

                            @Override
                            public void onItemClick(
                                    MonthlyRecapAdapter.RecapItem item) {

                                Intent intent =
                                        new Intent(
                                                getContext(),
                                                CustomerRecapDetailsActivity.class);

                                if(currentUserType.equals("Customer")){

                                    intent.putExtra(
                                            "READ_ONLY",
                                            true);

                                }

                                intent.putExtra(
                                        "CUSTOMER_ID",
                                        item.customerId);

                                intent.putExtra(
                                        "FILTER_MONTH_INDEX",
                                        selectedMonth);

                                intent.putExtra(
                                        "FILTER_YEAR_STRING",
                                        selectedYear);

                                startActivity(intent);

                            }
                            @Override
                            public void onEditClick(
                                    MonthlyRecapAdapter.RecapItem item) {

                                viewModel.getRepository()
                                        .getExecutor()
                                        .execute(() -> {

                                            Customer customer =
                                                    viewModel
                                                            .getRepository()
                                                            .getCustomerByIdSync(
                                                                    item.customerId);

                                            if(customer==null){
                                                return;
                                            }

                                            requireActivity().runOnUiThread(() -> {

                                                EditCustomerDialog dialog =
                                                        new EditCustomerDialog(

                                                                customer,

                                                                updatedCustomer -> {

                                                                    viewModel.updateCustomer(
                                                                            updatedCustomer);

                                                                    Toast.makeText(
                                                                            requireContext(),
                                                                            "Customer Updated",
                                                                            Toast.LENGTH_SHORT
                                                                    ).show();

                                                                    runMonthlyCalculation();

                                                                });

                                                dialog.show(
                                                        getChildFragmentManager(),
                                                        "EDIT_CUSTOMER");

                                            });

                                        });


                            }


                        });

        adapter.setOwner(
                currentUserType.equals("Owner"));

        binding.rvMonthlyRecap.setAdapter(adapter);
    }

    private void runMonthlyCalculation() {
        final int selectedMonthIdx = selectedMonth;
        final String selectedYearStr = selectedYear;
        final int yearInt =
                Integer.parseInt(selectedYearStr);

        final int totalDaysInMonth = DateUtils.getDaysInMonth(selectedMonthIdx, yearInt);
        final String yearMonthPrefix = DateUtils.getYearMonthPrefix(selectedMonthIdx, yearInt); // e.g. "2026-06%"

        viewModel.getRepository().getExecutor().execute(() -> {

            List<Customer> allCustomers;

            if(currentUserType.equals("Customer")){

                allCustomers = new ArrayList<>();

                Customer customer =
                        viewModel.getRepository()
                                .getCustomerByMobileSync(
                                        currentMobile
                                );

                if(customer!=null){
                    allCustomers.add(customer);
                }

            }
            else{

                allCustomers =
                        viewModel.getRepository()
                                .getAllCustomersSync();

            }

            if (allCustomers == null) {
                allCustomers = new ArrayList<>();
            }

            final List<Customer> customersList = allCustomers;


            List<Delivery> monthDeliveries = viewModel.getRepository().getDeliveriesForMonthSync(yearMonthPrefix);
            if (monthDeliveries == null) {
                monthDeliveries = new ArrayList<>();
            }


            Map<Long, List<Delivery>> customerDeliveriesMap = new HashMap<>();
            for (Delivery d : monthDeliveries) {
                List<Delivery> list = customerDeliveriesMap.get(d.getCustomerId());
                if (list == null) {
                    list = new ArrayList<>();
                    customerDeliveriesMap.put(d.getCustomerId(), list);
                }
                list.add(d);
            }


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

                if (pendingCount < 0) {
                    pendingCount = 0;
                }

                double pct = totalDaysInMonth > 0
                        ? ((double) deliveredCount / totalDaysInMonth) * 100.0
                        : 0.0;


                if (!customerFilter.trim().isEmpty()
                        && !customer.getName().toLowerCase()
                        .contains(customerFilter.toLowerCase())) {
                    continue;
                }


                if (deliveredCount < minDeliveriesFilter) {
                    continue;
                }


                if (minPendingFilter > 0
                        && pendingCount > minPendingFilter) {
                    continue;
                }

                recapItems.add(
                        new MonthlyRecapAdapter.RecapItem(
                                customer.getId(),
                                customer.getName(),
                                totalDaysInMonth,
                                deliveredCount,
                                pendingCount,
                                pct
                        )
                );

                totalDeliveriesSum += deliveredCount;
                totalPendingSum += pendingCount;
            }

            final int finalTotalCustomers = customersList.size();
            final int finalTotalDeliveries = totalDeliveriesSum;
            final int finalTotalPending = totalPendingSum;
            final double finalAveragePercentage = (finalTotalDeliveries + finalTotalPending) > 0 
                    ? ((double) finalTotalDeliveries / (finalTotalDeliveries + finalTotalPending)) * 100.0 
                    : 0.0;


            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {

                    if (binding == null) return; // 🔥 IMPORTANT FIX

                    adapter.setData(recapItems);

                    binding.recapTotalCustomers.setText(String.valueOf(finalTotalCustomers));
                    binding.recapTotalDeliveries.setText(String.valueOf(finalTotalDeliveries));
                    binding.recapTotalPending.setText(String.valueOf(finalTotalPending));
                    binding.recapAveragePercentage.setText(
                            String.format(Locale.getDefault(), "%.2f%%", finalAveragePercentage)
                    );
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
