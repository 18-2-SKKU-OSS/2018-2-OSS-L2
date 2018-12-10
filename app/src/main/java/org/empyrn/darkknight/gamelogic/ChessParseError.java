package org.empyrn.darkknight.gamelogic;

/**
 * Exception class to represent parse errors in FEN or algebraic notation.
 * @author petero
 */
/**
  * FEN 또는 대수 표기법의 구문 분석 오류를 나타내는 예외 클래스입니다.
  * @ petero
*/
public class ChessParseError extends Exception {
	private static final long serialVersionUID = -6051856171275301175L;
	
	public Position pos;
	
	public ChessParseError() {
    }
    public ChessParseError(String msg) {
        super(msg);
        pos = null;
    }
    public ChessParseError(String msg, Position pos) {
    	super(msg);
    	this.pos = pos;
    }
}
