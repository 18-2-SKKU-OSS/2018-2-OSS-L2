package org.empyrn.darkknight.bluetooth;

import org.empyrn.darkknight.GameMode;
import com.nemesis.materialchess.R;
import org.empyrn.darkknight.gamelogic.ChessController;
import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.TextIO;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


public class BluetoothGameController {
	// Context
	Context context;

	// Debugging
	private static final String TAG = "BluetoothChessGame";
	private static final boolean D = true;

	// Message types sent from the BluetoothGameService Handler
	// 블루투스게임서비스 핸들러로 부터 보내진 메시지 타입 
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothGameService Handler
	// 블루투스게임서비스 핸들러로부터 받은 key name
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// 각각의 변수들에 대한 설명 및 요약
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// String buffer for outgoing messages
	// private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the game services
	private BluetoothGameService mGameService = null;

	private ChessController ctrl;
	private GameMode preferredMode;

	public BluetoothGameController(Context context, ChessController ctrl,
			GameMode preferredMode) {
		this.context = context;
		this.ctrl = ctrl;
		this.preferredMode = preferredMode;

		// get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// if the adapter is null, then Bluetooth is not supported
		// 어댑터가 null일때, 블루투스 지원을 하지 않음
		if (mBluetoothAdapter == null) {
			Toast.makeText(context, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			return;
		}
	}

	public void connectToDevice(BluetoothDevice device) {
		mGameService.connect(device);
	}

	public void setupBluetoothService() {
		if (!mBluetoothAdapter.isEnabled()) {
			Toast.makeText(
					context,
					R.string.bt_not_enabled_leaving,
					Toast.LENGTH_LONG).show();
			return;
		}

		Log.d(TAG, "setupBluetoothService()");

		// Initialize the BluetoothGameService to perform bluetooth connections
		// 블루투스 연결을 위한 블루투스게임서비스 초기화
		mGameService = new BluetoothGameService(context, mHandler);
		mGameService.start();
	}

	public void stopBluetoothService() {
		if (mGameService != null)
			mGameService.stop();
	}

	/**
	 * Sends a message.
	 * 메시지를 보냄
	 * 
	 * @param message
	 *            A string of text to send.
	 * 전달할 문자열
	 */
	private void sendMessage(String message) {
		// 무언가 하기 전에 연결이 되어있는지 우선 확인함
		// Check that we're actually connected before trying anything
		if (mGameService.getState() != BluetoothGameService.STATE_CONNECTED) {
			return;
		}

		// Check that there's actually something to send
		// 보내지는 것이 실제로 있는지 확인
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothGameService to write
			// 메시지 바이트를 받고 블루투스게임서비스에게 write하라고 요청함
			byte[] send = message.getBytes();
			mGameService.write(send);
		}
	}

	public void sendMove(Move m) {
		this.sendMessage(TextIO.moveToUCIString(m));
	}

	// The action listener for the EditText widget, to listen for the return key
	// 반환 값을 받음
	/*
	 * private TextView.OnEditorActionListener mWriteListener = new
	 * TextView.OnEditorActionListener() { public boolean
	 * onEditorAction(TextView view, int actionId, KeyEvent event) { // If the
	 * action is a key-up event on the return key, send the message if (actionId
	 * == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
	 * String message = view.getText().toString(); sendMessage(message); } if(D)
	 * Log.i(TAG, "END onEditorAction"); return true; } };
	 */

	// The Handler that gets information back from the BluetoothGameService
	// 블루투스게임서버로 부터 받은 정보를 핸들러가 가져옴
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothGameService.STATE_CONNECTED:
					// mTitle.setText(R.string.title_connected_to);
					// mTitle.append(mConnectedDeviceName);

					String color;
					if (preferredMode.playerWhite())
						color = "white";
					else
						color = "black";

					// begin a new game
					ctrl.newGame(preferredMode);
					ctrl.setGuiPaused(true);
					ctrl.setGuiPaused(false);
					ctrl.startGame();

					Toast.makeText(
							context,
							"Bluetooth connection established: starting new game playing "
									+ color + " against "
									+ mConnectedDeviceName, Toast.LENGTH_LONG)
							.show();

					break;
				case BluetoothGameService.STATE_CONNECTING:
					// mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothGameService.STATE_LISTEN:
					break;
				case BluetoothGameService.STATE_LOST_CONNECTION:
					ctrl.setGuiPaused(true);
					Toast.makeText(
							context,
							"Bluetooth connection to " + mConnectedDeviceName + " lost, game aborted", Toast.LENGTH_LONG)
							.show();
					mGameService.reset();
					break;
				case BluetoothGameService.STATE_NONE:
					// mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				// 버퍼로 부터 문자열을 구성
				String writeMessage = new String(writeBuf);
				System.out.println("BLUETOOTH_out: " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				// 버퍼에 있는 유효한 바이트의 문자열을 구성
				String readMessage = new String(readBuf, 0, msg.arg1);

				// make the move from Bluetooth
				// 블루투스로 부터 move를 만듬
				Move m = TextIO.UCIstringToMove(readMessage);
				ctrl.makeBluetoothMove(m);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				// 연결된 기기의 이름을 저장함
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				break;
			case MESSAGE_TOAST:
				System.out.println(msg.getData().getString("toast"));
				Toast.makeText(context, msg.getData().getString("toast"),
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// when DeviceListActivity returns with a device to connect
			// device list activity가 연결할 기기와 리턴될 때
			if (resultCode == Activity.RESULT_OK) {
				// get the device MAC address
				// 기기의 MAC 주소를 받음
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 블루투스디바이스 객체를 받음
				// get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// 디바이스와 연결하려 시도함
				// attempt to connect to the device
				mGameService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// when the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a game session
				setupBluetoothService();
			} else {
				// user did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(context, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public void reset() {
		ctrl.setGuiPaused(true);
		mGameService.reset();
		Toast.makeText(
				context,
				"Bluetooth game service reset", Toast.LENGTH_LONG)
				.show();
	}

	public BluetoothAdapter getmBluetoothAdapter() {
		return mBluetoothAdapter;
	}
}
