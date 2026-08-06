package in.esmartsolution.milkflow.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import in.esmartsolution.milkflow.R;
import in.esmartsolution.milkflow.activities.EditCustomer_Activity;
import in.esmartsolution.milkflow.databinding.FragmentMapBinding;
import in.esmartsolution.milkflow.dialogs.CustomerDetailsDialog;
import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.models.Delivery;
import in.esmartsolution.milkflow.models.DeliveryWithStaff;
import in.esmartsolution.milkflow.utils.DateUtils;
import in.esmartsolution.milkflow.viewmodel.MilkViewModel;
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

    private boolean isMapReady = false;
    private List<Customer> cachedCustomers = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedCustomerId = getArguments().getLong("CUSTOMER_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String currentUserType = requireContext()
                .getSharedPreferences("UserSession", MODE_PRIVATE)
                .getString("userType", "");

        if (currentUserType.equals("Customer")) {
            Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnLocationFinder.setOnClickListener(v -> checkLocationPermissionAndCenter());

        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.isMapReady = true;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        googleMap.setOnMarkerClickListener(marker -> {
            Customer customer = markerLookup.get(marker.getId());
            if (customer != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 18f));
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

        viewModel.getAllCustomers().observe(getViewLifecycleOwner(), customers -> {
            cachedCustomers = customers;
            if (isMapReady) {
                updateMapMarkers(customers);
            }
        });

        String today = DateUtils.getTodayDateString();
        viewModel.getDeliveriesForDate(today).observe(getViewLifecycleOwner(), deliveries -> {
            if (cachedCustomers != null && isMapReady) {
                updateMapMarkers(cachedCustomers);
            }
        });
    }

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
            List<Delivery> todayDeliveries = viewModel.getRepository().getDeliveriesForDateSync(today);

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

    // ============================================================
    // 🔥 FIXED METHOD - Null check added!
    // ============================================================
    private void renderMarkersOnUI(List<Customer> customers, Map<Long, Boolean> deliveryStatusMap) {
        // ✅ FIX 1: Check if googleMap is null
        if (googleMap == null) {
            Log.e("MapFragment", "❌ googleMap is null!");
            return;
        }

        // ✅ FIX 2: Check if binding is null
        if (binding == null) {
            Log.e("MapFragment", "❌ binding is null!");
            return;
        }

        // ✅ FIX 3: Check if customers is null
        if (customers == null) {
            customers = java.util.Collections.emptyList();
        }

        // ✅ FIX 4: Check if deliveryStatusMap is null
        if (deliveryStatusMap == null) {
            deliveryStatusMap = new HashMap<>();
        }

        googleMap.clear();
        markerLookup.clear();

        // ✅ FIX 5: Safe access to cardNoMarkers
        try {
            binding.cardNoMarkers.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e("MapFragment", "❌ Error setting visibility: " + e.getMessage());
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        Marker selectedMarker = null;
        LatLng selectedLatLng = null;
        boolean hasValidMarkers = false;

        for (Customer customer : customers) {
            if (customer.getLatitude() == 0 && customer.getLongitude() == 0) {
                continue;
            }

            LatLng latLng = new LatLng(customer.getLatitude(), customer.getLongitude());
            boundsBuilder.include(latLng);
            hasValidMarkers = true;

            boolean delivered = deliveryStatusMap.getOrDefault(customer.getId(), false);
            float markerColor = delivered ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;

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

            if (customer.getId() == selectedCustomerId) {
                selectedMarker = marker;
                selectedLatLng = latLng;
            }
        }

        if (selectedLatLng != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 18f));
            if (selectedMarker != null) {
                selectedMarker.showInfoWindow();
            }
            return;
        }

        if (hasValidMarkers) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                return;
            } catch (Exception e) {
                Log.e("MAP_TEST", "Error building bounds: " + e.getMessage());
            }
        }

        try {
            LatLng nashik = new LatLng(19.9975, 73.7898);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nashik, 12f));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCustomerDetailsDialog(Customer customer) {
        String currentUserType = requireContext()
                .getSharedPreferences("UserSession", MODE_PRIVATE)
                .getString("userType", "");

        if (currentUserType.equals("Customer")) {
            Toast.makeText(getContext(), "Read Only Mode", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = DateUtils.getTodayDateString();

        viewModel.getRepository().getExecutor().execute(() -> {
            DeliveryWithStaff delivery = viewModel.getDeliveryWithStaff(customer.getId(), today);

            String staffDisplay = "Not assigned";
            long staffId = 0;

            if (delivery != null) {
                if (delivery.staffName != null && !delivery.staffName.isEmpty()) {
                    staffDisplay = "Staff Name: " + delivery.staffName + "\n Staff ID: " + delivery.staffId;
                } else if (delivery.staffId > 0) {
                    staffDisplay = "Staff Name: NA \n Staff ID: " + delivery.staffId;
                } else {
                    staffDisplay = "Staff Name: NA \n Staff ID: NA";
                }
            }

            final String finalStaffDisplay = staffDisplay;
            final long finalStaffId = staffId;

            requireActivity().runOnUiThread(() -> {
                CustomerDetailsDialog.DialogCallback callback = new CustomerDetailsDialog.DialogCallback() {
                    @Override
                    public void onDeliver(Customer c) {
                        String todayDate = DateUtils.getTodayDateString();
                        String nowTime = DateUtils.getCurrentTimeString();

                        SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE);
                        long loggedInStaffId = prefs.getLong("staff_id", 0);
                        String staffName = prefs.getString("staff_name", "Staff");

                        viewModel.deliverCustomer(
                                customer.getId(),
                                todayDate,
                                nowTime,
                                loggedInStaffId,
                                staffName,
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
                                            Toast.makeText(getContext(), "Delivered!", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                        );
                    }

                    @Override
                    public void onEdit(Customer c) {
                        Intent intent = new Intent(getContext(), EditCustomer_Activity.class);
                        intent.putExtra("CUSTOMER_ID", c.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onCancel() {
                        // Smooth close
                    }
                };

                CustomerDetailsDialog dialog = new CustomerDetailsDialog(customer, finalStaffDisplay, callback);
                dialog.show(getChildFragmentManager(), "CustomerDetailsDialog");
            });
        });
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

        if (cachedCustomers != null && !cachedCustomers.isEmpty()) {
            updateMapMarkers(cachedCustomers);
        } else {
            LatLng nashik = new LatLng(19.9975, 73.7898);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nashik, 12f));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationAndCenter();
            } else {
                Toast.makeText(getContext(), "Location services are required.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        googleMap = null;
    }
}