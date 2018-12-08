package org.empyrn.darkknight.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * 블루투스를 설정하고 관리하는 작업을 하는 클래스
 * 
 * connections with other devices. It has a thread that listens for incoming
 * 다른 기기들과의 연결. 들어오는 연결을 받기위한 쓰레드를 가진다.
 * 
 * connections, a thread for connecting with a device, and a thread for
 * 기기와 연결을 하기 위한 쓰레드
 * 
 * performing data transmissions when connected.
 * 연결이 되었을 때 데이터 전송을 위한 쓰레드
 */


public class BluetoothGameService {
	// Debugging
	// 디버깅
	private static final String TAG = "BluetoothGameControllerService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	// 서버 소켓이 만들어 졌을때 SDP record의 이름
	private static final String NAME = "BluetoothGameController";

	// Unique UUID for this application

	private static final UUID MY_UUID = UUID.fromString("72caefa0-568b-11e0-b8af-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	// 현재 상태를 나타내는 상수들
	// 아무것도 하지 않을때 0
	// 들어오는 연결을 받을때 1
	// 나가는 연결을 초기화 2
	// 장치와 연결 3
	// 연결 끊김 4
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device
	public static final int STATE_LOST_CONNECTION = 4; // lost connection

	/**
	 * Constructor. Prepares a new BluetoothGameController session.
	 * 
	 * @param context
	 *            The UI Activity Context
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothGameService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * 현재 chat 연결의 상태를 설정
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 * 현재 연결 상태를 저장하는 변수
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;


		// Give the new state to the Handler so the UI Activity can update
		// 핸들러에게 새로운 상태 줌. UI Activity가 업데이트를 할 수 있도록
		mHandler.obtainMessage(BluetoothGameController.MESSAGE_STATE_CHANGE,
				state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state.
	 * 연재 연결 상태 값을 반환함
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode.
	 * chat 서비스를 시작함
	 * 
	 * accept thread가 리스닝 모드에서 세션을 시작함
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		// 연결을 하려고 시도하는 쓰레드를 취소
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// 현재 연결하는 쓰레드 취소
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		// 쓰레드 시작(블루투스 서버소켓 listen)
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN);

		Message msg = mHandler
				.obtainMessage(BluetoothGameController.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothGameController.TOAST,
				"Bluetooth chess service started");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Reset the service.
	 * 서버 리셋
	 */
	public synchronized void reset() {
		// Cancel any thread attempting to make a connection
		// 연결을 하려고 시도하는 쓰레드를 취소
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// 현재 연결하는 쓰레드 취소
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		// 쓰레드 시작(블루투스 서버소켓 listen)
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}

		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 기기와 연결을 시작하기 위해 connect thread 시작
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 * 연결하기 위한 블루투스 기기
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		// 연결을 하려고 시도하는 쓰레드를 취소
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		// 현재 연결하는 쓰레드 취소
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		// 쓰레드 시작(블루투스 서버소켓 listen)
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 블루투수 연결을 관리하는 것을 시작하기 위해 연결된 쓰레드 시작
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * 블루투스 소켓 연결이 되는 곳
	 * @param device
	 *            The BluetoothDevice that has been connected
	 * 블루투스 기기 연결됨
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// Cancel the thread that completed the connection
		// 연결이 끝난 쓰레드 cancel
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// 현재 연결하는 쓰레드 cancel
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		// 받이진 쓰레드 cancel 하나의 기기에만 연결을 원하기 때문에
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		// 연결을 관리하고 전송을 수행하는 쓰레드 시작
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		// 연결된 기기의 이름을 UI Activity에 보내줌
		Message msg = mHandler
				.obtainMessage(BluetoothGameController.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothGameController.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop all threads
	 * 모든 쓰레드 정지
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");

		Message msg = mHandler
				.obtainMessage(BluetoothGameController.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothGameController.TOAST,
				"Bluetooth chess service stopped");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		// 임시 객체 생성
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		// 연결된 쓰레드의 복사본과 동기화
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		// write 비동기화 수행
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 * 연결 시도 실패를 나타냄
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		// 실패 메세지 보냄
		Message msg = mHandler
				.obtainMessage(BluetoothGameController.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothGameController.TOAST,
				"Unable to connect to device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 * 연결이 끊어졌음을 나타내고 UI activity에 알림
	 */
	private void connectionLost() {
		setState(STATE_LOST_CONNECTION);
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * 이 쓰레드는 들어오는 연결을 listen하는 중 수행됨.
	 * like a server-side client. It runs until a connection is accepted (or
	 * server-side client 처럼 작동함.
	 * 연결이 accept되거나 cancel될때 까지 동작함.
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		// 로컬 서버 소켓
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			// 새로운 리스닝 서버 소켓 만듬
			try {
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D)
				Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			// 연결이 되어있지 않다면 서버 소켓을 들음.
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					// blocking call -> 연결에 성공하거나 exception 둘 중 하나 반환
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// If a connection was accepted
				// 연결이 accept될때
				if (socket != null) {
					synchronized (BluetoothGameService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							// 연결된 쓰레드 시작
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						// 준비가 안되어있거나 연결되지 않았다면 새로운 소켓 종료
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread");
		}

		public void cancel() {
			if (D)
				Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * 이 쓰레드는 기기와 나가는 연결이 시도 될때 수행됨.
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			// 주어진 블루투스 소켓과 연결되어있는 블루투스 소켓을 받아옴
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			// 발견을 cancel함, 연결을 느리게 할 수 있기 때문에
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			// 블루투스 소켓과의 연결
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				// blocking call -> 성공적 연결 or exception을 반환함
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				// Start the service over to restart listening mode
				// 듣기 모드 다시 시작 위해 서비스 시작
				BluetoothGameService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			// 끝났기 때문에 연결된 쓰레드 초기화
			synchronized (BluetoothGameService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			// 연결된 쓰레드 시작
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * 원격 기기와 연결하고 있을 때 작동하는 쓰레드
	 * incoming and outgoing transmissions.
	 * 모든 입출력 전송들을 관리한다.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			// 블루투스소켓 입출력 스트림을 받아옴
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// Keep listening to the InputStream while connected
			// 연결 중일 때 입력스트림을 계속해서 listen
			while (true) {
				try {
					// Read from the InputStream
					// 입력스트림으로 부터 읽어옴
					bytes = mmInStream.read(buffer);

					// Send the obtained bytes to the UI Activity
					mHandler.obtainMessage(
							BluetoothGameController.MESSAGE_READ, bytes, -1,
							buffer).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// Share the sent message back to the UI Activity
				// 보낸 메시지 공유
				mHandler.obtainMessage(BluetoothGameController.MESSAGE_WRITE,
						-1, -1, buffer).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
