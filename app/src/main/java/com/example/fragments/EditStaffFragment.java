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

import com.example.R;
import com.example.databinding.FragmentEditStaffBinding;
import com.example.models.LoginResponse;
import com.example.models.Staff;
import com.example.network.ApiClient;
import com.example.network.ApiService;
import com.google.gson.Gson;

import android.content.SharedPreferences;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStaffFragment extends Fragment {

    private FragmentEditStaffBinding binding;
    private Staff staffToEdit;
    private String accountId;
    private static final String TAG = "EditStaffFragment";

    public EditStaffFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditStaffBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //  GET STAFF DATA FROM JSON STRING - ADD THIS HERE
        if (getArguments() != null) {
            String staffJson = getArguments().getString("staff_data_json");
            if (staffJson != null) {
                Gson gson = new Gson();
                staffToEdit = gson.fromJson(staffJson, Staff.class);
            }
        }

        // Get account_id from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        accountId = prefs.getString("account_id", "");

        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
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
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Update button
        binding.btnUpdateStaff.setOnClickListener(v -> updateStaff());
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

        // Validate Password (only if user wants to change it)
        if (!TextUtils.isEmpty(password)) {
            if (password.length() < 3) {
                binding.etPassword.setError("Password must be at least 3 characters");
                binding.etPassword.requestFocus();
                return;
            }

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
        } else {
            // If password is empty, use existing password
            password = staffToEdit.getPassword();
        }

        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(), " Staff updated successfully!", Toast.LENGTH_LONG).show();
                        // Navigate back to staff list
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    } else {
                        String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), " Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), " Failed to update staff", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                binding.btnUpdateStaff.setEnabled(true);
                binding.btnUpdateStaff.setText("Update Staff");
                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(requireContext(), " Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}