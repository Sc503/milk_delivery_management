package com.example.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.databinding.DialogEditCustomerBinding;
import com.example.models.Customer;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class EditCustomerDialog extends DialogFragment {

    public interface Listener{
        void onSave(Customer customer);
    }

    private final Customer customer;
    private final Listener listener;

    private double latitude;
    private double longitude;

    public EditCustomerDialog(
            Customer customer,
            Listener listener){

        this.customer = customer;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        latitude = customer.getLatitude();
        longitude = customer.getLongitude();

        DialogEditCustomerBinding binding =
                DialogEditCustomerBinding.inflate(
                        LayoutInflater.from(requireContext()));

        binding.etName.setText(customer.getName());

        binding.etMobile.setText(customer.getMobile());

        binding.etAddress.setText(customer.getAddress());

        binding.etMilkQuantity.setText(
                String.valueOf(customer.getMilkQuantity()));

        binding.etMilkRate.setText(
                String.valueOf(customer.getMilkRate()));

        binding.etLatitude.setText(
                String.valueOf(customer.getLatitude()));

        binding.etLongitude.setText(
                String.valueOf(customer.getLongitude()));

        ActivityResultLauncher<Intent> launcher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() == Activity.RESULT_OK &&
                                    result.getData() != null) {

                                Intent data = result.getData();

                                latitude = data.getDoubleExtra("LATITUDE", 0);

                                longitude = data.getDoubleExtra("LONGITUDE", 0);

                                String address =
                                        data.getStringExtra("ADDRESS");

                                binding.etAddress.setText(address);
                            }
                        });

//        binding.btnChangeLocation.setOnClickListener(v -> {
//
//            Intent intent =
//                    new Intent(requireContext(),
//                            MapPickerActivity.class);
//
//            intent.putExtra("LATITUDE", latitude);
//            intent.putExtra("LONGITUDE", longitude);
//
//            launcher.launch(intent);
//
//        });

        return new AlertDialog.Builder(requireContext())



                .setTitle("Edit Customer")

                .setView(binding.getRoot())

                .setPositiveButton(
                        "Save",
                        (dialog, which)->{

                            customer.setName(
                                    binding.etName
                                            .getText()
                                            .toString()
                                            .trim());

                            customer.setMobile(
                                    binding.etMobile
                                            .getText()
                                            .toString()
                                            .trim());

                            String newAddress =
                                    binding.etAddress
                                            .getText()
                                            .toString()
                                            .trim();

                            customer.setAddress(newAddress);

                            try {

                                android.location.Geocoder geocoder =
                                        new android.location.Geocoder(
                                                requireContext(),
                                                java.util.Locale.getDefault());

                                java.util.List<android.location.Address> addresses =
                                        geocoder.getFromLocationName(
                                                newAddress,
                                                1);

                                if (addresses != null &&
                                        !addresses.isEmpty()) {

                                    android.location.Address address =
                                            addresses.get(0);

                                    customer.setLatitude(
                                            address.getLatitude());

                                    customer.setLongitude(
                                            address.getLongitude());
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            customer.setMilkQuantity(
                                    Double.parseDouble(
                                            binding.etMilkQuantity
                                                    .getText()
                                                    .toString()));

                            customer.setMilkRate(
                                    Double.parseDouble(
                                            binding.etMilkRate
                                                    .getText()
                                                    .toString()));

                            listener.onSave(customer);

                        })

                .setNegativeButton(
                        "Cancel",
                        null)

                .create();
    }
}