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
import android.net.wifi.WifiManager;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 41;
    private static final int BG = Color.rgb(7, 13, 25);
    private static final int PANEL = Color.rgb(16, 29, 49);
    private static final int CYAN = Color.rgb(44, 211, 255);
    private static final int MUTED = Color.rgb(145, 165, 190);
    private static final int GREEN = Color.rgb(55, 225, 151);
    private static final int ORANGE = Color.rgb(255, 184, 77);
    private static final int RED = Color.rgb(255, 91, 112);

    private TextView connectionValue, networkValue, signalValue, wifiValue;
    private TextView pingValue, jitterValue, lossValue, gameVerdict, statusText;
    private Button scanButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean scanning = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
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
        root.setBackgroundColor(BG);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("◉", 30, CYAN, true);
        header.addView(logo);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(10), 0, 0, 0);
        names.addView(text("HOLYDON", 17, Color.WHITE, true));
        names.addView(text("NETWORK RADAR  /  GAMING EDITION", 10, CYAN, true));
        header.addView(names);
        root.addView(header);
        root.addView(space(24));

        root.addView(text("NETWORK INTELLIGENCE", 11, MUTED, true));
        root.addView(space(8));
        root.addView(text("Can your network\nhandle gaming?", 29, Color.WHITE, true));
        root.addView(space(18));

        LinearLayout hero = card();
        hero.addView(text("CURRENT CONNECTION", 11, MUTED, true));
        connectionValue = text("CHECKING...", 23, CYAN, true);
        hero.addView(connectionValue);
        hero.addView(space(5));
        networkValue = text("Detecting active network...", 13, MUTED, false);
        hero.addView(networkValue);
        root.addView(hero);
        root.addView(space(12));

        LinearLayout verdict = card();
        verdict.addView(text("GAMING READINESS", 11, MUTED, true));
        verdict.addView(space(7));
        gameVerdict = text("NOT TESTED", 25, Color.WHITE, true);
        verdict.addView(gameVerdict);
        verdict.addView(space(5));
        verdict.addView(text("Based on repeated connection latency, jitter and failed probes.", 12, MUTED, false));
        root.addView(verdict);
        root.addView(space(12));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout p = metricCard("PING ESTIMATE", "-- ms");
        LinearLayout j = metricCard("JITTER", "-- ms");
        pingValue = (TextView) p.getChildAt(2);
        jitterValue = (TextView) j.getChildAt(2);
        addHalf(row1, p, true);
        addHalf(row1, j, false);
        root.addView(row1);
        root.addView(space(8));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout l = metricCard("FAILED TESTS", "--");
        LinearLayout s = metricCard("CELL SIGNAL", "-- dBm");
        lossValue = (TextView) l.getChildAt(2);
        signalValue = (TextView) s.getChildAt(2);
        addHalf(row2, l, true);
        addHalf(row2, s, false);
        root.addView(row2);
        root.addView(space(8));

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout w = metricCard("WI-FI SIGNAL", "-- dBm");
        wifiValue = (TextView) w.getChildAt(2);
        LinearLayout spacer = new LinearLayout(this);
        addHalf(row3, w, true);
        addHalf(row3, spacer, false);
        root.addView(row3);
        root.addView(space(18));

        scanButton = new Button(this);
        scanButton.setText("▶   TEST GAMING CONNECTION");
        scanButton.setTextColor(Color.rgb(4, 17, 31));
        scanButton.setTextSize(13);
        scanButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scanButton.setAllCaps(false);
        scanButton.setBackground(round(CYAN, 16));
        scanButton.setOnClickListener(v -> runDiagnostic());
        root.addView(scanButton, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(space(18));

        LinearLayout info = card();
        info.addView(text("RADAR STATUS", 11, MUTED, true));
        statusText = text("Ready. Run a test to check whether this connection is suitable for gaming.", 14, Color.WHITE, false);
        info.addView(space(8));
        info.addView(statusText);
        root.addView(info);
        root.addView(space(16));
        root.addView(text("HOW TO READ YOUR RESULT", 11, MUTED, true));
        root.addView(space(8));
        root.addView(toolRow("✓", "GREAT FOR GAMING", "Low delay and stable connection"));
        root.addView(space(7));
        root.addView(toolRow("◉", "PLAYABLE", "Some delay may be noticeable"));
        root.addView(space(7));
        root.addView(toolRow("!", "POOR FOR GAMING", "High delay, jitter or failed tests"));
        root.addView(space(16));
        TextView note = text("Ping is a TCP connection-time estimate to Cloudflare, not the exact ping to your game server. Game servers, region, Wi-Fi congestion and the game itself can change your real in-game ping.", 11, MUTED, false);
        root.addView(note);
        root.addView(space(20));
        TextView footer = text("HOLYDON LABS  •  NETWORK RADAR", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
        setContentView(scroll);
    }

    private void addHalf(LinearLayout row, View child, boolean left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(94), 1);
        lp.setMargins(left ? 0 : dp(5), 0, left ? dp(5) : 0, 0);
        row.addView(child, lp);
    }

    private LinearLayout metricCard(String title, String value) {
        LinearLayout c = card();
        c.addView(text(title, 10, MUTED, true));
        c.addView(space(10));
        c.addView(text(value, 21, Color.WHITE, true));
        return c;
    }

    private LinearLayout toolRow(String icon, String title, String subtitle) {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.addView(text(icon, 22, CYAN, true));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, 0, 0);
        details.addView(text(title, 12, Color.WHITE, true));
        details.addView(space(4));
        details.addView(text(subtitle, 11, MUTED, false));
        c.addView(details);
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(round(PANEL, 18));
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
            List<String> needed = new ArrayList<String>();
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_PHONE_STATE);
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[needed.size()]), REQUEST_PERMISSIONS);
        }
    }

    private void refreshNetworkInfo() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = cm == null ? null : cm.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                connectionValue.setText("NO CONNECTION");
                networkValue.setText("No active internet connection detected");
            } else {
                connectionValue.setText(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    ? "INTERNET READY" : "NETWORK CONNECTED");
                String type = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" :
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobile data" :
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet" : "Other network";
                networkValue.setText(type + "  •  " + getCarrierName());
            }
            readCellSignal();
            readWifiSignal();
        } catch (Exception e) {
            connectionValue.setText("STATUS UNAVAILABLE");
            networkValue.setText("Could not read connection details");
        }
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
                signalValue.setText("ALLOW ACCESS");
                return;
            }
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            List<CellInfo> cells = tm == null ? null : tm.getAllCellInfo();
            if (cells != null) {
                for (CellInfo cell : cells) {
                    if (cell != null && cell.isRegistered()) {
                        int dbm = cell.getCellSignalStrength().getDbm();
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

    @SuppressWarnings("deprecation")
    private void readWifiSignal() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifi == null || !wifi.isWifiEnabled()) {
                wifiValue.setText("OFF");
                return;
            }
            int rssi = wifi.getConnectionInfo().getRssi();
            wifiValue.setText(rssi <= -100 || rssi >= 0 ? "-- dBm" : rssi + " dBm");
        } catch (Exception e) {
            wifiValue.setText("-- dBm");
        }
    }

    private boolean hasInternetNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void runDiagnostic() {
        if (scanning) return;
        scanning = true;
        scanButton.setEnabled(false);
        scanButton.setText("TESTING CONNECTION...");
        gameVerdict.setText("ANALYSING...");
        gameVerdict.setTextColor(CYAN);
        statusText.setText("Running five timed connection checks. Keep this screen open...");
        executor.execute(() -> {
            ArrayList<Long> times = new ArrayList<Long>();
            int failed = 0;
            for (int i = 0; i < 5; i++) {
                Socket socket = new Socket();
                long start = System.nanoTime();
                try {
                    socket.connect(new InetSocketAddress("1.1.1.1", 443), 2200);
                    long elapsed = (System.nanoTime() - start) / 1000000L;
                    times.add(elapsed);
                } catch (Exception e) {
                    failed++;
                } finally {
                    try { socket.close(); } catch (Exception ignored) { }
                }
                if (i < 4) {
                    try { Thread.sleep(250); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            final int failures = failed;
            final int completed = times.size();
            long average = -1;
            long jitter = -1;
            if (!times.isEmpty()) {
                long sum = 0;
                for (Long time : times) sum += time;
                average = sum / times.size();
                if (times.size() > 1) {
                    long differences = 0;
                    for (int i = 1; i < times.size(); i++)
                        differences += Math.abs(times.get(i) - times.get(i - 1));
                    jitter = differences / (times.size() - 1);
                } else jitter = 0;
            }
            final long avgResult = average;
            final long jitterResult = jitter;
            final boolean validated = hasInternetNetwork();

            runOnUiThread(() -> {
                pingValue.setText(avgResult >= 0 ? avgResult + " ms" : "--");
                jitterValue.setText(jitterResult >= 0 ? jitterResult + " ms" : "--");
                lossValue.setText(failures + "/5");
                if (completed == 0) {
                    if (!validated) {
                        setVerdict("NO INTERNET", RED);
                        statusText.setText("The phone is connected to a network, but Android cannot confirm working internet. Check data/Wi-Fi and try again.");
                    } else {
                        setVerdict("TEST BLOCKED", ORANGE);
                        statusText.setText("Internet is available, but the test endpoint did not respond. This may be a network restriction; it is not proof that all internet access is down.");
                    }
                } else {
                    int failedPercent = failures * 20;
                    if (avgResult <= 60 && jitterResult <= 15 && failedPercent == 0) {
                        setVerdict("GREAT FOR GAMING", GREEN);
                        statusText.setText("Good result: low connection delay, steady timing and no failed checks. You should be fine for most online games, subject to the game server.");
                    } else if (avgResult <= 100 && jitterResult <= 30 && failedPercent <= 20) {
                        setVerdict("PLAYABLE", ORANGE);
                        statusText.setText("You can likely play, but you may notice occasional lag. Move closer to Wi-Fi or improve mobile signal for a better result.");
                    } else {
                        setVerdict("POOR FOR GAMING", RED);
                        statusText.setText("This connection may cause lag or disconnections. High delay, unstable timing or failed checks were detected. Try another network or move closer to your router.");
                    }
                }
                scanButton.setEnabled(true);
                scanButton.setText("▶   TEST AGAIN");
                scanning = false;
                refreshNetworkInfo();
            });
        });
    }

    private void setVerdict(String value, int color) {
        gameVerdict.setText(value);
        gameVerdict.setTextColor(color);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS) refreshNetworkInfo();
    }

    @Override protected void onResume() {
        super.onResume();
        if (connectionValue != null) refreshNetworkInfo();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
