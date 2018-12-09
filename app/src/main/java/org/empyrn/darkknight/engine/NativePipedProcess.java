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
	// 프로세스 시작
	public final void initialize() {
		if (!processAlive) {
			startProcess();
			processAlive = true;
		}
	}

	/** Shut down process. */
	// 프로세스 종료
	public final void shutDown() {
		if (processAlive) {
			writeLineToProcess("quit");
			processAlive = false;
		}
	}

	/**
	 * Read a line from the process.
	 * 프로세스로 부터 한줄 읽어옴
	 * @param timeoutMillis Maximum time to wait for data
	 * 데이터 기다리는 최대 시간
	 * @return The line, without terminating newline characters,
	 * 라인, 종료하는 개행 문자 없이
	 *         or empty string if no data available,
	 * 가능한 데이터가 없으면 empty string
	 *         or null if I/O error.
	 * 인풋 아웃풋 에러시 null
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
	// 프로세스에 한 줄 씀. 개행 문자가 자동적으로 추가됨.
	public final synchronized void writeLineToProcess(String data) {
//		System.out.printf("GUI -> Engine: %s\n", data);
		writeToProcess(data + "\n");
	}

	/** Start the child process. */
	// 자식 프로세스 시작
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
