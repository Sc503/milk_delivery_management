package com.example.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.adapters.StaffAdapter;
import com.example.dao.StaffDao;
import com.example.database.AppDatabase;
import com.example.databinding.ActivityStaffListBinding;
import com.example.models.Staff;
import com.example.models.StaffListResponse;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffList_Activity extends AppCompatActivity {

    private ActivityStaffListBinding binding;
    private StaffAdapter adapter;
    private Context context;
    private List<Staff> staffList = new ArrayList<>();
    private String accountId;
    private static final String TAG = "StaffList_Activity";

    private TextView tvTotalStaffCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaffListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;

        tvTotalStaffCount = binding.tvTotalStaffCount;

        setupToolbar();

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        accountId = prefs.getString("account_id", "");

        if (accountId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
        }

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

        binding.recyclerStaff.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerStaff.setAdapter(adapter);

        loadStaffList();
    }

    //  IMPORTANT: Reload staff list when returning from AddStaff_Activity
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading staff list...");
        loadStaffList();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Staff List");
        }
    }

    private void loadStaffList() {
        if (accountId.isEmpty()) {
            Toast.makeText(this, "Account ID not found", Toast.LENGTH_SHORT).show();
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
                            saveStaffToLocalDatabase(staffData);
                            staffList.clear();
                            staffList.addAll(staffData);
                            adapter.setData(staffList);
                            updateTotalStaffCount(staffList.size());

                            Toast.makeText(StaffList_Activity.this,
                                    "Loaded " + staffList.size() + " staff members", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(StaffList_Activity.this, "No staff members found", Toast.LENGTH_SHORT).show();
                            staffList.clear();
                            adapter.setData(staffList);
                            updateTotalStaffCount(0);
                        }
                    } else {
                        String msg = staffResponse.getMessage() != null ? staffResponse.getMessage() : "Error loading staff";
                        Toast.makeText(StaffList_Activity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(StaffList_Activity.this, "Failed to load staff list", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StaffListResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(StaffList_Activity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateTotalStaffCount(int count) {
        if (tvTotalStaffCount != null) {
            tvTotalStaffCount.setText(String.valueOf(count));
        }
    }

    private void saveStaffToLocalDatabase(List<Staff> staffList) {
        AppDatabase db = AppDatabase.getInstance(this);
        StaffDao staffDao = db.staffDao();

        new Thread(() -> {
            staffDao.deleteAll();
            staffDao.insertAll(staffList);
            Log.d("STAFF_SYNC", "Saved " + staffList.size() + " staff members to local database");
        }).start();
    }

    private void showStaffDetails(Staff staff) {
        String details = "ID: " + staff.getId() + "\n" +
                "Name: " + staff.getName() + "\n" +
                "Mobile: " + staff.getMobile() + "\n" +
                "User Type: " + staff.getUsertype() + "\n" +
                "Status: " + (staff.getIsactive() == 1 ? "Active" : "Inactive");

        new AlertDialog.Builder(this)
                .setTitle("Staff Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    public void filterStaff(String text) {
        if (adapter != null) {
            adapter.filter(text);
            updateTotalStaffCount(adapter.getFilteredCount());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.staff_toolbar_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search_staff);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search staff...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterStaff(newText);
                        return true;
                    }
                });

                searchView.setOnCloseListener(() -> {
                    filterStaff("");
                    return false;
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_add_staff) {
            Intent intent = new Intent(this, AddStaff_Activity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_search_staff) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}