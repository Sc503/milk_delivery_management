package com.example.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.databinding.ActivityLoginBinding;
import com.example.models.LoginResponse;
import com.example.models.MyData;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String TAG = "LoginActivity";
    private String selectedUserType = "admin";

    // SharedPreferences keys
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_REMEMBER = "rememberMe";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_STAFF_ID = "staff_id"; //  NEW KEY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //  CHECK IF USER IS ALREADY LOGGED IN
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER, false);
        String userType = prefs.getString(KEY_USER_TYPE, null);
        String mobile = prefs.getString(KEY_MOBILE, null);

        //  If Remember Me is checked and user is logged in, skip login screen
        if (rememberMe && userType != null && mobile != null) {
            // User is already logged in, go directly to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setupUserTypeDropdown();

        // Load saved credentials (auto-fill fields)
        loadSavedCredentials();

        // Login button
        binding.btnLogin.setOnClickListener(v -> performLogin());

        // Create Account link
        binding.txtCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });

        // Forgot Password
        binding.txtForgotPassword.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Forgot Password feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Remember Me checkbox - Save/Clear when toggled
//        binding.checkRemember.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                // Save credentials when checked
//                saveCredentials();
//            } else {
//                // Clear saved credentials when unchecked
//                clearSavedCredentials();
//            }
//        });
    }

    private void loadSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String savedMobile = prefs.getString(KEY_MOBILE, "");
        String savedPassword = prefs.getString(KEY_PASSWORD, "");
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER, false);
        String savedUserType = prefs.getString(KEY_USER_TYPE, "admin");

        // Set Remember Me checkbox
        binding.checkRemember.setChecked(rememberMe);

        // If Remember Me is checked, fill the fields
        if (rememberMe) {
            binding.edtMobile.setText(savedMobile);
            binding.edtPassword.setText(savedPassword);

            // Set user type dropdown
            if (savedUserType.equals("admin") || savedUserType.equals("Owner")) {
                binding.edtUserType.setText("Admin", false);
                selectedUserType = "admin";
            } else {
                binding.edtUserType.setText("Staff", false);
                selectedUserType = "staff";
            }
        }
    }

    private void saveCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String mobile = binding.edtMobile.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(mobile) && !TextUtils.isEmpty(password)) {
            editor.putString(KEY_MOBILE, mobile);
            editor.putString(KEY_PASSWORD, password);
            editor.putString(KEY_USER_TYPE, selectedUserType);
            editor.putBoolean(KEY_REMEMBER, true);
            editor.apply();
            Log.d(TAG, "Credentials saved");
        }
    }

    private void clearSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_MOBILE);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_ACCOUNT_ID);
        editor.remove(KEY_STAFF_ID); //  Also clear staff_id
        editor.putBoolean(KEY_REMEMBER, false);
        editor.apply();
        Log.d(TAG, "Credentials cleared");
    }

    private void setupUserTypeDropdown() {
        String[] userTypes = {"Admin", "Staff"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                userTypes
        );
        AutoCompleteTextView autoCompleteTextView = binding.edtUserType;
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setText("Admin", false);

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            selectedUserType = userTypes[position].toLowerCase();
            Log.d(TAG, "Selected User Type: " + selectedUserType);

            // Update saved user type if Remember Me is checked
            if (binding.checkRemember.isChecked()) {
                saveCredentials();
            }
        });
    }

    private void performLogin() {
        String mobile = binding.edtMobile.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();

        // Validate
        if (TextUtils.isEmpty(mobile)) {
            binding.edtMobile.setError("Enter Mobile Number");
            binding.edtMobile.requestFocus();
            return;
        }

        if (mobile.length() < 10) {
            binding.edtMobile.setError("Enter valid 10-digit mobile number");
            binding.edtMobile.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.edtPassword.setError("Enter Password");
            binding.edtPassword.requestFocus();
            return;
        }

        // Save credentials if Remember Me is checked
        if (binding.checkRemember.isChecked()) {
            saveCredentials();
        } else {
            clearSavedCredentials();
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("Logging in...");

        Log.d(TAG, "User Type: " + selectedUserType);
        Log.d(TAG, "Mobile: " + mobile);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        if (selectedUserType.equals("admin")) {
            Call<LoginResponse> call = apiService.loginAdmin(mobile, password);
            call.enqueue(new LoginCallback("admin"));
        } else {
            showAccountIdDialog(mobile, password);
        }
    }

    private void showAccountIdDialog(String mobile, String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Account ID");

        final EditText input = new EditText(this);
        input.setHint("Account ID");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Login", (dialog, which) -> {
            String accountId = input.getText().toString().trim();
            if (TextUtils.isEmpty(accountId)) {
                Toast.makeText(this, "Please enter Account ID", Toast.LENGTH_SHORT).show();
                binding.btnLogin.setEnabled(true);
                binding.btnLogin.setText("LOGIN");
                return;
            }

            // Save account ID for staff if Remember Me is checked
            if (binding.checkRemember.isChecked()) {
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_ACCOUNT_ID, accountId);
                editor.putString(KEY_MOBILE, mobile);
                editor.putString(KEY_PASSWORD, password);
                editor.putString(KEY_USER_TYPE, "staff");
                editor.putBoolean(KEY_REMEMBER, true);
                editor.apply();
            }

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<LoginResponse> call = apiService.loginStaff(mobile, password, accountId);
            call.enqueue(new LoginCallback("staff"));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText("LOGIN");
            dialog.cancel();
        });

        builder.show();
    }

    private class LoginCallback implements Callback<LoginResponse> {
        private String userType;

        LoginCallback(String userType) {
            this.userType = userType;
        }

        @Override
        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText("LOGIN");

            LoginResponse loginResponse = response.body();

            if (loginResponse.getData() != null) {
                MyData data = loginResponse.getData();

                // Save user data
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();


                    // NEW: Save staff_id for staff users
                    if (data != null) {
                        editor.putString(KEY_ACCOUNT_ID, String.valueOf(data.getId()));
                        editor.putString(KEY_NAME, data.getBusinessName());
                        editor.putString(KEY_MOBILE, data.getMobile());

                        // ✅ THIS MUST BE HERE FOR STAFF USERS
                        if (userType.equals("staff")) {
                            editor.putLong("staff_id", data.getId());  // ← MUST BE "staff_id"
                            Log.d("STAFF_DEBUG", "✅ Staff ID saved: " + data.getId());
                        } else {
                            editor.putLong("staff_id", 0);
                        }
                    }

                // Save userType as "Owner" or "Staff" (matches MainActivity)
                if (userType.equals("admin")) {
                    editor.putString(KEY_USER_TYPE, "Owner");
                } else {
                    editor.putString(KEY_USER_TYPE, "Staff");
                }

                // Keep Remember Me preference
                if (binding.checkRemember.isChecked()) {
                    editor.putBoolean(KEY_REMEMBER, true);
                }

                editor.apply();

                // Go to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<LoginResponse> call, Throwable t) {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText("LOGIN");
            Log.e(TAG, "Network Error: " + t.getMessage());
            Toast.makeText(LoginActivity.this, " Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}