package com.example.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.example.databinding.FragmentMapBinding;
import com.example.dialogs.CustomerDetailsDialog;
import com.example.dialogs.EditCustomerDialog;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private MilkViewModel viewModel;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final Map<String, Customer> markerLookup = new HashMap<>();

    private long selectedCustomerId = -1;

    private LatLng lastCameraPosition;
    private float lastZoomLevel = 15f;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // ✅ Flag to track if map is ready
    private boolean isMapReady = false;
    // ✅ Cache customers for refresh
    private List<Customer> cachedCustomers = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedCustomerId =
                    getArguments().getLong(
                            "CUSTOMER_ID",
                            -1
                    );
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String currentUserType =
                requireContext()
                        .getSharedPreferences(
                                "UserSession",
                                android.content.Context.MODE_PRIVATE)
                        .getString(
                                "userType",
                                ""
                        );

        if(currentUserType.equals("Customer")){
            Toast.makeText(
                    getContext(),
                    "Access Denied",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        super.onViewCreated(view, savedInstanceState);

        // Initialize supporting Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnLocationFinder.setOnClickListener(v -> checkLocationPermissionAndCenter());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.isMapReady = true;

        // Custom styling can be applied if needed
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        googleMap.setOnMarkerClickListener(marker -> {
            Customer customer = markerLookup.get(marker.getId());
            if (customer != null) {
                // Smooth single tap zoom
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                marker.getPosition(),
                                18f
                        )
                );
                marker.showInfoWindow();
                openCustomerDetailsDialog(customer);
                return true;
            }
            return false;
        });

        googleMap.setOnCameraIdleListener(() -> {
            if (googleMap != null) {
                lastCameraPosition = googleMap.getCameraPosition().target;
                lastZoomLevel = googleMap.getCameraPosition().zoom;
            }
        });

        // ✅ Load and watch pending deliveries for today
        viewModel.getAllCustomers()
                .observe(getViewLifecycleOwner(), customers -> {
                    cachedCustomers = customers;
                    if (isMapReady) {
                        updateMapMarkers(customers);
                    }
                });

        String today = DateUtils.getTodayDateString();
        viewModel.getDeliveriesForDate(today)
                .observe(getViewLifecycleOwner(), deliveries -> {
                    // ✅ Refresh markers when deliveries change
                    if (cachedCustomers != null && isMapReady) {
                        updateMapMarkers(cachedCustomers);
                    }
                });
    }

    // ✅ Public method to refresh map from outside (called after edit)
    public void refreshMap() {
        if (isMapReady && viewModel != null) {
            viewModel.getAllCustomers().observe(getViewLifecycleOwner(), customers -> {
                cachedCustomers = customers;
                updateMapMarkers(customers);
            });
        }
    }

    private void updateMapMarkers(List<Customer> customers) {
        if (googleMap == null || customers == null) {
            return;
        }

        final String today = DateUtils.getTodayDateString();

        viewModel.getRepository().getExecutor().execute(() -> {
            List<Delivery> todayDeliveries =
                    viewModel.getRepository().getDeliveriesForDateSync(today);

            Map<Long, Boolean> deliveryStatusMap = new HashMap<>();
            if (todayDeliveries != null) {
                for (Delivery d : todayDeliveries) {
                    if ("Delivered".equalsIgnoreCase(d.getStatus())) {
                        deliveryStatusMap.put(d.getCustomerId(), true);
                    }
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    renderMarkersOnUI(customers, deliveryStatusMap);
                });
            }
        });
    }

    private void renderMarkersOnUI(List<Customer> customers, Map<Long, Boolean> deliveryStatusMap) {
        android.util.Log.d("MAP_TEST", "renderMarkersOnUI called - Customer count: " + customers.size());

        if (googleMap == null) return;

        googleMap.clear();
        markerLookup.clear();
        binding.cardNoMarkers.setVisibility(View.GONE);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        Marker selectedMarker = null;
        LatLng selectedLatLng = null;

        for (Customer customer : customers) {
            // ✅ Skip customers with invalid coordinates
            if (customer.getLatitude() == 0 && customer.getLongitude() == 0) {
                android.util.Log.d("MAP_TEST", "Skipping customer with zero coordinates: " + customer.getName());
                continue;
            }

            LatLng latLng = new LatLng(customer.getLatitude(), customer.getLongitude());
            boundsBuilder.include(latLng);

            boolean delivered = deliveryStatusMap.getOrDefault(customer.getId(), false);

            float markerColor = delivered
                    ? BitmapDescriptorFactory.HUE_GREEN
                    : BitmapDescriptorFactory.HUE_RED;

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(latLng)
                            .title(customer.getName())
                            .snippet(customer.getAddress())
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            );

            if (marker != null) {
                markerLookup.put(marker.getId(), customer);
                android.util.Log.d("MAP_TEST", "✅ Marker added for: " + customer.getName() +
                        " Lat: " + customer.getLatitude() + " Lng: " + customer.getLongitude());
            }

            // select customer store कर
            if (customer.getId() == selectedCustomerId) {
                selectedMarker = marker;
                selectedLatLng = latLng;
            }
        }

        // 🔥 PRIORITY 1: selected customer focus
        if (selectedLatLng != null) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            selectedLatLng,
                            18f
                    )
            );
            if (selectedMarker != null) {
                selectedMarker.showInfoWindow();
            }
            return; // stop further camera logic
        }

        // 🔥 PRIORITY 2: If there are customers, zoom to fit all
        if (!customers.isEmpty()) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100; // offset from edges of the map in pixels
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, padding)
                );
                android.util.Log.d("MAP_TEST", "✅ Zooming to fit all customers");
                return;
            } catch (Exception e) {
                android.util.Log.e("MAP_TEST", "Error building bounds: " + e.getMessage());
            }
        }

        // 🔥 PRIORITY 3: Default - Nashik
        try {
            android.util.Log.d("MAP_TEST", "Moving To Nashik");
            LatLng nashik = new LatLng(19.9975, 73.7898);
            googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            nashik,
                            12f
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCustomerDetailsDialog(Customer customer) {
        String currentUserType =
                requireContext()
                        .getSharedPreferences(
                                "UserSession",
                                android.content.Context.MODE_PRIVATE)
                        .getString(
                                "userType",
                                ""
                        );

        if(currentUserType.equals("Customer")){
            Toast.makeText(
                    getContext(),
                    "Read Only Mode",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        CustomerDetailsDialog dialog = new CustomerDetailsDialog(customer, new CustomerDetailsDialog.DialogCallback() {
            @Override
            public void onDeliver(Customer c) {
                String today = DateUtils.getTodayDateString();
                String nowTime = DateUtils.getCurrentTimeString();

                viewModel.deliverCustomer(
                        customer.getId(),
                        today,
                        nowTime,
                        () -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    viewModel.getRepository()
                                            .getExecutor()
                                            .execute(() -> {
                                                List<Customer> freshCustomers =
                                                        viewModel.getRepository()
                                                                .getAllCustomersSync();

                                                if (getActivity() != null) {
                                                    getActivity().runOnUiThread(() -> {
                                                        updateMapMarkers(freshCustomers);
                                                    });
                                                }
                                            });

                                    Toast.makeText(
                                            getContext(),
                                            "Delivered",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                            }
                        }
                );
            }

            // ✅ onEdit callback
            @Override
            public void onEdit(Customer c) {
                // Open EditCustomerDialog
                EditCustomerDialog editDialog = new EditCustomerDialog(c, editedCustomer -> {
                    // ✅ Save edited customer to database
                    viewModel.updateCustomer(editedCustomer);

                    // ✅ Refresh map after a short delay
                    new Handler().postDelayed(() -> {
                        refreshMap();
                        Toast.makeText(getContext(),
                                "Customer updated! Location refreshed on map.",
                                Toast.LENGTH_SHORT).show();
                    }, 300);
                });
                editDialog.show(getChildFragmentManager(), "EditCustomerDialog");
            }

            @Override
            public void onCancel() {
                // Smooth close
            }
        });
        dialog.show(getChildFragmentManager(), "CustomerDetailsDialog");
    }

    private void checkLocationPermissionAndCenter() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        enableMyLocationAndCenter();
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationAndCenter() {
        if (googleMap == null) return;
        googleMap.setMyLocationEnabled(true);

        // If there are cached customers, zoom to them instead of Nashik
        if (cachedCustomers != null && !cachedCustomers.isEmpty()) {
            updateMapMarkers(cachedCustomers);
        } else {
            LatLng nashik = new LatLng(19.9975, 73.7898);
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            nashik,
                            12f
                    )
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationAndCenter();
            } else {
                Toast.makeText(getContext(), "Location services are required to auto position and fetch GPS coordinates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}