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
import androidx.appcompat.app.AppCompatDelegate;  // ✅ Import add kara
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

//  Import for header views
import android.widget.ImageView;
import android.widget.TextView;

import com.example.models.LoginResponse;
import com.example.models.MyData;
import com.example.network.ApiClient;
import com.example.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//  Step 8.1: Imports for ActivityResultLauncher
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.net.Uri;
import android.provider.MediaStore;
import android.app.Activity;

//  Step 9.2: Imports for Glide and File operations
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

//  Step 10.3: Import for AlertDialog
import androidx.appcompat.app.AlertDialog;

//  Import for Gson (optional - if needed for debugging)
import com.google.gson.Gson;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;
    private String currentUserType;

    private ActionBarDrawerToggle toggle;

    //  Header views
    private ImageView imgProfile;
    private ImageView imgCamera;
    private TextView txtHeaderName;
    private TextView txtHeaderBusiness;
    private TextView txtHeaderRole;

    //  Step 8.2: Variables for image picking
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ THEME CHECK - Apply saved theme BEFORE loading layout
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //  CHECK SESSION - If no userType or mobile, redirect to Login
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserType = pref.getString("userType", null);
        String mobile = pref.getString("mobile", null);

        //  If not logged in, redirect to Login
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

        //  Step 8.3: Register ActivityResultLauncher for image picking
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            //  Save image and load with Glide
                            String path = saveProfileImage(imageUri);

                            if (!path.isEmpty()) {
                                //  Step 10.9: Animation
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

                                //  Save path
                                prefs.edit()
                                        .putString("profile_" + accountId, path)
                                        .apply();

                                Log.d("MainActivity", " Photo saved at: " + path);
                                Log.d("MainActivity", " Account ID: " + accountId);

                                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        //  Drawer Header - Bind views and load data
        View headerView = binding.navView.getHeaderView(0);
        imgProfile = headerView.findViewById(R.id.imgProfile);
        imgCamera = headerView.findViewById(R.id.imgCamera);
        txtHeaderName = headerView.findViewById(R.id.txtHeaderName);
        txtHeaderBusiness = headerView.findViewById(R.id.txtHeaderBusiness);
        txtHeaderRole = headerView.findViewById(R.id.txtHeaderRole);

        loadHeaderData();

        //  Camera icon click - Always opens gallery (to add/change photo)
        imgCamera.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        //  Profile Image click - Add photo if empty, else show full image
        imgProfile.setOnClickListener(v -> {

            SharedPreferences pref1 =
                    getSharedPreferences("UserSession", MODE_PRIVATE);

            String accountId =
                    pref1.getString("account_id", "");

            String image =
                    pref1.getString("profile_" + accountId, "");

            Log.d("MainActivity", "🔍 Click - Account ID: " + accountId);
            Log.d("MainActivity", "🔍 Click - Image Path: " + image);

            //  जर photo नसेल तर Gallery उघडा
            if (image.isEmpty()) {
                Log.d("MainActivity", "📷 No photo found, opening gallery...");
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
                return;
            }

            //  जर photo असेल तर full screen मध्ये दाखवा
            File imageFile = new File(image);
            if (!imageFile.exists()) {
                Log.d("MainActivity", "❌ Photo file not found, opening gallery...");
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

        //  Step 10.7: Long Press - Remove Photo (alternative way)
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
            // ✅ Open MonthlyRecap_Activity directly
            Intent intent = new Intent(MainActivity.this, MonthlyRecap_Activity.class);
            startActivity(intent);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
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
                        //  Clear all session data
                        getSharedPreferences("UserSession", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply();

                        //  Redirect to Login
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

        // ✅ Filter button hide kara - MonthlyRecap_Activity madhe already filter ahe
        if (filter != null) {
            filter.setVisible(false);
        }

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

    //  Step 9.3: Method to save profile image to internal storage
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

            Log.d("MainActivity", "📁 Saving to: " + file.getAbsolutePath());

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

            Log.d("MainActivity", " Image saved successfully: " + file.getAbsolutePath());
            Log.d("MainActivity", " File size: " + file.length() + " bytes");

            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e("MainActivity", "❌ Error saving image: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    //  Method to load header data - Direct from SharedPreferences
    private void loadHeaderData() {

        SharedPreferences pref =
                getSharedPreferences("UserSession", MODE_PRIVATE);

        String role =
                pref.getString("userType", "");

        String accountId =
                pref.getString("account_id", "");

        String headerName =
                pref.getString("header_name", "");

        //  Directly get business_name from SharedPreferences (saved during login)
        String business =
                pref.getString("business_name", "");

        Log.d("MainActivity", "📋 Loading header data...");
        Log.d("MainActivity", "📋 Account ID: " + accountId);
        Log.d("MainActivity", "📋 Header Name: " + headerName);
        Log.d("MainActivity", "📋 Business Name: " + business);
        Log.d("MainActivity", "📋 Role: " + role);

        //  Set header text
        txtHeaderName.setText(headerName);

        //  Set business name with fallback
        if (business == null || business.trim().isEmpty()) {
            business = "Milk Delivery";
        }
        txtHeaderBusiness.setText(business);

        txtHeaderRole.setText(role);

        //  Load profile photo from internal storage
        String imagePath = pref.getString("profile_" + accountId, "");
        Log.d("MainActivity", "📋 Image Path from prefs: " + imagePath);

        if (!imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Log.d("MainActivity", " Loading saved photo from: " + imagePath);
                Glide.with(this)
                        .load(imageFile)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(imgProfile);
            } else {
                Log.d("MainActivity", "❌ Image file not found at: " + imagePath);
                imgProfile.setImageResource(R.drawable.ic_user);
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_user);
        }

        //  For Staff only: If business_name is empty, fetch from API
        // Owner business_name is already saved during login
        if ("Staff".equals(role) && (business == null || business.trim().isEmpty())) {
            Log.d("MainActivity", "📋 Staff: Fetching business_name from API...");
            fetchBusinessNameFromAPI(accountId);
        }
    }

    //  Helper method to fetch business_name for Staff from API
    private void fetchBusinessNameFromAPI(String accountId) {
        ApiService api =
                ApiClient.getClient().create(ApiService.class);

        api.getProfile(accountId).enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {

                Log.d("PROFILE_API", "📡 Code = " + response.code());
                Log.d("PROFILE_API", "📡 Success = " + response.isSuccessful());

                if (response.body() == null || response.body().getData() == null) {
                    Log.d("PROFILE_API", "❌ No data in response");
                    return;
                }

                MyData owner = response.body().getData();
                String businessName = owner.getBusinessName();

                Log.d("PROFILE", "🏢 Business Name from API: " + businessName);

                if (businessName != null && !businessName.trim().isEmpty()) {
                    //  Save business_name to SharedPreferences for future use
                    SharedPreferences prefs =
                            getSharedPreferences("UserSession", MODE_PRIVATE);
                    prefs.edit()
                            .putString("business_name", businessName)
                            .apply();

                    //  Update UI
                    txtHeaderBusiness.setText(businessName);
                    Log.d("MainActivity", " Staff business_name updated: " + businessName);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call,
                                  Throwable t) {
                Log.e("PROFILE_API", "❌ Failure = " + t.getMessage());
            }
        });
    }
}