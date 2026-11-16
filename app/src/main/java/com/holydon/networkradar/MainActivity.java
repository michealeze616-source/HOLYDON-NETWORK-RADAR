package com.holydon.networkradar;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 41;
    private final int bg = Color.rgb(7, 13, 25);
    private final int panel = Color.rgb(16, 29, 49);
    private final int cyan = Color.rgb(44, 211, 255);
    private final int muted = Color.rgb(145, 165, 190);
    private TextView connectionValue, networkValue, signalValue, latencyValue, statusText;
    private Button scanButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildScreen();
        refreshNetworkInfo();
        requestDiagnosticPermissions();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(bg);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = text("◉", 30, cyan, true);
        header.addView(logo);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(10), 0, 0, 0);
        names.addView(text("HOLYDON", 17, Color.WHITE, true));
        names.addView(text("NETWORK RADAR", 12, cyan, true));
        header.addView(names);
        root.addView(header);
        root.addView(space(24));

        root.addView(text("NETWORK INTELLIGENCE", 11, muted, true));
        root.addView(space(8));
        root.addView(text("Your connection.\nUnder the radar.", 29, Color.WHITE, true));
        root.addView(space(18));

        LinearLayout hero = card();
        hero.addView(text("CURRENT CONNECTION", 11, muted, true));
        connectionValue = text("CHECKING...", 23, cyan, true);
        hero.addView(connectionValue);
        hero.addView(space(5));
        networkValue = text("Detecting active network...", 13, muted, false);
        hero.addView(networkValue);
        root.addView(hero);
        root.addView(space(12));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout left = metricCard("CELL SIGNAL", "-- dBm");
        LinearLayout right = metricCard("LATENCY", "-- ms");
        signalValue = (TextView) left.getChildAt(2);
        latencyValue = (TextView) right.getChildAt(2);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(106), 1);
        half.setMargins(0, 0, dp(6), 0);
        metrics.addView(left, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(106), 1);
        half2.setMargins(dp(6), 0, 0, 0);
        metrics.addView(right, half2);
        root.addView(metrics);
        root.addView(space(18));

        scanButton = new Button(this);
        scanButton.setText("▶   RUN NETWORK DIAGNOSTIC");
        scanButton.setTextColor(Color.rgb(4, 17, 31));
        scanButton.setTextSize(13);
        scanButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scanButton.setAllCaps(false);
        scanButton.setBackground(round(cyan, 16));
        scanButton.setOnClickListener(v -> runDiagnostic());
        root.addView(scanButton, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(space(18));

        LinearLayout info = card();
        info.addView(text("RADAR STATUS", 11, muted, true));
        statusText = text("Ready to scan. Tap the button to test connectivity.", 14, Color.WHITE, false);
        info.addView(space(8));
        info.addView(statusText);
        root.addView(info);
        root.addView(space(16));
        root.addView(text("TOOLS", 11, muted, true));
        root.addView(space(8));
        root.addView(toolRow("◈", "SIM & CARRIER DETECTION", "Dual-SIM support where available"));
        root.addView(space(8));
        root.addView(toolRow("⌁", "NETWORK HISTORY", "Local performance history — coming next"));
        root.addView(space(8));
        root.addView(toolRow("⚡", "SMART ALERTS", "Connection alerts — coming next"));
        root.addView(space(22));
        TextView footer = text("HOLYDON LABS  •  BUILD 1.0.0", 10, muted, true);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
        setContentView(scroll);
    }

    private LinearLayout metricCard(String title, String value) {
        LinearLayout c = card();
        c.addView(text(title, 10, muted, true));
        c.addView(space(12));
        c.addView(text(value, 22, Color.WHITE, true));
        return c;
    }

    private LinearLayout toolRow(String icon, String title, String subtitle) {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);
        TextView symbol = text(icon, 23, cyan, true);
        c.addView(symbol);
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, 0, 0);
        details.addView(text(title, 12, Color.WHITE, true));
        details.addView(space(4));
        details.addView(text(subtitle, 11, muted, false));
        c.addView(details);
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(round(panel, 18));
        return c;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private View space(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return v;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void requestDiagnosticPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
            }
        }
    }

    private void refreshNetworkInfo() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        if (caps == null) {
            connectionValue.setText("NO CONNECTION");
            networkValue.setText("No active internet network detected");
            return;
        }
        connectionValue.setText("CONNECTED");
        String type = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" :
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobile data" :
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet" : "Other network";
        networkValue.setText(type + "  •  " + getCarrierName());
        readCellSignal();
    }

    private String getCarrierName() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String name = tm == null ? null : tm.getNetworkOperatorName();
            return name == null || name.trim().isEmpty() ? "Carrier unavailable" : name;
        } catch (SecurityException e) {
            return "Carrier permission needed";
        }
    }

    private void readCellSignal() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                signalValue.setText("PERMISSION");
                return;
            }
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            List<CellInfo> cells = tm == null ? null : tm.getAllCellInfo();
            if (cells != null) {
                for (CellInfo cell : cells) {
                    if (cell != null && cell.isRegistered()) {
                        CellSignalStrength strength = cell.getCellSignalStrength();
                        int dbm = strength.getDbm();
                        signalValue.setText(dbm == Integer.MAX_VALUE ? "-- dBm" : dbm + " dBm");
                        return;
                    }
                }
            }
            signalValue.setText("-- dBm");
        } catch (Exception e) {
            signalValue.setText("-- dBm");
        }
    }

    private void runDiagnostic() {
        scanButton.setEnabled(false);
        scanButton.setText("SCANNING NETWORK...");
        statusText.setText("Testing reachability to 1.1.1.1...");
        executor.execute(() -> {
            long latency = -1;
            try {
                long start = System.nanoTime();
                boolean reachable = InetAddress.getByName("1.1.1.1").isReachable(3500);
                long elapsed = (System.nanoTime() - start) / 1000000L;
                if (reachable) latency = elapsed;
            } catch (Exception ignored) { }
            final long result = latency;
            runOnUiThread(() -> {
                latencyValue.setText(result >= 0 ? result + " ms" : "TIMEOUT");
                statusText.setText(result >= 0
                    ? "Diagnostic complete. Host responded in " + result + " ms."
                    : "No ping response. Your network may block ICMP; this does not always mean the internet is down.");
                scanButton.setEnabled(true);
                scanButton.setText("▶   RUN NETWORK DIAGNOSTIC");
                refreshNetworkInfo();
            });
        });
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS) refreshNetworkInfo();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
