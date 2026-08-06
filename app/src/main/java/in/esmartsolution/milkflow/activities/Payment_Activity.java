package in.esmartsolution.milkflow.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.esmartsolution.milkflow.adapters.PaymentAdapter;
import in.esmartsolution.milkflow.databinding.ActivityPaymentBinding;
import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.models.Payment;
import in.esmartsolution.milkflow.viewmodel.MilkViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Payment_Activity extends AppCompatActivity {

    private ActivityPaymentBinding binding;
    private MilkViewModel viewModel;
    private PaymentAdapter adapter;
    private List<Customer> allCustomers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        adapter = new PaymentAdapter(customer -> {
            // Open PaymentDetails_Activity
            android.content.Intent intent = new android.content.Intent(Payment_Activity.this, PaymentDetails_Activity.class);
            intent.putExtra("customerId", customer.getId());
            startActivity(intent);
        });

        binding.rvPaymentCustomers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPaymentCustomers.setAdapter(adapter);

        // Search
        if (binding.searchBar.getEditText() != null) {
            binding.searchBar.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Load Customers
        viewModel.getAllCustomers().observe(this, customers -> {
            if (customers != null) {
                allCustomers = customers;
                adapter.setData(customers);
                updateDashboardStats(customers);
            }
        });

        // Chip: All
        binding.chipAll.setOnClickListener(v -> {
            adapter.setData(allCustomers);
            updateDashboardStats(allCustomers);
        });

        // Chip: Paid
        binding.chipPaid.setOnClickListener(v -> {
            new Thread(() -> {
                List<Customer> paidCustomers = new ArrayList<>();
                String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

                for (Customer customer : allCustomers) {
                    Payment payment = viewModel.getPayment(customer.getId(), month);
                    if (payment != null && "Paid".equalsIgnoreCase(payment.getStatus())) {
                        paidCustomers.add(customer);
                    }
                }

                runOnUiThread(() -> {
                    adapter.setFilteredData(paidCustomers);
                    updateDashboardStats(paidCustomers);
                });
            }).start();
        });

        // Chip: Pending
        binding.chipPending.setOnClickListener(v -> {
            new Thread(() -> {
                List<Customer> pendingCustomers = new ArrayList<>();
                String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

                for (Customer customer : allCustomers) {
                    Payment payment = viewModel.getPayment(customer.getId(), month);
                    if (payment == null || !"Paid".equalsIgnoreCase(payment.getStatus())) {
                        pendingCustomers.add(customer);
                    }
                }

                runOnUiThread(() -> {
                    adapter.setFilteredData(pendingCustomers);
                    updateDashboardStats(pendingCustomers);
                });
            }).start();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Payments");
        }
    }

    private void updateDashboardStats(List<Customer> customers) {
        if (customers == null) return;

        new Thread(() -> {
            int total = customers.size();
            int paid = 0;
            int pending = 0;
            double totalCollection = 0;

            String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

            for (Customer c : customers) {
                Payment payment = viewModel.getPayment(c.getId(), month);
                if (payment != null && "Paid".equalsIgnoreCase(payment.getStatus())) {
                    paid++;
                } else {
                    pending++;
                }

                int deliveredDays = viewModel.getDeliveredDaysCount(c.getId(), month);
                totalCollection += deliveredDays * c.getMilkQuantity() * c.getMilkRate();
            }

            final int finalTotal = total;
            final int finalPaid = paid;
            final int finalPending = pending;
            final double finalTotalCollection = totalCollection;

            runOnUiThread(() -> {
                binding.txtTotalCustomers.setText("Total Customers : " + finalTotal);
                binding.txtPaidCustomers.setText("Paid : " + finalPaid);
                binding.txtPendingCustomers.setText("Pending : " + finalPending);
                binding.txtTotalCollection.setText("Total Collection : ₹" + finalTotalCollection);
            });
        }).start();
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