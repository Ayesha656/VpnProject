package com.example.vpn_sample_2;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyVpnService extends VpnService {

    private static final String TAG = "MyVpnService";
    private ParcelFileDescriptor vpnInterface;
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp");
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private String serverIp = "192.168.2.2";  // Example IP, replace with actual server IP
    private int servicePortNumber = 8080;  // Example port, replace with actual port number
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Thread vpnThread = new Thread(this::runVpnConnection);
            vpnThread.start();
        }
        return START_STICKY;
    }

    private void runVpnConnection() {
        try {
            if (establishVpnConnection()) {
                Log.i(TAG, "VPN interface established successfully.");
                isRunning.set(true);
                interceptPackets();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during VPN connection: " + e.getMessage(), e);
        } finally {
            stopVpnConnection();
        }
    }

    private boolean establishVpnConnection() {
        if (vpnInterface == null) {
            Builder builder = new Builder();
            try {
                builder.addAddress(serverIp, 24)
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("192.168.1.1");

                PendingIntent configureIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_IMMUTABLE);

                vpnInterface = builder.setSession("MyVPN")
                        .setConfigureIntent(configureIntent)
                        .establish();

                return vpnInterface != null;

            } catch (Exception e) {
                Log.e(TAG, "Failed to establish VPN interface: " + e.getMessage(), e);
            }
        } else {
            handler.post(() -> Toast.makeText(MyVpnService.this, "VPN connection already established", Toast.LENGTH_SHORT).show());
        }
        return false;
    }

    private void stopVpnConnection() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                vpnInterface = null;
                isRunning.set(false);
                Log.i(TAG, "VPN interface closed.");
            } catch (IOException e) {
                Log.e(TAG, "Failed to close VPN interface: " + e.getMessage(), e);
            }
        }
    }

    private void interceptPackets() {
        if (vpnInterface == null) {
            Log.e(TAG, "VPN interface is null. Cannot intercept packets.");
            return;
        }

        try (FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
             FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor())) {

            ByteBuffer packet = ByteBuffer.allocate(32767);
            while (isRunning.get()) {
                int length = in.read(packet.array());
                if (length > 0) {
                    packet.limit(length);

                    // Example: Process packets (if needed)
                    if (isHttpPacket(packet)) {
                        String httpPayload = extractHttpPayload(packet);
                        if (httpPayload != null && isImageRequest(httpPayload, IMAGE_EXTENSIONS)) {
                            Log.i(TAG, "Filtered Image Request: " + httpPayload);
                            // Handle the image request here (e.g., block, modify)
                        }
                    }

                    // Send the packet back
                    out.write(packet.array(), 0, length);
                    packet.clear();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error in interceptPackets: " + e.getMessage(), e);
        }
    }

    private boolean isHttpPacket(ByteBuffer packet) {
        // Check if the packet contains an HTTP request
        String payload = new String(packet.array(), 0, packet.limit());
        return payload.startsWith("GET") || payload.startsWith("POST") ||
                payload.startsWith("PUT") || payload.startsWith("DELETE");
    }

    private String extractHttpPayload(ByteBuffer packet) {
        // Extract the HTTP payload from the packet
        String payload = new String(packet.array(), 0, packet.limit());
        if (payload.contains("HTTP/")) {
            int endOfHeaders = payload.indexOf("\r\n\r\n");
            if (endOfHeaders != -1) {
                return payload.substring(0, endOfHeaders + 4); // Include end of headers
            }
        }
        return null;
    }

    private boolean isImageRequest(String httpPayload, List<String> imageExtensions) {
        // Check if the HTTP request URL ends with one of the specified image extensions
        int start = httpPayload.indexOf(" ");
        int end = httpPayload.indexOf(" ", start + 1);
        if (start != -1 && end != -1) {
            String url = httpPayload.substring(start + 1, end);
            for (String extension : imageExtensions) {
                if (url.contains(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpnConnection();
    }
}
