package com.example.vpn_sample_2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private MyVpnService vpnService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {


            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            vpnService = new MyVpnService();

            Button startButton = findViewById(R.id.start_button);
            Button stopButton = findViewById(R.id.stop_button);

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startVpn();
                }
            });

            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopVpn();
                }
            });
        }

        private void startVpn() {
            String[] imageExtensions = {".jpg", ".png"};
            String[] packageNames = {"com.example.app"};
            vpnService.startVpn(imageExtensions, packageNames, new Handler.Callback() {
                @Override
                public boolean handleMessage(android.os.Message msg) {
                    // yahan callback walai messages dalnai hain
                    return true;
                }
            });
        }

        private void stopVpn() {
            vpnService.stopVpn();
        }

    }
