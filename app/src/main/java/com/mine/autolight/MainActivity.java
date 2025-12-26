package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private Button btnStart;
    private TextView tvState;
    private EditText etSensor1, etSensor2, etSensor3, etSensor4;
    private EditText etBrightness1, etBrightness2, etBrightness3, etBrightness4;
    private MySettings sett;
    private boolean isExpanded = false;
    private boolean isDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        // Collapsible Logic
        Button btnExpand = findViewById(R.id.btn_expand);
        LinearLayout llHidden = findViewById(R.id.ll_hidden_settings);
        llHidden.setVisibility(View.GONE);
        
        btnExpand.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            llHidden.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            btnExpand.setText(isExpanded ? "Hide Configuration" : "Show Configuration");
            if (isExpanded) {
                refillCollapsibleSettings();
                requestNotificationPermission();
            }
        });

        // Start/Stop Logic
        tvState = findViewById(R.id.tv_service_state);
        btnStart = findViewById(R.id.btn_start_stop);
        btnStart.setOnClickListener(v -> {
            if (LightService.isRunning) {
                killService();
                displayServiceStatus(0);
            } else {
                if (requestNotificationPermission()) {
                    runService();
                    displayServiceStatus(-1);
                }
            }
        });

        // Ping State logic
        Button btnState = findViewById(R.id.btn_get_state);
        btnState.setOnClickListener(v -> {
            displayServiceStatus(LightService.isRunning ? 1 : 0);
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_PING);
        });

        // Initialize Views & Settings
        initSettingsViews();
        refillCollapsibleSettings();

        // Save Button
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveCurrentSettings());

        // RadioGroup logic
        RadioGroup rgWorkMode = findViewById(R.id.rg_work_mode);
        syncWorkModeUI();
        rgWorkMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_work_always) sett.mode = Constants.WORK_MODE_ALWAYS;
            else if (checkedId == R.id.rb_work_portrait) sett.mode = Constants.WORK_MODE_PORTRAIT;
            else if (checkedId == R.id.rb_work_landscape) sett.mode = Constants.WORK_MODE_LANDSCAPE;
            else if (checkedId == R.id.rb_work_unlock) sett.mode = Constants.WORK_MODE_UNLOCK;
            sett.save();
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
        });

        TextView tvHelp = findViewById(R.id.tv_dontkillmyapp);
        if (tvHelp != null) tvHelp.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void initSettingsViews() {
        etSensor1 = findViewById(R.id.et_sensor_value_1);
        etSensor2 = findViewById(R.id.et_sensor_value_2);
        etSensor3 = findViewById(R.id.et_sensor_value_3);
        etSensor4 = findViewById(R.id.et_sensor_value_4);
        etBrightness1 = findViewById(R.id.et_brightness_value_1);
        etBrightness2 = findViewById(R.id.et_brightness_value_2);
        etBrightness3 = findViewById(R.id.et_brightness_value_3);
        etBrightness4 = findViewById(R.id.et_brightness_value_4);
    }

    private void refillCollapsibleSettings() {
        if (etSensor1 == null) return;
        etSensor1.setText(String.valueOf(sett.l1));
        etSensor2.setText(String.valueOf(sett.l2));
        etSensor3.setText(String.valueOf(sett.l3));
        etSensor4.setText(String.valueOf(sett.l4));
        etBrightness1.setText(String.valueOf(sett.b1));
        etBrightness2.setText(String.valueOf(sett.b2));
        etBrightness3.setText(String.valueOf(sett.b3));
        etBrightness4.setText(String.valueOf(sett.b4));
    }

    private void saveCurrentSettings() {
        try {
            sett.l1 = Integer.parseInt(etSensor1.getText().toString());
            sett.l2 = Integer.parseInt(etSensor2.getText().toString());
            sett.l3 = Integer.parseInt(etSensor3.getText().toString());
            sett.l4 = Integer.parseInt(etSensor4.getText().toString());
            sett.b1 = Integer.parseInt(etBrightness1.getText().toString());
            sett.b2 = Integer.parseInt(etBrightness2.getText().toString());
            sett.b3 = Integer.parseInt(etBrightness3.getText().toString());
            sett.b4 = Integer.parseInt(etBrightness4.getText().toString());
            sett.save();
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncWorkModeUI() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) ((RadioButton)findViewById(R.id.rb_work_always)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_PORTRAIT) ((RadioButton)findViewById(R.id.rb_work_portrait)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_LANDSCAPE) ((RadioButton)findViewById(R.id.rb_work_landscape)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_UNLOCK) ((RadioButton)findViewById(R.id.rb_work_unlock)).setChecked(true);
    }

    private void displayServiceStatus(int status) {
        int color = android.R.color.darker_gray;
        String text = "";
        if (status == 0) {
            btnStart.setText("Start");
            color = android.R.color.holo_red_dark;
            text = "Service Stopped";
        } else if (status == 1) {
            btnStart.setText("Stop");
            color = android.R.color.holo_green_dark;
            text = "Service Running";
        } else if (status == -1) {
            text = "Starting service...";
        }
        tvState.setTextColor(getResources().getColor(color, null));
        tvState.setText(text);
    }

    private void runService() {
        Intent i = new Intent(this, LightService.class);
        ContextCompat.startForegroundService(this, i);
    }

    private void killService() {
        stopService(new Intent(this, LightService.class));
    }

    private boolean requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
                return false;
            }
        }
        return true;
    }

    private void sendBroadcastToService(int payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayServiceStatus(LightService.isRunning ? 1 : 0);
    }
}
