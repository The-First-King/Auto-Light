package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
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
            btnExpand.setText(isExpanded ? R.string.hide_config : R.string.show_config);
            if (isExpanded) {
                refillCollapsibleSettings();
                requestNotificationPermission();
            }
        });

        // Start/Stop Logic
        tvState = findViewById(R.id.tv_service_state);
        btnStart = findViewById(R.id.btn_start_stop);
        btnStart.setOnClickListener(v -> {
            if (isServiceRunning()) {
                killService();
                displayServiceStatus(0);
            } else {
                if (requestNotificationPermission()) {
                    runService();
                    displayServiceStatus(-1);
                }
            }
        });

        // Ping State logic remains same
        Button btnState = findViewById(R.id.btn_get_state);
        btnState.setOnClickListener(v -> {
            displayServiceStatus(isServiceRunning() ? 1 : 0);
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_PING);
        });

        // Initialize Settings Views
        etSensor1 = findViewById(R.id.et_sensor_value_1);
        etSensor2 = findViewById(R.id.et_sensor_value_2);
        etSensor3 = findViewById(R.id.et_sensor_value_3);
        etSensor4 = findViewById(R.id.et_sensor_value_4);
        etBrightness1 = findViewById(R.id.et_brightness_value_1);
        etBrightness2 = findViewById(R.id.et_brightness_value_2);
        etBrightness3 = findViewById(R.id.et_brightness_value_3);
        etBrightness4 = findViewById(R.id.et_brightness_value_4);

        refillCollapsibleSettings();

        // Save Button
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> {
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

    private void syncWorkModeUI() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) ((RadioButton)findViewById(R.id.rb_work_always)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_PORTRAIT) ((RadioButton)findViewById(R.id.rb_work_portrait)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_LANDSCAPE) ((RadioButton)findViewById(R.id.rb_work_landscape)).setChecked(true);
        else if (sett.mode == Constants.WORK_MODE_UNLOCK) ((RadioButton)findViewById(R.id.rb_work_unlock)).setChecked(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkAndRequestPermissions()) {
            displayServiceStatus(isServiceRunning() ? 1 : 0);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (LightService.class.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    private void runService() {
        Intent i = new Intent(this, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
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

    private boolean checkAndRequestPermissions() {
        if (Settings.System.canWrite(this)) return true;
        if (!isDialogShown) {
            new AlertDialog.Builder(this)
                .setMessage(R.string.permission_request)
                .setPositiveButton(R.string.settings, (d, id) -> {
                    Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                    isDialogShown = false;
                })
                .setCancelable(false).show();
            isDialogShown = true;
        }
        return false;
    }

    private void displayServiceStatus(int status) {
        int color = android.R.color.darker_gray;
        String text = "";
        if (status == 0) {
            btnStart.setText(R.string.start);
            color = android.R.color.holo_red_dark;
            text = getString(R.string.service_stopped);
        } else if (status == 1) {
            btnStart.setText(R.string.stop);
            color = android.R.color.holo_green_dark;
            text = getString(R.string.service_running);
        } else if (status == -1) {
            text = getString(R.string.starting_service);
        }
        tvState.setTextColor(getResources().getColor(color, null));
        tvState.setText(text);
    }

    private void sendBroadcastToService(int payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        i.setPackage(getPackageName()); // Crucial for security in 2025
        sendBroadcast(i);
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

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openAppSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
