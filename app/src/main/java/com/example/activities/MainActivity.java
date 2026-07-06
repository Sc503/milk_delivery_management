package com.example.activities;

import com.example.fragments.PaymentFragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.ActivityMainBinding;

import com.example.fragments.AboutUsFragment;
import com.example.fragments.AddCustomerFragment;
import com.example.fragments.AddStaffFragment;
import com.example.fragments.MapFragment;
import com.example.fragments.MonthlyRecapFragment;
import com.example.fragments.SettingsFragment;
import com.example.fragments.StaffListFragment;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.utils.DateUtils;
import com.example.utils.PermissionManager;
import com.example.viewmodel.MilkViewModel;
import com.google.android.material.navigation.NavigationView;
import android.content.SharedPreferences;
import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.database.AppDatabase;
import com.example.models.User;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;
    private String currentUserType;

    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ CHECK SESSION - If no userType or mobile, redirect to Login
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserType = pref.getString("userType", null);
        String mobile = pref.getString("mobile", null);

        // ✅ If not logged in, redirect to Login
        if (currentUserType == null || mobile == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Setup MVVM and DB ViewModel
        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        // DEBUG LOGS
        Log.d("CHECK", "UserType = " + currentUserType);
        Log.d("CHECK", "Mobile = " + mobile);

        // SAFE DEFAULT (important)
        if (currentUserType == null) {
            currentUserType = "Guest";
        }

        // UI / MENU SETUP FIRST
        setupMenuByRole();

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Welcome " + currentUserType);
        }

        // Navigation slide-toggle drawer hooks
        toggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.app_name,
                R.string.app_name);

        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        binding.navView.setItemIconTintList(getResources().getColorStateList(R.color.primary));
        binding.navView.setNavigationItemSelectedListener(this);

        if (getIntent().getBooleanExtra("OPEN_CUSTOMER_LOCATION", false)) {
            long customerId = getIntent().getLongExtra("CUSTOMER_ID", -1);
            Bundle bundle = new Bundle();
            bundle.putLong("CUSTOMER_ID", customerId);
            MapFragment fragment = new MapFragment();
            fragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return;
        }

        // Load map as initial starting fragment
        if (savedInstanceState == null) {
            navigateToMenuItem(R.id.nav_home);
        }
    }

    /**
     * Public helper to let fragments or menus route actions
     */
    public void navigateToMenuItem(int itemId) {
        Fragment fragment = null;
        String tag = "";
        String toolbarTitle = "Milk Delivery";

        if (itemId == R.id.nav_home) {
            fragment = new MapFragment();
            tag = "MAP_FRAGMENT";
            toolbarTitle = "Milk Delivery";
            binding.navView.setCheckedItem(R.id.nav_home);
        } else if (itemId == R.id.nav_add_customer) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return;
            }
            fragment = new AddCustomerFragment();
            tag = "ADD_CUSTOMER_FRAGMENT";
            toolbarTitle = "Add New Customer";
            binding.navView.setCheckedItem(R.id.nav_add_customer);
        } else if (itemId == R.id.nav_staff_list) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return;
            }
            fragment = new StaffListFragment();
            tag = "STAFF_LIST_FRAGMENT";
            toolbarTitle = "Staff List";
            binding.navView.setCheckedItem(R.id.nav_staff_list);
        } else if (itemId == R.id.nav_monthly_recap) {
//            fragment = new MonthlyRecapFragment();

            Intent intent = new Intent(MainActivity.this, MonthlyRecap_Activity.class);
            startActivity(intent);

//            tag = "MONTHLY_RECAP_FRAGMENT";
//            toolbarTitle = "Monthly Recap";
//            binding.navView.setCheckedItem(R.id.nav_monthly_recap);
        } else if (itemId == R.id.nav_payments) {
            fragment = new PaymentFragment();
            tag = "PAYMENT_FRAGMENT";
            toolbarTitle = "Payments";
            binding.navView.setCheckedItem(R.id.nav_payments);
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "SETTINGS_FRAGMENT";
            toolbarTitle = "Settings";
            binding.navView.setCheckedItem(R.id.nav_settings);
        } else if (itemId == R.id.nav_about_us) {
            fragment = new AboutUsFragment();
            tag = "ABOUT_FRAGMENT";
            toolbarTitle = "About Us";
            binding.navView.setCheckedItem(R.id.nav_about_us);
        } else if (itemId == R.id.nav_logout) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // ✅ Clear all session data
                        getSharedPreferences("UserSession", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply();

                        // ✅ Redirect to Login
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return;
        }

        if (fragment != null) {
            activeFragment = fragment;

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.addToBackStack(null);
            transaction.commit();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(toolbarTitle);

                // 👇 Show/hide back button based on fragment
                if (fragment instanceof AddStaffFragment) {

                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    toggle.setDrawerIndicatorEnabled(false);

                } else {

                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    toggle.setDrawerIndicatorEnabled(true);
                    toggle.syncState();
                }
            }
        }

        invalidateOptionsMenu();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navigateToMenuItem(item.getItemId());
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search_staff);

        SearchView searchView =
                (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Search Staff");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                Fragment fragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);

                if (fragment instanceof StaffListFragment) {
                    ((StaffListFragment) fragment).filterStaff(newText);
                }

                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem addCustomer = menu.findItem(R.id.action_add_shortcut);
        MenuItem addStaff = menu.findItem(R.id.action_add_staff);
        MenuItem filter = menu.findItem(R.id.action_filter);
        MenuItem searchStaff = menu.findItem(R.id.action_search_staff);

        if (addCustomer != null)
            addCustomer.setVisible(activeFragment instanceof MapFragment);

        if (addStaff != null)
            addStaff.setVisible(activeFragment instanceof StaffListFragment);

        if (filter != null)
            filter.setVisible(activeFragment instanceof MonthlyRecapFragment);

        if (searchStaff != null)
            searchStaff.setVisible(activeFragment instanceof StaffListFragment);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {

            if (activeFragment instanceof AddStaffFragment) {
                navigateToMenuItem(R.id.nav_staff_list);
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }

            return true;
        }

        if (item.getItemId() == R.id.action_add_shortcut) {
            navigateToMenuItem(R.id.nav_add_customer);
            return true;
        }

        if (item.getItemId() == R.id.action_add_staff) {

//            activeFragment = new AddStaffFragment();

            // Create Account link
                Intent intent = new Intent(MainActivity.this, AddStaff_Activity.class);
                startActivity(intent);


//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, activeFragment)
//                    .commit();

//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setTitle("Add Staff");
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            }
//
//            invalidateOptionsMenu();
            return true;
        }

        if (item.getItemId() == R.id.action_filter) {

            Fragment fragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (fragment instanceof MonthlyRecapFragment) {
                ((MonthlyRecapFragment) fragment).showFilterDialog();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupMenuByRole() {
        if (binding == null || binding.navView == null) return;
        if (currentUserType == null) return;

        Menu menu = binding.navView.getMenu();

        Log.d("CHECK", "UserType = " + currentUserType);

        // First: show all items (reset state)
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(true);
        }

        if ("Owner".equals(currentUserType)) {
            Log.d("CHECK", "Owner menu loaded - everything visible");
            // Owner sees everything
        } else if ("Staff".equals(currentUserType)) {
            Log.d("CHECK", "Staff menu loaded - limited access");
            // Staff: Hide admin-only items
            hide(menu, R.id.nav_add_customer);
            hide(menu, R.id.nav_payments);
            hide(menu, R.id.nav_staff_list);
        }
    }

    private void hide(Menu menu, int id) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(false);
        }
    }
}