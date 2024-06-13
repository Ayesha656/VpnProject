package com.example.vpn_sample_2;
import android.content.Intent;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class MyVpnService extends VpnService{
    private static final String TAG = "MyVpnService";
    private ParcelFileDescriptor vpnInterface = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VPN Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VPN Service Started");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setupVpn();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return START_STICKY;
    }

    private void setupVpn() throws IOException {
        Builder builder = new Builder();
        builder.setSession("MyVPNService")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0);

        vpnInterface = builder.establish();

        Log.d(TAG, "VPN Interface Established");

        DatagramChannel tunnel = DatagramChannel.open();
        tunnel.connect(new InetSocketAddress("10.0.0.2", 8087));

        ByteBuffer packet = ByteBuffer.allocate(32767);
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        while (true) {
            int length = in.read(packet.array());
            if (length > 0) {
                packet.limit(length);
                // Parse the packet and Add the  filtering logic
            }
            packet.clear();
        }
    }

    private boolean isImageUrl(String url) {
        String[] extensions = {".jpg", ".png"};
        for (String extension : extensions) {
            if (url.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void communicateWithBackend(final String url) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                // Make network request to backend server
                // For example, using HttpURLConnection or any networking library
                return true; // Assume URL validation successful
            }

            @Override
            protected void onPostExecute(Boolean result) {
                // Handle result of URL validation
            }
        }.execute();
    }

    public void startVpn(String[] imageExtensions, String[] packageNames, android.os.Handler.Callback callback) {
        Intent intent = new Intent(this, MyVpnService.class);
        startService(intent);
        Log.d(TAG, "VPN Service Started with imageExtensions: " + Arrays.toString(imageExtensions) + " and packageNames: " + Arrays.toString(packageNames));
    }

    public void stopVpn() {
        Intent intent = new Intent(this, MyVpnService.class);
        stopService(intent);
        Log.d(TAG, "VPN Service Stopped");
    }

}
