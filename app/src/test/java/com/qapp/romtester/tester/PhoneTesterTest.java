package com.qapp.romtester.tester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.telephony.TelephonyManager;

import org.junit.Test;

/**
 * openDialer()/openSms() aren't covered here: they construct a real android.content.Intent,
 * whose methods throw on the JVM unit-test stub jar (no device/Robolectric here) - same
 * category of limitation as Sensor/BluetoothAdapter/Camera elsewhere in this codebase.
 */
public class PhoneTesterTest {

    private static final int SDK = 23;

    @Test
    public void readTelephonyInfo_reportsNullOperatorWhenBlank() {
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        when(telephonyManager.getSimState()).thenReturn(TelephonyManager.SIM_STATE_READY);
        when(telephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        when(telephonyManager.getNetworkOperatorName()).thenReturn("");
        PhoneTester tester = new PhoneTester(mock(Activity.class), telephonyManager,
                mock(PhoneTester.Listener.class), SDK);

        PhoneTester.TelephonyInfo info = tester.readTelephonyInfo();

        assertEquals(TelephonyManager.SIM_STATE_READY, info.simState);
        assertEquals(TelephonyManager.PHONE_TYPE_GSM, info.phoneType);
        assertEquals(TelephonyManager.NETWORK_TYPE_LTE, info.networkType);
        assertNull(info.operatorNameOrNull);
    }

    @Test
    public void readTelephonyInfo_reportsOperatorWhenPresent() {
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        when(telephonyManager.getNetworkOperatorName()).thenReturn("Acme Mobile");
        PhoneTester tester = new PhoneTester(mock(Activity.class), telephonyManager,
                mock(PhoneTester.Listener.class), SDK);

        assertEquals("Acme Mobile", tester.readTelephonyInfo().operatorNameOrNull);
    }
}
