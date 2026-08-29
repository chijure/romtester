package com.qapp.romtester;

import android.os.Build;
import android.os.StatFs;

import java.io.File;

public final class StorageUtils {

    private StorageUtils() {
    }

    public static long availableBytes(File dir) {
        return availableBytes(new StatFs(dir.getPath()), Build.VERSION.SDK_INT);
    }

    @SuppressWarnings("deprecation")
    static long availableBytes(StatFs statFs, int sdkInt) {
        if (sdkInt >= 18) {
            return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        }
        return (long) statFs.getAvailableBlocks() * statFs.getBlockSize();
    }

    public static long totalBytes(File dir) {
        return totalBytes(new StatFs(dir.getPath()), Build.VERSION.SDK_INT);
    }

    @SuppressWarnings("deprecation")
    static long totalBytes(StatFs statFs, int sdkInt) {
        if (sdkInt >= 18) {
            return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
        }
        return (long) statFs.getBlockCount() * statFs.getBlockSize();
    }
}
