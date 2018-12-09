package org.empyrn.darkknight.gamelogic;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author petero
 */
public class TextIO {
    static public final String startPosFEN = new String("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	
    /** Parse a FEN string and return a chess Position object. */
   //FEN 문자열을 구문 분석하고 체스 Position 객체를 반환합니다. 
    
	   public static final Position readFEN(String fen) throws ChessParseError {
        Position pos = new Position();
        String[] words = fen.split(" ");
        if (words.length < 2) {
            throw new ChessParseError("Too few spaces");
        }
        for (int i = 0; i < words.length; i++) {
        	words[i] = words[i].trim();
        }
        
        //Chess Piece placement
        //체스조각 위치 
        int row = 7;
        int col = 0;
        for (int i = 0; i < words[0].length(); i++) {
            char c = words[0].charAt(i);
            switch (c) {
                case '1': col += 1; break;
                case '2': col += 2; break;
                case '3': col += 3; break;
                case '4': col += 4; break;
                case '5': col += 5; break;
                case '6': col += 6; break;
                case '7': col += 7; break;
                case '8': col += 8; break;
                case '/': row--; col = 0; break;
                case 'P': safeSetPiece(pos, col, row, Piece.WPAWN);   col++; break;
                case 'N': safeSetPiece(pos, col, row, Piece.WKNIGHT); col++; break;
		case 'B': safeSetPiece(pos, col, row, Piece.WBISHOP); col++; break;
		case 'R': safeSetPiece(pos, col, row, Piece.WROOK);   col++; break;
		case 'Q': safeSetPiece(pos, col, row, Piece.WQUEEN);  col++; break;
		case 'K': safeSetPiece(pos, col, row, Piece.WKING);   col++; break;
		case 'p': safeSetPiece(pos, col, row, Piece.BPAWN);   col++; break;
		case 'n': safeSetPiece(pos, col, row, Piece.BKNIGHT); col++; break;
		case 'b': safeSetPiece(pos, col, row, Piece.BBISHOP); col++; break;
		case 'r': safeSetPiece(pos, col, row, Piece.BROOK);   col++; break;
		case 'q': safeSetPiece(pos, col, row, Piece.BQUEEN);  col++; break;
		case 'k': safeSetPiece(pos, col, row, Piece.BKING);   col++; break;
                default: throw new ChessParseError("Invalid piece", pos);
            }
        }
        if (words[1].length() == 0) {
            throw new ChessParseError("Invalid side", pos);
        }
        pos.setWhiteMove(words[1].charAt(0) == 'w');

        // Castling rights
        //오른쪽 캐스팅
        int castleMask = 0;
        if (words.length > 2) {
            for (int i = 0; i < words[2].length(); i++) {
                char c = words[2].charAt(i);
                switch (c) {
                    case 'K':
                        castleMask |= (1 << Position.H1_CASTLE);
                        break;
                    case 'Q':
                        castleMask |= (1 << Position.A1_CASTLE);
                        break;
                    case 'k':
                        castleMask |= (1 << Position.H8_CASTLE);
                        break;
                    case 'q':
                        castleMask |= (1 << Position.A8_CASTLE);
                        break;
                    case '-':
                        break;
                    default:
                        throw new ChessParseError("Invalid castling flags", pos);
                }
            }
        }
        pos.setCastleMask(castleMask);
        removeBogusCastleFlags(pos);

        if (words.length > 3) {
            // En passant target square
            String epString = words[3];
            if (!epString.equals("-")) {
                if (epString.length() < 2) {
                    throw new ChessParseError("Invalid en passant square", pos);
                }
                pos.setEpSquare(getSquare(epString));
            }
        }

        try {
            if (words.length > 4) {
                pos.halfMoveClock = Integer.parseInt(words[4]);
            }
            if (words.length > 5) {
                pos.fullMoveCounter = Integer.parseInt(words[5]);
            }
        } catch (NumberFormatException nfe) {
            // Ignore errors here, since the fields are optional
	        // 해당 필드가 선택적이기 때문에 이곳의 에러는 무시한다.
        }

        // Each side must have exactly one king
        // 각각의 플레이어 쪽은 킹을 가지고 있어야 한다. 
        int wKings = 0;
        int bKings = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = pos.getPiece(Position.getSquare(x, y));
                if (p == Piece.WKING) {
                    wKings++;
                } else if (p == Piece.BKING) {
                    bKings++;
                }
            }
        }
        if (wKings != 1) {
            throw new ChessParseError("White must have exactly one king", pos);
        }
        if (bKings != 1) {
            throw new ChessParseError("Black must have exactly one king", pos);
        }

        // Make sure king can not be captured
        // 킹을 붙잡을수 있는지에 대한 여부를 확인한다.
        Position pos2 = new Position(pos);
        pos2.setWhiteMove(!pos.whiteMove);
        if (MoveGen.inCheck(pos2)) {
            throw new ChessParseError("King capture possible", pos);
        }

        fixupEPSquare(pos);

        return pos;
    }

    public static final void removeBogusCastleFlags(Position pos) {
    	int castleMask = pos.getCastleMask();
    	int validCastle = 0;
    	if (pos.getPiece(4) == Piece.WKING) {
    		if (pos.getPiece(0) == Piece.WROOK) validCastle |= (1 << Position.A1_CASTLE);
    		if (pos.getPiece(7) == Piece.WROOK) validCastle |= (1 << Position.H1_CASTLE);
    	}
    	if (pos.getPiece(60) == Piece.BKING) {
    		if (pos.getPiece(56) == Piece.BROOK) validCastle |= (1 << Position.A8_CASTLE);
    		if (pos.getPiece(63) == Piece.BROOK) validCastle |= (1 << Position.H8_CASTLE);
    	}
    	castleMask &= validCastle;
    	pos.setCastleMask(castleMask);
    }

    /** Remove pseudo-legal EP square if it is not legal, ie would leave king in check. */
    public static final void fixupEPSquare(Position pos) {
        int epSquare = pos.getEpSquare();
        if (epSquare >= 0) {
            ArrayList<Move> moves = MoveGen.instance.pseudoLegalMoves(pos);
            moves = MoveGen.removeIllegal(pos, moves);
            boolean epValid = false;
            for (Move m : moves) {
                if (m.to == epSquare) {
                    if (pos.getPiece(m.from) == (pos.whiteMove ? Piece.WPAWN : Piece.BPAWN)) {
                        epValid = true;
                        break;
                    }
                }
            }
            if (!epValid) {
                pos.setEpSquare(-1);
            }
        }
    }

    private static final void safeSetPiece(Position pos, int col, int row, int p) throws ChessParseError {
        if (row < 0) throw new ChessParseError("Too many rows");
        if (col > 7) throw new ChessParseError("Too many columns");
        if ((p == Piece.WPAWN) || (p == Piece.BPAWN)) {
            if ((row == 0) || (row == 7))
                throw new ChessParseError("Pawn on first/last rank");
        }
        pos.setPiece(Position.getSquare(col, row), p);
    }
    
    /** Return a FEN string corresponding to a chess Position object. */
   //체스 위치 객체에 해당하는 FEN 문자열을 반환합니다. 
    public static final String toFEN(Position pos) {
        StringBuilder ret = new StringBuilder();
        // Piece placement
        for (int r = 7; r >=0; r--) {
            int numEmpty = 0;
            for (int c = 0; c < 8; c++) {
                int p = pos.getPiece(Position.getSquare(c, r));
                if (p == Piece.EMPTY) {
                    numEmpty++;
                } else {
                    if (numEmpty > 0) {
                        ret.append(numEmpty);
                        numEmpty = 0;
                    }
                    switch (p) {
                        case Piece.WKING:   ret.append('K'); break;
                        case Piece.WQUEEN:  ret.append('Q'); break;
                        case Piece.WROOK:   ret.append('R'); break;
                        case Piece.WBISHOP: ret.append('B'); break;
                        case Piece.WKNIGHT: ret.append('N'); break;
                        case Piece.WPAWN:   ret.append('P'); break;
                        case Piece.BKING:   ret.append('k'); break;
                        case Piece.BQUEEN:  ret.append('q'); break;
                        case Piece.BROOK:   ret.append('r'); break;
                        case Piece.BBISHOP: ret.append('b'); break;
                        case Piece.BKNIGHT: ret.append('n'); break;
                        case Piece.BPAWN:   ret.append('p'); break;
                        default: throw new RuntimeException();
                    }
                }
            }
            if (numEmpty > 0) {
                ret.append(numEmpty);
            }
            if (r > 0) {
                ret.append('/');
            }
        }
        ret.append(pos.whiteMove ? " w " : " b ");

        // Castling rights
        boolean anyCastle = false;
        if (pos.h1Castle()) {
            ret.append('K');
            anyCastle = true;
        }
        if (pos.a1Castle()) {
            ret.append('Q');
            anyCastle = true;
        }
        if (pos.h8Castle()) {
            ret.append('k');
            anyCastle = true;
        }
        if (pos.a8Castle()) {
            ret.append('q');
            anyCastle = true;
        }
        if (!anyCastle) {
            ret.append('-');
        }
        
        // En passant target square
        {
            ret.append(' ');
            if (pos.getEpSquare() >= 0) {
                int x = Position.getX(pos.getEpSquare());
                int y = Position.getY(pos.getEpSquare());
                ret.append((char)(x + 'a'));
                ret.append((char)(y + '1'));
            } else {
                ret.append('-');
            }
        }

        // Move counters
        ret.append(' ');
        ret.append(pos.halfMoveClock);
        ret.append(' ');
        ret.append(pos.fullMoveCounter);

        return ret.toString();
    }
    
    /**
     * Convert a chess move to human readable form.
     * @param pos      The chess position.
     * @param move     The executed move.
     * @param longForm If true, use long notation, eg Ng1-f3.
     *                 Otherwise, use short notation, eg Nf3
     */
   //사람이 읽을수 있도록 움직임을 변환한다. 
    public static final String moveToString(Position pos, Move move, boolean longForm) {
        ArrayList<Move> moves = MoveGen.instance.pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        return moveToString(pos, move, longForm, moves);
    }
    private static final String moveToString(Position pos, Move move, boolean longForm, 
    										 List<Move> moves) {
    	if (move.equals(new Move(0, 0, 0)))
    		return "--";
        StringBuilder ret = new StringBuilder();
        int wKingOrigPos = Position.getSquare(4, 0);
        int bKingOrigPos = Position.getSquare(4, 7);
        if (move.from == wKingOrigPos && pos.getPiece(wKingOrigPos) == Piece.WKING) {
            // Check white castle
            if (move.to == Position.getSquare(6, 0)) {
                    ret.append("O-O");
            } else if (move.to == Position.getSquare(2, 0)) {
                ret.append("O-O-O");
            }
        } else if (move.from == bKingOrigPos && pos.getPiece(bKingOrigPos) == Piece.BKING) {
            // Check white castle
            if (move.to == Position.getSquare(6, 7)) {
                ret.append("O-O");
            } else if (move.to == Position.getSquare(2, 7)) {
                ret.append("O-O-O");
            }
        }
        if (ret.length() == 0) {
            int p = pos.getPiece(move.from);
            ret.append(pieceToChar(p));
            int x1 = Position.getX(move.from);
            int y1 = Position.getY(move.from);
            int x2 = Position.getX(move.to);
            int y2 = Position.getY(move.to);
            if (longForm) {
                ret.append((char)(x1 + 'a'));
                ret.append((char) (y1 + '1'));
                ret.append(isCapture(pos, move) ? 'x' : '-');
            } else {
                if (p == (pos.whiteMove ? Piece.WPAWN : Piece.BPAWN)) {
                    if (isCapture(pos, move)) {
                        ret.append((char) (x1 + 'a'));
                    }
                } else {
                    int numSameTarget = 0;
                    int numSameFile = 0;
                    int numSameRow = 0;
                    int mSize = moves.size();
                    for (int mi = 0; mi < mSize; mi++) {
                    	Move m = moves.get(mi);
                        if ((pos.getPiece(m.from) == p) && (m.to == move.to)) {
                            numSameTarget++;
                            if (Position.getX(m.from) == x1)
                                numSameFile++;
                            if (Position.getY(m.from) == y1)
                                numSameRow++;
                        }
                    }
                    if (numSameTarget < 2) {
                        // No file/row info needed
                    } else if (numSameFile < 2) {
                        ret.append((char) (x1 + 'a'));   
			 // Only file info needed
			    //파일 정보만 필요 
                    } else if (numSameRow < 2) {
                        ret.append((char) (y1 + '1'));   
			    //Only row info needed
			    //오직 열정보만 필요 
                    } else {
                        ret.append((char) (x1 + 'a'));   
			    // File and row info needed
			    //파일과 열정보 필요 
                        ret.append((char) (y1 + '1'));
                    }
                }
                if (isCapture(pos, move)) {
                    ret.append('x');
                }
            }
            ret.append((char) (x2 + 'a'));
            ret.append((char) (y2 + '1'));
            if (move.promoteTo != Piece.EMPTY)
                ret.append(pieceToChar(move.promoteTo));
        }
        UndoInfo ui = new UndoInfo();
        pos.makeMove(move, ui);
        boolean givesCheck = MoveGen.inCheck(pos);
        if (givesCheck) {
            ArrayList<Move> nextMoves = MoveGen.instance.pseudoLegalMoves(pos);
            nextMoves = MoveGen.removeIllegal(pos, nextMoves);
            if (nextMoves.size() == 0) {
                ret.append('#');
            } else {
                ret.append('+');
            }
        }
        pos.unMakeMove(move, ui);

        return ret.toString();
    }

    private static final boolean isCapture(Position pos, Move move) {
        if (pos.getPiece(move.to) == Piece.EMPTY) {
            int p = pos.getPiece(move.from);
            if ((p == (pos.whiteMove ? Piece.WPAWN : Piece.BPAWN)) && (move.to == pos.getEpSquare())) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

	private final static class MoveInfo {
		int piece;					// -1 for unspecified
		int fromX, fromY, toX, toY; // -1 for unspecified
		int promPiece;				// -1 for unspecified
		MoveInfo() { piece = fromX = fromY = toX = toY = promPiece = -1; }
	}
    
    /**
     * Convert a chess move string to a Move object.
     * The string may specify any combination of piece/source/target/promotion
     * information as long as it matches exactly one valid move.
     */
	//체스의 이동을 문자열로 표시한다. 가능한한 타당한 이동을 매치시킨다.
    public static final Move stringToMove(Position pos, String strMove) {
    	if (strMove.equals("--"))
    		return new Move(0, 0, 0);

    	strMove = strMove.replaceAll("=", "");
    	strMove = strMove.replaceAll("\\+", "");
    	strMove = strMove.replaceAll("#", "");
        boolean wtm = pos.whiteMove;

    	MoveInfo info = new MoveInfo();
    	boolean capture = false;
    	if (strMove.equals("O-O") || strMove.equals("0-0") || strMove.equals("o-o")) {
    		info.piece = wtm ? Piece.WKING : Piece.BKING;
    		info.fromX = 4;
    		info.toX = 6;
    		info.fromY = info.toY = wtm ? 0 : 7;
    		info.promPiece= Piece.EMPTY;
    	} else if (strMove.equals("O-O-O") || strMove.equals("0-0-0") || strMove.equals("o-o-o")) {
    		info.piece = wtm ? Piece.WKING : Piece.BKING;
    		info.fromX = 4;
    		info.toX = 2;
    		info.fromY = info.toY = wtm ? 0 : 7;
    		info.promPiece= Piece.EMPTY;
    	} else {
    		boolean atToSq = false;
    		for (int i = 0; i < strMove.length(); i++) {
    			char c = strMove.charAt(i);
    			if (i == 0) {
    				int piece = charToPiece(wtm, c);
    				if (piece >= 0) {
    					info.piece = piece;
    					continue;
    				}
    			}
    			int tmpX = c - 'a';
    			if ((tmpX >= 0) && (tmpX < 8)) {
    				if (atToSq || (info.fromX >= 0))
    					info.toX = tmpX;
    				else
    					info.fromX = tmpX;
    			}
    			int tmpY = c - '1';
    			if ((tmpY >= 0) && (tmpY < 8)) {
    				if (atToSq || (info.fromY >= 0))
    					info.toY = tmpY;
    				else
    					info.fromY = tmpY;
    			}
    			if ((c == 'x') || (c == '-')) {
    				atToSq = true;
    				if (c == 'x')
    					capture = true;
    			}
    			if (i == strMove.length() - 1) {
    				int promPiece = charToPiece(wtm, c);
    				if (promPiece >= 0) {
    					info.promPiece = promPiece;
    				}
    			}
    		}
    		if ((info.fromX >= 0) && (info.toX < 0)) {
    			info.toX = info.fromX;
    			info.fromX = -1;
    		}
    		if ((info.fromY >= 0) && (info.toY < 0)) {
    			info.toY = info.fromY;
    			info.fromY = -1;
    		}
    		if (info.piece < 0) {
    			boolean haveAll = (info.fromX >= 0) && (info.fromY >= 0) &&
    							  (info.toX >= 0) && (info.toY >= 0);
    			if (!haveAll)
    				info.piece = wtm ? Piece.WPAWN : Piece.BPAWN;
    		}
    		if (info.promPiece < 0)
    			info.promPiece = Piece.EMPTY;
    	}

        ArrayList<Move> moves = MoveGen.instance.pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);

        ArrayList<Move> matches = new ArrayList<Move>(2);
        for (int i = 0; i < moves.size(); i++) {
        	Move m = moves.get(i);
        	int p = pos.getPiece(m.from);
        	boolean match = true;
        	if ((info.piece >= 0) && (info.piece != p))
        		match = false;
        	if ((info.fromX >= 0) && (info.fromX != Position.getX(m.from)))
        		match = false;
        	if ((info.fromY >= 0) && (info.fromY != Position.getY(m.from)))
        		match = false;
        	if ((info.toX >= 0) && (info.toX != Position.getX(m.to)))
        		match = false;
        	if ((info.toY >= 0) && (info.toY != Position.getY(m.to)))
        		match = false;
        	if ((info.promPiece >= 0) && (info.promPiece != m.promoteTo))
        		match = false;
        	if (match) {
        		matches.add(m);
        	}
        }
        int nMatches = matches.size();
        if (nMatches == 0)
        	return null;
        else if (nMatches == 1)
        	return matches.get(0);
        if (!capture)
        	return null;
        Move move = null;
        for (int i = 0; i < matches.size(); i++) {
        	Move m = matches.get(i);
        	int capt = pos.getPiece(m.to);
        	if (capt != Piece.EMPTY) {
        		if (move == null)
        			move = m;
        		else
        			return null;
        	}
        }
        return move;
    }

    /** Convert a move object to UCI string format. */
	//물체의 이동을 UCI 형식으로 바꾼다.
    public static final String moveToUCIString(Move m) {
        String ret = squareToString(m.from);
        ret += squareToString(m.to);
        switch (m.promoteTo) {
            case Piece.WQUEEN:
            case Piece.BQUEEN:
                ret += "q";
                break;
            case Piece.WROOK:
            case Piece.BROOK:
                ret += "r";
                break;
            case Piece.WBISHOP:
            case Piece.BBISHOP:
                ret += "b";
                break;
            case Piece.WKNIGHT:
            case Piece.BKNIGHT:
                ret += "n";
                break;
            default:
                break;
        }
        return ret;
    }

    /**
     * Convert a string in UCI move format to a Move object.
     * @return A move object, or null if move has invalid syntax
     */
	//이동을 UCI 포맷으로 바꾸고 타당하지 않는 이동은 null 표시 
    public static final Move UCIstringToMove(String move) {
        Move m = null;
        if ((move.length() < 4) || (move.length() > 5))
            return m;
        int fromSq = TextIO.getSquare(move.substring(0, 2));
        int toSq   = TextIO.getSquare(move.substring(2, 4));
        if ((fromSq < 0) || (toSq < 0)) {
            return m;
        }
        char prom = ' ';
        boolean white = true;
        if (move.length() == 5) {
            prom = move.charAt(4);
            if (Position.getY(toSq) == 7) {
                white = true;
            } else if (Position.getY(toSq) == 0) {
                white = false;
            } else {
                return m;
            }
        }
        int promoteTo;
        switch (prom) {
            case ' ':
                promoteTo = Piece.EMPTY;
                break;
            case 'q':
                promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
                break;
            case 'r':
                promoteTo = white ? Piece.WROOK : Piece.BROOK;
                break;
            case 'b':
                promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
                break;
            case 'n':
                promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
                break;
            default:
                return m;
        }
        m = new Move(fromSq, toSq, promoteTo);
        return m;
    }
    
    /**
     * Convert a string, such as "e4" to a square number.
     * @return The square number, or -1 if not a legal square.
     */
    public static final int getSquare(String s) {
        int x = s.charAt(0) - 'a';
        int y = s.charAt(1) - '1';
        if ((x < 0) || (x > 7) || (y < 0) || (y > 7))
            return -1;
        return Position.getSquare(x, y);
    }

    /**
     * Convert a square number to a string, such as "e4".
     */
    public static final String squareToString(int square) {
        StringBuilder ret = new StringBuilder();
        int x = Position.getX(square);
        int y = Position.getY(square);
        ret.append((char) (x + 'a'));
        ret.append((char) (y + '1'));
        return ret.toString();
    }

    /**
     * Create an ascii representation of a position.
     */
    public static final String asciiBoard(Position pos) {
        StringBuilder ret = new StringBuilder(400);
        String nl = String.format("%n");
        ret.append("    +----+----+----+----+----+----+----+----+"); ret.append(nl);
        for (int y = 7; y >= 0; y--) {
            ret.append("    |");
            for (int x = 0; x < 8; x++) {
                ret.append(' ');
                int p = pos.getPiece(Position.getSquare(x, y));
                if (p == Piece.EMPTY) {
                    boolean dark = Position.darkSquare(x, y);
                    ret.append(dark ? ".. |" : "   |");
                } else {
                    ret.append(Piece.isWhite(p) ? ' ' : '*');
                    String pieceName = pieceToChar(p);
                    if (pieceName.length() == 0)
                        pieceName = "P";
                    ret.append(pieceName);
                    ret.append(" |");
                }
            }
            ret.append(nl);
            ret.append("    +----+----+----+----+----+----+----+----+");
            ret.append(nl);
        }
        return ret.toString();
    }
    //convert piece with character
   //피스조각을 문자열로 바꾼다
    private final static String pieceToChar(int p) {
        switch (p) {
            case Piece.WQUEEN:  case Piece.BQUEEN:  return "Q";
            case Piece.WROOK:   case Piece.BROOK:   return "R";
            case Piece.WBISHOP: case Piece.BBISHOP: return "B";
            case Piece.WKNIGHT: case Piece.BKNIGHT: return "N";
            case Piece.WKING:   case Piece.BKING:   return "K";
        }
        return "";
    }
    //convert character with piece
	  //문자열을 피스조각으로 바꾼다.
    private final static int charToPiece(boolean white, char c) {
    	switch (c) {
    	case 'Q': case 'q': return white ? Piece.WQUEEN  : Piece.BQUEEN;
    	case 'R': case 'r': return white ? Piece.WROOK   : Piece.BROOK;
    	case 'B':           return white ? Piece.WBISHOP : Piece.BBISHOP;
    	case 'N': case 'n': return white ? Piece.WKNIGHT : Piece.BKNIGHT;
    	case 'K': case 'k': return white ? Piece.WKING   : Piece.BKING;
    	case 'P': case 'p': return white ? Piece.WPAWN   : Piece.BPAWN;
    	}
    	return -1;
    }

    /** Add an = sign to a promotion move, as required by the PGN standard. */
	public final static String pgnPromotion(String str) {
		int idx = str.length() - 1;
		while (idx > 0) {
			char c = str.charAt(idx);
			if ((c != '#') && (c != '+'))
				break;
			idx--;
		}
		if ((idx > 0) && (charToPiece(true, str.charAt(idx)) != -1))
			idx--;
		return str.substring(0, idx + 1) + '=' + str.substring(idx + 1, str.length());
	}
}
