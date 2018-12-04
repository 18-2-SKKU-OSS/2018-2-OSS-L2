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
	public void setThinkingInfo(String pvStr, String bookInfo, List<Move> pvMoves, List<Move> bookMoves);
	
	/** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
	public void requestPromotePiece();

	/** Run code on the GUI thread. */
	public void runOnUIThread(Runnable runnable);

	/** Report that user attempted to make an invalid move. */
	public void reportInvalidMove(Move m);

	/** Called when computer made a move. GUI can notify user, for example by playing a sound. */
	public void computerMoveMade();
	public void humanMoveMade(Move m);

	/** Report remaining thinking time to GUI. */
	public void setRemainingTime(long wTime, long bTime, long nextUpdate);
}
