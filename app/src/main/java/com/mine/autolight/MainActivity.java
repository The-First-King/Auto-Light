package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private Button btnStart;
    private TextView tvState;

    private EditText etSensor1, etSensor2, etSensor3, etSensor4;
    private EditText etBrightness1, etBrightness2, etBrightness3, etBrightness4;

    private MySettings sett;

    private boolean isExpanded = false;
    private boolean isDialogShown = false;

    // Use a constant for the preference key
    private static final String PREF_ENABLED = "service_enabled_by_user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        Button btnExpand = findViewById(R.id.btn_expand);
        LinearLayout llHidden = findViewById(R.id.ll_hidden_settings);
        llHidden.setVisibility(View.GONE);

        btnExpand.setOnClickListener(arg0 -> {
            if (isExpanded) {
                llHidden.setVisibility(View.GONE);
                btnExpand.setText(R.string.show_config);
                isExpanded = false;
            } else {
                llHidden.setVisibility(View.VISIBLE);
                btnExpand.setText(R.string.hide_config);
                isExpanded = true;
                refillCollapsibleSettings();
                requestNotificationPermission();
            }
        });

        tvState = findViewById(R.id.tv_service_state);
        btnStart = findViewById(R.id.btn_start_stop);
        btnStart.setOnClickListener(arg0 -> {
            if (isServiceRunning()) {
                setServiceEnabledPref(false); // Save that user wants it OFF
                killService();
                displayServiceStatus(0);
            } else {
                setServiceEnabledPref(true); // Save that user wants it ON
                runService();
                displayServiceStatus(-1);
            }
        });

        Button btnState = findViewById(R.id.btn_get_state);
        btnState.setOnClickListener(arg0 -> {
            displayServiceStatus(isServiceRunning() ? 1 : 0);
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_PING);
        });

        etSensor1 = findViewById(R.id.et_sensor_value_1);
        etSensor2 = findViewById(R.id.et_sensor_value_2);
        etSensor3 = findViewById(R.id.et_sensor_value_3);
        etSensor4 = findViewById(R.id.et_sensor_value_4);

        etBrightness1 = findViewById(R.id.et_brightness_value_1);
        etBrightness2 = findViewById(R.id.et_brightness_value_2);
        etBrightness3 = findViewById(R.id.et_brightness_value_3);
        etBrightness4 = findViewById(R.id.et_brightness_value_4);

        refillCollapsibleSettings();

        Button btnSave = findViewById(R.id.btn_save_settings);
        btnSave.setOnClickListener(arg0 -> {
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
        });

        RadioButton rbWAlways = findViewById(R.id.rb_work_always);
        RadioButton rbWPortrait = findViewById(R.id.rb_work_portrait);
        RadioButton rbWLandscape = findViewById(R.id.rb_work_landscape);
        RadioButton rbWUnlock = findViewById(R.id.rb_work_unlock);

        if (sett.mode == Constants.WORK_MODE_ALWAYS) rbWAlways.setChecked(true);
        if (sett.mode == Constants.WORK_MODE_PORTRAIT) rbWPortrait.setChecked(true);
        if (sett.mode == Constants.WORK_MODE_LANDSCAPE) rbWLandscape.setChecked(true);
        if (sett.mode == Constants.WORK_MODE_UNLOCK) rbWUnlock.setChecked(true);

        RadioGroup rgWorkMode = findViewById(R.id.rg_work_mode);
        rgWorkMode.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == R.id.rb_work_always) sett.mode = Constants.WORK_MODE_ALWAYS;
            if (checkedId == R.id.rb_work_portrait) sett.mode = Constants.WORK_MODE_PORTRAIT;
            if (checkedId == R.id.rb_work_landscape) sett.mode = Constants.WORK_MODE_LANDSCAPE;
            if (checkedId == R.id.rb_work_unlock) sett.mode = Constants.WORK_MODE_UNLOCK;

            sett.save();
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openAppSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkAndRequestPermissions()) {
            // FIX: Only auto-start if the user hasn't explicitly stopped it
            if (!isServiceRunning() && getServiceEnabledPref()) {
                runService();
            }
            displayServiceStatus(isServiceRunning() ? 1 : 0);
        }
    }

    private void setServiceEnabledPref(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("AutoLightPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

    private boolean getServiceEnabledPref() {
        SharedPreferences prefs = getSharedPreferences("AutoLightPrefs", MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, true);
    }

    private void runService() {
        Intent serviceIntent = new Intent(this, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void killService() {
        stopService(new Intent(this, LightService.class));
    }

    private boolean isServiceRunning() {
        return LightService.isRunning;
    }

    private void sendBroadcastToService(int payload) {
        Intent i = new Intent();
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        i.setAction(Constants.SERVICE_INTENT_ACTION);
        sendBroadcast(i);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (this.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
            }
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Settings.System.canWrite(this)) {
            return true;
        } else {
            if (isDialogShown) return false;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.permission_request);
            builder.setPositiveButton(R.string.settings, (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                isDialogShown = false;
            });
            builder.setCancelable(false).show();
            isDialogShown = true;
            return false;
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void displayServiceStatus(int status) {
        switch (status) {
            case 0:
                btnStart.setText(R.string.start);
                tvState.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                tvState.setText(R.string.service_stopped);
                break;
            case 1:
                btnStart.setText(R.string.stop);
                tvState.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                tvState.setText(R.string.service_running);
                break;
            case -1:
                tvState.setText(R.string.starting_service);
                break;
        }
    }

    private void refillCollapsibleSettings() {
        etSensor1.setText(String.valueOf(sett.l1));
        etSensor2.setText(String.valueOf(sett.l2));
        etSensor3.setText(String.valueOf(sett.l3));
        etSensor4.setText(String.valueOf(sett.l4));
        etBrightness1.setText(String.valueOf(sett.b1));
        etBrightness2.setText(String.valueOf(sett.b2));
        etBrightness3.setText(String.valueOf(sett.b3));
        etBrightness4.setText(String.valueOf(sett.b4));
    }
}
