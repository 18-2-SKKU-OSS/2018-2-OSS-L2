package org.empyrn.darkknight.gamelogic;
// Functions related to elapsed time and movement of pieces
//경과 시간과 피스의 이동에 관한 함수
public class TimeControl {
	private long timeControl;
	private int movesPerSession;
	private long increment;

	private long whiteBaseTime;
	private long blackBaseTime;

	int currentMove;
	boolean whiteToMove;

	long elapsed; 
	// Accumulated elapsed time for this move.
	// 이동에 대해 누적 된 경과 시간.
	long timerT0; 
	// Time when timer started. 0 if timer is stopped.
	// 타이머가 시작된 시간. 타이머가 정지하면 0.

	/** Constructor. Sets time control to "game in 5min". */
	// 생성자. 시간 컨트롤을 5분으로 설정합니다.
	public TimeControl() {
		setTimeControl(5 * 60 * 1000, 0, 0); //compute 5minute
		reset();
	}

	public final void reset() {
		currentMove = 1;
		whiteToMove = true;
		elapsed = 0;
		timerT0 = 0;
	}

	/** Set time control to "moves" moves in "time" milliseconds, + inc milliseconds per move. */
	//시간 제어를 "이동"으로 설정하면 "시간"밀리 초, 이동 당 + 밀리 초가됩니다.
	public final void setTimeControl(long time, int moves, long inc) {
		timeControl = time;
		movesPerSession = moves;
		increment = inc;
	}
	//setting move
	//이동 설정 
	public final void setCurrentMove(int move, boolean whiteToMove, long whiteBaseTime, long blackBaseTime) {
		currentMove = move;
		this.whiteToMove = whiteToMove;
		this.whiteBaseTime = whiteBaseTime;
		this.blackBaseTime = blackBaseTime;
		timerT0 = 0;
		elapsed = 0;
	}
	//check timer
	//시간 체크  
	public final boolean clockRunning() {
		return timerT0 != 0;
	}
	//setting start time
	//시작시간 설정 
	public final void startTimer(long now) {
		if (!clockRunning()) {
			timerT0 = now;
		}
	}
	//setting stop time
	//멈추는 시간 설정  
	public final void stopTimer(long now) {
		if (clockRunning()) {
			long timerT1 = now;
			long currElapsed = timerT1 - timerT0;
			timerT0 = 0;
			if (currElapsed > 0) {
				elapsed += currElapsed;
			}
		}
	}

	/** Compute new remaining time after a move is made. */
	//새롭게 남은시간을 계산  
	public final int moveMade(long now) {
		stopTimer(now);
		long remaining = getRemainingTime(whiteToMove, now);
		remaining += increment;
		if (getMovesToTC() == 1) {
			remaining += timeControl;
		}
		elapsed = 0;
		return (int)remaining;
	}

	/** Get remaining time */
	//남은 시간 얻기  
	public final int getRemainingTime(boolean whiteToMove, long now) {
		long remaining = whiteToMove ? whiteBaseTime : blackBaseTime;
		if (whiteToMove == this.whiteToMove) { 
			remaining -= elapsed;
			if (timerT0 != 0) {
				remaining -= now - timerT0;
			}
		}
		return (int)remaining;
	}
	//setting initial time
	//시작시간 설정
	public final int getInitialTime() {
		return (int)timeControl;
	}
	//return increment time
	//증가된 시간 리턴 
	public final int getIncrement() {
		return (int)increment;
	}
	//comptutate moving 
	//이동을 계산
	public final int getMovesToTC() {
		if (movesPerSession <= 0)
			return 0;
		int nextTC = 1;
		while (nextTC <= currentMove)
			nextTC += movesPerSession;
		return nextTC - currentMove;
	}
}
