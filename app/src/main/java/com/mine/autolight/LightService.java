package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

public class LightService extends Service {

    public static boolean isRunning = false;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AutoLightServiceChannel";

    private MySettings settings;
    private LightControl lightControl;

    // Receives system broadcasts only
    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            boolean isLandscape =
                    getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            lightControl.setLandscape(isLandscape);

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                lightControl.prepareForScreenOn();
            } else if (Intent.ACTION_USER_PRESENT.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
                if (settings.mode == Constants.WORK_MODE_UNLOCK) {
                    lightControl.onScreenUnlock();
                } else {
                    lightControl.startListening();
                }
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                if (settings.mode == Constants.WORK_MODE_UNLOCK) {
                    lightControl.onScreenUnlock();
                } else {
                    lightControl.startListening();
                }
            }
        }
    };

    // Receives app-internal commands only
    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Constants.SERVICE_INTENT_ACTION.equals(intent.getAction())) return;

            int payload = intent.getIntExtra(Constants.SERVICE_INTENT_EXTRA, -1);

            if (payload == Constants.SERVICE_INTENT_PAYLOAD_PING) {
                String status = "Lux: " + lightControl.getLastSensorValue()
                        + "\nBrightness: " + lightControl.getSetBrightness();
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
            } else if (payload == Constants.SERVICE_INTENT_PAYLOAD_SET) {
                lightControl.reconfigure();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        settings = new MySettings(this);
        lightControl = new LightControl(this);

        // System broadcasts
        IntentFilter sys = new IntentFilter();
        sys.addAction(Intent.ACTION_SCREEN_ON);
        sys.addAction(Intent.ACTION_SCREEN_OFF);
        sys.addAction(Intent.ACTION_USER_PRESENT);
        sys.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        sys.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        // Internal command broadcasts
        IntentFilter cmd = new IntentFilter(Constants.SERVICE_INTENT_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // System broadcasts originate from system
            registerReceiver(systemReceiver, sys, Context.RECEIVER_EXPORTED);

            // Internal commands should not be exported
            registerReceiver(commandReceiver, cmd, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(systemReceiver, sys);
            registerReceiver(commandReceiver, cmd);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification.Builder builder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        ? new Notification.Builder(this, CHANNEL_ID)
                        : new Notification.Builder(this);

        Notification notification = builder
                .setContentTitle("Auto Light Active")
                .setContentText("Monitoring brightness modes")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        boolean isLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        lightControl.setLandscape(isLandscape);

        if (settings.mode == Constants.WORK_MODE_ALWAYS) {
            lightControl.startListening();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Light Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;

        if (lightControl != null) {
            lightControl.stopListening();
        }

        try { unregisterReceiver(systemReceiver); } catch (Exception ignored) { }
        try { unregisterReceiver(commandReceiver); } catch (Exception ignored) { }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
