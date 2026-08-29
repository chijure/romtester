package com.qapp.romtester.report;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportRendererTest {

    @Test
    public void classifyChange_regressionWhenPassBecomesFail() {
        assertEquals(ReportRenderer.ChangeKind.REGRESSION, ReportRenderer.classifyChange("PASS", "FAIL"));
    }

    @Test
    public void classifyChange_improvementWhenFailBecomesPass() {
        assertEquals(ReportRenderer.ChangeKind.IMPROVEMENT, ReportRenderer.classifyChange("FAIL", "PASS"));
    }

    @Test
    public void classifyChange_unchangedWhenStatusIdentical() {
        assertEquals(ReportRenderer.ChangeKind.UNCHANGED, ReportRenderer.classifyChange("PASS", "PASS"));
        assertEquals(ReportRenderer.ChangeKind.UNCHANGED, ReportRenderer.classifyChange("MANUAL", "MANUAL"));
    }

    @Test
    public void classifyChange_unchangedIsSafeWithNullOldStatus() {
        // A check that only exists in the newer report has no matching old entry.
        assertEquals(ReportRenderer.ChangeKind.OTHER, ReportRenderer.classifyChange(null, "PASS"));
    }

    @Test
    public void classifyChange_otherForAnyOtherTransition() {
        assertEquals(ReportRenderer.ChangeKind.OTHER, ReportRenderer.classifyChange("NOT_APPLICABLE", "MANUAL"));
    }

    @Test
    public void markerForChange_matchesEachKind() {
        assertEquals("↓ ", ReportRenderer.markerForChange(ReportRenderer.ChangeKind.REGRESSION));
        assertEquals("↑ ", ReportRenderer.markerForChange(ReportRenderer.ChangeKind.IMPROVEMENT));
        assertEquals("= ", ReportRenderer.markerForChange(ReportRenderer.ChangeKind.UNCHANGED));
        assertEquals("~ ", ReportRenderer.markerForChange(ReportRenderer.ChangeKind.OTHER));
    }
}
