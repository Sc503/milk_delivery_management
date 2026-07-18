package com.example.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.ActivityMonthlyRecapBinding;
import com.example.fragments.FilterBottomSheet;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.adapters.MonthlyRecapAdapter;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyRecap_Activity extends AppCompatActivity {

    private ActivityMonthlyRecapBinding binding;
    private MilkViewModel viewModel;
    private MonthlyRecapAdapter adapter;

    private String currentUserType;
    private String currentMobile;

    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH);
    private String selectedYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    private String customerFilter = "";
    private int minDeliveriesFilter = 0;
    private int minPendingFilter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        EdgeToEdge.enable(this);

        binding = ActivityMonthlyRecapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserType = prefs.getString("userType", "");
        currentMobile = prefs.getString("mobile", "");

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        setupRecyclerView();

        binding.ivFilter.setOnClickListener(v -> showFilterBottomSheet());

        runMonthlyCalculation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        runMonthlyCalculation();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Monthly Recap");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goToHomeFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        goToHomeFragment();
        super.onBackPressed();
    }

    private void goToHomeFragment() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("OPEN_HOME_FRAGMENT", true);
        startActivity(intent);
        finish();
    }

    private void showFilterBottomSheet() {
        if (currentUserType.equals("Customer")) {
            Toast.makeText(this, "Filters not available for Customer", Toast.LENGTH_SHORT).show();
            return;
        }

        FilterBottomSheet sheet = new FilterBottomSheet(
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

        sheet.show(getSupportFragmentManager(), "FILTER_SHEET");
    }

    public void showFilterDialog() {
        showFilterBottomSheet();
    }

    private void setupRecyclerView() {
        binding.rvMonthlyRecap.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MonthlyRecapAdapter(
                new MonthlyRecapAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(MonthlyRecapAdapter.RecapItem item) {
                        Intent intent = new Intent(MonthlyRecap_Activity.this, CustomerRecapDetailsActivity.class);

                        if (currentUserType.equals("Customer")) {
                            intent.putExtra("READ_ONLY", true);
                        }

                        intent.putExtra("CUSTOMER_ID", item.customerId);
                        intent.putExtra("FILTER_MONTH_INDEX", selectedMonth);
                        intent.putExtra("FILTER_YEAR_STRING", selectedYear);

                        startActivity(intent);
                    }
                }
        );

        binding.rvMonthlyRecap.setAdapter(adapter);
    }

    private void runMonthlyCalculation() {
        final int selectedMonthIdx = selectedMonth;
        final String selectedYearStr = selectedYear;
        final int yearInt = Integer.parseInt(selectedYearStr);
        final int totalDaysInMonth = DateUtils.getDaysInMonth(selectedMonthIdx, yearInt);
        final String yearMonthPrefix = DateUtils.getYearMonthPrefix(selectedMonthIdx, yearInt);

        //  Get current month and year
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        viewModel.getRepository().getExecutor().execute(() -> {
            List<Customer> allCustomers;

            if (currentUserType.equals("Customer")) {
                allCustomers = new ArrayList<>();
                Customer customer = viewModel.getRepository().getCustomerByMobileSync(currentMobile);
                if (customer != null) {
                    allCustomers.add(customer);
                }
            } else {
                allCustomers = viewModel.getRepository().getAllCustomersSync();
            }

            if (allCustomers == null) {
                allCustomers = new ArrayList<>();
            }

            //  Get deliveries for selected month/year ONLY
            List<Delivery> monthDeliveries = viewModel.getRepository().getDeliveriesForMonthSync(yearMonthPrefix);
            if (monthDeliveries == null) {
                monthDeliveries = new ArrayList<>();
            }

            //  Check if selected month/year is the CURRENT month/year
            boolean isCurrentMonthYear = (selectedMonthIdx == currentMonth && yearInt == currentYear);

            //  Check if ANY deliveries exist for this month/year
            boolean hasDeliveries = !monthDeliveries.isEmpty();

            //  Create map of customer deliveries
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

            for (Customer customer : allCustomers) {
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

                //  Apply customer name filter
                if (!customerFilter.trim().isEmpty()
                        && !customer.getName().toLowerCase().contains(customerFilter.toLowerCase())) {
                    continue;
                }

                //  Apply deliveries filter
                if (deliveredCount < minDeliveriesFilter) {
                    continue;
                }

                //  Apply pending filter
                if (minPendingFilter > 0 && pendingCount > minPendingFilter) {
                    continue;
                }

                //  CRITICAL LOGIC:
                //  1. If selected month/year is CURRENT month/year → Show ALL customers (delivered + pending)
                //  2. If selected month/year has SOME deliveries → Show ONLY customers with deliveries
                //  3. If selected month/year has NO deliveries → Show 0 customers

                if (isCurrentMonthYear) {
                    //  Current Month: Show ALL customers (even with 0 deliveries)
                    recapItems.add(new MonthlyRecapAdapter.RecapItem(
                            customer.getId(),
                            customer.getName(),
                            totalDaysInMonth,
                            deliveredCount,
                            pendingCount,
                            pct
                    ));
                } else if (hasDeliveries && !list.isEmpty()) {
                    //  Selected month has deliveries: Show ONLY customers who have deliveries
                    recapItems.add(new MonthlyRecapAdapter.RecapItem(
                            customer.getId(),
                            customer.getName(),
                            totalDaysInMonth,
                            deliveredCount,
                            pendingCount,
                            pct
                    ));
                }
                //  Else: Skip (no deliveries, not current month)

                totalDeliveriesSum += deliveredCount;
                totalPendingSum += pendingCount;
            }

            //  Use filtered customers count (recapItems.size())
            final int finalTotalCustomers = recapItems.size();
            final int finalTotalDeliveries = totalDeliveriesSum;
            final int finalTotalPending = totalPendingSum;
            final double finalAveragePercentage = (finalTotalDeliveries + finalTotalPending) > 0
                    ? ((double) finalTotalDeliveries / (finalTotalDeliveries + finalTotalPending)) * 100.0
                    : 0.0;

            runOnUiThread(() -> {
                if (binding == null) return;

                adapter.setData(recapItems);

                binding.tvTotalCustomers.setText(String.valueOf(finalTotalCustomers));
                binding.recapTotalCustomers.setText(String.valueOf(finalTotalCustomers));
                binding.recapTotalDeliveries.setText(String.valueOf(finalTotalDeliveries));
                binding.recapTotalPending.setText(String.valueOf(finalTotalPending));
                binding.recapAveragePercentage.setText(
                        String.format(Locale.getDefault(), "%.2f%%", finalAveragePercentage)
                );
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}