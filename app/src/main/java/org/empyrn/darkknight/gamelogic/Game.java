package org.empyrn.darkknight.gamelogic;

import java.util.ArrayList;
import java.util.List;

import org.empyrn.darkknight.PGNOptions;
import org.empyrn.darkknight.engine.ComputerPlayer;
import org.empyrn.darkknight.gamelogic.GameTree.Node;


/**
 *
 * @author petero
 */
/**
 *
 * @저자 petero
 */
public class Game {
    boolean pendingDrawOffer;
    GameTree tree;
    private ComputerPlayer computerPlayer;
    TimeControl timeController;
    private boolean gamePaused;

    PgnToken.PgnTokenReceiver gameTextListener;

    public Game(ComputerPlayer computerPlayer, PgnToken.PgnTokenReceiver gameTextListener,
    			int timeControl, int movesPerSession, int timeIncrement) {
        this.computerPlayer = computerPlayer;
        this.gameTextListener = gameTextListener;
        tree = new GameTree(gameTextListener);
        timeController = new TimeControl();
        timeController.setTimeControl(timeControl, movesPerSession, timeIncrement);
        gamePaused = false;
        newGame();
    }

	final void fromByteArray(byte[] data) {
		tree.fromByteArray(data);
		updateTimeControl(true);
	}

	public final void setComputerPlayer(ComputerPlayer computerPlayer) {
    	this.computerPlayer = computerPlayer;
	}

	public final void setGamePaused(boolean gamePaused) {
		if (gamePaused != this.gamePaused) {
			this.gamePaused = gamePaused;
	        updateTimeControl(false);
		}
	}

	final void setPos(Position pos) {
		tree.setStartPos(new Position(pos));
        updateTimeControl(false);
	}

	final boolean readPGN(String pgn, PGNOptions options) throws ChessParseError {
		boolean ret = tree.readPGN(pgn, options);
		if (ret)
			updateTimeControl(false);
		return ret;
	}
	
	final Position currPos() {
		return tree.currentPos;
	}

	/**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
       /**
      * 플레이어의 이동 / 명령 문자열에 따라 게임 상태를 업데이트합니다.
      * @param str 처리 할 이동 또는 명령입니다.
      * @return str가 인식되면 true를 반환하고 그렇지 않으면 false를 반환합니다.
      */
    public final boolean processString(String str) {
        if (getGameState() != GameState.ALIVE)
            return false;
		if (str.startsWith("draw ")) {
			String drawCmd = str.substring(str.indexOf(" ") + 1);
			handleDrawCmd(drawCmd);
	    	return true;
		} else if (str.equals("resign")) {
			addToGameTree(new Move(0, 0, 0), "resign");
			return true;
		}

        Move m = TextIO.UCIstringToMove(str);
        if (m != null) {
            ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos());
            moves = MoveGen.removeIllegal(currPos(), moves);
            boolean legal = false;
            for (int i = 0; i < moves.size(); i++) {
            	if (m.equals(moves.get(i))) {
            		legal = true;
            		break;
            	}
            }
            if (!legal)
            	m = null;
        }
        if (m == null) {
            m = TextIO.stringToMove(currPos(), str);
        }
        if (m == null) {
            return false;
        }

        addToGameTree(m, pendingDrawOffer ? "draw offer" : "");
        return true;
    }
    
    private final void addToGameTree(Move m, String playerAction) {
    	if (m.equals(new Move(0, 0, 0))) { 
		// Don't create more than one null move at a node
		// 노드에서 둘 이상의 null 이동을 만들지 않습니다.
    		List<Move> varMoves = tree.variations();
    		for (int i = varMoves.size() - 1; i >= 0; i--) {
            	if (varMoves.get(i).equals(m)) {
            		tree.deleteVariation(i);
            	}
    		}
    	}

        List<Move> varMoves = tree.variations();
        boolean movePresent = false;
        int varNo;
        for (varNo = 0; varNo < varMoves.size(); varNo++) {
        	if (varMoves.get(varNo).equals(m)) {
        		movePresent = true;
        		break;
        	}
        }
        if (!movePresent) {
        	String moveStr = TextIO.moveToUCIString(m);
        	varNo = tree.addMove(moveStr, playerAction, 0, "", "");
        }
        tree.reorderVariation(varNo, 0);
        tree.goForward(0);
        int remaining = timeController.moveMade(System.currentTimeMillis());
        tree.setRemainingTime(remaining);
        updateTimeControl(true);
    	pendingDrawOffer = false;
    }

	private final void updateTimeControl(boolean discardElapsed) {
		int move = currPos().fullMoveCounter;
		boolean wtm = currPos().whiteMove;
		if (discardElapsed || (move != timeController.currentMove) || (wtm != timeController.whiteToMove)) {
			int initialTime = timeController.getInitialTime();
			int whiteBaseTime = tree.getRemainingTime(true, initialTime);
			int blackBaseTime = tree.getRemainingTime(false, initialTime);
			timeController.setCurrentMove(move, wtm, whiteBaseTime, blackBaseTime);
		}
		long now = System.currentTimeMillis();
		if (gamePaused || (getGameState() != GameState.ALIVE)) {
			timeController.stopTimer(now);
		} else {
			timeController.startTimer(now);
		}
	}

    public final String getGameStateString() {
        switch (getGameState()) {
            case ALIVE:
                return "";
            case WHITE_MATE:
                return "Game over, white mates!";
            case BLACK_MATE:
                return "Game over, black mates!";
            case WHITE_STALEMATE:
            case BLACK_STALEMATE:
                return "Game over, draw by stalemate!";
            case DRAW_REP:
            {
            	String ret = "Game over, draw by repetition!";
            	String drawInfo = tree.getGameStateInfo();
            	if (drawInfo.length() > 0) {
            		ret = ret + " [" + drawInfo+ "]";
            	}
            	return ret;
            }
            case DRAW_50:
            {
                String ret = "Game over, draw by 50 move rule!";
            	String drawInfo = tree.getGameStateInfo();
            	if (drawInfo.length() > 0) {
            		ret = ret + " [" + drawInfo + "]";  
            	}
            	return ret;
            }
            case DRAW_NO_MATE:
                return "Game over, draw by impossibility of mate!";
            case DRAW_AGREE:
                return "Game over, draw by agreement!";
            case RESIGN_WHITE:
                return "Game over, white resigns!";
            case RESIGN_BLACK:
                return "Game over, black resigns!";
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Get the last played move, or null if no moves played yet.
     */
    /**
      * 마지막으로 한 동작을 가져 오거나 아직 동작하지 않은 경우 null입니다.
     */
    public final Move getLastMove() {
    	return tree.currentNode.move;
    }
    
    /** Return true if there is a move to redo. */
    /** 재실행으로 넘어갈 경우 true를 반환합니다. */
    public final boolean canRedoMove() {
    	int nVar = tree.variations().size();
    	return nVar > 0;
    }
    
    public final int numVariations() {
    	if (tree.currentNode == tree.rootNode)
    		return 1;
    	tree.goBack();
    	int nChildren = tree.variations().size();
    	tree.goForward(-1);
    	return nChildren;
    }

    public final void changeVariation(int delta) {
    	if (tree.currentNode == tree.rootNode)
    		return;
    	tree.goBack();
    	int defChild = tree.currentNode.defaultChild;
    	int nChildren = tree.variations().size();
    	int newChild = defChild + delta;
    	newChild = Math.max(newChild, 0);
    	newChild = Math.min(newChild, nChildren - 1);
    	tree.goForward(newChild);
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    public final void removeVariation() {
    	if (numVariations() <= 1)
    		return;
    	tree.goBack();
    	int defChild = tree.currentNode.defaultChild;
    	tree.deleteVariation(defChild);
    	tree.goForward(-1);
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    public static enum GameState {
        ALIVE,
        WHITE_MATE,         // White mates                          - 흰색이 이길 경우
        BLACK_MATE,         // Black mates                          - 검은색이 이길 경우
        WHITE_STALEMATE,    // White is stalemated                  - 흰색이 교착상태인 경우
        BLACK_STALEMATE,    // Black is stalemated                  - 검은색이 교착상태인 경우
        DRAW_REP,           // Draw by 3-fold repetition            - 3-fold 반복으로 비긴 경우
        DRAW_50,            // Draw by 50 move rule                 - 50 움직임 규칙으로 비긴 경우
        DRAW_NO_MATE,       // Draw by impossibility of check mate  - 체크메이트 불가능으로 비긴 경우
        DRAW_AGREE,         // Draw by agreement                    - 합의로 비긴 경우
        RESIGN_WHITE,       // White resigns                        - 흰색이 포기한 경우
        RESIGN_BLACK        // Black resigns                        - 검은색이 포기한 경우
    }

    /**
     * Get the current state (draw, mate, ongoing, etc) of the game.
     * 게임의 현재 상태/노드 상태 반환
     */
    public final GameState getGameState() {
    	return tree.getGameState();
    }

    /**
     * Check if a draw offer is available.
     * @return True if the current player has the option to accept a draw offer.
     */
      /**
      * 비기는게 가능한지 확인하십시오.
      * @return 현재 플레이어가 비기는 제안을 수락 할 수있는 옵션이 있어야한다.
      */
    public final boolean haveDrawOffer() {
    	return tree.currentNode.playerAction.equals("draw offer");
    }

    public final void undoMove() {
    	Move m = tree.currentNode.move;
    	if (m != null) {
    		tree.goBack();
    		pendingDrawOffer = false;
    		updateTimeControl(true);
    	}
    }

    public final void redoMove() {
    	if (canRedoMove()) {
    		tree.goForward(-1);
            pendingDrawOffer = false;
            updateTimeControl(true);
        }
    }

    public final void newGame() {
    	tree = new GameTree(gameTextListener);
        if (computerPlayer != null)
        	computerPlayer.clearTT();
        timeController.reset();
        pendingDrawOffer = false;
        updateTimeControl(true);
    }


    /**
     * Return the last zeroing position and a list of moves
     * to go from that position to the current position.
     */
    /**
      * 마지막 제로 위치 및 이동 목록을 반환합니다.
      * 그 위치에서 현재 위치로 이동하십시오.
     */
    public final Pair<Position, ArrayList<Move>> getUCIHistory() {
    	Pair<List<Node>, Integer> ml = tree.getMoveList();
        List<Node> moveList = ml.first;
        Position pos = new Position(tree.startPos);
        ArrayList<Move> mList = new ArrayList<Move>();
        Position currPos = new Position(pos);
        UndoInfo ui = new UndoInfo();
        int nMoves = ml.second;
        for (int i = 0; i < nMoves; i++) {
        	Node n = moveList.get(i);
        	mList.add(n.move);
        	currPos.makeMove(n.move, ui);
        	if (currPos.halfMoveClock == 0) {
        		pos = new Position(currPos);
        		mList.clear();
        	}
        }
        return new Pair<Position, ArrayList<Move>>(pos, mList);
    }

    private final void handleDrawCmd(String drawCmd) {
    	Position pos = tree.currentPos;
        if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
            boolean rep = drawCmd.startsWith("rep");
            Move m = null;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (ms.length() > 0) {
                m = TextIO.stringToMove(pos, ms);
            }
            boolean valid;
            if (rep) {
                valid = false;
                UndoInfo ui = new UndoInfo();
                int repetitions = 0;
                Position posToCompare = new Position(tree.currentPos);
                if (m != null) {
                    posToCompare.makeMove(m, ui);
                    repetitions = 1;
                }
                Pair<List<Node>, Integer> ml = tree.getMoveList();
                List<Node> moveList = ml.first;
                Position tmpPos = new Position(tree.startPos);
                if (tmpPos.drawRuleEquals(posToCompare))
                	repetitions++;
                int nMoves = ml.second;
                for (int i = 0; i < nMoves; i++) {
                	Node n = moveList.get(i);
                	tmpPos.makeMove(n.move, ui);
                	TextIO.fixupEPSquare(tmpPos);
                    if (tmpPos.drawRuleEquals(posToCompare))
                    	repetitions++;
                }
                if (repetitions >= 3)
                    valid = true;
            } else {
                Position tmpPos = new Position(pos);
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    tmpPos.makeMove(m, ui);
                }
                valid = tmpPos.halfMoveClock >= 100;
            }
            if (valid) {
            	String playerAction = rep ? "draw rep" : "draw 50";
                if (m != null)
                	playerAction += " " + TextIO.moveToString(pos, m, false);
            	addToGameTree(new Move(0, 0, 0), playerAction);
            } else {
                pendingDrawOffer = true;
                if (m != null) {
                    processString(ms);
                }
            }
        } else if (drawCmd.startsWith("offer ")) {
            pendingDrawOffer = true;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (TextIO.stringToMove(pos, ms) != null) {
                processString(ms);
            }
        } else if (drawCmd.equals("accept")) {
            if (haveDrawOffer())
            	addToGameTree(new Move(0, 0, 0), "draw accept");
        }
    }
}
