package com.qapp.romtester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TestReport {

    public static class CheckEntry {
        public final String name;
        public final String status;
        public final String detail;

        public CheckEntry(String name, String status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    public final long timestamp;
    public final String model;
    public final String androidVersion;
    public final String fingerprint;
    public final int passCount;
    public final int failCount;
    public final int naCount;
    public final int manualCount;
    public final List<CheckEntry> checks;

    public TestReport(long timestamp, String model, String androidVersion, String fingerprint,
                       int passCount, int failCount, int naCount, int manualCount, List<CheckEntry> checks) {
        this.timestamp = timestamp;
        this.model = model;
        this.androidVersion = androidVersion;
        this.fingerprint = fingerprint;
        this.passCount = passCount;
        this.failCount = failCount;
        this.naCount = naCount;
        this.manualCount = manualCount;
        this.checks = checks;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("timestamp", timestamp);
        obj.put("model", model);
        obj.put("androidVersion", androidVersion);
        obj.put("fingerprint", fingerprint);
        obj.put("pass", passCount);
        obj.put("fail", failCount);
        obj.put("na", naCount);
        obj.put("manual", manualCount);
        JSONArray array = new JSONArray();
        for (CheckEntry entry : checks) {
            JSONObject checkObj = new JSONObject();
            checkObj.put("name", entry.name);
            checkObj.put("status", entry.status);
            checkObj.put("detail", entry.detail);
            array.put(checkObj);
        }
        obj.put("checks", array);
        return obj;
    }

    public static TestReport fromJson(JSONObject obj) throws JSONException {
        JSONArray array = obj.getJSONArray("checks");
        List<CheckEntry> checks = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject checkObj = array.getJSONObject(i);
            checks.add(new CheckEntry(checkObj.getString("name"), checkObj.getString("status"),
                    checkObj.getString("detail")));
        }
        return new TestReport(obj.getLong("timestamp"), obj.getString("model"),
                obj.getString("androidVersion"), obj.getString("fingerprint"),
                obj.getInt("pass"), obj.getInt("fail"), obj.getInt("na"), obj.getInt("manual"), checks);
    }
}
