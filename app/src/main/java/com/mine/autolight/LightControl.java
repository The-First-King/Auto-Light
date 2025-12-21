package com.mine.autolight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;

import android.content.ContentResolver;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class LightControl implements SensorEventListener {
	private final SensorManager sMgr;
	private final Sensor lightSensor;
	private final MySettings sett;
	private final ContentResolver cResolver;

	private final Context mContext;
	private final TelephonyManager telephonyManager;

	private final Handler delayer = new Handler();

	private final long pause = 2000;

	private boolean onListen = false;
	private boolean landscape = false;

	private float lux = 0;
	private int tempBrightness = 0;

	LightControl(Context context) {
		sett = new MySettings(context);
		cResolver = context.getContentResolver();
		sMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

		mContext = context;
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			lux = event.values[0];
			setBrightness((int) lux);
		}
	}

	private void scheduleSuspend() {
		if (sett.mode == Constants.WORK_MODE_ALWAYS)
			return;

		if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape)
			return;

		if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape)
			return;

		delayer.postDelayed(new Runner(), pause);
	}

	private class Runner implements Runnable {
		public void run() {
			boolean ringing = false;
			if (mContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
				ringing = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING;
			}

			if (ringing) {
				delayer.postDelayed(new Runner(), pause);
			} else {
				stopListening();
			}

		}
	}

	public void startListening() {
		if (!onListen) {
			sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
			onListen = true;
		}

		scheduleSuspend();
	}

	public void stopListening() {
		if (onListen)
			sMgr.unregisterListener(this);

		delayer.removeCallbacksAndMessages(null);
		onListen = false;
	}

	/**
	 * Improved interpolation:
	 * - Keeps the same 4 configurable points and same segment selection logic.
	 * - Interpolates in log10(lux + 1) domain (non-linear mapping that better matches human perception).
	 * - Falls back to exact endpoint brightnesses for lux <= l1 and lux >= l4.
	 */
	private void setBrightness(int lux) {
		int brightness;
		// keep old behavior at extremes
		if (lux <= sett.l1) {
			brightness = sett.b1;
		} else if (lux >= sett.l4) {
			brightness = sett.b4;
		} else {
			// determine active segment (same logic as original)
			float x1, y1, x2, y2;
			if (lux <= sett.l2) {
				x1 = sett.l1;
				x2 = sett.l2;
				y1 = sett.b1;
				y2 = sett.b2;
			} else if (lux <= sett.l3) {
				x1 = sett.l2;
				x2 = sett.l3;
				y1 = sett.b2;
				y2 = sett.b3;
			} else {
				x1 = sett.l3;
				x2 = sett.l4;
				y1 = sett.b3;
				y2 = sett.b4;
			}

			// map lux values to log domain to get a perceptually better interpolation
			double lx = Math.log10((double) lux + 1.0);
			double lx1 = Math.log10((double) x1 + 1.0);
			double lx2 = Math.log10((double) x2 + 1.0);

			double t = 0.0;
			if (Double.compare(lx2, lx1) != 0) {
				t = (lx - lx1) / (lx2 - lx1);
				// clamp t just in case
				if (t < 0.0) t = 0.0;
				if (t > 1.0) t = 1.0;
			} else {
				// identical endpoints — choose the lower
				t = 0.0;
			}

			brightness = (int) Math.round(y1 + (y2 - y1) * t);
		}

		tempBrightness = brightness;

		// Apply brightness only if different; wrap Settings access in try/catch to be safe
		try {
			int currentBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
			if (currentBrightness != brightness) {
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
			}
		} catch (Exception ex) {
			// ignore failures to read/write system settings (keeps app safe)
		}
	}

	public void reconfigure() {
		stopListening();
		sett.load();
		startListening();
	}

	public void setLandscape(boolean land) {
		landscape = land;
	}

	public void onScreenUnlock() {
		Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

		startListening();
	}

	public int getLastSensorValue() {
		return (int) lux;
	}

	public int getSetBrightness() {
		return tempBrightness;
	}
}
