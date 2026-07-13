package com.example.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.ActivityPaymentDashboardBinding;
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

public class PaymentDashboard_Activity extends AppCompatActivity {

    private ActivityPaymentDashboardBinding binding;
    private MilkViewModel viewModel;

    private int paid = 0;
    private int pending = 0;
    private double totalCollection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        calculateStats();

        // Export Excel
        binding.btnExportExcel.setOnClickListener(v -> {
            new Thread(() -> {
                File file = ExcelExporter.exportExcel(
                        this,
                        viewModel.getAllCustomersSync(),
                        viewModel
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, "Excel Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });
            }).start();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Payment Dashboard");
        }
    }

    private void calculateStats() {
        paid = 0;
        pending = 0;
        totalCollection = 0;

        new Thread(() -> {
            List<Customer> customers = viewModel.getAllCustomersSync();
            String month = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                    .format(new java.util.Date());

            if (customers != null) {
                for (Customer c : customers) {
                    Payment p = viewModel.getPayment(c.getId(), month);
                    if (p != null && "Paid".equalsIgnoreCase(p.getStatus())) {
                        paid++;
                    } else {
                        pending++;
                    }

                    int deliveredDays = viewModel.getDeliveredDaysCount(c.getId(), month);
                    totalCollection += c.getMilkQuantity() * c.getMilkRate() * deliveredDays;
                }
            }

            final int finalPaid = paid;
            final int finalPending = pending;
            final double finalTotalCollection = totalCollection;
            final int finalCustomers = customers != null ? customers.size() : 0;

            runOnUiThread(() -> {
                binding.txtCustomers.setText("Customers: " + finalCustomers);
                binding.txtPaid.setText("Paid: " + finalPaid);
                binding.txtPending.setText("Pending: " + finalPending);
                binding.txtCollection.setText("₹" + finalTotalCollection + " Total Collection");
                setupChart(finalPaid, finalPending);
            });
        }).start();
    }

    private void setupChart(int paidCount, int pendingCount) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(paidCount, "Paid"));
        entries.add(new PieEntry(pendingCount, "Pending"));

        PieDataSet dataSet = new PieDataSet(entries, "Payments");
        dataSet.setColors(new int[]{0xFF10B981, 0xFFEF4444});

        PieData data = new PieData(dataSet);
        data.setValueTextSize(14f);
        data.setValueTextColor(0xFFFFFFFF);

        binding.pieChart.setData(data);
        binding.pieChart.setCenterText("Milk Flow");
        binding.pieChart.setCenterTextSize(16f);
        binding.pieChart.setCenterTextColor(0xFF1A2332);
        binding.pieChart.setHoleRadius(40f);
        binding.pieChart.setTransparentCircleRadius(45f);
        binding.pieChart.animateY(1000);
        binding.pieChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}