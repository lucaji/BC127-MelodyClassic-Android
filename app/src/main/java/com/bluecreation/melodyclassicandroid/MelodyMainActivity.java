//
// Melody Android
// MelodyMainActivity.java
//
// Copyright (c) 2013 Cambridge Executive (BlueCreation). All rights reserved.
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// This code can only be used to connect to a Product containing Melody. This can be Melody
// software or a BlueCreation module or Sierra Wireless module.
//

package com.bluecreation.melodyclassicandroid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bluecreation.melody.SppService;
import com.bluecreation.melody.SppService.ConnectionState;

public class MelodyMainActivity extends Activity {

	// Debugging
	private static final String DEBUG_TAG = MelodyMainActivity.class.getSimpleName();
	// Flag to display log messages
	private static final boolean DEBUG_MODE = true;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private TextView mInEditText;
	private TextView connectedText;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the melody services
	private SppService mSppService = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "+++ ON CREATE +++");
		}

		// Set up the window layout
		// setContentView(R.layout.main);
		setContentView(R.layout.activity_main);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "++ ON START ++");
		}

		addVersionNameInTitle();

		// If BT is not on, request that it be enabled.
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			// Setup melody SPP service
			if (mSppService == null) {
				setupMelody();
			}
		}
	}

	private String getVersionDescriptionString() {
		String msgString;
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			msgString = "Melody Android v" + pInfo.versionName;
			msgString += "\nMelodySppService v" + SppService.version;
		} catch (NameNotFoundException e) {
			msgString = "\nError (NameNotFoundException): " + e.getMessage();
		} catch (Exception ex) {
			msgString = "\nException: " + ex.getMessage();
		}
		return msgString;
	}

	private void addVersionNameInTitle() {
		String versionName = getVersionDescriptionString();
		setTitle(versionName);
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "+ ON RESUME +");
		}

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mSppService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mSppService.getState() == ConnectionState.STATE_NONE) {
				// Start the melody services to be able to accept connections
				mSppService.start();
			}
		}
	}

	private void setupMelody() {
		Log.d(DEBUG_TAG, "setupMelody()");
		Log.d(DEBUG_TAG, DEBUG_MODE?"DEBUG MODE ON":"DEBUG MODE OFF");

		// Initialize the array adapter for the conversation thread
		mInEditText = findViewById(R.id.edit_text_in);
		connectedText = findViewById(R.id.ConnectedText);

		// Initialize the compose field with a listener for the return key
		// Layout Views
		// private ListView mConversationView;
		EditText mOutEditText = findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		Button mSendButton = findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			// Send a message using content of the edit text widget
			EditText view = findViewById(R.id.edit_text_out);
			String message = view.getText().toString();
			sendMessage(message);
			}
		});

		// Initialize the MelodySppService to perform bluetooth connections
		mSppService = SppService.getInstance();
		mSppService.registerListener(sppListener);

		updateConnectedText();
	}

	void updateConnectedText() {
		String status = "Unknown";
		switch (mSppService.getState()) {
		case STATE_NONE:
			status = "Not Connected";
			break;
		case STATE_LISTEN:
			status = "Listening...";
			break;
		case STATE_CONNECTED:
			status = "Connected";
			break;
		case STATE_CONNECTING:
			status = "Connecting...";
			break;
		}
		connectedText.setText(status);
	}

	SppService.Listener sppListener = new SppService.Listener() {
		@Override
		public void onStateChanged(final ConnectionState state) {
			if (DEBUG_MODE) {
				Log.i(DEBUG_TAG, "MESSAGE_STATE_CHANGE: " + state);
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				updateConnectedText();

				if (state == ConnectionState.STATE_CONNECTED) {
					mInEditText.setText("", TextView.BufferType.EDITABLE);
					if (DEBUG_MODE) {
						// send the time and date upon connection when in debug mode
						String s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
						sendMessage(s);
					}
				}
				}
			});
		}

		@Override
		public void onRemoteDeviceConnected(String deviceName) {
			mConnectedDeviceName = deviceName;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				Toast.makeText(getApplicationContext(), "Connection to " + mConnectedDeviceName + " Successful", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onDataReceived(final byte[] data, final int length) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				String readMessage = new String(data, 0, length);
				mInEditText.setText(readMessage);
				}
			});
		}

		@Override
		public void onConnectionLost() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onConnectionFailed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "- ON PAUSE -");
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "-- ON STOP --");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the melody services
		if (mSppService != null) {
			mSppService.unregisterListener(sppListener);
			mSppService.stop();
			mSppService = null;
		}

		if (DEBUG_MODE) {
			Log.e(DEBUG_TAG, "--- ON DESTROY ---");
		}
	}

	private void ensureDiscoverable() {
		if (DEBUG_MODE) {
			Log.d(DEBUG_TAG, "ensure discoverable");
		}
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	private void disconnect_device() {
		if (mSppService.getState() != ConnectionState.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		mSppService.disconnect();
	}

	/**
	 * Sends a message.
	 *
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mSppService.getState() != ConnectionState.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the MelodyService to write
			byte[] send = message.getBytes();
			mSppService.send(send);
		}
	}

	// The action listener for the EditText widget, to listen for the return key
	private final TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (DEBUG_MODE) {
				Log.i(DEBUG_TAG, "END onEditorAction");
			}
			return true;
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG_MODE) {
			Log.d(DEBUG_TAG, "onActivityResult " + resultCode);
		}
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up melodyservice
				setupMelody();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(DEBUG_TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data) {
		// Get the device MAC address
		Bundle extras = data.getExtras();
		if (extras == null) { return; }
		String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		if (address == null) { return; }
		if (BluetoothAdapter.checkBluetoothAddress(address)) {
			// Get the BluetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mSppService.connect(device);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.connect_scan) {
			// Launch the DeviceListActivity to see devices and do scan
			if (mSppService.getState() == ConnectionState.STATE_CONNECTED) {
				Toast.makeText(this, "Already connected!", Toast.LENGTH_SHORT).show();
			} else {
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
			return true;
		} else if (itemId == R.id.disconnect_device) {
			// disconnect a connected to device.
			disconnect_device();
			return true;
		} else if (itemId == R.id.discoverable) {
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		} else if (itemId == R.id.about) {
			String msgString = getVersionDescriptionString();
			Toast.makeText(this, msgString, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	public void imageButtonOnClick(View v) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sierrawireless.com/"));
		startActivity(browserIntent);
	}
}
