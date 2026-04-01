package com.trilateration.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WifiScanner {

    public interface ScanCallback {
        void onResult(List<AccessPoint> aps, String error);
    }

    public static class AccessPoint {
        public final String ssid;
        public final String bssid;
        public final int    rssi;
        public final double distance;
        public double x, y;

        AccessPoint(String ssid, String bssid, int rssi) {
            this.ssid     = ssid;
            this.bssid    = bssid;
            this.rssi     = rssi;
            this.distance = Trilateration.rssiToDistance(rssi);
        }

        public String label() {
            String name = (ssid != null && !ssid.isEmpty()) ? ssid : bssid;
            return name.length() > 12 ? name.substring(0, 12) + "…" : name;
        }
    }

    private final WifiManager wifiManager;
    private final Context     context;

    public WifiScanner(Context ctx) {
        this.context     = ctx.getApplicationContext();
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void scan(ScanCallback callback) {
        if (wifiManager == null) {
            callback.onResult(null, "WiFi not available on this device.");
            return;
        }
        if (!wifiManager.isWifiEnabled()) {
            callback.onResult(null, "Please enable WiFi and try again.");
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                context.unregisterReceiver(this);
                List<ScanResult> raw = wifiManager.getScanResults();
                if (raw == null || raw.isEmpty()) {
                    callback.onResult(null, "No WiFi networks found nearby.");
                    return;
                }

                List<AccessPoint> aps = new ArrayList<>();
                for (ScanResult sr : raw) {
                    aps.add(new AccessPoint(sr.SSID, sr.BSSID, sr.level));
                }
                aps.sort(Comparator.comparingInt((AccessPoint a) -> a.rssi).reversed());

                while (aps.size() > 3) aps.remove(aps.size() - 1);

                if (aps.size() < 3) {
                    callback.onResult(aps, "Need at least 3 APs — only " + aps.size() + " found.");
                    return;
                }

                assignRelativePositions(aps);
                callback.onResult(aps, null);
            }
        };

        context.registerReceiver(receiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    private void assignRelativePositions(List<AccessPoint> aps) {
        AccessPoint a1 = aps.get(0);
        AccessPoint a2 = aps.get(1);
        AccessPoint a3 = aps.get(2);

        double d12 = a1.distance + a2.distance;
        double d13 = a1.distance + a3.distance;
        double d23 = a2.distance + a3.distance;

        a1.x = 0; a1.y = 0;
        a2.x = d12; a2.y = 0;

        double cosA = (d12*d12 + d13*d13 - d23*d23) / (2.0*d12*d13);
        cosA = Math.max(-1.0, Math.min(1.0, cosA));
        a3.x = d13 * cosA;
        a3.y = d13 * Math.sqrt(Math.max(0.0, 1.0 - cosA*cosA));
    }
}
