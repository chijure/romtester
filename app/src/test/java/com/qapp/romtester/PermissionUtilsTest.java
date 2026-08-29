package com.qapp.romtester;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Test;

public class PermissionUtilsTest {

    private static final int PRE_M = 22;
    private static final int POST_M = 23;

    @Test
    public void has_alwaysTrueBelowMarshmallow() {
        Context context = mock(Context.class);
        assertTrue(PermissionUtils.has(context, "any.permission", PRE_M));
    }

    @Test
    public void has_checksSelfPermissionOnMarshmallowAndAbove() {
        Context context = mock(Context.class);
        when(context.checkSelfPermission("granted.permission")).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(context.checkSelfPermission("denied.permission")).thenReturn(PackageManager.PERMISSION_DENIED);

        assertTrue(PermissionUtils.has(context, "granted.permission", POST_M));
        assertFalse(PermissionUtils.has(context, "denied.permission", POST_M));
    }

    @Test
    public void hasAll_falseIfAnyPermissionMissing() {
        Context context = mock(Context.class);
        when(context.checkSelfPermission("a")).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(context.checkSelfPermission("b")).thenReturn(PackageManager.PERMISSION_DENIED);

        assertFalse(PermissionUtils.hasAll(context, POST_M, "a", "b"));
    }

    @Test
    public void hasAll_trueWhenEveryPermissionGranted() {
        Context context = mock(Context.class);
        when(context.checkSelfPermission("a")).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(context.checkSelfPermission("b")).thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(PermissionUtils.hasAll(context, POST_M, "a", "b"));
    }

    @Test
    public void granted_falseForEmptyResults() {
        assertFalse(PermissionUtils.granted(new int[0]));
    }

    @Test
    public void granted_trueWhenFirstResultGranted() {
        assertTrue(PermissionUtils.granted(new int[]{PackageManager.PERMISSION_GRANTED}));
    }

    @Test
    public void granted_falseWhenFirstResultDenied() {
        assertFalse(PermissionUtils.granted(new int[]{PackageManager.PERMISSION_DENIED}));
    }

    @Test
    public void missing_collectsOnlyDeniedPermissions() {
        Context context = mock(Context.class);
        when(context.checkSelfPermission("a")).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(context.checkSelfPermission("b")).thenReturn(PackageManager.PERMISSION_DENIED);
        when(context.checkSelfPermission("c")).thenReturn(PackageManager.PERMISSION_DENIED);

        String[] result = PermissionUtils.missing(context, POST_M, "a", "b", "c");

        assertArrayEquals(new String[]{"b", "c"}, result);
    }

    @Test
    public void missing_emptyWhenAllGranted() {
        Context context = mock(Context.class);
        when(context.checkSelfPermission("a")).thenReturn(PackageManager.PERMISSION_GRANTED);

        assertArrayEquals(new String[0], PermissionUtils.missing(context, POST_M, "a"));
    }
}
