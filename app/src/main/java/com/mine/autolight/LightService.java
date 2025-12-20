package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat; // If using androidx, or use native Notification.Builder

public class LightService extends Service {

    private static final String CHANNEL_ID = "AutoLightServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Create the Notification Channel (Required for Android 8.0+)
        createNotificationChannel();

        // 2. Build the notification
        // This notification stays in the tray while Auto-Light is active
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto-Light Active")
                .setContentText("Monitoring ambient light to optimize brightness.")
                .setSmallIcon(R.mipmap.ic_launcher) // Use your app icon
                .build();
        }

        // 3. Start as Foreground
        // ID must be a non-zero integer
        startForeground(1, notification);

        // ... existing sensor initialization code ...
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto-Light Background Service",
                    NotificationManager.IMPORTANCE_LOW // Low importance = no sound/interruption
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    
    // ... rest of your onSensorChanged logic ...
}
