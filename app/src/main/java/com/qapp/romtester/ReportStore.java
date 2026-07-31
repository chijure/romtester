package com.qapp.romtester;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReportStore {

    private static final String DIR_NAME = "reports";

    public static File saveReport(Context context, TestReport report)
            throws IOException, JSONException {
        File dir = getDir(context);
        File file = new File(dir, "report_" + report.timestamp + ".json");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(report.toJson().toString().getBytes("UTF-8"));
        }
        return file;
    }

    public static List<TestReport> listReports(Context context) {
        List<TestReport> reports = new ArrayList<>();
        File[] files = getDir(context).listFiles();
        if (files == null) {
            return reports;
        }
        for (File file : files) {
            try {
                reports.add(readReport(file));
            } catch (Exception ignored) {
                // Skip corrupt or unreadable report files.
            }
        }
        Collections.sort(reports, new Comparator<TestReport>() {
            @Override
            public int compare(TestReport a, TestReport b) {
                return b.timestamp > a.timestamp ? 1 : (b.timestamp < a.timestamp ? -1 : 0);
            }
        });
        return reports;
    }

    public static TestReport readReport(File file) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read, "UTF-8"));
            }
        }
        return TestReport.fromJson(new JSONObject(sb.toString()));
    }

    public static File fileForTimestamp(Context context, long timestamp) {
        return new File(getDir(context), "report_" + timestamp + ".json");
    }

    public static void deleteReport(Context context, long timestamp) {
        fileForTimestamp(context, timestamp).delete();
    }

    public static void clearAll(Context context) {
        File[] files = getDir(context).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    private static File getDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
