package com.qapp.romtester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestReportTest {

    @Test
    public void toJsonThenFromJson_roundTripsAllFields() throws Exception {
        List<TestReport.CheckEntry> checks = new ArrayList<>();
        checks.add(new TestReport.CheckEntry("Sound", "PASS", "Beep played"));
        checks.add(new TestReport.CheckEntry("Camera", "FAIL", "No camera hardware"));
        checks.add(new TestReport.CheckEntry("Touch", "MANUAL", "Open touch test"));

        TestReport original = new TestReport(1234567890L, "Pixel 9", "15", "google/pixel/redfin",
                5, 2, 1, 3, checks);

        JSONObject json = original.toJson();
        TestReport restored = TestReport.fromJson(json);

        assertEquals(original.timestamp, restored.timestamp);
        assertEquals(original.model, restored.model);
        assertEquals(original.androidVersion, restored.androidVersion);
        assertEquals(original.fingerprint, restored.fingerprint);
        assertEquals(original.passCount, restored.passCount);
        assertEquals(original.failCount, restored.failCount);
        assertEquals(original.naCount, restored.naCount);
        assertEquals(original.manualCount, restored.manualCount);
        assertEquals(original.checks.size(), restored.checks.size());
        for (int i = 0; i < original.checks.size(); i++) {
            assertEquals(original.checks.get(i).name, restored.checks.get(i).name);
            assertEquals(original.checks.get(i).status, restored.checks.get(i).status);
            assertEquals(original.checks.get(i).detail, restored.checks.get(i).detail);
        }
    }

    @Test
    public void toJsonThenFromJson_handlesEmptyChecklist() throws Exception {
        TestReport original = new TestReport(1L, "Model", "8", "fp", 0, 0, 0, 0,
                new ArrayList<TestReport.CheckEntry>());

        TestReport restored = TestReport.fromJson(original.toJson());

        assertTrue(restored.checks.isEmpty());
    }

    @Test
    public void toJsonThenFromJson_preservesSpecialCharactersInDetail() throws Exception {
        String tricky = "Line with \"quotes\", a\nnewline, and unicode ñ é";
        List<TestReport.CheckEntry> checks = Arrays.asList(
                new TestReport.CheckEntry("Weird", "PASS", tricky));
        TestReport original = new TestReport(2L, "Model", "8", "fp", 1, 0, 0, 0, checks);

        TestReport restored = TestReport.fromJson(original.toJson());

        assertEquals(tricky, restored.checks.get(0).detail);
    }
}
