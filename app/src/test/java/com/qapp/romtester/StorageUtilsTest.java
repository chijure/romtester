package com.qapp.romtester;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.StatFs;

import org.junit.Test;

public class StorageUtilsTest {

    @Test
    public void availableBytes_usesLongVariantOnApi18AndAbove() {
        StatFs statFs = mock(StatFs.class);
        when(statFs.getAvailableBlocksLong()).thenReturn(1000L);
        when(statFs.getBlockSizeLong()).thenReturn(4096L);

        assertEquals(1000L * 4096L, StorageUtils.availableBytes(statFs, 18));
    }

    @Test
    public void availableBytes_usesDeprecatedIntVariantBelowApi18() {
        StatFs statFs = mock(StatFs.class);
        when(statFs.getAvailableBlocks()).thenReturn(500);
        when(statFs.getBlockSize()).thenReturn(2048);

        assertEquals(500L * 2048L, StorageUtils.availableBytes(statFs, 17));
    }

    @Test
    public void totalBytes_usesLongVariantOnApi18AndAbove() {
        StatFs statFs = mock(StatFs.class);
        when(statFs.getBlockCountLong()).thenReturn(2000L);
        when(statFs.getBlockSizeLong()).thenReturn(4096L);

        assertEquals(2000L * 4096L, StorageUtils.totalBytes(statFs, 18));
    }

    @Test
    public void totalBytes_usesDeprecatedIntVariantBelowApi18() {
        StatFs statFs = mock(StatFs.class);
        when(statFs.getBlockCount()).thenReturn(800);
        when(statFs.getBlockSize()).thenReturn(2048);

        assertEquals(800L * 2048L, StorageUtils.totalBytes(statFs, 17));
    }
}
