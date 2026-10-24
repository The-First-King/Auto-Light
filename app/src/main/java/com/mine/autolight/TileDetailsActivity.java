package com.mine.autolight;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Switch;

// Panel opened from the Auto Light tile: service on/off and QR or barcode reader mode.
public class TileDetailsActivity extends Activity {

    private Switch swService, swQrMode;
    private boolean updating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile_details);
        setTitle(R.string.app_name);

        // Panel width: 90% of the screen (height wraps the content)
        getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9f),
                WindowManager.LayoutParams.WRAP_CONTENT);

        swService = findViewById(R.id.switch_service);
        swQrMode = findViewById(R.id.switch_qr_mode);

        swService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updating) return;
            ServiceController.setServiceOn(this, isChecked);
            refresh();
        });

        swQrMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updating) return;
            if (isChecked) ServiceController.startQrMode(this);
            else ServiceController.endQrMode(this);
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        updating = true;
        boolean qrActive = ServiceController.isQrModeActive(this);
        swService.setChecked(ServiceController.isEnabledByUser(this) && !qrActive);
        swQrMode.setChecked(qrActive);
        // QR mode can only start while the service is on; it stays switchable while active so it can be ended
        swQrMode.setEnabled(ServiceController.isEnabledByUser(this) || qrActive);
        updating = false;
    }
}
