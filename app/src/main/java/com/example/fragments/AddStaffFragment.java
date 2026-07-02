package com.example.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.databinding.FragmentAddStaffBinding;
import com.example.models.LoginResponse;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import android.content.SharedPreferences;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddStaffFragment extends Fragment {

    private FragmentAddStaffBinding binding;
    private String accountId;
    private static final String TAG = "AddStaffFragment";

    public AddStaffFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddStaffBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get account_id from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        accountId = prefs.getString("account_id", "");

        Log.d(TAG, "Account ID from SharedPreferences: " + accountId);

        resetButtonState();

        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
            binding.btnSaveStaff.setEnabled(false);
        }

        // Cancel button
        binding.btnCancel.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Save button
        binding.btnSaveStaff.setOnClickListener(v -> saveStaff());
    }

    private void resetButtonState() {
        binding.btnSaveStaff.setEnabled(true);
        binding.btnSaveStaff.setText("Create Staff");
    }

    private void saveStaff() {
        String name = binding.etStaffName.getText().toString().trim();
        String mobile = binding.etMobile1.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Mobile: " + mobile);
        Log.d(TAG, "Password: " + password);
        Log.d(TAG, "Confirm Password: " + confirmPassword);
        Log.d(TAG, "Account ID: " + accountId);

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

        if (mobile.length() < 10) {
            binding.etMobile1.setError("Enter valid 10-digit mobile number");
            binding.etMobile1.requestFocus();
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Enter Password");
            binding.etPassword.requestFocus();
            return;
        }

        if (password.length() < 3) {
            binding.etPassword.setError("Password must be at least 3 characters");
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

        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Disable button and show progress
        binding.btnSaveStaff.setEnabled(false);
        binding.btnSaveStaff.setText("Creating...");

        Log.d(TAG, "========== API CALL DETAILS ==========");
        Log.d(TAG, "URL: https://smartmr.in/milkflowapp/createstaff.php");
        Log.d(TAG, "Parameters:");
        Log.d(TAG, "  account_id: " + accountId);
        Log.d(TAG, "  name: " + name);
        Log.d(TAG, "  mobile: " + mobile);
        Log.d(TAG, "  password: " + password);
        Log.d(TAG, "======================================");

        // Call API
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<LoginResponse> call = apiService.createStaff(accountId, name, mobile, password);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                // ✅ Re-enable button FIRST
                resetButtonState();

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "isSuccessful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // ✅ Get status - returns Boolean
                    Boolean status = loginResponse.getStatus();
                    String message = loginResponse.getMessage();

                    Log.d(TAG, "Status: " + status);
                    Log.d(TAG, "Message: " + message);

                    // ✅ Check if status is true
                    if (status != null && status) {
                        // ✅ Success
                        String msg = message != null ? message : "Staff created successfully!";
                        Toast.makeText(requireContext(), "✅ " + msg, Toast.LENGTH_LONG).show();
                        clearFields();

                        // Navigate back to staff list
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    } else {
                        // ❌ Error from API
                        String msg = message != null ? message : "Failed to create staff";
                        Toast.makeText(requireContext(), "❌ " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // ❌ Response not successful
                    String errorMsg = "Failed to create staff";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBody);
                            errorMsg = "Server error: " + response.code();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(requireContext(), "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // ✅ Re-enable button on network error
                resetButtonState();

                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(requireContext(), "❌ Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearFields() {
        binding.etStaffName.setText("");
        binding.etMobile1.setText("");
        binding.etPassword.setText("");
        binding.etConfirmPassword.setText("");
        binding.etAddress.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}