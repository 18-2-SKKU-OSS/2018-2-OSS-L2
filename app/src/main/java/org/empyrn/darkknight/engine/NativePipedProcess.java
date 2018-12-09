package org.empyrn.darkknight.engine;

public class NativePipedProcess {
	static {
		System.loadLibrary("jni");
	}

	private boolean processAlive;

	NativePipedProcess() {
		processAlive = false;
	}

	/** Start process. */
	// ���μ��� ����
	public final void initialize() {
		if (!processAlive) {
			startProcess();
			processAlive = true;
		}
	}

	/** Shut down process. */
	// ���μ��� ����
	public final void shutDown() {
		if (processAlive) {
			writeLineToProcess("quit");
			processAlive = false;
		}
	}

	/**
	 * Read a line from the process.
	 * ���μ����� ���� ���� �о��
	 * @param timeoutMillis Maximum time to wait for data
	 * ������ ��ٸ��� �ִ� �ð�
	 * @return The line, without terminating newline characters,
	 * ����, �����ϴ� ���� ���� ����
	 *         or empty string if no data available,
	 * ������ �����Ͱ� ������ empty string
	 *         or null if I/O error.
	 * ��ǲ �ƿ�ǲ ������ null
	 */
	public final String readLineFromProcess(int timeoutMillis) {
		String ret = readFromProcess(timeoutMillis);
		if (ret == null)
			return null;
		if (ret.length() > 0) {
//			System.out.printf("Engine -> GUI: %s\n", ret);
		}
		return ret;
	}

	/** Write a line to the process. \n will be added automatically. */
	// ���μ����� �� �� ��. ���� ���ڰ� �ڵ������� �߰���.
	public final synchronized void writeLineToProcess(String data) {
//		System.out.printf("GUI -> Engine: %s\n", data);
		writeToProcess(data + "\n");
	}

	/** Start the child process. */
	// �ڽ� ���μ��� ����
	private final native void startProcess();

	/**
	 * Read a line of data from the process.
	 * Return as soon as there is a full line of data to return, 
	 * or when timeoutMillis milliseconds have passed.
	 */
	private final native String readFromProcess(int timeoutMillis);

	/** Write data to the process. */
	private final native void writeToProcess(String data);
}
