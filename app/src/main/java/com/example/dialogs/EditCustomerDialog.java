package com.example.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogEditCustomerBinding;
import com.example.models.Customer;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditCustomerDialog extends DialogFragment {

    public interface Listener {
        void onSave(Customer customer);
    }

    private final Customer customer;
    private final Listener listener;

    private double latitude;
    private double longitude;
    private String addressText;

    public EditCustomerDialog(Customer customer, Listener listener) {
        this.customer = customer;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        latitude = customer.getLatitude();
        longitude = customer.getLongitude();
        addressText = customer.getAddress();

        DialogEditCustomerBinding binding =
                DialogEditCustomerBinding.inflate(
                        LayoutInflater.from(requireContext()));

        binding.etName.setText(customer.getName());
        binding.etMobile.setText(customer.getMobile());
        binding.etAddress.setText(customer.getAddress());
        binding.etMilkQuantity.setText(String.valueOf(customer.getMilkQuantity()));
        binding.etMilkRate.setText(String.valueOf(customer.getMilkRate()));
        binding.etLatitude.setText(String.valueOf(customer.getLatitude()));
        binding.etLongitude.setText(String.valueOf(customer.getLongitude()));

        //  ActivityResultLauncher for MapPicker
        ActivityResultLauncher<Intent> launcher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() == Activity.RESULT_OK &&
                                    result.getData() != null) {

                                Intent data = result.getData();

                                // ✅ Get Lat/Lang from MapPicker
                                latitude = data.getDoubleExtra("LATITUDE", 0);
                                longitude = data.getDoubleExtra("LONGITUDE", 0);
                                addressText = data.getStringExtra("ADDRESS");

                                // ✅ Update UI with new values
                                binding.etAddress.setText(addressText);
                                binding.etLatitude.setText(String.valueOf(latitude));
                                binding.etLongitude.setText(String.valueOf(longitude));

                                // ✅ Log for debugging
                                android.util.Log.d("EditCustomerDialog",
                                        "📍 Location updated: Lat=" + latitude + ", Lng=" + longitude);
                            }
                        });

        //  MapPicker button click
        binding.btnChangeLocation.setOnClickListener(v -> {

            Intent intent =
                    new Intent(requireContext(),
                            com.example.activities.MapPickerActivity.class);

            intent.putExtra("LATITUDE", latitude);
            intent.putExtra("LONGITUDE", longitude);
            intent.putExtra("ADDRESS", addressText);

            launcher.launch(intent);
        });

        // ✅ NEW: Address change listener - Auto fetch Lat/Lang from Address
        binding.etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String address = binding.etAddress.getText().toString().trim();
                if (!address.isEmpty()) {
                    fetchLatLngFromAddress(address, binding);
                }
            }
        });

        // ✅ NEW: Latitude change listener - Auto fetch Address from Lat/Lang
        binding.etLatitude.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String latStr = binding.etLatitude.getText().toString().trim();
                String lngStr = binding.etLongitude.getText().toString().trim();
                if (!latStr.isEmpty() && !lngStr.isEmpty()) {
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        if (lat != 0 && lng != 0) {
                            fetchAddressFromLatLng(lat, lng, binding);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                }
            }
        });

        // ✅ NEW: Longitude change listener - Auto fetch Address from Lat/Lang
        binding.etLongitude.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String latStr = binding.etLatitude.getText().toString().trim();
                String lngStr = binding.etLongitude.getText().toString().trim();
                if (!latStr.isEmpty() && !lngStr.isEmpty()) {
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        if (lat != 0 && lng != 0) {
                            fetchAddressFromLatLng(lat, lng, binding);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                }
            }
        });

        return new AlertDialog.Builder(requireContext())
                .setTitle("Edit Customer")
                .setView(binding.getRoot())
                .setPositiveButton(
                        "Save",
                        (dialog, which) -> {

                            // ✅ Set basic info
                            customer.setName(binding.etName.getText().toString().trim());
                            customer.setMobile(binding.etMobile.getText().toString().trim());

                            // ✅ Use address from EditText
                            String newAddress = binding.etAddress.getText().toString().trim();
                            customer.setAddress(newAddress);

                            // ✅ Use Lat/Lang from EditText fields
                            try {
                                double newLat = Double.parseDouble(binding.etLatitude.getText().toString().trim());
                                double newLng = Double.parseDouble(binding.etLongitude.getText().toString().trim());

                                if (newLat != 0 && newLng != 0) {
                                    customer.setLatitude(newLat);
                                    customer.setLongitude(newLng);
                                } else {
                                    // Fallback: Use stored values
                                    if (latitude != 0 && longitude != 0) {
                                        customer.setLatitude(latitude);
                                        customer.setLongitude(longitude);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // If EditText values are invalid, keep existing
                                if (latitude != 0 && longitude != 0) {
                                    customer.setLatitude(latitude);
                                    customer.setLongitude(longitude);
                                }
                            }

                            customer.setMilkQuantity(
                                    Double.parseDouble(binding.etMilkQuantity.getText().toString()));
                            customer.setMilkRate(
                                    Double.parseDouble(binding.etMilkRate.getText().toString()));

                            // ✅ Log final values
                            android.util.Log.d("EditCustomerDialog",
                                    "💾 Saving: Lat=" + customer.getLatitude() +
                                            ", Lng=" + customer.getLongitude());

                            listener.onSave(customer);
                        })
                .setNegativeButton("Cancel", null)
                .create();
    }

    // ✅ NEW: Method to fetch Lat/Lang from Address using Geocoder
    private void fetchLatLngFromAddress(String address, DialogEditCustomerBinding binding) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                // ✅ Update Lat/Lang fields
                binding.etLatitude.setText(String.valueOf(lat));
                binding.etLongitude.setText(String.valueOf(lng));

                // ✅ Store values
                latitude = lat;
                longitude = lng;

                Toast.makeText(requireContext(),
                        "📍 Location found: " + lat + ", " + lng,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "❌ Address not found. Please check the address.",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "❌ Error fetching location. Please check internet connection.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ NEW: Method to fetch Address from Lat/Lang using Geocoder
    private void fetchAddressFromLatLng(double lat, double lng, DialogEditCustomerBinding binding) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addressObj = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Get full address
                for (int i = 0; i <= addressObj.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(addressObj.getAddressLine(i));
                }

                String fullAddress = sb.toString();

                // ✅ Update Address field
                binding.etAddress.setText(fullAddress);

                // ✅ Store address
                addressText = fullAddress;

                Toast.makeText(requireContext(),
                        "📍 Address found: " + fullAddress,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "❌ Address not found for these coordinates.",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "❌ Error fetching address. Please check internet connection.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}