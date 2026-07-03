package com.example.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.databinding.FragmentPaymentDetailsBinding;
import com.example.models.Customer;
import com.example.models.Payment;
import com.example.utils.PdfGenerator;
import com.example.viewmodel.MilkViewModel;

import androidx.core.content.FileProvider;
import android.net.Uri;
import android.content.Intent;
import android.content.ClipData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PaymentDetailsFragment extends Fragment {

    private FragmentPaymentDetailsBinding binding;
    private MilkViewModel viewModel;
    private long customerId;

    private String selectedMonth;

    private Customer currentCustomer;
    private int deliveredDays;
    private double totalAmount;
    private String paymentStatus = "Pending";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentPaymentDetailsBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity())
                .get(MilkViewModel.class);

        if (getArguments() != null) {
            customerId = getArguments().getLong("customerId");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Calendar calendar = Calendar.getInstance();

        selectedMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(calendar.getTime());

        binding.btnSelectMonth.setText(
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(calendar.getTime()));

        viewModel.getCustomerById(customerId)
                .observe(getViewLifecycleOwner(), this::loadPayment);

        binding.btnSelectMonth.setOnClickListener(v -> {

            String[] months = {
                    "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
                    "2026-07","2026-08","2026-09","2026-10","2026-11","2026-12"
            };

            String[] displayMonths = {
                    "January 2026","February 2026","March 2026","April 2026",
                    "May 2026","June 2026","July 2026","August 2026",
                    "September 2026","October 2026","November 2026","December 2026"
            };

            new AlertDialog.Builder(requireContext())
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

        // PDF
        binding.btnDownloadPdf.setOnClickListener(v -> {

            if (currentCustomer == null) return;

            File file = PdfGenerator.generateInvoice(
                    requireContext(),
                    currentCustomer,
                    deliveredDays,
                    totalAmount,
                    paymentStatus
            );

            if (file != null && file.exists()) {
                Toast.makeText(getContext(),
                        "PDF Saved:\n" + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();

                // Open the PDF
                try {
                    Uri uri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".provider",
                            file
                    );

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(getContext(),
                            "Error opening PDF: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        });

        // SHARE PDF
        binding.btnSharePdf.setOnClickListener(v -> {

            if (currentCustomer == null) return;

            File file = PdfGenerator.generateInvoice(
                    requireContext(),
                    currentCustomer,
                    deliveredDays,
                    totalAmount,
                    paymentStatus
            );

            if (file != null && file.exists()) {
                Uri uri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                
                // Grant permission via ClipData (recommended for Android 5.0+)
                intent.setClipData(ClipData.newRawUri(null, uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Create chooser and grant permission to it
                Intent chooser = Intent.createChooser(intent, "Share Invoice");
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(chooser);
            } else {
                Toast.makeText(getContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        });

        // WHATSAPP
        binding.btnWhatsApp.setOnClickListener(v -> {

            if (currentCustomer == null) return;

            Calendar cal = Calendar.getInstance();
            cal.set(
                    Integer.parseInt(selectedMonth.substring(0,4)),
                    Integer.parseInt(selectedMonth.substring(5,7)) - 1,
                    1
            );

            String month =
                    new SimpleDateFormat(
                            "MMMM yyyy",
                            Locale.getDefault()
                    ).format(cal.getTime());

            String message =
                    "Milk Bill - " + month + "\n\n"
                            + "Customer: " + currentCustomer.getName() + "\n"
                            + "Mobile: " + currentCustomer.getMobile() + "\n"
                            + "Days: " + deliveredDays + "\n"
                            + "Qty: " + currentCustomer.getMilkQuantity() + " L\n"
                            + "Rate: ₹" + currentCustomer.getMilkRate() + "\n"
                            + "Total: ₹" + totalAmount + "\n"
                            + "Status: " + paymentStatus;
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.setPackage("com.whatsapp");
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(getContext(),
                        "WhatsApp not installed",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // FAB SHARE
        binding.fabShare.setOnClickListener(v -> {

            if (currentCustomer == null) {
                Toast.makeText(getContext(),
                        "No customer selected",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.set(
                    Integer.parseInt(selectedMonth.substring(0, 4)),
                    Integer.parseInt(selectedMonth.substring(5, 7)) - 1,
                    1
            );

            String month = new SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
            ).format(cal.getTime());

            String message =
                    "Milk Invoice\n\n"
                            + "Month: " + month + "\n\n"
                            + "Customer: " + currentCustomer.getName() + "\n"
                            + "Mobile: " + currentCustomer.getMobile() + "\n"
                            + "Days: " + deliveredDays + "\n"
                            + "Qty: " + currentCustomer.getMilkQuantity() + "L\n"
                            + "Rate: ₹" + currentCustomer.getMilkRate() + "\n"
                            + "Total: ₹" + totalAmount + "\n"
                            + "Status: " + paymentStatus;

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);

            startActivity(Intent.createChooser(intent, "Share Invoice"));
        });
    }

    // ✅ STEP 9.14.4 - STATUS UI METHOD
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

    private void loadPayment(Customer customer) {

        currentCustomer = customer;

        binding.txtCustomerName.setText(customer.getName());
        binding.txtMobile.setText(customer.getMobile());
        binding.txtRate.setText("Rate : ₹" + customer.getMilkRate());
        binding.txtQty.setText("Quantity : " + customer.getMilkQuantity() + " L");

        new Thread(() -> {

            deliveredDays =
                    viewModel.getDeliveredDaysCount(
                            customerId,
                            selectedMonth
                    );
            totalAmount =
                    deliveredDays *
                            customer.getMilkQuantity() *
                            customer.getMilkRate();

            Payment payment = viewModel.getPayment(customerId, selectedMonth);

            requireActivity().runOnUiThread(() -> {

                binding.txtDays.setText("Delivered Days : " + deliveredDays);
                binding.txtTotal.setText("Total Amount : ₹" + totalAmount);

                if (payment == null) {
                    paymentStatus = "Pending";
                } else {
                    paymentStatus = "Paid";
                }

                updateStatusUI(paymentStatus);

                binding.btnMarkPaid.setOnClickListener(v -> {

                    Payment newPayment =
                            new Payment(
                                    customerId,
                                    selectedMonth,
                                    totalAmount,
                                    "Paid"
                            );

                    // 💾 Save to Room + Firebase (via repository)
                    viewModel.savePayment(newPayment);



                    paymentStatus = "Paid";
                    updateStatusUI(paymentStatus);

                    Toast.makeText(getContext(),
                            "Payment Saved & Backed Up",
                            Toast.LENGTH_SHORT).show();
                });

            });

        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}