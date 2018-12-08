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
 * ��������� �����ϰ� �����ϴ� �۾��� �ϴ� Ŭ����
 * 
 * connections with other devices. It has a thread that listens for incoming
 * �ٸ� ������� ����. ������ ������ �ޱ����� �����带 ������.
 * 
 * connections, a thread for connecting with a device, and a thread for
 * ���� ������ �ϱ� ���� ������
 * 
 * performing data transmissions when connected.
 * ������ �Ǿ��� �� ������ ������ ���� ������
 */


public class BluetoothGameService {
	// Debugging
	// �����
	private static final String TAG = "BluetoothGameControllerService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	// ���� ������ ����� ������ SDP record�� �̸�
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
	// ���� ���¸� ��Ÿ���� �����
	// �ƹ��͵� ���� ������ 0
	// ������ ������ ������ 1
	// ������ ������ �ʱ�ȭ 2
	// ��ġ�� ���� 3
	// ���� ���� 4
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
	 * ���� chat ������ ���¸� ����
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 * ���� ���� ���¸� �����ϴ� ����
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;


		// Give the new state to the Handler so the UI Activity can update
		// �ڵ鷯���� ���ο� ���� ��. UI Activity�� ������Ʈ�� �� �� �ֵ���
		mHandler.obtainMessage(BluetoothGameController.MESSAGE_STATE_CHANGE,
				state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state.
	 * ���� ���� ���� ���� ��ȯ��
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode.
	 * chat ���񽺸� ������
	 * 
	 * accept thread�� ������ ��忡�� ������ ������
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		// ������ �Ϸ��� �õ��ϴ� �����带 ���
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// ���� �����ϴ� ������ ���
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		// ������ ����(������� �������� listen)
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
	 * ���� ����
	 */
	public synchronized void reset() {
		// Cancel any thread attempting to make a connection
		// ������ �Ϸ��� �õ��ϴ� �����带 ���
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// ���� �����ϴ� ������ ���
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		// ������ ����(������� �������� listen)
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}

		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * ���� ������ �����ϱ� ���� connect thread ����
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 * �����ϱ� ���� ������� ���
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		// ������ �Ϸ��� �õ��ϴ� �����带 ���
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		// ���� �����ϴ� ������ ���
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		// ������ ����(������� �������� listen)
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * ������� ������ �����ϴ� ���� �����ϱ� ���� ����� ������ ����
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * ������� ���� ������ �Ǵ� ��
	 * @param device
	 *            The BluetoothDevice that has been connected
	 * ������� ��� �����
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// Cancel the thread that completed the connection
		// ������ ���� ������ cancel
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// ���� �����ϴ� ������ cancel
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		// ������ ������ cancel �ϳ��� ��⿡�� ������ ���ϱ� ������
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		// ������ �����ϰ� ������ �����ϴ� ������ ����
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		// ����� ����� �̸��� UI Activity�� ������
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
	 * ��� ������ ����
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
		// �ӽ� ��ü ����
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		// ����� �������� ���纻�� ����ȭ
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		// write �񵿱�ȭ ����
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 * ���� �õ� ���и� ��Ÿ��
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		// ���� �޼��� ����
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
	 * ������ ���������� ��Ÿ���� UI activity�� �˸�
	 */
	private void connectionLost() {
		setState(STATE_LOST_CONNECTION);
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * �� ������� ������ ������ listen�ϴ� �� �����.
	 * like a server-side client. It runs until a connection is accepted (or
	 * server-side client ó�� �۵���.
	 * ������ accept�ǰų� cancel�ɶ� ���� ������.
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		// ���� ���� ����
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			// ���ο� ������ ���� ���� ����
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
			// ������ �Ǿ����� �ʴٸ� ���� ������ ����.
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					// blocking call -> ���ῡ �����ϰų� exception �� �� �ϳ� ��ȯ
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// If a connection was accepted
				// ������ accept�ɶ�
				if (socket != null) {
					synchronized (BluetoothGameService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							// ����� ������ ����
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						// �غ� �ȵǾ��ְų� ������� �ʾҴٸ� ���ο� ���� ����
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
	 * �� ������� ���� ������ ������ �õ� �ɶ� �����.
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
			// �־��� ������� ���ϰ� ����Ǿ��ִ� ������� ������ �޾ƿ�
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
			// �߰��� cancel��, ������ ������ �� �� �ֱ� ������
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			// ������� ���ϰ��� ����
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				// blocking call -> ������ ���� or exception�� ��ȯ��
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
				// ��� ��� �ٽ� ���� ���� ���� ����
				BluetoothGameService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			// ������ ������ ����� ������ �ʱ�ȭ
			synchronized (BluetoothGameService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			// ����� ������ ����
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
	 * ���� ���� �����ϰ� ���� �� �۵��ϴ� ������
	 * incoming and outgoing transmissions.
	 * ��� ����� ���۵��� �����Ѵ�.
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
			// ����������� ����� ��Ʈ���� �޾ƿ�
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
			// ���� ���� �� �Է½�Ʈ���� ����ؼ� listen
			while (true) {
				try {
					// Read from the InputStream
					// �Է½�Ʈ������ ���� �о��
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
				// ���� �޽��� ����
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
