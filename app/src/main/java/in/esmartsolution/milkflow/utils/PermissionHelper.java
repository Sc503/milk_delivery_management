package in.esmartsolution.milkflow.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // ✅ WiFi Direct requires location on ALL Android 10+ devices
        // This is MANDATORY for Samsung devices
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // ✅ WiFi state permissions
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);

        // ✅ Nearby WiFi Devices permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        // ✅ Storage permissions (for older Android versions)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        return permissions.toArray(new String[0]);
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : getRequiredPermissions()) {
            // ✅ Skip WRITE_EXTERNAL_STORAGE check on Android 11+ (Scoped Storage)
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                continue;
            }

            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}