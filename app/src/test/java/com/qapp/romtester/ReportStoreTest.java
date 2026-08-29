package com.qapp.romtester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ReportStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Context context;

    @Before
    public void setUp() {
        context = mock(Context.class);
        when(context.getFilesDir()).thenReturn(temporaryFolder.getRoot());
    }

    private TestReport reportWithTimestamp(long timestamp) {
        return new TestReport(timestamp, "Model", "8", "fp", 1, 0, 0, 0,
                new ArrayList<TestReport.CheckEntry>());
    }

    @Test
    public void saveReport_thenReadReport_roundTrips() throws Exception {
        TestReport report = reportWithTimestamp(1000L);

        File file = ReportStore.saveReport(context, report);
        TestReport read = ReportStore.readReport(file);

        assertEquals(report.timestamp, read.timestamp);
        assertEquals(report.model, read.model);
    }

    @Test
    public void listReports_sortsByTimestampDescending() throws Exception {
        ReportStore.saveReport(context, reportWithTimestamp(100L));
        ReportStore.saveReport(context, reportWithTimestamp(300L));
        ReportStore.saveReport(context, reportWithTimestamp(200L));

        List<TestReport> reports = ReportStore.listReports(context);

        assertEquals(3, reports.size());
        assertEquals(300L, reports.get(0).timestamp);
        assertEquals(200L, reports.get(1).timestamp);
        assertEquals(100L, reports.get(2).timestamp);
    }

    @Test
    public void listReports_skipsCorruptFiles() throws Exception {
        ReportStore.saveReport(context, reportWithTimestamp(100L));
        File dir = new File(temporaryFolder.getRoot(), "reports");
        File corrupt = new File(dir, "report_garbage.json");
        try (FileWriter writer = new FileWriter(corrupt)) {
            writer.write("this is not valid json {{{");
        }

        List<TestReport> reports = ReportStore.listReports(context);

        assertEquals(1, reports.size());
        assertEquals(100L, reports.get(0).timestamp);
    }

    @Test
    public void deleteReport_removesOnlyThatFile() throws Exception {
        ReportStore.saveReport(context, reportWithTimestamp(100L));
        ReportStore.saveReport(context, reportWithTimestamp(200L));

        ReportStore.deleteReport(context, 100L);
        List<TestReport> reports = ReportStore.listReports(context);

        assertEquals(1, reports.size());
        assertEquals(200L, reports.get(0).timestamp);
    }

    @Test
    public void clearAll_removesEveryReport() throws Exception {
        ReportStore.saveReport(context, reportWithTimestamp(100L));
        ReportStore.saveReport(context, reportWithTimestamp(200L));

        ReportStore.clearAll(context);

        assertTrue(ReportStore.listReports(context).isEmpty());
    }

    @Test
    public void fileForTimestamp_pointsAtExpectedFileName() {
        File file = ReportStore.fileForTimestamp(context, 42L);

        assertEquals("report_42.json", file.getName());
        assertFalse(file.exists());
    }
}
