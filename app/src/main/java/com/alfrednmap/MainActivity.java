package com.alfrednmap;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(10, 10, 12);
    private static final int PANEL = Color.rgb(20, 20, 23);
    private static final int RED = Color.rgb(255, 65, 65);
    private static final int TEXT = Color.rgb(245, 245, 247);
    private static final int MUTED = Color.rgb(170, 170, 178);
    private static final int DEVICE_TIMEOUT_MILLIS = 400;
    private static final int NAME_LOOKUP_TIMEOUT_SECONDS = 2;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService discoveryExecutor = Executors.newFixedThreadPool(32);
    private TextView localIpText;
    private TextView gatewayText;
    private LinearLayout devicesList;
    private Button refreshButton;
    private Button startScanButton;
    private Button stopScanButton;
    private Button clearButton;
    private TextView scanStatusText;
    private volatile String currentIpAddress;
    private volatile int currentPrefixLength;
    private volatile ScanRun activeScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(28), dp(22), dp(30));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("ALFRED // PRIVATE NETWORK", 12, RED, true);
        eyebrow.setLetterSpacing(0.12f);
        content.addView(eyebrow, matchWrap());

        TextView title = text("DEVICE SCANNER", 30, TEXT, true);
        title.setPadding(0, dp(7), 0, 0);
        content.addView(title, matchWrap());

        TextView subtitle = text("DISCOVER DEVICES ON YOUR WI-FI", 12, MUTED, true);
        subtitle.setLetterSpacing(0.08f);
        subtitle.setPadding(0, dp(2), 0, dp(20));
        content.addView(subtitle, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(RED);
        content.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        LinearLayout localPanel = panel();
        content.addView(localPanel, matchWrap());

        LinearLayout networkHeader = new LinearLayout(this);
        networkHeader.setGravity(Gravity.CENTER_VERTICAL);
        localPanel.addView(networkHeader, matchWrap());

        TextView localHeading = text("●  YOUR PHONE & ROUTER", 12, RED, true);
        localHeading.setLetterSpacing(0.06f);
        networkHeader.addView(localHeading, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        refreshButton = new Button(this);
        refreshButton.setText("↻");
        refreshButton.setTextColor(TEXT);
        refreshButton.setTextSize(20);
        refreshButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        refreshButton.setAllCaps(false);
        refreshButton.setGravity(Gravity.CENTER);
        refreshButton.setPadding(0, 0, 0, 0);
        refreshButton.setBackground(rounded(Color.rgb(38, 25, 27), RED, 8));
        refreshButton.setContentDescription("Refresh phone and router IP addresses");
        refreshButton.setOnClickListener(view -> refreshLocalNetwork());
        networkHeader.addView(refreshButton, new LinearLayout.LayoutParams(dp(46), dp(44)));

        localIpText = text("IP: searching ...", 14, TEXT, true);
        localIpText.setPadding(0, dp(12), 0, dp(5));
        localPanel.addView(localIpText, matchWrap());

        gatewayText = text("Gateway: searching ...", 14, MUTED, true);
        localPanel.addView(gatewayText, matchWrap());

        LinearLayout devicesPanel = panel();
        LinearLayout.LayoutParams devicesPanelParams = matchWrap();
        devicesPanelParams.topMargin = dp(14);
        content.addView(devicesPanel, devicesPanelParams);

        TextView devicesHeading = text("DEVICES ON YOUR NETWORK", 12, RED, true);
        devicesHeading.setLetterSpacing(0.06f);
        devicesPanel.addView(devicesHeading, matchWrap());

        LinearLayout scanButtons = new LinearLayout(this);
        scanButtons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams scanButtonsParams = matchWrap();
        scanButtonsParams.topMargin = dp(12);
        devicesPanel.addView(scanButtons, scanButtonsParams);

        startScanButton = actionButton("▶  SCAN", RED);
        startScanButton.setOnClickListener(view -> startNetworkScan());
        scanButtons.addView(startScanButton, weightedButton());

        stopScanButton = actionButton("■  STOP", Color.rgb(75, 75, 82));
        stopScanButton.setEnabled(false);
        stopScanButton.setOnClickListener(view -> stopNetworkScan());
        LinearLayout.LayoutParams stopParams = weightedButton();
        stopParams.leftMargin = dp(6);
        scanButtons.addView(stopScanButton, stopParams);

        clearButton = actionButton("CLEAR", Color.rgb(75, 75, 82));
        clearButton.setOnClickListener(view -> clearResults());
        LinearLayout.LayoutParams clearParams = weightedButton();
        clearParams.leftMargin = dp(6);
        scanButtons.addView(clearButton, clearParams);

        scanStatusText = text("Ready. Press SCAN to find devices.", 13, MUTED, false);
        scanStatusText.setPadding(0, dp(12), 0, dp(6));
        devicesPanel.addView(scanStatusText, matchWrap());

        devicesList = new LinearLayout(this);
        devicesList.setOrientation(LinearLayout.VERTICAL);
        TextView emptyDevices = text("No scan results yet.", 13, MUTED, false);
        devicesList.addView(emptyDevices, matchWrap());
        devicesPanel.addView(devicesList, matchWrap());

        TextView limitation = text(
                "PRIVATE NETWORK ONLY\n"
                        + "10.x.x.x  •  172.16–31.x.x  •  192.168.x.x\n\n"
                        + "Device names depend on your network's DNS. Some devices may not respond. "
                        + "Tap Scan to find devices. No typing is needed.",
                12,
                MUTED,
                false);
        limitation.setLineSpacing(dp(3), 1f);
        limitation.setPadding(0, dp(22), 0, 0);
        content.addView(limitation, matchWrap());

        TextView developer = text("DEVELOPER  //  TAMBET SARAPU", 11, MUTED, true);
        developer.setGravity(Gravity.CENTER);
        developer.setLetterSpacing(0.06f);
        developer.setPadding(0, dp(24), 0, 0);
        content.addView(developer, matchWrap());

        setContentView(scrollView);
        refreshLocalNetwork();
    }

    private void refreshLocalNetwork() {
        refreshButton.setEnabled(false);
        refreshButton.setText("…");
        localIpText.setText("IP: searching ...");
        gatewayText.setText("Gateway: searching ...");

        executor.execute(() -> {
            String localIp = "IP: unavailable";
            String gateway = "Gateway: unavailable";
            String ownAddress = null;
            int prefixLength = 0;
            try {
                ConnectivityManager connectivity = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.Network activeNetwork = connectivity == null
                        ? null : connectivity.getActiveNetwork();
                LinkProperties properties = activeNetwork == null
                        ? null : connectivity.getLinkProperties(activeNetwork);
                if (properties != null) {
                    for (LinkAddress linkAddress : properties.getLinkAddresses()) {
                        InetAddress address = linkAddress.getAddress();
                        if (address instanceof Inet4Address
                                && PrivateIpv4Address.isAllowed(address.getHostAddress())) {
                            ownAddress = address.getHostAddress();
                            prefixLength = linkAddress.getPrefixLength();
                            localIp = "IP: " + ownAddress;
                            break;
                        }
                    }
                    for (RouteInfo route : properties.getRoutes()) {
                        InetAddress address = route.getGateway();
                        if (route.isDefaultRoute()
                                && address instanceof Inet4Address
                                && !address.isAnyLocalAddress()) {
                            gateway = "Gateway: " + address.getHostAddress();
                            break;
                        }
                    }
                }
            } catch (SecurityException exception) {
                gateway = "Gateway: access denied";
            }

            String finalLocalIp = localIp;
            String finalGateway = gateway;
            String finalOwnAddress = ownAddress;
            int finalPrefixLength = prefixLength;
            runOnUiThread(() -> {
                currentIpAddress = finalOwnAddress;
                currentPrefixLength = finalPrefixLength;
                localIpText.setText(finalLocalIp);
                gatewayText.setText(finalGateway);
                refreshButton.setEnabled(true);
                refreshButton.setText("↻");
                if (finalOwnAddress == null && activeScan == null) {
                    scanStatusText.setText(
                            "No private network detected. Connect to Wi-Fi, then refresh.");
                }
            });
        });
    }

    private void startNetworkScan() {
        String ownAddress = currentIpAddress;
        int prefixLength = currentPrefixLength;
        if (ownAddress == null) {
            scanStatusText.setText("No private network IP. Tap refresh after connecting to Wi-Fi.");
            return;
        }

        List<String> targets = PrivateNetworkRange.addressesToCheck(ownAddress, prefixLength);
        if (targets.isEmpty()) {
            scanStatusText.setText("Could not determine a scannable private subnet.");
            return;
        }

        ScanRun run = new ScanRun(targets.size());
        activeScan = run;
        startScanButton.setEnabled(false);
        stopScanButton.setEnabled(true);
        showDeviceMessage("Waiting for device responses ...");
        scanStatusText.setText("Scanning " + targets.size() + " private addresses ...");
        run.setCoordinator(scanExecutor.submit(() -> scanDevices(run, ownAddress, prefixLength)));
    }

    private void stopNetworkScan() {
        ScanRun run = activeScan;
        if (run == null) {
            return;
        }
        run.cancel();
        activeScan = null;
        startScanButton.setEnabled(true);
        stopScanButton.setEnabled(false);
        List<String> found = run.foundAddresses();
        showDevices(found, Collections.emptyMap());
        scanStatusText.setText("Scan stopped. " + found.size() + " device(s) found so far.");
    }

    private void clearResults() {
        ScanRun run = activeScan;
        if (run != null) {
            run.cancel();
            activeScan = null;
        }
        startScanButton.setEnabled(true);
        stopScanButton.setEnabled(false);
        showDeviceMessage("No scan results yet.");
        scanStatusText.setText("Results cleared. Press SCAN to start.");
    }

    private void scanDevices(ScanRun run, String ownAddress, int prefixLength) {
        List<String> targets = PrivateNetworkRange.addressesToCheck(ownAddress, prefixLength);
        java.util.concurrent.CompletionService<ProbeResult> completed =
                new java.util.concurrent.ExecutorCompletionService<>(discoveryExecutor);
        for (String target : targets) {
            if (run.isCancelled()) {
                break;
            }
            Future<ProbeResult> probe = completed.submit(() -> {
                try {
                    boolean reachable = InetAddress.getByName(target)
                            .isReachable(DEVICE_TIMEOUT_MILLIS);
                    return new ProbeResult(target, reachable, false);
                } catch (IOException exception) {
                    return new ProbeResult(target, false, true);
                }
            });
            run.addProbe(probe);
        }

        int submitted = run.probeCount();
        int completedCount = 0;
        int failedChecks = 0;
        while (completedCount < submitted && !run.isCancelled()) {
            try {
                Future<ProbeResult> finished = completed.poll(250, TimeUnit.MILLISECONDS);
                if (finished == null) {
                    continue;
                }
                ProbeResult result = finished.get();
                completedCount++;
                if (result.failed) {
                    failedChecks++;
                } else if (result.reachable) {
                    run.addFound(result.address);
                }
                if (completedCount % 8 == 0 || result.reachable) {
                    updateScanProgress(run, completedCount, targets.size(), failedChecks);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                run.cancel();
            } catch (java.util.concurrent.ExecutionException exception) {
                failedChecks++;
                completedCount++;
            } catch (java.util.concurrent.CancellationException exception) {
                completedCount++;
            }
        }

        if (run.isCancelled()) {
            return;
        }
        List<String> results = run.foundAddresses();
        runOnUiThread(() -> {
            if (activeScan == run) {
                scanStatusText.setText("Host checks complete. Looking up device names ...");
            }
        });
        Map<String, String> names = resolveDeviceNames(results, run);
        if (run.isCancelled()) {
            return;
        }
        int finalFailedChecks = failedChecks;
        runOnUiThread(() -> {
            if (activeScan != run) {
                return;
            }
            showDevices(results, names);
            scanStatusText.setText("Scan complete. " + results.size()
                    + " device(s) found. " + finalFailedChecks + " check(s) failed.");
            activeScan = null;
            startScanButton.setEnabled(true);
            stopScanButton.setEnabled(false);
        });
    }

    private void updateScanProgress(ScanRun run, int completed, int total, int failed) {
        List<String> found = run.foundAddresses();
        runOnUiThread(() -> {
            if (activeScan != run) {
                return;
            }
            showDevices(found, Collections.emptyMap());
            scanStatusText.setText("Scanning ... " + completed + " / " + total
                    + " checked • " + found.size() + " found • " + failed + " failed");
        });
    }

    private void showDevices(List<String> addresses, Map<String, String> names) {
        List<String> sorted = new ArrayList<>(addresses);
        Collections.sort(sorted);
        devicesList.removeAllViews();
        if (sorted.isEmpty()) {
            showDeviceMessage("No responding devices found yet.");
            return;
        }
        for (String address : sorted) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(rounded(Color.rgb(12, 12, 14), Color.rgb(55, 55, 60), 8));
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription("Copy IP address " + address);
            row.setOnClickListener(view -> copyAddress(address));

            TextView name = text(
                    DeviceNameFormatter.displayName(address, names.get(address)),
                    14,
                    TEXT,
                    true);
            row.addView(name, matchWrap());
            TextView ip = text(address + "   •   TAP TO COPY", 12, MUTED, true);
            LinearLayout.LayoutParams ipParams = matchWrap();
            ipParams.topMargin = dp(4);
            row.addView(ip, ipParams);

            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.topMargin = dp(6);
            devicesList.addView(row, rowParams);
        }
    }

    private void copyAddress(String address) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Could not access clipboard", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Device IP address", address));
        Toast.makeText(this, "Copied " + address, Toast.LENGTH_SHORT).show();
    }

    private void showDeviceMessage(String message) {
        devicesList.removeAllViews();
        TextView emptyDevices = text(message, 13, MUTED, false);
        emptyDevices.setLineSpacing(dp(4), 1f);
        devicesList.addView(emptyDevices, matchWrap());
    }

    private Map<String, String> resolveDeviceNames(List<String> addresses, ScanRun run) {
        Map<String, String> names = Collections.synchronizedMap(new HashMap<>());
        CountDownLatch completed = new CountDownLatch(addresses.size());
        List<Future<?>> lookups = new ArrayList<>();
        for (String address : addresses) {
            if (run.isCancelled()) {
                break;
            }
            lookups.add(discoveryExecutor.submit(() -> {
                try {
                    String hostName = InetAddress.getByName(address).getCanonicalHostName();
                    String displayName = DeviceNameFormatter.displayName(address, hostName);
                    if (!"Unknown device".equals(displayName)) {
                        names.put(address, displayName);
                    }
                } catch (IOException exception) {
                    // A missing local DNS name is shown as "Unknown device".
                } finally {
                    completed.countDown();
                }
            }));
        }

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(NAME_LOOKUP_TIMEOUT_SECONDS);
        try {
            while (completed.getCount() > 0
                    && !run.isCancelled()
                    && System.nanoTime() < deadline) {
                completed.await(100, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            run.cancel();
        }
        if (run.isCancelled() || completed.getCount() > 0) {
            for (Future<?> lookup : lookups) {
                lookup.cancel(true);
            }
        }
        return names;
    }

    private Button actionButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(4), dp(6), dp(4), dp(6));
        button.setBackground(rounded(color, color, 8));
        return button;
    }

    private LinearLayout.LayoutParams weightedButton() {
        return new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(rounded(PANEL, Color.rgb(43, 43, 48), 10));
        return panel;
    }

    private TextView text(String value, int size, int color, boolean mono) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(mono ? Typeface.MONOSPACE : Typeface.DEFAULT);
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    @Override
    protected void onDestroy() {
        ScanRun run = activeScan;
        if (run != null) {
            run.cancel();
        }
        executor.shutdownNow();
        scanExecutor.shutdownNow();
        discoveryExecutor.shutdownNow();
        super.onDestroy();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class ProbeResult {
        final String address;
        final boolean reachable;
        final boolean failed;

        ProbeResult(String address, boolean reachable, boolean failed) {
            this.address = address;
            this.reachable = reachable;
            this.failed = failed;
        }
    }

    private static final class ScanRun {
        private final int targetCount;
        private final List<Future<?>> probes = new ArrayList<>();
        private final List<String> found = new ArrayList<>();
        private volatile boolean cancelled;
        private Future<?> coordinator;

        ScanRun(int targetCount) {
            this.targetCount = targetCount;
        }

        synchronized void setCoordinator(Future<?> future) {
            coordinator = future;
            if (cancelled) {
                future.cancel(true);
            }
        }

        synchronized void addProbe(Future<?> probe) {
            if (cancelled) {
                probe.cancel(true);
            } else {
                probes.add(probe);
            }
        }

        synchronized int probeCount() {
            return probes.size();
        }

        synchronized void addFound(String address) {
            if (!found.contains(address)) {
                found.add(address);
            }
        }

        synchronized List<String> foundAddresses() {
            return new ArrayList<>(found);
        }

        boolean isCancelled() {
            return cancelled;
        }

        synchronized void cancel() {
            cancelled = true;
            for (Future<?> probe : probes) {
                probe.cancel(true);
            }
            if (coordinator != null) {
                coordinator.cancel(true);
            }
        }
    }
}
