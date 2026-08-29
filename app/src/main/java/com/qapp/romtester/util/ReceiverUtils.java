package com.qapp.romtester.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public final class ReceiverUtils {

    private ReceiverUtils() {
    }

    public static Intent register(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        return register(context, receiver, filter, Build.VERSION.SDK_INT);
    }

    public static Intent register(Context context, BroadcastReceiver receiver, IntentFilter filter, int sdkInt) {
        if (sdkInt >= 33) {
            return context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
        return context.registerReceiver(receiver, filter);
    }

    public static void unregisterQuietly(Context context, BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // Not registered (already unregistered, or registration failed earlier) - fine to ignore.
        }
    }
}
