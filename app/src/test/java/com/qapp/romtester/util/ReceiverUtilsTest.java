package com.qapp.romtester.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import org.junit.Test;

public class ReceiverUtilsTest {

    @Test
    public void register_usesExportedFlagOverloadOnApi33AndAbove() {
        Context context = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        IntentFilter filter = new IntentFilter();

        ReceiverUtils.register(context, receiver, filter, 33);

        verify(context).registerReceiver(eq(receiver), eq(filter), eq(Context.RECEIVER_NOT_EXPORTED));
        verify(context, never()).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void register_usesTwoArgOverloadBelowApi33() {
        Context context = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        IntentFilter filter = new IntentFilter();

        ReceiverUtils.register(context, receiver, filter, 32);

        verify(context).registerReceiver(receiver, filter);
        verify(context, never()).registerReceiver(any(), any(), anyInt());
    }

    @Test
    public void unregisterQuietly_swallowsIllegalArgumentException() {
        Context context = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("not registered"))
                .when(context).unregisterReceiver(receiver);

        ReceiverUtils.unregisterQuietly(context, receiver); // must not throw
    }
}
