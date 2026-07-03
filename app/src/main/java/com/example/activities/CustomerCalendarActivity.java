package com.example.activities;


import android.content.Intent;
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.net.Uri;

public class CustomerCalendarActivity extends AppCompatActivity {

    private ActivityCustomerCalendarBinding binding;
    private MilkViewModel viewModel;
    private CalendarGridAdapter gridAdapter;

    private long customerId;
    private int currentMonth; // 0-based
    private int currentYear;

    private String currentUserType;

    private boolean readOnly;

    private Customer currentCustomer;

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

        currentUserType =
                getSharedPreferences(
                        "UserSession",
                        MODE_PRIVATE
                )
                        .getString(
                                "userType",
                                ""
                        );

        setSupportActionBar(binding.calendarToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Retrieve arguments
        customerId = getIntent().getLongExtra("CUSTOMER_ID", -1);
        currentMonth = getIntent().getIntExtra("SELECTED_MONTH", -1);
        currentYear = getIntent().getIntExtra("SELECTED_YEAR", -1);

        readOnly =
                getIntent()
                        .getBooleanExtra(
                                "READ_ONLY",
                                false
                        );


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

        // Month paginations
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
        binding.btnScreenshot.setOnClickListener(v -> {
            captureAndShareReport();

        });

        binding.btnDownloadPdf.setOnClickListener(v -> {
            createPdf(false);
        });

        binding.btnSharePdf.setOnClickListener(v -> {
            createPdf(true);
        });

        renderCalendarDaysAndStats();
    }

    private void setupRecyclerView() {
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
    }

    private void loadCustomerProfile() {
        viewModel.getCustomerById(customerId).observe(this, customer -> {
            if (customer != null) {

                currentCustomer = customer;
                binding.customerProfileName.setText(customer.getName());
                binding.customerProfileMobile.setText(customer.getMobile());
                binding.customerProfileAddress.setText(customer.getAddress());
                if(currentUserType.equals("Customer")){

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
        final String yearMonthPrefix = DateUtils.getYearMonthString(currentMonth, currentYear); // YYYY-MM

        // Find weekday offset for the first day of this month (June 1st, 2026 is e.g. Monday)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int weekdayFirst = cal.get(Calendar.DAY_OF_WEEK); // Sunday = 1, Monday = 2 ...

        // Sunday(1)->0 blank cells, Monday(2)->1 blank, ... Saturday(7)->6 blank cells
        final int blankPaddingCells = weekdayFirst - 1;

        viewModel.getRepository().getExecutor().execute(() -> {
            // Read customer deliveries
            List<Delivery> allDeliveries = viewModel.getRepository().getDeliveriesForCustomerSync(customerId);
            if (allDeliveries == null) {
                allDeliveries = new ArrayList<>();
            }

            // Index them by Date key "YYYY-MM-DD"
            Map<String, Delivery> dateLookup = new HashMap<>();
            for (Delivery d : allDeliveries) {

                dateLookup.put(
                        d.getDeliveryDate(),
                        d
                );

                Log.d(
                        "ROW_ID",
                        d.getId()
                                + " | "
                                + d.getCustomerId()
                                + " | "
                                + d.getDeliveryDate()
                                + " | "
                                + d.getStatus()
                );
            }

            final List<CalendarGridAdapter.CalendarDay> gridList = new ArrayList<>();
            int deliveredCount = 0;

            // Step 1: Prepend empty cells for correct column alignments
            for (int p = 0; p < blankPaddingCells; p++) {
                gridList.add(new CalendarGridAdapter.CalendarDay("", 0, "", false));
            }

            // Step 2: Build actual month dates
            String todayStr = DateUtils.getTodayDateString();

            for (int dayNum = 1; dayNum <= totalDaysInMonth; dayNum++) {
                String matchKey = String.format(Locale.getDefault(), "%s-%02d", yearMonthPrefix, dayNum);
                boolean isToday = matchKey.equals(todayStr);

                String status = "Pending"; // default to Pending
                Delivery recorded = dateLookup.get(matchKey);
                if (recorded != null) {
                    status = recorded.getStatus();
                }

                if ("Delivered".equalsIgnoreCase(status)) {
                    deliveredCount++;
                }

                gridList.add(new CalendarGridAdapter.CalendarDay(matchKey, dayNum, status, isToday));
            }

            final int finalDelivered = deliveredCount;
            final int finalPending = totalDaysInMonth - deliveredCount;
            final double finalPercentage = totalDaysInMonth > 0 ? ((double) finalDelivered / totalDaysInMonth) * 100.0 : 0.0;

            int deliveredDaysResult = 0;
            double amountResult = 0;
            double quantityResult = 0;
            double rateResult = 0;

            if (currentCustomer != null) {
                String month = DateUtils.getYearMonthString(currentMonth, currentYear);
                deliveredDaysResult = viewModel.getDeliveredDaysCount(customerId, month);
                quantityResult = currentCustomer.getMilkQuantity();
                rateResult = currentCustomer.getMilkRate();
                amountResult = deliveredDaysResult * quantityResult * rateResult;
            }

            final int finalDeliveredDays = deliveredDaysResult;
            final double finalAmount = amountResult;
            final double finalQuantity = quantityResult;
            final double finalRate = rateResult;

            // Render details back on UI
            runOnUiThread(() -> {
                gridAdapter = new CalendarGridAdapter(gridList, clickedDay -> {
                    if (clickedDay.dayNumber != 0) {
                        // Commercial-grade feature: Toggle delivery status on tapping day!
                        toggleDeliveryState(clickedDay);
                    }
                });
                binding.rvCalendarGrid.setAdapter(gridAdapter);

                // Update text fields
                binding.statTotalDays.setText(String.valueOf(totalDaysInMonth));
                binding.statDeliveredDays.setText(String.valueOf(finalDelivered));
                binding.statPendingDays.setText(String.valueOf(finalPending));
                binding.statDeliveryPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", finalPercentage));

                if (currentCustomer != null) {
                    binding.tvRate.setText("₹" + finalRate);
                    binding.tvQuantity.setText(finalQuantity + " L");
                    binding.tvDeliveredDays.setText(String.valueOf(finalDeliveredDays));
                    binding.tvTotalAmount.setText("₹" + finalAmount);
                }
            });

            for (Delivery d : allDeliveries) {

                dateLookup.put(
                        d.getDeliveryDate(),
                        d
                );

                Log.d(
                        "ROOM_DATA",
                        d.getDeliveryDate() + " -> " + d.getStatus()
                );
            }

        });
    }

    private void toggleDeliveryState(CalendarGridAdapter.CalendarDay day) {

        if(readOnly){

            Toast.makeText(
                    this,
                    "Read Only Mode",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if(currentUserType.equals("Customer")){

            Toast.makeText(
                    this,
                    "Access Denied",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }
        if ("Delivered".equalsIgnoreCase(day.status)) {
            // Switch to Pending
            viewModel.markDeliveryPending(customerId, day.dateString, () -> 
                runOnUiThread(() -> {
                    Toast.makeText(this, "Marked as Pending", Toast.LENGTH_SHORT).show();
                    renderCalendarDaysAndStats();
                })
            );
        } else {
            // Switch to Delivered
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
            finish(); // return back to details page
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void captureAndShareReport() {

        View content = binding.reportContainer.getChildAt(0);

        Bitmap bitmap = Bitmap.createBitmap(
                content.getWidth(),
                content.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        content.draw(canvas);

        saveBitmap(bitmap);

        shareBitmap(bitmap);
    }
    private void saveBitmap(Bitmap bitmap) {

        try {

            File folder = new File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "Reports"
            );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(
                    folder,
                    "CustomerReport_" + System.currentTimeMillis() + ".png"
            );

            FileOutputStream out = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            out.flush();
            out.close();   // <-- हे add कर

            Toast.makeText(
                    this,
                    "Screenshot Saved",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    private void shareBitmap(Bitmap bitmap) {

        try {

            File folder = new File(getCacheDir(), "reports");

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, "MonthlyReport.png");

            FileOutputStream out = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            out.flush();
            out.close();

            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(share, "Share Report"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPdf(boolean share) {

        View view = binding.reportContainer.getChildAt(0);

        Bitmap bitmap = Bitmap.createBitmap(
                view.getWidth(),
                view.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        try {

            File folder = new File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "Reports");

            if (!folder.exists())
                folder.mkdirs();

            File pdfFile = new File(
                    folder,
                    "CustomerReport_" + System.currentTimeMillis() + ".pdf");

            android.graphics.pdf.PdfDocument document =
                    new android.graphics.pdf.PdfDocument();

            android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            1
                    ).create();

            android.graphics.pdf.PdfDocument.Page page =
                    document.startPage(pageInfo);

            page.getCanvas().drawBitmap(bitmap,0,0,null);

            document.finishPage(page);

            FileOutputStream out = new FileOutputStream(pdfFile);

            document.writeTo(out);

            out.close();

            document.close();

            if(share){

                Uri uri =
                        androidx.core.content.FileProvider.getUriForFile(
                                this,
                                getPackageName()+".provider",
                                pdfFile);

                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("application/pdf");

                intent.putExtra(Intent.EXTRA_STREAM,uri);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(intent,"Share PDF"));

            }else{

                Toast.makeText(
                        this,
                        "PDF Saved Successfully",
                        Toast.LENGTH_SHORT
                ).show();
            }

        }catch (Exception e){

            e.printStackTrace();

            Toast.makeText(
                    this,
                    e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
