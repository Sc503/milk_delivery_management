package com.example.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Marker selectedMarker;
    private double latitude = 0;
    private double longitude = 0;
    private String address = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        // Get initial location from intent
        latitude = getIntent().getDoubleExtra("LATITUDE", 19.9975);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 73.7898);

        // Get address from intent if available
        address = getIntent().getStringExtra("ADDRESS");
        if (address == null) address = "";

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Select Location Button
        Button btnSelect = findViewById(R.id.btn_select_location);
        btnSelect.setOnClickListener(v -> {
            if (selectedMarker != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LATITUDE", selectedMarker.getPosition().latitude);
                resultIntent.putExtra("LONGITUDE", selectedMarker.getPosition().longitude);
                resultIntent.putExtra("ADDRESS", address);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location on map", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel Button
        Button btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Enable zoom controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Add initial marker
        LatLng initialLocation = new LatLng(latitude, longitude);
        selectedMarker = googleMap.addMarker(new MarkerOptions()
                .position(initialLocation)
                .title("Selected Location")
                .draggable(true));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f));

        // ✅ Get address for initial location
        if (!address.isEmpty()) {
            // Address already set from intent
        } else {
            updateAddress(latitude, longitude);
        }

        // Handle map click to add/update marker
        googleMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .draggable(true));

            // ✅ Update address when map is clicked
            updateAddress(latLng.latitude, latLng.longitude);
        });

        // Handle marker drag
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {}

            @Override
            public void onMarkerDrag(@NonNull Marker marker) {}

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                // ✅ Update address when marker is dragged
                updateAddress(marker.getPosition().latitude, marker.getPosition().longitude);
            }
        });
    }

    // ✅ Method to get address from Lat/Lang using Geocoder
    private void updateAddress(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addressObj = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Get full address
                for (int i = 0; i <= addressObj.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(addressObj.getAddressLine(i));
                }

                this.address = sb.toString();

                // ✅ Update marker title with address
                if (selectedMarker != null) {
                    selectedMarker.setTitle(this.address);
                    selectedMarker.showInfoWindow();
                }

                Toast.makeText(this, "📍 " + this.address, Toast.LENGTH_SHORT).show();
            } else {
                this.address = "Lat: " + lat + ", Lng: " + lng;
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.address = "Lat: " + lat + ", Lng: " + lng;
        }
    }
}