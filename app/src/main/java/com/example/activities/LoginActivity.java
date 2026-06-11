package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {

            String phone = binding.edtPhone.getText()
                    .toString()
                    .trim();

            if (phone.isEmpty()) {
                binding.edtPhone.setError("Enter Mobile Number");
                return;
            }

            if (phone.length() < 10) {
                binding.edtPhone.setError("Enter Valid Mobile Number");
                return;
            }

            Toast.makeText(
                    LoginActivity.this,
                    "Login Successful",
                    Toast.LENGTH_SHORT
            ).show();

            startActivity(
                    new Intent(
                            LoginActivity.this,
                            MainActivity.class
                    )
            );

            finish();
        });
    }
}