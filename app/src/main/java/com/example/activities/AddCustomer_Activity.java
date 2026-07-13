package com.example.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.ActivityAddCustomerBinding;
import com.example.models.Customer;
import com.example.repository.MilkRepository;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddCustomer_Activity extends AppCompatActivity {

    private ActivityAddCustomerBinding binding;
    private MilkViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQ_LOCATION_COORDINATES = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCustomerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Mobile Number -> Only 10 digits
        binding.etMobileNumber.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(10)
        });

        binding.btnGetLocation.setOnClickListener(v -> checkPermissionAndFetchGPS());
        binding.btnSaveCustomer.setOnClickListener(v -> validateAndSaveCustomer());
        binding.btnSearchAddress.setOnClickListener(v -> searchAddressCoordinates());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Customer");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissionAndFetchGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_COORDINATES);
            return;
        }
        fetchCoordinates();
    }

    @SuppressLint("MissingPermission")
    private void fetchCoordinates() {
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                binding.etLatitude.setText(String.valueOf(latitude));
                binding.etLongitude.setText(String.valueOf(longitude));

                Toast.makeText(this, "Location loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Current location not available. Turn ON GPS.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Location Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void searchAddressCoordinates() {
        String addressText = binding.etAddress.getText().toString().trim();

        if (addressText.isEmpty()) {
            Toast.makeText(this, "Enter Address First", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocationName(addressText, 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                binding.etLatitude.setText(String.valueOf(address.getLatitude()));
                binding.etLongitude.setText(String.valueOf(address.getLongitude()));

                Toast.makeText(this, "Location Found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Address Not Found", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error Finding Address", Toast.LENGTH_LONG).show();
        }
    }

    private void validateAndSaveCustomer() {
        final String name = binding.etCustomerName.getText().toString().trim();
        final String mobile = binding.etMobileNumber.getText().toString().trim();
        String rawAddress = binding.etAddress.getText().toString().trim();
        String latStr = binding.etLatitude.getText().toString().trim();
        String lngStr = binding.etLongitude.getText().toString().trim();

        String qtyStr = binding.etMilkQuantity.getText().toString().trim();
        String rateStr = binding.etMilkRate.getText().toString().trim();

        // Validate Name
        if (TextUtils.isEmpty(name)) {
            binding.layCustomerName.setError("Customer Name is required");
            binding.etCustomerName.requestFocus();
            return;
        } else {
            binding.layCustomerName.setError(null);
        }

        // Validate Mobile
        if (TextUtils.isEmpty(mobile)) {
            binding.layMobileNumber.setError("Mobile Number is required");
            binding.etMobileNumber.requestFocus();
            return;
        } else if (!mobile.matches("^[6-9]\\d{9}$")) {
            binding.layMobileNumber.setError("Enter valid 10 digit mobile number");
            binding.etMobileNumber.requestFocus();
            return;
        } else {
            binding.layMobileNumber.setError(null);
        }

        final String address = TextUtils.isEmpty(rawAddress) ? "No Address Specified" : rawAddress;

        // Validate Latitude
        if (TextUtils.isEmpty(latStr)) {
            binding.layLatitude.setError("Required");
            return;
        } else {
            binding.layLatitude.setError(null);
        }

        // Validate Longitude
        if (TextUtils.isEmpty(lngStr)) {
            binding.layLongitude.setError("Required");
            return;
        } else {
            binding.layLongitude.setError(null);
        }

        // Validate Quantity
        if (TextUtils.isEmpty(qtyStr)) {
            binding.etMilkQuantity.setError("Enter Milk Quantity");
            return;
        }

        // Validate Rate
        if (TextUtils.isEmpty(rateStr)) {
            binding.etMilkRate.setError("Enter Milk Rate");
            return;
        }

        double latitude;
        double longitude;
        double quantity;
        double rate;

        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lngStr);
            quantity = Double.parseDouble(qtyStr);
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = DateUtils.getTodayDateString();

        Customer customer = new Customer(
                name,
                mobile,
                address,
                latitude,
                longitude,
                today,
                quantity,
                rate
        );

        viewModel.insertCustomer(customer, new MilkRepository.OnIdReturnedListener() {
            @Override
            public void onIdReturned(long newId) {
                runOnUiThread(() -> {
                    Toast.makeText(AddCustomer_Activity.this,
                            "✅ Customer: " + name + " saved successfully!",
                            Toast.LENGTH_LONG).show();

                    clearFields();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(AddCustomer_Activity.this, "❌ " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void clearFields() {
        binding.etCustomerName.setText("");
        binding.etMobileNumber.setText("");
        binding.etAddress.setText("");
        binding.etLatitude.setText("");
        binding.etLongitude.setText("");
        binding.layCustomerName.setError(null);
        binding.layMobileNumber.setError(null);
        binding.layLatitude.setError(null);
        binding.layLongitude.setError(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION_COORDINATES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCoordinates();
            } else {
                Toast.makeText(this, "Permission denied. Location coordinates must be entered manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}