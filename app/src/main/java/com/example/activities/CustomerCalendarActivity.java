package com.example.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.R;
import com.example.databinding.ActivityCustomerCalendarBinding;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.adapters.CalendarGridAdapter;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerCalendarActivity extends AppCompatActivity {

    private ActivityCustomerCalendarBinding binding;
    private MilkViewModel viewModel;
    private CalendarGridAdapter gridAdapter;

    private long customerId;
    private int currentMonth;
    private int currentYear;

    private String currentUserType;
    private boolean readOnly;

    private final String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        currentUserType = getSharedPreferences("UserSession", MODE_PRIVATE)
                .getString("userType", "");

        setSupportActionBar(binding.calendarToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        customerId = getIntent().getLongExtra("CUSTOMER_ID", -1);
        currentMonth = getIntent().getIntExtra("SELECTED_MONTH", -1);
        currentYear = getIntent().getIntExtra("SELECTED_YEAR", -1);
        readOnly = getIntent().getBooleanExtra("READ_ONLY", false);

        Calendar cal = Calendar.getInstance();
        if (currentMonth == -1) {
            currentMonth = cal.get(Calendar.MONTH);
        }
        if (currentYear == -1) {
            currentYear = cal.get(Calendar.YEAR);
        }

        if (customerId == -1) {
            Toast.makeText(this, "Customer profile not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        loadCustomerProfile();
        updateMonthYearHeader();

        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            updateMonthYearHeader();
            renderCalendarDaysAndStats();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            updateMonthYearHeader();
            renderCalendarDaysAndStats();
        });

        renderCalendarDaysAndStats();
    }

    private void setupRecyclerView() {
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
    }

    private void loadCustomerProfile() {
        viewModel.getCustomerById(customerId).observe(this, customer -> {
            if (customer != null) {
                binding.customerProfileName.setText(customer.getName());
                binding.customerProfileMobile.setText(customer.getMobile());
                binding.customerProfileAddress.setText(customer.getAddress());

                if (currentUserType.equals("Customer")) {
                    binding.customerProfileMobile.setVisibility(View.GONE);
                }

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(customer.getName() + " Calendar");
                }
            }
        });
    }

    private void updateMonthYearHeader() {
        binding.txtCalendarMonthYear.setText(months[currentMonth] + " " + currentYear);
    }

    private void renderCalendarDaysAndStats() {
        final int totalDaysInMonth = DateUtils.getDaysInMonth(currentMonth, currentYear);
        final String yearMonthPrefix = DateUtils.getYearMonthString(currentMonth, currentYear);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int weekdayFirst = cal.get(Calendar.DAY_OF_WEEK);
        final int blankPaddingCells = weekdayFirst - 1;

        viewModel.getRepository().getExecutor().execute(() -> {
            List<Delivery> allDeliveries = viewModel.getRepository().getDeliveriesForCustomerSync(customerId);
            if (allDeliveries == null) {
                allDeliveries = new ArrayList<>();
            }

            Map<String, Delivery> dateLookup = new HashMap<>();
            for (Delivery d : allDeliveries) {
                dateLookup.put(d.getDeliveryDate(), d);
            }

            final List<CalendarGridAdapter.CalendarDay> gridList = new ArrayList<>();
            int deliveredCount = 0;

            // Empty padding cells
            for (int p = 0; p < blankPaddingCells; p++) {
                gridList.add(new CalendarGridAdapter.CalendarDay("", 0, "", false));
            }

            String todayStr = DateUtils.getTodayDateString();

            for (int dayNum = 1; dayNum <= totalDaysInMonth; dayNum++) {
                String matchKey = String.format(Locale.getDefault(), "%s-%02d", yearMonthPrefix, dayNum);
                boolean isToday = matchKey.equals(todayStr);

                String status = "Pending";
                Delivery recorded = dateLookup.get(matchKey);
                if (recorded != null) {
                    String existingStatus = recorded.getStatus();
                    if (existingStatus != null && !existingStatus.isEmpty()) {
                        status = existingStatus;
                    }
                }

                if ("Delivered".equalsIgnoreCase(status)) {
                    deliveredCount++;
                }

                gridList.add(new CalendarGridAdapter.CalendarDay(matchKey, dayNum, status, isToday));
            }

            final int finalDelivered = deliveredCount;
            final int finalPending = totalDaysInMonth - deliveredCount;
            final double finalPercentage = totalDaysInMonth > 0 ? ((double) finalDelivered / totalDaysInMonth) * 100.0 : 0.0;

            runOnUiThread(() -> {
                // ✅ Set adapter
                gridAdapter = new CalendarGridAdapter(gridList, clickedDay -> {
                    if (clickedDay.dayNumber != 0) {
                        toggleDeliveryState(clickedDay);
                    }
                });
                binding.rvCalendarGrid.setAdapter(gridAdapter);

                // ✅ Update stats
                binding.statTotalDays.setText(String.valueOf(totalDaysInMonth));
                binding.statDeliveredDays.setText(String.valueOf(finalDelivered));
                binding.statPendingDays.setText(String.valueOf(finalPending));
                binding.statDeliveryPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", finalPercentage));
            });
        });
    }

    private void toggleDeliveryState(CalendarGridAdapter.CalendarDay day) {
        if (readOnly) {
            Toast.makeText(this, "Read Only Mode", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserType.equals("Customer")) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Delivered".equalsIgnoreCase(day.status)) {
            viewModel.markDeliveryPending(customerId, day.dateString, () ->
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Marked as Pending", Toast.LENGTH_SHORT).show();
                        renderCalendarDaysAndStats();
                    })
            );
        } else {
            String nowTime = DateUtils.getCurrentTimeString();
            viewModel.deliverCustomer(customerId, day.dateString, nowTime, () ->
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Marked as Delivered!", Toast.LENGTH_SHORT).show();
                        renderCalendarDaysAndStats();
                    })
            );
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}