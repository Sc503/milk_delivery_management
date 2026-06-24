package com.example.utils;

public class PermissionManager {

    public static boolean canAddCustomer(String userType) {

        return "Owner".equals(userType)
                || "Staff".equals(userType);
    }

    public static boolean canDeleteCustomer(String userType) {

        return "Owner".equals(userType);
    }

    public static boolean canDeliver(String userType) {

        return "Owner".equals(userType)
                || "Staff".equals(userType);
    }

    public static boolean canOpenPayments(String userType) {

        return "Owner".equals(userType)
                || "Staff".equals(userType);
    }

    public static boolean canOpenSettings(String userType) {

        return "Owner".equals(userType);
    }

    public static boolean canResetDatabase(String userType) {

        return "Owner".equals(userType);
    }

    public static boolean canBackup(String userType) {

        return "Owner".equals(userType)
                || "Staff".equals(userType);
    }

    public static boolean canViewMap(String userType) {

        return !"Customer".equals(userType);
    }

    public static boolean isCustomer(String userType) {

        return "Customer".equals(userType);
    }
}