package com.example.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.databinding.ActivityCreateAccountBinding;
import com.example.models.LoginResponse;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAccountActivity extends AppCompatActivity {

    private ActivityCreateAccountBinding binding;
    private static final String TAG = "CreateAccountActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create Account button
        binding.btnCreateAccount.setOnClickListener(v -> createAccount());

        // Back to Login link
        binding.tvLoginLink.setOnClickListener(v -> {
            finish(); // Go back to Login
        });
    }

    private void createAccount() {
        String ownerName = binding.etOwnerName.getText().toString().trim();
        String businessName = binding.etBusinessName.getText().toString().trim();
        String mobile = binding.etMobile.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validate Owner Name
        if (TextUtils.isEmpty(ownerName)) {
            binding.etOwnerName.setError("Enter Owner Name");
            binding.etOwnerName.requestFocus();
            return;
        }

        // Validate Business Name
        if (TextUtils.isEmpty(businessName)) {
            binding.etBusinessName.setError("Enter Business Name");
            binding.etBusinessName.requestFocus();
            return;
        }

        // Validate Mobile
        if (TextUtils.isEmpty(mobile)) {
            binding.etMobile.setError("Enter Mobile Number");
            binding.etMobile.requestFocus();
            return;
        }

        if (mobile.length() < 10) {
            binding.etMobile.setError("Enter valid 10-digit mobile number");
            binding.etMobile.requestFocus();
            return;
        }

        // Validate City
        if (TextUtils.isEmpty(city)) {
            binding.etCity.setError("Enter City");
            binding.etCity.requestFocus();
            return;
        }

        // Validate Address
        if (TextUtils.isEmpty(address)) {
            binding.etAddress.setError("Enter Address");
            binding.etAddress.requestFocus();
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Enter Password");
            binding.etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.etConfirmPassword.setError("Confirm Password");
            binding.etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }

        //  Disable button and show progress
        binding.btnCreateAccount.setEnabled(false);
        binding.btnCreateAccount.setText("Creating...");

        Log.d(TAG, "========== CREATE ACCOUNT ==========");
        Log.d(TAG, "Owner Name: " + ownerName);
        Log.d(TAG, "Business Name: " + businessName);
        Log.d(TAG, "Mobile: " + mobile);
        Log.d(TAG, "City: " + city);
        Log.d(TAG, "Address: " + address);
        Log.d(TAG, "Password: " + password);
        Log.d(TAG, "====================================");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<LoginResponse> call = apiService.createAccount(ownerName, businessName, mobile, city, address, password);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                //  RE-ENABLE BUTTON
                binding.btnCreateAccount.setEnabled(true);
                binding.btnCreateAccount.setText("Create Account");

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "isSuccessful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d(TAG, "Status: " + loginResponse.getStatus());
                    Log.d(TAG, "Message: " + loginResponse.getMessage());
                    Log.d(TAG, "Account ID: " + loginResponse.getAccountID());

                    //  Check if account was created successfully
                    if (loginResponse.getStatus().equals("true") || loginResponse.getStatus().equals(true)) {
                        Toast.makeText(CreateAccountActivity.this,
                                " Account created successfully! \n\nAccount ID: " + loginResponse.getAccountID(),
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Unknown error";
                        Toast.makeText(CreateAccountActivity.this, " " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    //  Handle error response
                    String errorMsg = "Failed to create account";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBody);
                            errorMsg = "Server error: " + response.code();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(CreateAccountActivity.this, " " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                //  RE-ENABLE BUTTON
                binding.btnCreateAccount.setEnabled(true);
                binding.btnCreateAccount.setText("Create Account");

                Log.e(TAG, "Network Error: " + t.getMessage());
                t.printStackTrace();

                Toast.makeText(CreateAccountActivity.this, " Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}