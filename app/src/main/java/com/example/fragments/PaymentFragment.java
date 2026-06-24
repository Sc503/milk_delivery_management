package com.example.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapters.PaymentAdapter;
import com.example.databinding.FragmentPaymentBinding;
import com.example.models.Customer;
import com.example.models.Payment;
import com.example.viewmodel.MilkViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding;
    private MilkViewModel viewModel;
    private PaymentAdapter adapter;

    private List<Customer> allCustomers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentPaymentBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity())
                .get(MilkViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        adapter = new PaymentAdapter(customer -> {

            Bundle bundle = new Bundle();
            bundle.putLong("customerId", customer.getId());

            PaymentDetailsFragment fragment = new PaymentDetailsFragment();
            fragment.setArguments(bundle);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.example.R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.rvPaymentCustomers.setLayoutManager(
                new LinearLayoutManager(getContext()));

        binding.rvPaymentCustomers.setAdapter(adapter);

        // SEARCH
        binding.searchBar.getEditText()
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.filter(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        // LOAD CUSTOMERS
        viewModel.getAllCustomers()
                .observe(getViewLifecycleOwner(), customers -> {
                    allCustomers = customers;
                    adapter.setData(customers);
                    updateDashboardStats(customers);
                });

        // ALL
        binding.chipAll.setOnClickListener(v -> {
            adapter.setData(allCustomers);
            updateDashboardStats(allCustomers);
        });

        // PAID
        binding.chipPaid.setOnClickListener(v -> {

            new Thread(() -> {

                List<Customer> paidCustomers = new ArrayList<>();
                String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        .format(new Date());

                for (Customer customer : allCustomers) {

                    Payment payment = viewModel.getPayment(customer.getId(), month);

                    if (payment != null &&
                            "Paid".equalsIgnoreCase(payment.getStatus())) {
                        paidCustomers.add(customer);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    adapter.setFilteredData(paidCustomers);
                    updateDashboardStats(paidCustomers);
                });

            }).start();
        });

        // PENDING
        binding.chipPending.setOnClickListener(v -> {

            new Thread(() -> {

                List<Customer> pendingCustomers = new ArrayList<>();
                String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        .format(new Date());

                for (Customer customer : allCustomers) {

                    Payment payment = viewModel.getPayment(customer.getId(), month);

                    if (payment == null ||
                            !"Paid".equalsIgnoreCase(payment.getStatus())) {
                        pendingCustomers.add(customer);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    adapter.setFilteredData(pendingCustomers);
                    updateDashboardStats(pendingCustomers);
                });

            }).start();
        });

    }


    // DASHBOARD + PIE UPDATE
    private void updateDashboardStats(List<Customer> customers) {

        if (customers == null) return;

        new Thread(() -> {

            int total = customers.size();
            int paid = 0;
            int pending = 0;
            double totalCollection = 0;

            String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(new Date());

            for (Customer c : customers) {

                Payment payment = viewModel.getPayment(c.getId(), month);

                if (payment != null &&
                        "Paid".equalsIgnoreCase(payment.getStatus())) {
                    paid++;
                } else {
                    pending++;
                }

                totalCollection +=
                        c.getMilkQuantity()
                                * c.getMilkRate()
                                * viewModel.getDeliveredDaysCount(c.getId());
            }

            final int finalTotal = total;
            final int finalPaid = paid;
            final int finalPending = pending;
            final double finalTotalCollection = totalCollection;

            requireActivity().runOnUiThread(() -> {

                binding.txtTotalCustomers.setText("Total Customers : " + finalTotal);
                binding.txtPaidCustomers.setText("Paid : " + finalPaid);
                binding.txtPendingCustomers.setText("Pending : " + finalPending);
                binding.txtTotalCollection.setText("Total Collection : ₹" + finalTotalCollection);


            });

        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}