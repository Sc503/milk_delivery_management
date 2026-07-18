package com.example.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.R;
import com.example.databinding.ActivityEditStaffBinding;
import com.example.models.LoginResponse;
import com.example.models.Staff;
import com.example.network.ApiClient;
import com.example.network.ApiService;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStaff_Activity extends AppCompatActivity {

    private ActivityEditStaffBinding binding;
    private Staff staffToEdit;
    private String accountId;
    private static final String TAG = "EditStaff_Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  THEME CHECK
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        EdgeToEdge.enable(this);

        binding = ActivityEditStaffBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //  Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //  Setup Toolbar
        setupToolbar();

        // Get Staff Data from Intent
        String staffJson = getIntent().getStringExtra("staff_data_json");
        if (staffJson != null) {
            Gson gson = new Gson();
            staffToEdit = gson.fromJson(staffJson, Staff.class);
        }

        // Get account_id from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        accountId = prefs.getString("account_id", "");

        if (accountId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            binding.btnUpdateStaff.setEnabled(false);
        }

        // Populate fields with existing data
        if (staffToEdit != null) {
            binding.etStaffName.setText(staffToEdit.getName());
            binding.etMobile1.setText(staffToEdit.getMobile());
            binding.etStaffId.setText("ID: " + staffToEdit.getId());

            // Set status radio button
            if (staffToEdit.getIsactive() == 1) {
                binding.rbActive.setChecked(true);
            } else {
                binding.rbInactive.setChecked(true);
            }

            // Show current password (masked)
            if (staffToEdit.getPassword() != null && !staffToEdit.getPassword().isEmpty()) {
                binding.etPassword.setText(staffToEdit.getPassword());
            }
        }

        // Cancel button
        binding.btnCancel.setOnClickListener(v -> {
            finish();
        });

        // Update button
        binding.btnUpdateStaff.setOnClickListener(v -> updateStaff());
    }

    //  Setup Toolbar with Back Button
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Staff");
        }
    }

    //  Handle Back Button Click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateStaff() {
        String name = binding.etStaffName.getText().toString().trim();
        String mobile = binding.etMobile1.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Get selected status
        int status = 1; // Default Active
        if (binding.rbInactive.isChecked()) {
            status = 0; // Inactive
        }

        // Validate Name
        if (TextUtils.isEmpty(name)) {
            binding.etStaffName.setError("Enter Staff Name");
            binding.etStaffName.requestFocus();
            return;
        }

        // Validate Mobile
        if (TextUtils.isEmpty(mobile)) {
            binding.etMobile1.setError("Enter Mobile Number");
            binding.etMobile1.requestFocus();
            return;
        }

        if (mobile.length() > 10) {
            binding.etMobile1.setError("Enter valid 10-digit mobile number");
            binding.etMobile1.requestFocus();
            return;
        }

        //  Confirm Password Optional - फक्त Password बदलताना
        if (!TextUtils.isEmpty(password)) {
            // Password टाकला आहे, पण किमान 3 characters हवेत
            if (password.length() < 3) {
                binding.etPassword.setError("Password must be at least 3 characters");
                binding.etPassword.requestFocus();
                return;
            }

            //  Confirm Password टाकला असेल तरच Match Check करा
            if (!TextUtils.isEmpty(confirmPassword)) {
                if (!password.equals(confirmPassword)) {
                    binding.etConfirmPassword.setError("Passwords do not match");
                    binding.etConfirmPassword.requestFocus();
                    return;
                }
            }
            // जर Confirm Password रिकामा असेल तर त्याला Error दाखवू नका
        } else {
            // Password रिकामा असेल तर Existing Password वापरा
            password = staffToEdit.getPassword();
        }

        if (accountId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and show progress
        binding.btnUpdateStaff.setEnabled(false);
        binding.btnUpdateStaff.setText("Updating...");

        Log.d(TAG, "========== UPDATE STAFF API CALL ==========");
        Log.d(TAG, "Staff ID: " + staffToEdit.getId());
        Log.d(TAG, "Account ID: " + accountId);
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Mobile: " + mobile);
        Log.d(TAG, "Password: " + (TextUtils.isEmpty(password) ? "Not changed" : "Changed"));
        Log.d(TAG, "Status: " + status + (status == 1 ? " (Active)" : " (Inactive)"));
        Log.d(TAG, "==========================================");

        // Call API
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<LoginResponse> call = apiService.updateStaff(
                String.valueOf(staffToEdit.getId()),
                accountId,
                name,
                mobile,
                password,
                String.valueOf(status)
        );

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                binding.btnUpdateStaff.setEnabled(true);
                binding.btnUpdateStaff.setText("Update Staff");

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "isSuccessful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d(TAG, "Status: " + loginResponse.getStatus());
                    Log.d(TAG, "Message: " + loginResponse.getMessage());

                    if (loginResponse.getStatus() != null && loginResponse.getStatus()) {
                        Toast.makeText(EditStaff_Activity.this, "✅ Staff updated successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Unknown error";
                        Toast.makeText(EditStaff_Activity.this, "❌ Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(EditStaff_Activity.this, "❌ Failed to update staff", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                binding.btnUpdateStaff.setEnabled(true);
                binding.btnUpdateStaff.setText("Update Staff");
                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(EditStaff_Activity.this, "❌ Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}