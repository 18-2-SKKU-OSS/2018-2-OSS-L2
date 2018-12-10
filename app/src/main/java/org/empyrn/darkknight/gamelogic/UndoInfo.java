package org.empyrn.darkknight.gamelogic;

/**
 * English
 * Contains enough information to undo a previous move.
 * Set by makeMove(). Used by unMakeMove().
 * @author petero
 * korean
 * 이전 이동을 취소 할 수있는 충분한 정보가 들어 있습니다.
 * makeMove ()에 의해 설정됩니다. unMakeMove ()에 의해 사용됩니다.
 * 저자 petero
 */

// class to store piece movement info
//피스의 이동정보를 저장할 클래스
public class UndoInfo {
    int capturedPiece;
    int castleMask;
    int epSquare;
    int halfMoveClock;
}
