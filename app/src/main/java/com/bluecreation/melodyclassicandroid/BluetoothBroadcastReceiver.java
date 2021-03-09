package com.bluecreation.melodyclassicandroid;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bluecreation.melody.SppService;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String actionString = intent.getAction();
		Log.d("BTBroadCastReceiver", "onReceive: ActionString = " + actionString);
		int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

		if (newState == BluetoothAdapter.STATE_OFF) {
			SppService.getInstance().stop();
		} else if (newState == BluetoothAdapter.STATE_ON) {
			SppService.getInstance().start();
		}
	}
}
