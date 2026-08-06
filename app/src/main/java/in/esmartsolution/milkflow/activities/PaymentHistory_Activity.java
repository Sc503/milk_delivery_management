package in.esmartsolution.milkflow.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.esmartsolution.milkflow.adapters.PaymentHistoryAdapter;
import in.esmartsolution.milkflow.databinding.ActivityPaymentHistoryBinding;

import in.esmartsolution.milkflow.models.Payment;
import in.esmartsolution.milkflow.viewmodel.MilkViewModel;

import java.util.List;

public class PaymentHistory_Activity extends AppCompatActivity {

    private ActivityPaymentHistoryBinding binding;
    private MilkViewModel viewModel;
    private long customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        if (getIntent() != null) {
            customerId = getIntent().getLongExtra("customerId", -1);
        }

        binding.rvPaymentHistory.setLayoutManager(new LinearLayoutManager(this));

        PaymentHistoryAdapter adapter = new PaymentHistoryAdapter();
        binding.rvPaymentHistory.setAdapter(adapter);

        loadPaymentHistory(adapter);
    }

    private void loadPaymentHistory(PaymentHistoryAdapter adapter) {
        new Thread(() -> {
            List<Payment> list = viewModel.getPaymentHistory(customerId);
            runOnUiThread(() -> {
                if (list != null && !list.isEmpty()) {
                    adapter.setData(list);
                } else {
                    // Show empty state if needed
                    android.widget.Toast.makeText(this, "No payment history found", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Payment History");
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