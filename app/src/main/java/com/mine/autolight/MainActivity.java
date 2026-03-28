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
    private static final int SENSOR_MIN = 0;
    private static final int SENSOR_MAX = 200000;
    private static final int BRIGHTNESS_MIN = 1;
    private static final int BRIGHTNESS_MAX = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        Button btnExpand = findViewById(R.id.btn_expand);
        LinearLayout llHidden = findViewById(R.id.ll_hidden_settings);
        llHidden.setVisibility(View.GONE);

        btnExpand.setOnClickListener(v -> {
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

        btnStart.setOnClickListener(v -> {
            if (isServiceRunning()) {
                setServiceEnabledPref(false);
                killService();
                displayServiceStatus(0);
            } else {
                setServiceEnabledPref(true);
                runService();
                displayServiceStatus(-1);
            }
        });

        Button btnState = findViewById(R.id.btn_get_state);
        btnState.setOnClickListener(v -> {
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
        btnSave.setOnClickListener(v -> {
            if (validateAndSaveSettings()) {
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
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

    private boolean validateAndSaveSettings() {
        try {
            int l1 = Integer.parseInt(etSensor1.getText().toString());
            int l2 = Integer.parseInt(etSensor2.getText().toString());
            int l3 = Integer.parseInt(etSensor3.getText().toString());
            int l4 = Integer.parseInt(etSensor4.getText().toString());

            int b1 = Integer.parseInt(etBrightness1.getText().toString());
            int b2 = Integer.parseInt(etBrightness2.getText().toString());
            int b3 = Integer.parseInt(etBrightness3.getText().toString());
            int b4 = Integer.parseInt(etBrightness4.getText().toString());

            if (!isValidSensorValue(l1)) {
                showErrorDialog("1) Sensor must be between " + SENSOR_MIN + " and " + SENSOR_MAX);
                return false;
            }
            if (!isValidSensorValue(l2)) {
                showErrorDialog("2) Sensor must be between " + SENSOR_MIN + " and " + SENSOR_MAX);
                return false;
            }
            if (!isValidSensorValue(l3)) {
                showErrorDialog("3) Sensor must be between " + SENSOR_MIN + " and " + SENSOR_MAX);
                return false;
            }
            if (!isValidSensorValue(l4)) {
                showErrorDialog("4) Sensor must be between " + SENSOR_MIN + " and " + SENSOR_MAX);
                return false;
            }

            if (!isValidBrightnessValue(b1)) {
                showErrorDialog("1) Brightness must be between " + BRIGHTNESS_MIN + " and " + BRIGHTNESS_MAX);
                return false;
            }
            if (!isValidBrightnessValue(b2)) {
                showErrorDialog("2) Brightness must be between " + BRIGHTNESS_MIN + " and " + BRIGHTNESS_MAX);
                return false;
            }
            if (!isValidBrightnessValue(b3)) {
                showErrorDialog("3) Brightness must be between " + BRIGHTNESS_MIN + " and " + BRIGHTNESS_MAX);
                return false;
            }
            if (!isValidBrightnessValue(b4)) {
                showErrorDialog("4) Brightness must be between " + BRIGHTNESS_MIN + " and " + BRIGHTNESS_MAX);
                return false;
            }

            if (!(l1 < l2 && l2 < l3 && l3 < l4)) {
                showErrorDialog("Sensor values must be in ascending order: 1) < 2) < 3) < 4)");
				return false;
            }
			
			if (!(b1 < b2 && b2 < b3 && b3 < b4)) {
				showErrorDialog("Brightness values must be in ascending order: 1) < 2) < 3) < 4)");
				return false;
            }

            sett.l1 = l1;
            sett.l2 = l2;
            sett.l3 = l3;
            sett.l4 = l4;

            sett.b1 = b1;
            sett.b2 = b2;
            sett.b3 = b3;
            sett.b4 = b4;

            sett.save();
            return true;

        } catch (NumberFormatException e) {
            showErrorDialog("Invalid input. Please enter valid numbers.");
            return false;
        }
    }

    private boolean isValidSensorValue(int value) {
        return value >= SENSOR_MIN && value <= SENSOR_MAX;
    }

    private boolean isValidBrightnessValue(int value) {
        return value >= BRIGHTNESS_MIN && value <= BRIGHTNESS_MAX;
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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
            if (!isServiceRunning() && getServiceEnabledPref()) {
                runService();
            }
            displayServiceStatus(isServiceRunning() ? 1 : 0);
        }
    }

    private void setServiceEnabledPref(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREF_ENABLED_KEY, enabled).apply();
    }

    private boolean getServiceEnabledPref() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(Constants.PREF_ENABLED_KEY, true);
    }

    private void runService() {
        Intent serviceIntent = new Intent(this, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
        else startService(serviceIntent);
    }

    private void killService() {
        stopService(new Intent(this, LightService.class));
    }

    private boolean isServiceRunning() {
        return LightService.isRunning;
    }

    private void sendBroadcastToService(int payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.setPackage(getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        sendBroadcast(i);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
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
