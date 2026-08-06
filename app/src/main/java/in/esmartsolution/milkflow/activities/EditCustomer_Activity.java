package in.esmartsolution.milkflow.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.viewmodel.MilkViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditCustomer_Activity extends AppCompatActivity {

    private MilkViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQ_LOCATION_COORDINATES = 2002;
    private long customerId;
    private Customer currentCustomer;

    //  Views (Without Binding)
    private TextInputEditText etCustomerName, etMobileNumber, etAddress, etLatitude, etLongitude, etMilkQuantity, etMilkRate;
    private TextInputLayout layCustomerName, layMobileNumber, layLatitude, layLongitude;
    private MaterialButton btnSearchAddress, btnGetLocation, btnCancel, btnSaveCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer);

        initViews();

        setupToolbar();

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get Customer ID from Intent
        customerId = getIntent().getLongExtra("CUSTOMER_ID", -1);

        if (customerId == -1) {
            Toast.makeText(this, "Customer not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load Customer Data
        viewModel.getCustomerById(customerId).observe(this, customer -> {
            if (customer != null) {
                currentCustomer = customer;
                populateFields(customer);
            }
        });

        // Search Address
        btnSearchAddress.setOnClickListener(v -> searchAddressCoordinates());

        // Get Current Location
        btnGetLocation.setOnClickListener(v -> checkPermissionAndFetchGPS());

        // Cancel
        btnCancel.setOnClickListener(v -> finish());

        // Save/Update
        btnSaveCustomer.setOnClickListener(v -> updateCustomer());
    }

    private void initViews() {
        etCustomerName = findViewById(R.id.et_customer_name);
        etMobileNumber = findViewById(R.id.et_mobile_number);
        etAddress = findViewById(R.id.et_address);
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etMilkQuantity = findViewById(R.id.et_milk_quantity);
        etMilkRate = findViewById(R.id.et_milk_rate);

        layCustomerName = findViewById(R.id.lay_customer_name);
        layMobileNumber = findViewById(R.id.lay_mobile_number);
        layLatitude = findViewById(R.id.lay_latitude);
        layLongitude = findViewById(R.id.lay_longitude);

        btnSearchAddress = findViewById(R.id.btn_search_address);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSaveCustomer = findViewById(R.id.btn_save_customer);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Customer");
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

    private void populateFields(Customer customer) {
        etCustomerName.setText(customer.getName());
        etMobileNumber.setText(customer.getMobile());
        etAddress.setText(customer.getAddress());
        etLatitude.setText(String.valueOf(customer.getLatitude()));
        etLongitude.setText(String.valueOf(customer.getLongitude()));
        etMilkQuantity.setText(String.valueOf(customer.getMilkQuantity()));
        etMilkRate.setText(String.valueOf(customer.getMilkRate()));
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

                etLatitude.setText(String.valueOf(latitude));
                etLongitude.setText(String.valueOf(longitude));

                Toast.makeText(this, "Location loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Current location not available. Turn ON GPS.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Location Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void searchAddressCoordinates() {
        String addressText = etAddress.getText().toString().trim();

        if (addressText.isEmpty()) {
            Toast.makeText(this, "Enter Address First", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocationName(addressText, 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                etLatitude.setText(String.valueOf(address.getLatitude()));
                etLongitude.setText(String.valueOf(address.getLongitude()));

                Toast.makeText(this, "Location Found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Address Not Found", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error Finding Address", Toast.LENGTH_LONG).show();
        }
    }

    private void updateCustomer() {
        if (currentCustomer == null) {
            Toast.makeText(this, "Customer not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etCustomerName.getText().toString().trim();
        String mobile = etMobileNumber.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String latStr = etLatitude.getText().toString().trim();
        String lngStr = etLongitude.getText().toString().trim();
        String qtyStr = etMilkQuantity.getText().toString().trim();
        String rateStr = etMilkRate.getText().toString().trim();

        // Validate Name
        if (TextUtils.isEmpty(name)) {
            layCustomerName.setError("Customer Name is required");
            etCustomerName.requestFocus();
            return;
        } else {
            layCustomerName.setError(null);
        }

        // Validate Mobile
        if (TextUtils.isEmpty(mobile)) {
            layMobileNumber.setError("Mobile Number is required");
            etMobileNumber.requestFocus();
            return;
        } else if (!mobile.matches("^[6-9]\\d{9}$")) {
            layMobileNumber.setError("Enter valid 10 digit mobile number");
            etMobileNumber.requestFocus();
            return;
        } else {
            layMobileNumber.setError(null);
        }

        // Validate Latitude
        if (TextUtils.isEmpty(latStr)) {
            layLatitude.setError("Required");
            return;
        } else {
            layLatitude.setError(null);
        }

        // Validate Longitude
        if (TextUtils.isEmpty(lngStr)) {
            layLongitude.setError("Required");
            return;
        } else {
            layLongitude.setError(null);
        }

        // Validate Quantity
        if (TextUtils.isEmpty(qtyStr)) {
            etMilkQuantity.setError("Enter Milk Quantity");
            return;
        }

        // Validate Rate
        if (TextUtils.isEmpty(rateStr)) {
            etMilkRate.setError("Enter Milk Rate");
            return;
        }

        double latitude, longitude, quantity, rate;
        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lngStr);
            quantity = Double.parseDouble(qtyStr);
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Customer Object
        currentCustomer.setName(name);
        currentCustomer.setMobile(mobile);
        currentCustomer.setAddress(address);
        currentCustomer.setLatitude(latitude);
        currentCustomer.setLongitude(longitude);
        currentCustomer.setMilkQuantity(quantity);
        currentCustomer.setMilkRate(rate);

        // Save to Database
        viewModel.updateCustomer(currentCustomer);
        Toast.makeText(this, "✅ Customer updated successfully!", Toast.LENGTH_LONG).show();
        finish();
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
    }
}