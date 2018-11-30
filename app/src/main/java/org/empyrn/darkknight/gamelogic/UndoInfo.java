package org.empyrn.darkknight.gamelogic;

/**
 * Contains enough information to undo a previous move.
 * Set by makeMove(). Used by unMakeMove().
 * @author petero
 */

/*
이전의 상태로 되돌려 주는 함수
이전의 이동상태 정보를 가지고 있음
unMakeMove()함수를 사용했을 때 이전의 상태로 되돌아감
 */

public class UndoInfo {
    int capturedPiece;
    int castleMask;
    int epSquare;
    int halfMoveClock;
}
