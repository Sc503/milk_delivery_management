package com.example.activities;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.databinding.ActivityLoginBinding;
import com.example.models.LoginResponse;
import com.example.models.MyData;
import com.example.network.ApiClient;
import com.example.network.ApiService;

//  Media3 Imports
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

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
    private static final String KEY_STAFF_ID = "staff_id";
    private static final String KEY_OWNER_NAME = "owner_name";
    private static final String KEY_STAFF_NAME = "staff_name";
    private static final String KEY_BUSINESS_NAME = "business_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_HEADER_NAME = "header_name";

    //  Media3 ExoPlayer
    private ExoPlayer exoPlayer;

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

        //  NEW: How to Use App? Button
        binding.tvHowToUseApp.setOnClickListener(v -> showVideoDialog());
    }

    //  Video Dialog Function with Media3 ExoPlayer
    private void showVideoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_video_player);
        dialog.setCancelable(false);

        // Set dialog width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        PlayerView playerView = dialog.findViewById(R.id.playerView);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        Button btnOk = dialog.findViewById(R.id.btnOk);

        //  Create Media3 ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        //  Play video from res/raw/milkflowtutorial.mp4
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.milkflowtutorial);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);

        //  Close listener
        View.OnClickListener closeListener = v -> {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
            dialog.dismiss();
        };

        btnClose.setOnClickListener(closeListener);
        btnOk.setOnClickListener(closeListener);

        dialog.setOnDismissListener(d -> {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        });

        dialog.show();
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
        editor.remove(KEY_NAME);
        editor.remove(KEY_HEADER_NAME);
        editor.remove(KEY_OWNER_NAME);
        editor.remove(KEY_STAFF_NAME);
        editor.remove(KEY_BUSINESS_NAME);
        editor.remove(KEY_ROLE);
        editor.remove(KEY_STAFF_ID);
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
                Log.d(TAG, "✅ Staff account_id saved: " + accountId);
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

            if (response.isSuccessful() && response.body() != null) {
                LoginResponse loginResponse = response.body();

                if (loginResponse.getData() != null) {
                    MyData data = loginResponse.getData();

                    // Save user data
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    if (data != null) {
                        editor.putString(KEY_MOBILE, data.getMobile());

                        if (userType.equals("admin")) {
                            // ============================================
                            // Owner Login
                            // ============================================
                            editor.putString(KEY_HEADER_NAME, data.getOwnername());
                            editor.putString(KEY_BUSINESS_NAME, data.getBusinessName());
                            editor.putString(KEY_OWNER_NAME, data.getOwnername());
                            editor.putString(KEY_ROLE, "Owner");
                            editor.putString(KEY_NAME, data.getBusinessName());

                            Log.d(TAG, "✅ Owner header_name: " + data.getOwnername());
                            Log.d(TAG, "✅ Owner business_name: " + data.getBusinessName());

                            if (data.getId() != null) {
                                editor.putString(KEY_ACCOUNT_ID, String.valueOf(data.getId()));
                                Log.d(TAG, "✅ Owner account_id saved: " + data.getId());
                            } else {
                                editor.putString(KEY_ACCOUNT_ID, "0");
                                Log.e(TAG, "❌ Owner ID is null!");
                            }

                            // Owner has NO staff_id
                            editor.putLong(KEY_STAFF_ID, 0);

                        } else {
                            // ============================================
                            //  Staff Login
                            // ============================================
                            editor.putString(KEY_HEADER_NAME, data.getName());
                            editor.putString(KEY_STAFF_NAME, data.getName());
                            editor.putString(KEY_ROLE, "Staff");
                            editor.putString(KEY_NAME, data.getName());

                            Log.d(TAG, "✅ Staff header_name: " + data.getName());

                            if (data.getBusinessName() != null && !data.getBusinessName().isEmpty()) {
                                editor.putString(KEY_BUSINESS_NAME, data.getBusinessName());
                                Log.d(TAG, "✅ Staff business_name: " + data.getBusinessName());
                            } else {
                                editor.putString(KEY_BUSINESS_NAME, "");
                                Log.d(TAG, "⏳ Staff business_name: Empty");
                            }

                            if (data.getAccountId() != null) {
                                editor.putString(KEY_ACCOUNT_ID, String.valueOf(data.getAccountId()));
                                Log.d(TAG, "✅ Staff account_id saved: " + data.getAccountId());
                            } else {
                                editor.putString(KEY_ACCOUNT_ID, "0");
                                Log.e(TAG, "❌ Staff account_id is null!");
                            }

                            //  SAVE STAFF ID
                            if (data.getId() != null) {
                                editor.putLong(KEY_STAFF_ID, data.getId());
                                Toast.makeText(LoginActivity.this, "✅ Staff ID saved: " + data.getId(), Toast.LENGTH_LONG).show();
                                Log.d(TAG, "✅ Staff ID saved: " + data.getId());
                            } else {
                                editor.putLong(KEY_STAFF_ID, 0);
                                Log.e(TAG, "❌ Staff ID is null!");
                            }
                        }
                    }

                    //  Save user type
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

                    Log.d(TAG, "✅ Login Successful!");
                    Log.d(TAG, "User Type: " + prefs.getString(KEY_USER_TYPE, ""));
                    Log.d(TAG, "Header Name: " + prefs.getString(KEY_HEADER_NAME, ""));
                    Log.d(TAG, "Business Name: " + prefs.getString(KEY_BUSINESS_NAME, ""));
                    Log.d(TAG, "Account ID: " + prefs.getString(KEY_ACCOUNT_ID, ""));
                    Log.d(TAG, "Staff ID: " + prefs.getLong(KEY_STAFF_ID, 0));

                    //  Go to MainActivity with CLEAR_TASK flag
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "❌ Login failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<LoginResponse> call, Throwable t) {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText("LOGIN");
            Log.e(TAG, "Network Error: " + t.getMessage());
            Toast.makeText(LoginActivity.this, "❌ Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}