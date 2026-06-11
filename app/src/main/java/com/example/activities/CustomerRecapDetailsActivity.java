package com.example.activities;



import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.ActivityCustomerRecapDetailsBinding;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.adapters.DeliveryHistoryAdapter;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class CustomerRecapDetailsActivity extends AppCompatActivity {

    private ActivityCustomerRecapDetailsBinding binding;
    private MilkViewModel viewModel;
    private DeliveryHistoryAdapter historyAdapter;

    private long customerId;
    private int filterMonthIdx; // 0-based
    private int filterYearInt;  // e.g. 2026

    private final String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerRecapDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        setSupportActionBar(binding.recapDetailsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Retrieve arguments
        customerId = getIntent().getLongExtra("CUSTOMER_ID", -1);
        filterMonthIdx = getIntent().getIntExtra("FILTER_MONTH_INDEX", -1);
        String yearStr = getIntent().getStringExtra("FILTER_YEAR_STRING");

        Calendar c = Calendar.getInstance();
        if (filterMonthIdx == -1) {
            filterMonthIdx = c.get(Calendar.MONTH);
        }
        if (yearStr == null) {
            filterYearInt = c.get(Calendar.YEAR);
        } else {
            filterYearInt = Integer.parseInt(yearStr);
        }

        if (customerId == -1) {
            Toast.makeText(this, "Error: Customer profile not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        loadCustomerProfile();
        updateMonthYearHeader();

        // Month pagination buttons
        binding.btnDetailPrev.setOnClickListener(v -> {
            filterMonthIdx--;
            if (filterMonthIdx < 0) {
                filterMonthIdx = 11;
                filterYearInt--;
            }
            updateMonthYearHeader();
            calculateDetailedHistoryAndStats();
        });

        binding.btnDetailNext.setOnClickListener(v -> {
            filterMonthIdx++;
            if (filterMonthIdx > 11) {
                filterMonthIdx = 0;
                filterYearInt++;
            }
            updateMonthYearHeader();
            calculateDetailedHistoryAndStats();
        });

        binding.fabShowCalendar.setOnClickListener(v -> {
            // Quick jump straight to SCREEN 4 (Customer Calendar)
            Intent calendarIntent = new Intent(CustomerRecapDetailsActivity.this, CustomerCalendarActivity.class);
            calendarIntent.putExtra("CUSTOMER_ID", customerId);
            calendarIntent.putExtra("SELECTED_MONTH", filterMonthIdx);
            calendarIntent.putExtra("SELECTED_YEAR", filterYearInt);
            startActivity(calendarIntent);
        });

        calculateDetailedHistoryAndStats();
    }

    private void setupRecyclerView() {
        binding.rvDeliveryHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new DeliveryHistoryAdapter();
        binding.rvDeliveryHistory.setAdapter(historyAdapter);
    }

    private void loadCustomerProfile() {
        viewModel.getCustomerById(customerId).observe(this, customer -> {
            if (customer != null) {
                binding.detailName.setText(customer.getName());
                binding.detailMobile.setText(customer.getMobile());
                binding.detailAddress.setText(customer.getAddress());
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(customer.getName() + " Recap");
                }
            }
        });
    }

    private void updateMonthYearHeader() {
        binding.txtDetailMonthYear.setText(months[filterMonthIdx] + " " + filterYearInt);
    }

    private void calculateDetailedHistoryAndStats() {
        final int daysInMonth = DateUtils.getDaysInMonth(filterMonthIdx, filterYearInt);
        final String yearMonthPrefix = DateUtils.getYearMonthString(filterMonthIdx, filterYearInt); // YYYY-MM

        viewModel.getRepository().getExecutor().execute(() -> {
            // Step 1: Read all deliveries for this specific customer
            List<Delivery> allCustomerDeliveries = viewModel.getRepository().getDeliveriesForCustomerSync(customerId);
            if (allCustomerDeliveries == null) {
                allCustomerDeliveries = new ArrayList<>();
            }

            // Index them by date string "YYYY-MM-DD" for fast O(1) loop checks
            Map<String, Delivery> dateMap = new HashMap<>();
            for (Delivery d : allCustomerDeliveries) {
                dateMap.put(d.getDeliveryDate(), d);
            }

            // Step 2: Generate lists day-by-day (e.g. 1st to 30th/31st of selected month)
            List<Delivery> detailHistory = new ArrayList<>();
            int deliveredCount = 0;

            for (int day = 1; day <= daysInMonth; day++) {
                String targetDateKey = String.format(Locale.getDefault(), "%s-%02d", yearMonthPrefix, day);
                Delivery recorded = dateMap.get(targetDateKey);

                if (recorded != null) {
                    detailHistory.add(recorded);
                    if ("Delivered".equalsIgnoreCase(recorded.getStatus())) {
                        deliveredCount++;
                    }
                } else {
                    // If no delivery record is saved in SQLite, represent it as Pending
                    detailHistory.add(new Delivery(customerId, targetDateKey, "--", "Pending"));
                }
            }

            // Calculate ratios
            final List<Delivery> finalHistory = detailHistory;
            final int finalDelivered = deliveredCount;
            final int finalPending = daysInMonth - deliveredCount;
            final double finalPercentage = daysInMonth > 0 ? ((double) finalDelivered / daysInMonth) * 100.0 : 0.0;

            // Step 3: Publish variables back to UI
            runOnUiThread(() -> {
                historyAdapter.setData(finalHistory);
                
                binding.txtDetailStatTotal.setText(String.valueOf(daysInMonth));
                binding.txtDetailStatDelivered.setText(String.valueOf(finalDelivered));
                binding.txtDetailStatPending.setText(String.valueOf(finalPending));
                binding.txtDetailStatPercentage.setText(String.format(Locale.getDefault(), "%.2f%%", finalPercentage));
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // return to MonthlyRecap fragment smoothly
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
