package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private MySettings mySettings;
    private EditText etL1, etL2, etL3, etL4, etB1, etB2, etB3, etB4;
    private RadioGroup rgWorkMode;
    private TextView tvServiceState;
    private Button btnStartStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mySettings = new MySettings(this);
        initViews();
        loadUISettings();
        updateUIState(); // Check if service is already running on launch

        // 1. START / STOP TOGGLE
        btnStartStop.setOnClickListener(v -> toggleService());

        // 2. SAVE BUTTON
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> {
            saveUISettings();
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            // If running, restart to refresh the background process's memory
            if (isServiceRunning()) {
                startAutoLightService();
            }
        });

        // 3. GET DATA / REFRESH
        findViewById(R.id.btn_get_state).setOnClickListener(v -> updateUIState());
    }

    private void initViews() {
        tvServiceState = findViewById(R.id.tv_service_state);
        btnStartStop = findViewById(R.id.btn_start_stop);
        etL1 = findViewById(R.id.et_sensor_value_1);
        etL2 = findViewById(R.id.et_sensor_value_2);
        etL3 = findViewById(R.id.et_sensor_value_3);
        etL4 = findViewById(R.id.et_sensor_value_4);
        etB1 = findViewById(R.id.et_brightness_value_1);
        etB2 = findViewById(R.id.et_brightness_value_2);
        etB3 = findViewById(R.id.et_brightness_value_3);
        etB4 = findViewById(R.id.et_brightness_value_4);
        rgWorkMode = findViewById(R.id.rg_work_mode);
    }

    private void updateUIState() {
        if (isServiceRunning()) {
            tvServiceState.setText("Service: RUNNING");
            btnStartStop.setText(getString(android.R.string.cancel)); // Or your @string/stop
        } else {
            tvServiceState.setText("Service: STOPPED");
            btnStartStop.setText("START"); // Or your @string/run
        }
    }

    private void toggleService() {
        if (isServiceRunning()) {
            stopService(new Intent(this, LightService.class));
            Toast.makeText(this, "Stopping...", Toast.LENGTH_SHORT).show();
        } else {
            checkPermissionsAndStart();
        }
        // Give the system a moment to update then refresh UI
        tvServiceState.postDelayed(this::updateUIState, 300);
    }

    private void checkPermissionsAndStart() {
        // 1. Check Write Settings
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        // 2. Check Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                return;
            }
        }

        startAutoLightService();
    }

    private void startAutoLightService() {
        Intent intent = new Intent(this, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LightService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void loadUISettings() {
        etL1.setText(String.valueOf(mySettings.l1));
        etL2.setText(String.valueOf(mySettings.l2));
        etL3.setText(String.valueOf(mySettings.l3));
        etL4.setText(String.valueOf(mySettings.l4));
        etB1.setText(String.valueOf(mySettings.b1));
        etB2.setText(String.valueOf(mySettings.b2));
        etB3.setText(String.valueOf(mySettings.b3));
        etB4.setText(String.valueOf(mySettings.b4));

        int mode = mySettings.mode;
        if (mode == Constants.WORK_MODE_ALWAYS) rgWorkMode.check(R.id.rb_work_always);
        else if (mode == Constants.WORK_MODE_PORTRAIT) rgWorkMode.check(R.id.rb_work_portrait);
        else if (mode == Constants.WORK_MODE_LANDSCAPE) rgWorkMode.check(R.id.rb_work_landscape);
        else if (mode == Constants.WORK_MODE_UNLOCK) rgWorkMode.check(R.id.rb_work_unlock);
    }

    private void saveUISettings() {
        try {
            mySettings.l1 = Integer.parseInt(etL1.getText().toString());
            mySettings.l2 = Integer.parseInt(etL2.getText().toString());
            mySettings.l3 = Integer.parseInt(etL3.getText().toString());
            mySettings.l4 = Integer.parseInt(etL4.getText().toString());
            mySettings.b1 = Integer.parseInt(etB1.getText().toString());
            mySettings.b2 = Integer.parseInt(etB2.getText().toString());
            mySettings.b3 = Integer.parseInt(etB3.getText().toString());
            mySettings.b4 = Integer.parseInt(etB4.getText().toString());

            int checkedId = rgWorkMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_work_always) mySettings.mode = Constants.WORK_MODE_ALWAYS;
            else if (checkedId == R.id.rb_work_portrait) mySettings.mode = Constants.WORK_MODE_PORTRAIT;
            else if (checkedId == R.id.rb_work_landscape) mySettings.mode = Constants.WORK_MODE_LANDSCAPE;
            else if (checkedId == R.id.rb_work_unlock) mySettings.mode = Constants.WORK_MODE_UNLOCK;

            mySettings.save();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}
