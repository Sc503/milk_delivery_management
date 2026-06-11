package com.example.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.utils.DateUtils;
import com.example.viewmodel.MilkViewModel;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DeliverySyncListener.start(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup MVVM and DB ViewModel
        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        viewModel.readDeliveriesFromFirebase();


        // Pre-populate Database with beautiful demo records on first startup
        seedDatabaseOnFirstLaunch();

        setSupportActionBar(binding.toolbar);

        // Navigation slide-toggle drawer hooks
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.app_name, R.string.app_name); // uses standard string refs
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

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
            fragment = new AddCustomerFragment();
            tag = "ADD_CUSTOMER_FRAGMENT";
            toolbarTitle = "Add New Customer";
            binding.navView.setCheckedItem(R.id.nav_add_customer);
        } else if (itemId == R.id.nav_monthly_recap) {
            fragment = new MonthlyRecapFragment();
            tag = "MONTHLY_RECAP_FRAGMENT";
            toolbarTitle = "Monthly Recap";
            binding.navView.setCheckedItem(R.id.nav_monthly_recap);
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
        } else if (!(activeFragment instanceof MapFragment)) {
            // Default back behavior returns to Map screen
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
            // Only display "+ Add New" button when Map Fragment is active!
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

    /**
     * Seed Database with standard mock customers as shown in the images,
     * so that the application is immediately responsive, elegant, and interactive!
     */
    private void seedDatabaseOnFirstLaunch() {
        viewModel.getRepository().getExecutor().execute(() -> {
            com.example.database.AppDatabase db = com.example.database.AppDatabase.getInstance(this);
            int count = db.query("SELECT COUNT(*) FROM customers", null).getCount();
            if (count == 0) {
                // Generate default historical and current customers
                Customer c1 = new Customer("John Doe", "9876543210", "221B Baker Street, London", 51.523767, -0.1585557, "2026-06-01");
                Customer c2 = new Customer("David Smith", "9823485710", "Green Park, London", 51.502621, -0.143229, "2026-06-01");
                Customer c3 = new Customer("Michael Brown", "9812345678", "City Center, London", 51.507421, -0.127817, "2026-06-01");
                Customer c4 = new Customer("James Wilson", "9765432101", "Market Road, London", 51.512631, -0.168541, "2026-06-02");
                Customer c5 = new Customer("Robert Johnson", "9512348765", "Lake View Park, London", 51.492621, -0.183229, "2026-06-03");

                long id1 = db.customerDao().insert(c1);
                long id2 = db.customerDao().insert(c2);
                long id3 = db.customerDao().insert(c3);
                long id4 = db.customerDao().insert(c4);
                long id5 = db.customerDao().insert(c5);

                // Add standard delivery logs for June to make the Monthly Recap beautiful of 93.33% calculations!
                String selectedYearMonth = "2026-06-";
                int totalDays = 9; // up to current June 9, 2026

                // Customers deliveries seeding
                for (int d = 1; d <= totalDays; d++) {
                    String dateStr = String.format(java.util.Locale.getDefault(), "%s%02d", selectedYearMonth, d);
                    
                    // John Doe (Delivered all except June 5, which is Pending)
                    if (d != 5) {
                        db.deliveryDao().insert(new Delivery(id1, dateStr, "07:45 AM", "Delivered"));
                    } else {
                        db.deliveryDao().insert(new Delivery(id1, dateStr, "--", "Pending"));
                    }

                    // David Smith (Delivered all)
                    db.deliveryDao().insert(new Delivery(id2, dateStr, "07:50 AM", "Delivered"));

                    // Michael Brown (Delivered all except June 3, which is Pending)
                    if (d != 3) {
                        db.deliveryDao().insert(new Delivery(id3, dateStr, "07:40 AM", "Delivered"));
                    } else {
                        db.deliveryDao().insert(new Delivery(id3, dateStr, "--", "Pending"));
                    }

                    // James Wilson
                    db.deliveryDao().insert(new Delivery(id4, dateStr, "08:10 AM", "Delivered"));

                    // Robert Johnson
                    db.deliveryDao().insert(new Delivery(id5, dateStr, "08:05 AM", "Delivered"));
                }
            }
        });
    }
}
