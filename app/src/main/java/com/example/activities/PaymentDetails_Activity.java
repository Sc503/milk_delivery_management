package com.example.activities;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.ActivityPaymentDetailsBinding;
import com.example.models.Customer;
import com.example.models.Payment;
import com.example.utils.PdfGenerator;
import com.example.viewmodel.MilkViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PaymentDetails_Activity extends AppCompatActivity {

    private ActivityPaymentDetailsBinding binding;
    private MilkViewModel viewModel;
    private long customerId;

    private String selectedMonth;
    private Customer currentCustomer;
    private int deliveredDays;
    private double totalAmount;
    private String paymentStatus = "Pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        if (getIntent() != null) {
            customerId = getIntent().getLongExtra("customerId", -1);
        }

        Calendar calendar = Calendar.getInstance();
        selectedMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
        binding.btnSelectMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));

        viewModel.getCustomerById(customerId).observe(this, this::loadPayment);

        // Month Selector
        binding.btnSelectMonth.setOnClickListener(v -> {
            String[] months = {
                    "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06",
                    "2026-07", "2026-08", "2026-09", "2026-10", "2026-11", "2026-12"
            };
            String[] displayMonths = {
                    "January 2026", "February 2026", "March 2026", "April 2026",
                    "May 2026", "June 2026", "July 2026", "August 2026",
                    "September 2026", "October 2026", "November 2026", "December 2026"
            };

            new AlertDialog.Builder(this)
                    .setTitle("Select Month")
                    .setItems(displayMonths, (dialog, which) -> {
                        selectedMonth = months[which];
                        binding.btnSelectMonth.setText(displayMonths[which]);
                        if (currentCustomer != null) {
                            loadPayment(currentCustomer);
                        }
                    })
                    .show();
        });

        // Mark Paid
        binding.btnMarkPaid.setOnClickListener(v -> {
            Payment newPayment = new Payment(
                    customerId,
                    selectedMonth,
                    totalAmount,
                    "Paid"
            );
            viewModel.savePayment(newPayment);
            paymentStatus = "Paid";
            updateStatusUI(paymentStatus);
            Toast.makeText(this, "Payment Saved & Backed Up", Toast.LENGTH_SHORT).show();
        });

        // PDF Download
        binding.btnDownloadPdf.setOnClickListener(v -> {
            if (currentCustomer == null) return;
            File file = PdfGenerator.generateInvoice(
                    this,
                    currentCustomer,
                    deliveredDays,
                    totalAmount,
                    paymentStatus
            );
            if (file != null && file.exists()) {
                Toast.makeText(this, "PDF Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                try {
                    Uri uri = FileProvider.getUriForFile(
                            this,
                            "com.aistudio.milkdelivery.qyvjpt.provider",  // ✅ हे वापरा
                            file
                    );
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        });

        // Share PDF
        binding.btnSharePdf.setOnClickListener(v -> {
            if (currentCustomer == null) return;
            File file = PdfGenerator.generateInvoice(
                    this,
                    currentCustomer,
                    deliveredDays,
                    totalAmount,
                    paymentStatus
            );
            if (file != null && file.exists()) {
                Uri uri = FileProvider.getUriForFile(
                        this,
                        "com.aistudio.milkdelivery.qyvjpt.provider",  // ✅ हे वापरा
                        file
                );
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(ClipData.newRawUri(null, uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(intent, "Share Invoice");
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(chooser);
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        });

        // WhatsApp
        binding.btnWhatsApp.setOnClickListener(v -> {
            if (currentCustomer == null) return;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(selectedMonth.substring(0, 4)),
                    Integer.parseInt(selectedMonth.substring(5, 7)) - 1, 1);
            String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());

            String message = "Milk Bill - " + month + "\n\n" +
                    "Customer: " + currentCustomer.getName() + "\n" +
                    "Mobile: " + currentCustomer.getMobile() + "\n" +
                    "Days: " + deliveredDays + "\n" +
                    "Qty: " + currentCustomer.getMilkQuantity() + " L\n" +
                    "Rate: ₹" + currentCustomer.getMilkRate() + "\n" +
                    "Total: ₹" + totalAmount + "\n" +
                    "Status: " + paymentStatus;

            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Payment Details");
        }
    }

    private void loadPayment(Customer customer) {
        currentCustomer = customer;
        binding.txtCustomerName.setText(customer.getName());
        binding.txtMobile.setText(customer.getMobile());
        binding.txtRate.setText("Rate : ₹" + customer.getMilkRate());
        binding.txtQty.setText("Quantity : " + customer.getMilkQuantity() + " L");

        new Thread(() -> {
            deliveredDays = viewModel.getDeliveredDaysCount(customerId, selectedMonth);
            totalAmount = deliveredDays * customer.getMilkQuantity() * customer.getMilkRate();

            Payment payment = viewModel.getPayment(customerId, selectedMonth);

            runOnUiThread(() -> {
                binding.txtDays.setText("Delivered Days : " + deliveredDays);
                binding.txtTotal.setText("Total Amount : ₹" + totalAmount);

                if (payment == null) {
                    paymentStatus = "Pending";
                } else {
                    paymentStatus = "Paid";
                }
                updateStatusUI(paymentStatus);
            });
        }).start();
    }

    private void updateStatusUI(String status) {
        if (status == null) return;

        if ("Paid".equalsIgnoreCase(status)) {
            binding.txtStatus.setText("PAID 🟢");
            binding.txtStatus.setTextColor(Color.parseColor("#2E7D32"));
            binding.txtStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
        } else {
            binding.txtStatus.setText("PENDING 🔴");
            binding.txtStatus.setTextColor(Color.parseColor("#C62828"));
            binding.txtStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
        }
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