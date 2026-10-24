package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Fires when the QR-mode pause runs out and re-enables the service.
public class QrModeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceController.endQrMode(context);
    }
}
