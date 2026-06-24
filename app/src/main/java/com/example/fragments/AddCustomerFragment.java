package com.example.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.FragmentAddCustomerBinding;
import com.example.models.Customer;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


import java.util.Arrays;
import java.util.List;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddCustomerFragment extends Fragment {

    private FragmentAddCustomerBinding binding;
    private MilkViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQ_LOCATION_COORDINATES = 2002;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddCustomerBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        binding.btnGetLocation.setOnClickListener(v -> checkPermissionAndFetchGPS());
        binding.btnSaveCustomer.setOnClickListener(v -> validateAndSaveCustomer());
        binding.btnSearchAddress.setOnClickListener(v -> {searchAddressCoordinates();
        });


    }

    private void checkPermissionAndFetchGPS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_COORDINATES);
            return;
        }
        fetchCoordinates();
    }

    @SuppressLint("MissingPermission")
    private void fetchCoordinates() {

        Toast.makeText(getContext(),
                "Getting current location...",
                Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                binding.etLatitude.setText(String.valueOf(latitude));
                binding.etLongitude.setText(String.valueOf(longitude));

                Toast.makeText(getContext(),
                        "Location loaded successfully",
                        Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(getContext(),
                        "Current location not available. Turn ON GPS.",
                        Toast.LENGTH_LONG).show();
            }

        }).addOnFailureListener(e -> {

            Toast.makeText(getContext(),
                    "Location Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();

        });
    }
    private void searchAddressCoordinates() {

        String addressText =
                binding.etAddress.getText().toString().trim();

        if (addressText.isEmpty()) {

            Toast.makeText(
                    getContext(),
                    "Enter Address First",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            Geocoder geocoder =
                    new Geocoder(
                            requireContext(),
                            Locale.getDefault()
                    );

            List<Address> addressList =
                    geocoder.getFromLocationName(
                            addressText,
                            1
                    );

            if (addressList != null &&
                    !addressList.isEmpty()) {

                Address address =
                        addressList.get(0);

                binding.etLatitude.setText(
                        String.valueOf(address.getLatitude()));

                binding.etLongitude.setText(
                        String.valueOf(address.getLongitude()));

                Toast.makeText(
                        getContext(),
                        "Location Found",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        getContext(),
                        "Address Not Found",
                        Toast.LENGTH_LONG
                ).show();
            }

        } catch (IOException e) {

            Toast.makeText(
                    getContext(),
                    "Error Finding Address",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private void validateAndSaveCustomer() {

        String name = binding.etCustomerName.getText().toString().trim();
        String mobile = binding.etMobileNumber.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String latStr = binding.etLatitude.getText().toString().trim();
        String lngStr = binding.etLongitude.getText().toString().trim();

        // NEW
        String qtyStr = binding.etMilkQuantity.getText().toString().trim();
        String rateStr = binding.etMilkRate.getText().toString().trim();

        // Standard validation
        if (TextUtils.isEmpty(name)) {
            binding.layCustomerName.setError("Customer Name is required");
            binding.etCustomerName.requestFocus();
            return;
        } else {
            binding.layCustomerName.setError(null);
        }

        if (TextUtils.isEmpty(mobile)) {
            binding.layMobileNumber.setError("Mobile Number is required");
            binding.etMobileNumber.requestFocus();
            return;
        } else {
            binding.layMobileNumber.setError(null);
        }

        if (TextUtils.isEmpty(address)) {
            address = "No Address Specified";
        }

        if (TextUtils.isEmpty(latStr)) {
            binding.layLatitude.setError("Required");
            return;
        } else {
            binding.layLatitude.setError(null);
        }

        if (TextUtils.isEmpty(lngStr)) {
            binding.layLongitude.setError("Required");
            return;
        } else {
            binding.layLongitude.setError(null);
        }

        // NEW
        if (TextUtils.isEmpty(qtyStr)) {
            binding.etMilkQuantity.setError("Enter Milk Quantity");
            return;
        }

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

            // NEW
            quantity = Double.parseDouble(qtyStr);
            rate = Double.parseDouble(rateStr);

        } catch (NumberFormatException e) {

            Toast.makeText(
                    getContext(),
                    "Invalid number format",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String today = DateUtils.getTodayDateString();

        // NEW CUSTOMER OBJECT
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

        viewModel.insertCustomer(customer, newId -> {

            if (getActivity() != null) {

                getActivity().runOnUiThread(() -> {

                    Toast.makeText(
                            getContext(),
                            "Customer: " + name + " saved successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    clearFields();

                    if (getActivity() instanceof com.example.activities.MainActivity) {
                        ((com.example.activities.MainActivity) getActivity())
                                .navigateToMenuItem(R.id.nav_home);
                    }

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
                Toast.makeText(getContext(), "Permission denied. Location coordinates must be entered manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
