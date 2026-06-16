package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.databinding.ActivityLoginBinding;
import com.example.models.User;
import com.example.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this)
                .get(LoginViewModel.class);

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

            binding.userTypeLayout.setError(null);
            binding.mobileLayout.setError(null);
            binding.passwordLayout.setError(null);

            String type =
                    binding.edtUserType.getText()
                            .toString()
                            .trim();

            String mobile =
                    binding.edtMobile.getText()
                            .toString()
                            .trim();

            String password =
                    binding.edtPassword.getText()
                            .toString()
                            .trim();

            if (type.isEmpty()) {

                binding.userTypeLayout.setError(
                        "Select User Type");

                return;
            }

            if (mobile.length() != 10) {

                binding.mobileLayout.setError(
                        "Enter Valid Mobile Number");

                return;
            }

            if (password.length() < 4) {

                binding.passwordLayout.setError(
                        "Minimum 4 characters");

                return;
            }

            new Thread(() -> {

                User existingUser =
                        viewModel.getUser(
                                type,
                                mobile
                        );


                // First Login → Create Account
                if (existingUser == null) {

                    User user =
                            new User(
                                    type,
                                    mobile,
                                    password
                            );

                    viewModel.insertUser(user);

                    runOnUiThread(() -> {

                        binding.passwordLayout.setError(null);

                        Toast.makeText(
                                LoginActivity.this,
                                "Account Created Successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        openMain();

                    });

                }

                // Existing User Login
                else {

                    User loginUser =
                            viewModel.login(
                                    type,
                                    mobile,
                                    password
                            );

                    runOnUiThread(() -> {

                        if (loginUser == null) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Wrong Password",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login Successful",
                                    Toast.LENGTH_SHORT
                            ).show();

                            openMain();

                        }

                    });

                }

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

        if (isLoggedIn) {

            startActivity(
                    new Intent(
                            LoginActivity.this,
                            MainActivity.class));

            finish();
        }
    }
    private void openMain() {

        String type =
                binding.edtUserType
                        .getText()
                        .toString()
                        .trim();

        getSharedPreferences(
                "UserSession",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "userType",
                        type
                )
                .putBoolean(
                        "isLoggedIn",
                        true
                )
                .apply();

        startActivity(
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                )
        );

        finish();
    }
}
