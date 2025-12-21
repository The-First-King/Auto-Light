package com.mine.autolight;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    private MySettings mySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mySettings = new MySettings(this);

        // --- IMPORTANT: Replace R.id.YOUR_ID with the IDs found in your XML ---

        // START BUTTON LOGIC
        View startBtn = findViewById(getResources().getIdentifier("start", "id", getPackageName())); 
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> startServiceWithPermission());
        }

        // STOP BUTTON LOGIC
        View stopBtn = findViewById(getResources().getIdentifier("stop", "id", getPackageName()));
        if (stopBtn != null) {
            stopBtn.setOnClickListener(v -> {
                stopService(new Intent(this, LightService.class));
                Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void startServiceWithPermission() {
        if (Settings.System.canWrite(this)) {
            Intent intent = new Intent(this, LightService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Please allow Write Settings permission", Toast.LENGTH_LONG).show();
        }
    }
}
