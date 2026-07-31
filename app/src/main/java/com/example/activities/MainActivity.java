package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.ActivityMainBinding;
import com.example.fragments.CustomerListFragment;
import com.example.fragments.MapFragment;
import com.example.viewmodel.MilkViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.SharedPreferences;
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
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private MilkViewModel viewModel;
    private Fragment activeFragment;
    private String currentUserType;

    private ActionBarDrawerToggle toggle;

    private ImageView imgProfile;
    private ImageView imgCamera;
    private TextView txtHeaderName;
    private TextView txtHeaderBusiness;
    private TextView txtHeaderRole;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private boolean isActivityCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  If activity is not the root, finish it
        if (!isTaskRoot()) {
            Log.d("MAIN_ACTIVITY", "❌ Activity is not task root, finishing duplicate");
            finish();
            return;
        }

        // Prevent double creation - Fix for super.onCreate
        if (savedInstanceState != null && isActivityCreated) {
            Log.d("MAIN_ACTIVITY", "❌ Activity already created, skipping duplicate");
            //  Don't call super.onCreate again, just return
            return;
        }

        //  Theme check
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

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

        //  Setup Back Press Dispatcher for AndroidX
        setupBackPressedDispatcher();

        setupMenuByRole();

        setSupportActionBar(binding.toolbar);

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
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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
                                imgProfile.animate().alpha(1f).setDuration(400).start();

                                SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                                String accountId = prefs.getString("account_id", "");
                                prefs.edit().putString("profile_" + accountId, path).apply();

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
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        imgProfile.setOnClickListener(v -> {
            SharedPreferences pref1 = getSharedPreferences("UserSession", MODE_PRIVATE);
            String accountId = pref1.getString("account_id", "");
            String image = pref1.getString("profile_" + accountId, "");

            if (image.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
                return;
            }

            File imageFile = new File(image);
            if (!imageFile.exists()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            ImageView imageView = new ImageView(this);
            Glide.with(this).load(imageFile).into(imageView);
            builder.setView(imageView);
            builder.setPositiveButton("Close", (d, w) -> d.dismiss());
            builder.setNegativeButton("Delete", (d, w) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Remove Photo")
                        .setMessage("Delete profile photo?")
                        .setPositiveButton("Delete", (d2, w2) -> {
                            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                            String accId = prefs.getString("account_id", "");
                            String path = prefs.getString("profile_" + accId, "");
                            File file = new File(path);
                            if (file.exists()) file.delete();
                            prefs.edit().remove("profile_" + accId).apply();
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
                        SharedPreferences pref1 = getSharedPreferences("UserSession", MODE_PRIVATE);
                        String accountId = pref1.getString("account_id", "");
                        String path = pref1.getString("profile_" + accountId, "");
                        File file = new File(path);
                        if (file.exists()) file.delete();
                        pref1.edit().remove("profile_" + accountId).apply();
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
            activeFragment = fragment;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                invalidateOptionsMenu();
                supportInvalidateOptionsMenu();
            }, 100);
            isActivityCreated = true;
            return;
        }

        if (savedInstanceState == null) {
            BottomNavigationView bottomNav = binding.bottomNavigation;
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_map);
            }

            MapFragment mapFragment = new MapFragment();
            activeFragment = mapFragment;

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, mapFragment)
                    .commit();

            refreshMenuMultipleTimes();
        } else {
            activeFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (activeFragment == null) {
                activeFragment = new MapFragment();
            }
        }

        isActivityCreated = true;
    }

    //  Setup AndroidX Back Press Dispatcher
    private void setupBackPressedDispatcher() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle back press
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (!(activeFragment instanceof MapFragment) && !(activeFragment instanceof CustomerListFragment)) {
                    navigateToMenuItem(R.id.nav_home);
                    showBottomNavigation();
                } else {
                    //  Call finish() to close activity
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d("MAIN_ACTIVITY", "✅ onNewIntent called - Activity reused");
    }

    private void refreshMenuMultipleTimes() {
        binding.toolbar.post(() -> {
            invalidateOptionsMenu();
            supportInvalidateOptionsMenu();
            binding.toolbar.invalidate();
            Log.d("MAIN_ACTIVITY", "Menu refresh attempt 1");
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            invalidateOptionsMenu();
            supportInvalidateOptionsMenu();
            Log.d("MAIN_ACTIVITY", "Menu refresh attempt 2");
        }, 100);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            invalidateOptionsMenu();
            supportInvalidateOptionsMenu();
            if (binding.toolbar != null) {
                binding.toolbar.invalidate();
            }
            Log.d("MAIN_ACTIVITY", "Menu refresh attempt 3");
        }, 300);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            invalidateOptionsMenu();
            supportInvalidateOptionsMenu();
            Log.d("MAIN_ACTIVITY", "Menu refresh attempt 4");
        }, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showBottomNavigation();
        invalidateOptionsMenu();
        supportInvalidateOptionsMenu();
    }

    private void showBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
            Log.d("NAV_DEBUG", " Bottom nav SHOWN");
        }
    }

    private void hideBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.GONE);
            Log.d("NAV_DEBUG", " Bottom nav HIDDEN");
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        if (bottomNav == null) {
            Log.e("NAV_DEBUG", " Bottom navigation is NULL!");
            return;
        }

        Log.d("NAV_DEBUG", " Bottom navigation found, setting up...");
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.setSelectedItemId(R.id.nav_map);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_map) {
                MapFragment mapFragment = new MapFragment();
                activeFragment = mapFragment;
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, mapFragment)
                        .commit();
                showBottomNavigation();
                invalidateOptionsMenu();
                return true;
            } else if (itemId == R.id.nav_customer_list) {
                CustomerListFragment customerListFragment = new CustomerListFragment();
                activeFragment = customerListFragment;
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, customerListFragment)
                        .commit();
                showBottomNavigation();
                invalidateOptionsMenu();
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
            Intent intent = new Intent(MainActivity.this, AddCustomer_Activity.class);
            startActivity(intent);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_staff_list) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, StaffList_Activity.class);
            startActivity(intent);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_monthly_recap) {
            Intent intent = new Intent(MainActivity.this, MonthlyRecap_Activity.class);
            startActivity(intent);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_payments) {
            Intent intent = new Intent(MainActivity.this, Payment_Activity.class);
            startActivity(intent);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(MainActivity.this, Settings_Activity.class);
            startActivity(intent);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_about_us) {
            Intent intent = new Intent(MainActivity.this, AboutUs_Activity.class);
            startActivity(intent);
            hideBottomNavigation();
            return;

        } else if (itemId == R.id.nav_logout) {
            new AlertDialog.Builder(this)
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

                if (fragment instanceof MapFragment || fragment instanceof CustomerListFragment) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    toggle.setDrawerIndicatorEnabled(true);
                    toggle.syncState();
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    toggle.setDrawerIndicatorEnabled(false);
                }
            }
            invalidateOptionsMenu();
        }
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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addCustomer = menu.findItem(R.id.action_add_customer);
        MenuItem addStaff = menu.findItem(R.id.action_add_staff);
        MenuItem filter = menu.findItem(R.id.action_filter);
        MenuItem searchStaff = menu.findItem(R.id.action_search_staff);

        boolean isOwner = "Owner".equals(currentUserType);
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        boolean isMap = currentFragment instanceof MapFragment;

        if (addCustomer != null) {
            addCustomer.setVisible(isOwner && isMap);
        }

        if (addStaff != null) {
            addStaff.setVisible(false);
        }

        if (filter != null) {
            filter.setVisible(false);
        }

        if (searchStaff != null) {
            searchStaff.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof MapFragment || currentFragment instanceof CustomerListFragment) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }

            navigateToMenuItem(R.id.nav_home);
            showBottomNavigation();
            return true;
        }

        if (itemId == R.id.action_add_customer) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return true;
            }
            Intent intent = new Intent(MainActivity.this, AddCustomer_Activity.class);
            startActivity(intent);
            return true;
        }

        if (itemId == R.id.action_add_staff) {
            if (!"Owner".equals(currentUserType)) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
                return true;
            }
            Intent intent = new Intent(MainActivity.this, AddStaff_Activity.class);
            startActivity(intent);
            return true;
        }

        if (itemId == R.id.action_filter || itemId == R.id.action_search_staff) {
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
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String accountId = prefs.getString("account_id", "");

            if (accountId.isEmpty()) {
                Log.e("MainActivity", "❌ Account ID is empty!");
                return "";
            }

            File folder = new File(getFilesDir(), "profile");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, "profile_" + accountId + ".jpg");
            InputStream input = getContentResolver().openInputStream(uri);

            if (input == null) {
                Log.e("MainActivity", "❌ Failed to open input stream");
                return "";
            }

            FileOutputStream output = new FileOutputStream(file);
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
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);

        String role = pref.getString("userType", "");
        String accountId = pref.getString("account_id", "");
        String headerName = pref.getString("header_name", "");
        String business = pref.getString("business_name", "");

        Log.d("MainActivity", "📋 Loading header data...");
        Log.d("MainActivity", "📋 Account ID: " + accountId);
        Log.d("MainActivity", "📋 Header Name: " + headerName);
        Log.d("MainActivity", "📋 Business Name: " + business);
        Log.d("MainActivity", "📋 Role: " + role);

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
            Log.d("MainActivity", "📋 Staff: Fetching business_name from API...");
            fetchBusinessNameFromAPI(accountId);
        }
    }

    private void fetchBusinessNameFromAPI(String accountId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getProfile(accountId).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.body() == null || response.body().getData() == null) {
                    return;
                }

                MyData owner = response.body().getData();
                String businessName = owner.getBusinessName();

                if (businessName != null && !businessName.trim().isEmpty()) {
                    SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                    prefs.edit().putString("business_name", businessName).apply();
                    txtHeaderBusiness.setText(businessName);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("PROFILE_API", "❌ Failure = " + t.getMessage());
            }
        });
    }

    //  Removed onBackPressed() - Using OnBackPressedDispatcher instead
}