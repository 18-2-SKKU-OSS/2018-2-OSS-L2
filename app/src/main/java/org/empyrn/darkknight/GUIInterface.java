package org.empyrn.darkknight;

import java.util.List;

import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.Position;



/** Interface between the gui and the ChessController. */
/** gui와 ChessController 사이의 인터페이스 */
public interface GUIInterface {

	/** Update the displayed board position. */
	/** 보여지는 체스판 위치를 업데이트한다. */
	public void setPosition(Position pos, String variantInfo, List<Move> variantMoves);

	/** Mark square i as selected. Set to -1 to clear selection. */
    /** 선택한 대로 square i를 표시한다. 선택을 취소하려면 -1로 설정한다. */
	public void setSelection(int sq);

	/** Set the status text. */
    /** 상태 텍스트 설정 */
	public void setStatusString(String str);

	/** Update the list of moves. */
    /** moves 리스트를 업데이트한다. */
	public void moveListUpdated();

	/** Update the computer thinking information. */
    /** 컴퓨터(AI)의 생각 정보를 업데이트한다. */
	public void setThinkingInfo(String pvStr, String bookInfo, List<Move> pvMoves, List<Move> bookMoves);
	
	/** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
    /** 폰을 무슨 말로 바꿀지 물어본다. 끝나면 reportPromotePiece() 함수를 실행한다. */
	public void requestPromotePiece();

	/** Run code on the GUI thread. */
    /** GUI 쓰레드에서 코드를 실행한다. */
	public void runOnUIThread(Runnable runnable);

	/** Report that user attempted to make an invalid move. */
    /** 사용자가 유효하지 않은 move를 시도하였다고 알린다. */
	public void reportInvalidMove(Move m);

	/** Called when computer made a move. GUI can notify user, for example by playing a sound. */
    /** 컴퓨터(AI)가 move를 했을 때 부르는 함수. GUI는 사용자에게 알릴 수 있습니다. (예를 들어 소리를 재생하여서) */
	public void computerMoveMade();
	public void humanMoveMade(Move m);

	/** Report remaining thinking time to GUI. */
    /** 남아있는 thinking 시간을 GUI에게 알린다. */
	public void setRemainingTime(long wTime, long bTime, long nextUpdate);
}
