package com.example.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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

                // small delay for better UX (optional but smooth)
                marker.showInfoWindow();

                // open dialog
                openCustomerDetailsDialog(customer);

                return true;
            }

            return false;
        });

        // Load and watch pending deliveries for today
        viewModel.getAllCustomers()
                .observe(getViewLifecycleOwner(), this::updateMapMarkers);

        String today = DateUtils.getTodayDateString();

        viewModel.getDeliveriesForDate(today)
                .observe(getViewLifecycleOwner(), deliveries -> {

                    List<Customer> customers =
                            viewModel.getAllCustomers().getValue();

                    if (customers != null) {
                        updateMapMarkers(customers);
                    }
                });

        checkLocationPermissionAndCenter();
    }

    private void updateMapMarkers(List<Customer> customers) {
        if (googleMap == null) return;

        if (customers == null || customers.isEmpty()) {
            googleMap.clear();
            markerLookup.clear();
            binding.cardNoMarkers.setVisibility(View.VISIBLE);
            return;
        }

        final String today = DateUtils.getTodayDateString();

        // Run database fetch on background thread
        viewModel.getRepository().getExecutor().execute(() -> {
            List<Delivery> todayDeliveries = viewModel.getRepository().getDeliveriesForDateSync(today);

            android.util.Log.d("MAP_DEBUG", "Today = " + today);

            for (Delivery d : todayDeliveries) {

                android.util.Log.d(
                        "MAP_DEBUG",
                        "CustomerId = "
                                + d.getCustomerId()
                                + " Status = "
                                + d.getStatus()
                );
            }

            Map<Long, Boolean> deliveryStatusMap = new HashMap<>();
            if (todayDeliveries != null) {
                for (Delivery d : todayDeliveries) {
                    if ("Delivered".equalsIgnoreCase(d.getStatus())) {
                        deliveryStatusMap.put(d.getCustomerId(), true);
                    }
                }
            }

            // Post back to main thread to update UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    renderMarkersOnUI(customers, deliveryStatusMap);
                });
            }
        });
    }

    private void renderMarkersOnUI(List<Customer> customers, Map<Long, Boolean> deliveryStatusMap) {

        if (googleMap == null) return;

        googleMap.clear();
        markerLookup.clear();
        binding.cardNoMarkers.setVisibility(View.GONE);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        Marker selectedMarker = null;
        LatLng selectedLatLng = null;

        for (Customer customer : customers) {

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

        // 🔥 DEFAULT camera logic
        try {
            if (customers.size() == 1) {

                LatLng singleLatLng = new LatLng(
                        customers.get(0).getLatitude(),
                        customers.get(0).getLongitude()
                );

                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                singleLatLng,
                                14.0f
                        )
                );

            } else if (customers.size() > 1) {

                LatLngBounds bounds = boundsBuilder.build();

                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                120
                        )
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openCustomerDetailsDialog(Customer customer) {
        CustomerDetailsDialog dialog = new CustomerDetailsDialog(customer, new CustomerDetailsDialog.DialogCallback() {
            @Override
            public void onDeliver(Customer c) {
                String today = DateUtils.getTodayDateString();
                String nowTime = DateUtils.getCurrentTimeString();

                // Record the delivery as Delivered in database
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

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15.0f));
            } else {
                // Fallback coordinates if GPS is disabled but permissions granted (e.g. center of city/mock London)
                if (markerLookup.isEmpty()) {
                    LatLng alternate = new LatLng(51.523767, -0.1585557);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(alternate, 11.0f));
                }
            }
        });
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
