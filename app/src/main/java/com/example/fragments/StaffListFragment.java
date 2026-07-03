package com.example.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.adapters.StaffAdapter;
import com.example.databinding.FragmentStaffListBinding;
import com.example.models.Staff;
import com.example.models.StaffListResponse;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffListFragment extends Fragment {

    private FragmentStaffListBinding binding;
    private StaffAdapter adapter;
    private List<Staff> staffList = new ArrayList<>();
    private String accountId;
    private static final String TAG = "StaffListFragment";

    public StaffListFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStaffListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get account_id from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        accountId = prefs.getString("account_id", "");

        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
        }

        // Setup adapter
        adapter = new StaffAdapter(new StaffAdapter.Listener() {

            @Override
            public void onCall(Staff staff) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + staff.getMobile()));
                startActivity(intent);
            }

            @Override
            public void onDetails(Staff staff) {
                showStaffDetails(staff);
            }
        }, this);

        binding.recyclerStaff.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStaff.setAdapter(adapter);

        // Search
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        // ❌ REMOVED: btnSharePdf click listener

        // Load staff from API
        loadStaffList();
    }

    private void loadStaffList() {
        if (accountId.isEmpty()) {
            Toast.makeText(requireContext(), "Account ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<StaffListResponse> call = apiService.listStaff(accountId);

        call.enqueue(new Callback<StaffListResponse>() {
            @Override
            public void onResponse(Call<StaffListResponse> call, Response<StaffListResponse> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    StaffListResponse staffResponse = response.body();

                    if (staffResponse.isStatus()) {
                        List<Staff> staffData = staffResponse.getData();

                        if (staffData != null && !staffData.isEmpty()) {
                            staffList.clear();
                            staffList.addAll(staffData);
                            adapter.setData(staffList);
                            Toast.makeText(requireContext(), "Loaded " + staffList.size() + " staff members", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "No staff members found", Toast.LENGTH_SHORT).show();
                            staffList.clear();
                            adapter.setData(staffList);
                        }
                    } else {
                        String msg = staffResponse.getMessage() != null ? staffResponse.getMessage() : "Error loading staff";
                        Toast.makeText(requireContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load staff list", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StaffListResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDeleteDialog(Staff staff) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Staff")
                .setMessage("Are you sure you want to delete " + staff.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // TODO: Implement delete via API
                    Toast.makeText(requireContext(), "Delete: " + staff.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStaffDetails(Staff staff) {
        String details = "ID: " + staff.getId() + "\n" +
                "Name: " + staff.getName() + "\n" +
                "Mobile: " + staff.getMobile() + "\n" +
                "User Type: " + staff.getUsertype() + "\n" +
                "Status: " + (staff.getIsactive() == 1 ? "Active" : "Inactive");

        new AlertDialog.Builder(requireContext())
                .setTitle("Staff Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}