package com.qapp.romtester;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class NfcTestActivity extends Activity {

    private TextView statusText;
    private TextView tagText;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_test);

        statusText = findViewById(R.id.text_nfc_status);
        tagText = findViewById(R.id.text_nfc_tag);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        refreshStatus();
        handleIntent(getIntent());
    }

    private void refreshStatus() {
        if (nfcAdapter == null) {
            statusText.setText(getString(R.string.nfc_no_hardware));
        } else if (!nfcAdapter.isEnabled()) {
            statusText.setText(getString(R.string.nfc_disabled));
        } else {
            statusText.setText(getString(R.string.nfc_enabled_ready));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            return;
        }
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
        IntentFilter[] filters = {new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            tagText.setText(getString(R.string.nfc_tag_id_format, toHex(tag.getId())));
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
