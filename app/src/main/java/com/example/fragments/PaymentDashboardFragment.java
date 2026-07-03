package com.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.databinding.FragmentPaymentDashboardBinding;
import com.example.models.Customer;
import com.example.models.Payment;
import com.example.utils.ExcelExporter;
import com.example.viewmodel.MilkViewModel;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PaymentDashboardFragment extends Fragment {

    private FragmentPaymentDashboardBinding binding;
    private MilkViewModel viewModel;

    private int paid = 0;
    private int pending = 0;
    private double totalCollection = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentPaymentDashboardBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity())
                .get(MilkViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        calculateStats();

        // STEP 9.18.11 — EXPORT EXCEL BUTTON
        binding.btnExportExcel.setOnClickListener(v -> {

            new Thread(() -> {

                File file = ExcelExporter.exportExcel(
                        requireContext(),
                        viewModel.getAllCustomersSync(),
                        viewModel
                );

                requireActivity().runOnUiThread(() -> {

                    Toast.makeText(
                            getContext(),
                            "Excel Saved: " + file.getAbsolutePath(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            }).start();
        });
    }

    private void calculateStats() {


        paid = 0;
        pending = 0;
        totalCollection = 0;

        new Thread(() -> {

            List<Customer> customers = viewModel.getAllCustomersSync();

            String month = new java.text.SimpleDateFormat(
                    "yyyy-MM",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());

            for (Customer c : customers) {

                Payment p = viewModel.getPayment(c.getId(), month);

                if (p != null && "Paid".equalsIgnoreCase(p.getStatus())) {
                    paid++;
                } else {
                    pending++;
                }

                int deliveredDays = viewModel.getDeliveredDaysCount(
                        c.getId(),
                        month
                );

                totalCollection += c.getMilkQuantity()
                        * c.getMilkRate()
                        * deliveredDays;
            }

            requireActivity().runOnUiThread(() -> {

                binding.txtCustomers.setText("Customers: " + customers.size());
                binding.txtPaid.setText("Paid: " + paid);
                binding.txtPending.setText("Pending: " + pending);
                binding.txtCollection.setText("₹" + totalCollection + " Total Collection");

                setupChart();
            });

        }).start();
    }

    private void setupChart() {

        List<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry(paid, "Paid"));
        entries.add(new PieEntry(pending, "Pending"));

        PieDataSet dataSet = new PieDataSet(entries, "Payments");

        PieData data = new PieData(dataSet);

        binding.pieChart.setData(data);
        binding.pieChart.setCenterText("Dairy ERP");
        binding.pieChart.animateY(1000);
        binding.pieChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}