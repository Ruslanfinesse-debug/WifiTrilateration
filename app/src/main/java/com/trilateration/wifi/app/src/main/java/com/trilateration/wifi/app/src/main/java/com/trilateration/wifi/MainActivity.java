package com.trilateration.wifi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 101;

    private RadarView  radarView;
    private TextView   statusText;
    private TextView   coordText;
    private TextView   apListText;
    private Button     scanButton;

    private WifiScanner scanner;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radarView  = findViewById(R.id.radarView);
        statusText = findViewById(R.id.statusText);
        coordText  = findViewById(R.id.coordText);
        apListText = findViewById(R.id.apListText);
        scanButton = findViewById(R.id.scanButton);

        scanner = new WifiScanner(this);
        scanButton.setOnClickListener(v -> checkPermissionsAndScan());
    }

    private void checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, PERM_REQUEST);
        } else {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            setStatus("Location permission is required to scan WiFi.", true);
        }
    }

    private void startScan() {
        scanButton.setEnabled(false);
        setStatus("Scanning WiFi networks…", false);
        coordText.setText("");
        apListText.setText("");

        scanner.scan((aps, error) -> uiHandler.post(() -> {
            scanButton.setEnabled(true);

            if (error != null && aps == null) {
                setStatus("⚠ " + error, true);
                radarView.update(null, null);
                return;
            }

            if (aps == null || aps.size() < 3) {
                setStatus("⚠ Need at least 3 access points.", true);
                radarView.update(aps, null);
                return;
            }

            WifiScanner.AccessPoint a1 = aps.get(0);
            WifiScanner.AccessPoint a2 = aps.get(1);
            WifiScanner.AccessPoint a3 = aps.get(2);

            Trilateration.Point pos = Trilateration.compute(
                    a1.x, a1.y, a1.distance,
                    a2.x, a2.y, a2.distance,
                    a3.x, a3.y, a3.distance
            );

            if (pos != null) {
                setStatus("✓ Position estimated from " + aps.size() + " access points", false);
                coordText.setText(String.format("X: %.2f m    Y: %.2f m", pos.x, pos.y));
            } else {
                setStatus("⚠ Access points are collinear.", true);
                coordText.setText("—");
            }

            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (WifiScanner.AccessPoint ap : aps) {
                sb.append(String.format("AP%d  %-16s  %d dBm  ~%.1f m%n",
                        i++, ap.label(), ap.rssi, ap.distance));
            }
            apListText.setText(sb.toString().trim());
            radarView.update(aps, pos);
        }));
    }

    private void setStatus(String msg, boolean isError) {
        statusText.setText(msg);
        statusText.setTextColor(isError
                ? getColor(android.R.color.holo_orange_light)
                : getColor(android.R.color.holo_green_light));
    }
}
