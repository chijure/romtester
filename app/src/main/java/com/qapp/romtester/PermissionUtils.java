package com.qapp.romtester;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public final class PermissionUtils {

    private PermissionUtils() {
    }

    public static boolean has(Context context, String permission) {
        return has(context, permission, Build.VERSION.SDK_INT);
    }

    static boolean has(Context context, String permission, int sdkInt) {
        if (sdkInt < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAll(Context context, String... permissions) {
        return hasAll(context, Build.VERSION.SDK_INT, permissions);
    }

    static boolean hasAll(Context context, int sdkInt, String... permissions) {
        for (String permission : permissions) {
            if (!has(context, permission, sdkInt)) {
                return false;
            }
        }
        return true;
    }

    public static boolean granted(int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public static String[] missing(Context context, String... permissions) {
        return missing(context, Build.VERSION.SDK_INT, permissions);
    }

    static String[] missing(Context context, int sdkInt, String... permissions) {
        List<String> missing = new ArrayList<>();
        for (String permission : permissions) {
            if (!has(context, permission, sdkInt)) {
                missing.add(permission);
            }
        }
        return missing.toArray(new String[0]);
    }
}
