package org.empyrn.darkknight.gamelogic;

/** A token in a PGN data stream. Used by the PGN parser. */
// PGN 데이터 스트림의 토큰입니다. PGN 파서가 사용합니다. 
public class PgnToken {
	// These are tokens according to the PGN spec
	// 이들은 PGN 스펙에 따른 토큰입니다.
	public static final int STRING = 0;
	public static final int INTEGER = 1;
	public static final int PERIOD = 2;
	public static final int ASTERISK = 3;
	public static final int LEFT_BRACKET = 4;
	public static final int RIGHT_BRACKET = 5;
	public static final int LEFT_PAREN = 6;
	public static final int RIGHT_PAREN = 7;
	public static final int NAG = 8;
	public static final int SYMBOL = 9;

	// These are not tokens according to the PGN spec, but the parser
	// extracts these anyway for convenience.
	// 이들은 PGN 스펙에 따라 토큰이 아니지만 파서
	// 편의를 위해 이들을 추출합니다.
	public static final int COMMENT = 10;
	public static final int EOF = 11;

	// Actual token data
	// 실제 토큰 데이터
	int type;
	String token;

	PgnToken(int type, String token) {
		this.type = type;
		this.token = token;
	}
	
	public interface PgnTokenReceiver {
		/** If this method returns false, the object needs a full reinitialization, using clear() and processToken(). */
		//이 메소드가 false를 반환하면 객체는 clear () 및 processToken ()을 사용하여 전체 다시 초기화해야합니다. 
		public boolean isUpToDate();

		/** Clear object state. */
		//객체 상태 초기화 
		public void clear();

		/** Update object state with one token from a PGN game. */
		//pgn game으로 부터 표시와 함께 객체 상태 업데이트
		public void processToken(GameTree.Node node, int type, String token);

		/** 현재 이동상태를 지운다*/
		public void setCurrent(GameTree.Node node);
	};
}
