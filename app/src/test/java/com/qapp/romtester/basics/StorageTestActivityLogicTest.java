package com.qapp.romtester.basics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StorageTestActivityLogicTest {

    @Test
    public void computeFileSize_clampsToMinimumForSmallTargets() {
        long target = 128L * 1024 * 1024; // would compute to 1MB per file
        assertEquals(StorageTestActivity.MIN_FILE_SIZE, StorageTestActivity.computeFileSize(target));
    }

    @Test
    public void computeFileSize_usesEvenSplitInMidRange() {
        long target = 64L * 1024 * 1024 * StorageTestActivity.DESIRED_FILE_COUNT;
        assertEquals(64L * 1024 * 1024, StorageTestActivity.computeFileSize(target));
    }

    @Test
    public void computeFileSize_clampsToMaximumForHugeTargets() {
        long target = 1024L * 1024 * 1024 * 1024; // 1TB
        assertEquals(StorageTestActivity.MAX_FILE_SIZE, StorageTestActivity.computeFileSize(target));
    }

    @Test
    public void regionsEqual_trueForIdenticalRegion() {
        byte[] a = {1, 2, 3, 4};
        byte[] b = {1, 2, 3, 9};
        assertTrue(StorageTestActivity.regionsEqual(a, b, 3));
    }

    @Test
    public void regionsEqual_falseWhenMismatchWithinLength() {
        byte[] a = {1, 2, 3, 4};
        byte[] b = {1, 2, 9, 4};
        assertFalse(StorageTestActivity.regionsEqual(a, b, 3));
    }

    @Test
    public void regionsEqual_trueForZeroLength() {
        byte[] a = {1};
        byte[] b = {2};
        assertTrue(StorageTestActivity.regionsEqual(a, b, 0));
    }

    @Test
    public void readFully_readsExactLengthFromStream() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        InputStream in = new ByteArrayInputStream(data);
        byte[] buf = new byte[5];
        int read = StorageTestActivity.readFully(in, buf, 5);
        assertEquals(5, read);
        assertTrue(StorageTestActivity.regionsEqual(data, buf, 5));
    }

    @Test
    public void readFully_returnsShortCountOnEarlyEndOfStream() throws IOException {
        byte[] data = {1, 2, 3};
        InputStream in = new ByteArrayInputStream(data);
        byte[] buf = new byte[10];
        int read = StorageTestActivity.readFully(in, buf, 10);
        assertEquals(3, read);
    }

    @Test
    public void readFully_aggregatesAcrossShortReads() throws IOException {
        byte[] data = {9, 8, 7, 6, 5};
        InputStream oneByteAtATime = new InputStream() {
            private int pos = 0;

            @Override
            public int read() {
                return pos < data.length ? (data[pos++] & 0xFF) : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (pos >= data.length) {
                    return -1;
                }
                b[off] = data[pos++];
                return 1;
            }
        };
        byte[] buf = new byte[5];
        int read = StorageTestActivity.readFully(oneByteAtATime, buf, 5);
        assertEquals(5, read);
        assertTrue(StorageTestActivity.regionsEqual(data, buf, 5));
    }
}
