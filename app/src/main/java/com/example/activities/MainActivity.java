package com.example.activities;

import com.example.fragments.PaymentDetailsFragment;
import com.example.fragments.PaymentFragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;

import android.view.View;
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
import com.example.fragments.CustomerListFragment;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.SharedPreferences;
import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.database.AppDatabase;
import com.example.models.User;

import android.widget.ImageView;
import android.widget.TextView;

import com.example.models.LoginResponse;
import com.example.models.MyData;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.net.Uri;
import android.provider.MediaStore;
import android.app.Activity;

import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;
    private String currentUserType;

    private ActionBarDrawerToggle toggle;

    // Header views
    private ImageView imgProfile;
    private ImageView imgCamera;
    private TextView txtHeaderName;
    private TextView txtHeaderBusiness;
    private TextView txtHeaderRole;

    // Image picking
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // CHECK SESSION
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserType = pref.getString("userType", null);
        String mobile = pref.getString("mobile", null);

        if (currentUserType == null || mobile == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(MilkViewModel.class);

        Log.d("CHECK", "UserType = " + currentUserType);
        Log.d("CHECK", "Mobile = " + mobile);

        if (currentUserType == null) {
            currentUserType = "Guest";
        }

        setupMenuByRole();

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Welcome " + currentUserType);
        }

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

        setupBottomNavigation();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            String path = saveProfileImage(imageUri);

                            if (!path.isEmpty()) {
                                imgProfile.setAlpha(0f);
                                Glide.with(MainActivity.this)
                                        .load(new File(path))
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_user)
                                        .into(imgProfile);
                                imgProfile.animate()
                                        .alpha(1f)
                                        .setDuration(400)
                                        .start();

                                SharedPreferences prefs =
                                        getSharedPreferences("UserSession", MODE_PRIVATE);
                                String accountId =
                                        prefs.getString("account_id", "");

                                prefs.edit()
                                        .putString("profile_" + accountId, path)
                                        .apply();

                                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        View headerView = binding.navView.getHeaderView(0);
        imgProfile = headerView.findViewById(R.id.imgProfile);
        imgCamera = headerView.findViewById(R.id.imgCamera);
        txtHeaderName = headerView.findViewById(R.id.txtHeaderName);
        txtHeaderBusiness = headerView.findViewById(R.id.txtHeaderBusiness);
        txtHeaderRole = headerView.findViewById(R.id.txtHeaderRole);

        loadHeaderData();

        imgCamera.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        imgProfile.setOnClickListener(v -> {

            SharedPreferences pref1 =
                    getSharedPreferences("UserSession", MODE_PRIVATE);

            String accountId =
                    pref1.getString("account_id", "");

            String image =
                    pref1.getString("profile_" + accountId, "");

            if (image.isEmpty()) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
                return;
            }

            File imageFile = new File(image);
            if (!imageFile.exists()) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
                return;
            }

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(this);
            ImageView imageView = new ImageView(this);

            Glide.with(this)
                    .load(imageFile)
                    .into(imageView);

            builder.setView(imageView);
            builder.setPositiveButton("Close", (d, w) -> d.dismiss());

            builder.setNegativeButton("Delete", (d, w) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Remove Photo")
                        .setMessage("Delete profile photo?")
                        .setPositiveButton("Delete", (d2, w2) -> {
                            SharedPreferences prefs =
                                    getSharedPreferences("UserSession", MODE_PRIVATE);
                            String accId = prefs.getString("account_id", "");
                            String path = prefs.getString("profile_" + accId, "");
                            File file = new File(path);
                            if (file.exists())
                                file.delete();
                            prefs.edit()
                                    .remove("profile_" + accId)
                                    .apply();
                            imgProfile.setImageResource(R.drawable.ic_user);
                            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            builder.show();
        });

        imgProfile.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Remove Photo")
                    .setMessage("Delete profile photo?")
                    .setPositiveButton("Delete", (d, w) -> {
                        SharedPreferences pref1 =
                                getSharedPreferences("UserSession",
                                        MODE_PRIVATE);
                        String accountId =
                                pref1.getString("account_id", "");
                        String path =
                                pref1.getString("profile_" + accountId, "");
                        File file = new File(path);
                        if (file.exists())
                            file.delete();
                        pref1.edit()
                                .remove("profile_" + accountId)
                                .apply();
                        imgProfile.setImageResource(R.drawable.ic_user);
                        Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

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

        if (savedInstanceState == null) {
            BottomNavigationView bottomNav = binding.bottomNavigation;
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_map);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();
        }
    }

    // ✅ Show bottom navigation
    private void showBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
        }
    }

    // ✅ Hide bottom navigation
    private void hideBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        if (bottomNav == null) return;

        // ✅ Show bottom nav by default on main screen
        bottomNav.setVisibility(View.VISIBLE);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_map) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new MapFragment())
                        .commit();
                showBottomNavigation();
                return true;
            } else if (itemId == R.id.nav_customer_list) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CustomerListFragment())
                        .commit();
                showBottomNavigation();
                return true;
            }
            return false;
        });
    }

     public void navigateToMenuItem(int itemId) {
        Fragment fragment = null;
        String tag = "";
        String toolbarTitle = "Milk Delivery";

        if (itemId == R.id.nav_home) {
            fragment = new MapFragment();
            tag = "MAP_FRAGMENT";
            toolbarTitle = "Milk Delivery";
            binding.navView.setCheckedItem(R.id.nav_home);
            showBottomNavigation();

        } else if (itemId == R.id.nav_add_customer) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return;
            }
            fragment = new AddCustomerFragment();
            tag = "ADD_CUSTOMER_FRAGMENT";
            toolbarTitle = "Add New Customer";
            binding.navView.setCheckedItem(R.id.nav_add_customer);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_staff_list) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return;
            }
            fragment = new StaffListFragment();
            tag = "STAFF_LIST_FRAGMENT";
            toolbarTitle = "Staff List";
            binding.navView.setCheckedItem(R.id.nav_staff_list);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_monthly_recap) {

            fragment = new MonthlyRecapFragment();
            tag = "MONTHLY_RECAP_FRAGMENT";
            toolbarTitle = "Monthly Recap";
            binding.navView.setCheckedItem(R.id.nav_monthly_recap);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_payments) {
            fragment = new PaymentFragment();
            tag = "PAYMENT_FRAGMENT";
            toolbarTitle = "Payments";
            binding.navView.setCheckedItem(R.id.nav_payments);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "SETTINGS_FRAGMENT";
            toolbarTitle = "Settings";
            binding.navView.setCheckedItem(R.id.nav_settings);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_about_us) {
            fragment = new AboutUsFragment();
            tag = "ABOUT_FRAGMENT";
            toolbarTitle = "About Us";
            binding.navView.setCheckedItem(R.id.nav_about_us);
            hideBottomNavigation();

        } else if (itemId == R.id.nav_logout) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        getSharedPreferences("UserSession", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply();

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

            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof PaymentDetailsFragment) {

                getSupportFragmentManager().popBackStack();

                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                toggle.setDrawerIndicatorEnabled(true);
                toggle.syncState();

                return true;
            }

            if (currentFragment instanceof AddStaffFragment) {

                navigateToMenuItem(R.id.nav_staff_list);
                return true;
            }

            binding.drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        if (item.getItemId() == R.id.action_add_shortcut) {
            navigateToMenuItem(R.id.nav_add_customer);
            return true;
        }

        if (item.getItemId() == R.id.action_add_staff) {
            Intent intent = new Intent(MainActivity.this, AddStaff_Activity.class);
            startActivity(intent);
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

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(true);
        }

        if ("Owner".equals(currentUserType)) {
            Log.d("CHECK", "Owner menu loaded - everything visible");
        } else if ("Staff".equals(currentUserType)) {
            Log.d("CHECK", "Staff menu loaded - limited access");
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

    private String saveProfileImage(Uri uri) {
        try {
            SharedPreferences prefs =
                    getSharedPreferences("UserSession", MODE_PRIVATE);
            String accountId =
                    prefs.getString("account_id", "");

            if (accountId.isEmpty()) {
                Log.e("MainActivity", "❌ Account ID is empty!");
                return "";
            }

            File folder =
                    new File(getFilesDir(), "profile");
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                Log.d("MainActivity", "📁 Folder created: " + created);
            }

            File file =
                    new File(folder,
                            "profile_" + accountId + ".jpg");

            InputStream input =
                    getContentResolver().openInputStream(uri);

            if (input == null) {
                Log.e("MainActivity", "❌ Failed to open input stream");
                return "";
            }

            FileOutputStream output =
                    new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int len;

            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            input.close();
            output.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e("MainActivity", "❌ Error saving image: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    private void loadHeaderData() {

        SharedPreferences pref =
                getSharedPreferences("UserSession", MODE_PRIVATE);

        String role =
                pref.getString("userType", "");

        String accountId =
                pref.getString("account_id", "");

        String headerName =
                pref.getString("header_name", "");

        String business =
                pref.getString("business_name", "");

        txtHeaderName.setText(headerName);

        if (business == null || business.trim().isEmpty()) {
            business = "Milk Delivery";
        }
        txtHeaderBusiness.setText(business);

        txtHeaderRole.setText(role);

        String imagePath = pref.getString("profile_" + accountId, "");
        if (!imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(imgProfile);
            } else {
                imgProfile.setImageResource(R.drawable.ic_user);
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_user);
        }

        if ("Staff".equals(role) && (business == null || business.trim().isEmpty())) {
            fetchBusinessNameFromAPI(accountId);
        }
    }

    private void fetchBusinessNameFromAPI(String accountId) {
        ApiService api =
                ApiClient.getClient().create(ApiService.class);

        api.getProfile(accountId).enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {

                if (response.body() == null || response.body().getData() == null) {
                    return;
                }

                MyData owner = response.body().getData();
                String businessName = owner.getBusinessName();

                if (businessName != null && !businessName.trim().isEmpty()) {
                    SharedPreferences prefs =
                            getSharedPreferences("UserSession", MODE_PRIVATE);
                    prefs.edit()
                            .putString("business_name", businessName)
                            .apply();

                    txtHeaderBusiness.setText(businessName);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call,
                                  Throwable t) {
                Log.e("PROFILE_API", "❌ Failure = " + t.getMessage());
            }
        });
    }


    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else if (!(activeFragment instanceof MapFragment) && !(activeFragment instanceof CustomerListFragment)) {
            // If not on main screen, go back to Map and show bottom nav
            navigateToMenuItem(R.id.nav_home);
            showBottomNavigation();
        } else {
            super.onBackPressed();
        }
    }
}