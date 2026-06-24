package com.example.activities;

import com.example.fragments.PaymentFragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.firebase.DeliverySyncListener;
import com.example.fragments.AboutUsFragment;
import com.example.fragments.AddCustomerFragment;
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
import com.example.fragments.AddStaffFragment;



import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;
    private String currentUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DeliverySyncListener.start(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup MVVM and DB ViewModel
        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        SharedPreferences pref =
                getSharedPreferences("UserSession", MODE_PRIVATE);

        currentUserType =
                pref.getString("userType", null);

        String mobile =
                pref.getString("mobile", null);

// DEBUG LOGS
        Log.d("CHECK", "UserType = " + currentUserType);
        Log.d("CHECK", "Mobile = " + mobile);

// SAFE DEFAULT (important)
        if (currentUserType == null) {
            currentUserType = "Guest";
        }

// UI / MENU SETUP FIRST
        setupMenuByRole();

// THEN DATA LOAD
        viewModel.readDeliveriesFromFirebase();


//        insertUsers();     // ← Temporary

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    "Welcome " + currentUserType
            );
        }


        // Navigation slide-toggle drawer hooks
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.app_name, R.string.app_name); // uses standard string refs
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);


        if ("Customer".equals(currentUserType)) {

            // Drawer SHOULD NOT be locked
            binding.drawerLayout.setDrawerLockMode(
                    androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
            );

            navigateToMenuItem(R.id.nav_monthly_recap);
        }

        if (getIntent().getBooleanExtra(
                "OPEN_CUSTOMER_LOCATION",
                false
        )) {

            long customerId =
                    getIntent().getLongExtra(
                            "CUSTOMER_ID",
                            -1
                    );

            Bundle bundle =
                    new Bundle();

            bundle.putLong(
                    "CUSTOMER_ID",
                    customerId
            );

            MapFragment fragment =
                    new MapFragment();

            fragment.setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            fragment
                    )
                    .commit();

            return;
        }
        // Load map as initial starting fragment
        if (savedInstanceState == null) {

            if ("Customer".equals(currentUserType)) {

                navigateToMenuItem(R.id.nav_monthly_recap);

            } else {

                navigateToMenuItem(R.id.nav_home);

            }
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
            Log.d(
                    "USER_TYPE",
                    "Current User = " + currentUserType
            );
            if (!PermissionManager.canAddCustomer(currentUserType)) {

                Toast.makeText(
                        this,
                        "Access Denied",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            fragment = new AddCustomerFragment();

            tag = "ADD_CUSTOMER_FRAGMENT";

            toolbarTitle = "Add New Customer";

            binding.navView.setCheckedItem(
                    R.id.nav_add_customer);
        } else if (itemId == R.id.nav_add_staff) {

            fragment = new AddStaffFragment();

            tag = "ADD_STAFF_FRAGMENT";

            toolbarTitle = "Add Staff";

            binding.navView.setCheckedItem(R.id.nav_add_staff);
        } else if (itemId == R.id.nav_monthly_recap) {
            fragment = new MonthlyRecapFragment();
            tag = "MONTHLY_RECAP_FRAGMENT";
            toolbarTitle = "Monthly Recap";
            binding.navView.setCheckedItem(R.id.nav_monthly_recap);
        } else if (itemId == R.id.nav_payment) {
            fragment = new PaymentFragment();
            tag = "PAYMENT_FRAGMENT";
            toolbarTitle = "Payments";
            binding.navView.setCheckedItem(R.id.nav_payment);

        } else if (itemId == R.id.nav_staff_list) {

            fragment = new StaffListFragment();

            tag = "STAFF_LIST_FRAGMENT";

            toolbarTitle = "Staff List";

            binding.navView.setCheckedItem(R.id.nav_staff_list);

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
                    .setMessage("Are you sure?")

                    .setPositiveButton(
                            "Yes",
                            (dialog, which) -> {

                                getSharedPreferences(
                                        "UserSession",
                                        MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply();

                                startActivity(
                                        new Intent(
                                                MainActivity.this,
                                                LoginActivity.class));

                                finish();
                            })

                    .setNegativeButton(
                            "No",
                            null)

                    .show();

            return;
        }

        if (fragment != null) {
            activeFragment = fragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.commit();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(toolbarTitle);
            }
        }

        // Re-draw toolbar menu features
        invalidateOptionsMenu();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navigateToMenuItem(item.getItemId());
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {

            binding.drawerLayout.closeDrawer(GravityCompat.START);

        } else if ("Customer".equals(currentUserType)) {

            finish();

        } else if (!(activeFragment instanceof MapFragment)) {

            navigateToMenuItem(R.id.nav_home);

        } else {

            super.onBackPressed();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate dynamic options at toolbar
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addCustomerItem = menu.findItem(R.id.action_add_shortcut);
        if (addCustomerItem != null) {

            addCustomerItem.setVisible(activeFragment instanceof MapFragment);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_shortcut) {
            navigateToMenuItem(R.id.nav_add_customer);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupMenuByRole() {

        if (binding == null || binding.navView == null) return;
        if (currentUserType == null) return;

        Menu menu = binding.navView.getMenu();

        Log.d("CHECK", "UserType = " + currentUserType);

        // First: show all items
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(true);
        }

        if ("Owner".equals(currentUserType)) {

            Log.d("CHECK", "Owner menu loaded");

            MenuItem addStaff = menu.findItem(R.id.nav_add_staff);

            if (addStaff != null) {
                addStaff.setVisible(true);
            }

        }
        else if ("Staff".equals(currentUserType)) {

            Log.d("CHECK", "Staff menu loaded");

            hide(menu, R.id.nav_add_staff);

        }
        else if ("Customer".equals(currentUserType)) {

            Log.d("CHECK", "Customer menu loaded");

            hide(menu, R.id.nav_add_customer);
            hide(menu, R.id.nav_payment);
            hide(menu, R.id.nav_settings);
            hide(menu, R.id.nav_home);
            hide(menu, R.id.nav_add_staff);

            MenuItem recap = menu.findItem(R.id.nav_monthly_recap);
            if (recap != null)
                recap.setVisible(true);

            MenuItem about = menu.findItem(R.id.nav_about_us);
            if (about != null)
                about.setVisible(true);

            MenuItem logout = menu.findItem(R.id.nav_logout);
            if (logout != null)
                logout.setVisible(true);
        }
    }
    private void hide(Menu menu, int id) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(false);
        }
    }

    private void hideMenuItem(Menu menu, int id) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(false);
        }
    }
}

