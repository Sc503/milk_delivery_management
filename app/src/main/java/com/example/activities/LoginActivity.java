package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.database.AppDatabase;
import com.example.databinding.ActivityLoginBinding;
import com.example.models.User;
import com.example.viewmodel.LoginViewModel;
import android.app.ProgressDialog;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this)
                .get(LoginViewModel.class);

        progressDialog = new ProgressDialog(this);



        progressDialog.setTitle("Please Wait");

        progressDialog.setMessage("Logging In...");

        progressDialog.setCancelable(false);

        new Thread(() -> {

            AppDatabase db = AppDatabase.getInstance(this);

            db.userDao().insert(new User("Owner","9370734093","admin123"));
            db.userDao().insert(new User("Staff","8888888888","staff123"));
            db.userDao().insert(new User("Customer","9999999999","cust123"));

        }).start();
        String[] userTypes = {
                "Owner",
                "Staff",
                "Customer"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        userTypes
                );

        binding.edtUserType.setAdapter(adapter);

        // Initially disable
        binding.edtMobile.setEnabled(false);
        binding.edtPassword.setEnabled(false);

        binding.mobileLayout.setError("Select User Type First");
        binding.passwordLayout.setError("Select User Type First");

        // Enable after selecting user type
        binding.edtUserType.setOnItemClickListener((parent, view, position, id) -> {

            binding.userTypeLayout.setError(null);
            binding.mobileLayout.setError(null);
            binding.passwordLayout.setError(null);

            binding.edtMobile.setEnabled(true);
            binding.edtPassword.setEnabled(true);

        });

        // Login Button
        binding.btnLogin.setOnClickListener(v -> {

            String type = binding.edtUserType.getText().toString().trim();
            String mobile = binding.edtMobile.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();




            if (type.isEmpty()) {
                binding.userTypeLayout.setError("Select User Type");
                return;
            }

            if (mobile.length() != 10) {
                binding.mobileLayout.setError("Enter Valid Mobile Number");
                return;
            }

            if (password.length() < 4) {
                binding.passwordLayout.setError("Minimum 4 characters");
                return;
            }

            progressDialog.show();

            new Thread(() -> {

                User loginUser =
                        viewModel.login(
                                type,
                                mobile,
                                password
                        );

                runOnUiThread(() -> {

                    progressDialog.dismiss();

                    if (loginUser == null) {

                        binding.passwordLayout.setError(
                                "Invalid User"
                        );

                        return;
                    }

                    openMain(loginUser);

                });

            }).start();

        });

        binding.txtForgotPassword.setOnClickListener(v -> {

            String mobile =
                    binding.edtMobile.getText()
                            .toString()
                            .trim();

            String type =
                    binding.edtUserType.getText()
                            .toString()
                            .trim();

            if (type.isEmpty()) {

                Toast.makeText(
                        this,
                        "Select User Type First",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (mobile.length() != 10) {

                Toast.makeText(
                        this,
                        "Enter Mobile Number First",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            new Thread(() -> {

                User user =
                        viewModel.getUser(
                                type,
                                mobile
                        );

                runOnUiThread(() -> {

                    if (user == null) {

                        Toast.makeText(
                                this,
                                "User not found",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Password")
                                .setMessage("Your password is : " + user.getPassword())
                                .setPositiveButton("OK", null)
                                .show();

                    }

                });

            }).start();

        });

    }



    @Override
    protected void onStart() {
        super.onStart();

        boolean isLoggedIn =
                getSharedPreferences(
                        "UserSession",
                        MODE_PRIVATE)
                        .getBoolean(
                                "isLoggedIn",
                                false);

        boolean rememberMe =
                getSharedPreferences(
                        "UserSession",
                        MODE_PRIVATE)
                        .getBoolean(
                                "rememberMe",
                                false);

        // SAFE AUTO LOGIN LOGIC
        if (isLoggedIn || rememberMe) {

            startActivity(
                    new Intent(
                            LoginActivity.this,
                            MainActivity.class));

            finish();
        }
    }
    private void openMain(User user) {

        SharedPreferences.Editor editor =
                getSharedPreferences(
                        "UserSession",
                        MODE_PRIVATE)
                        .edit();

        editor.putString(
                "userType",
                user.getUserType());

        editor.putString(
                "mobile",
                user.getMobile());

        editor.putBoolean(
                "isLoggedIn",
                true);

        if (binding.checkRemember.isChecked()) {

            editor.putBoolean(
                    "rememberMe",
                    true);

        } else {

            editor.putBoolean(
                    "rememberMe",
                    false);
        }

        editor.apply();

        startActivity(
                new Intent(
                        LoginActivity.this,
                        MainActivity.class));

        finish();
    }




}
