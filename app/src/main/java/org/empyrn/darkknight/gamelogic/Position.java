package org.empyrn.darkknight.gamelogic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stores the state of a chess position.
 * All required state is stored, except for all previous positions
 * since the last capture or pawn move. That state is only needed
 * for three-fold repetition draw detection, and is better stored
 * in a separate hash table.
 * @author petero
 */
/**
  * 체스 위치의 상태를 저장합니다.
  * 모든 이전 상태를 제외하고 필요한 모든 상태가 저장됩니다.
  * 마지막 캡처 또는 폰 이동 이후. 그 상태가 필요하다.
  * 3 배 반복 그리기 감지를 위해, 그리고 더 잘 저장됩니다.
  * 별도의 해시 테이블에 있습니다.
 
*/
public class Position {
    private int[] squares;

    public boolean whiteMove;

    /** Bit definitions for the castleMask bit mask. */
    /** castleMask 비트 마스크에 대한 비트 정의. */
    public static final int A1_CASTLE = 0; /** White long castle. */
    public static final int H1_CASTLE = 1; /** White short castle. */
    public static final int A8_CASTLE = 2; /** Black long castle. */
    public static final int H8_CASTLE = 3; /** Black short castle. */
    
    private int castleMask;

    private int epSquare;
    
    /** Number of half-moves since last 50-move reset. */
    /** 마지막 50 이동 재설정 이후의 반 이동 수입니다. */
    public int halfMoveClock;
    
    /** Game move number, starting from 1. */
    /** 1에서 시작하는 게임 이동 번호. */
    public int fullMoveCounter;

    private long hashKey;           // Cached Zobrist hash key
    private int wKingSq, bKingSq;   // Cached king positions

    /** Initialize board to empty position. */
    /** 보드를 빈 위치로 초기화하십시오. */
    public Position() {
        squares = new int[64];
        for (int i = 0; i < 64; i++)
            squares[i] = Piece.EMPTY;
        whiteMove = true;
        castleMask = 0;
        epSquare = -1;
        halfMoveClock = 0;
        fullMoveCounter = 1;
        hashKey = computeZobristHash();
        wKingSq = bKingSq = -1;
    }

    public Position(Position other) {
        squares = new int[64];
        System.arraycopy(other.squares, 0, squares, 0, 64);
        whiteMove = other.whiteMove;
        castleMask = other.castleMask;
        epSquare = other.epSquare;
        halfMoveClock = other.halfMoveClock;
        fullMoveCounter = other.fullMoveCounter;
        hashKey = other.hashKey;
        wKingSq = other.wKingSq;
        bKingSq = other.bKingSq;
    }
    
    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        Position other = (Position)o;
        if (!drawRuleEquals(other))
            return false;
        if (halfMoveClock != other.halfMoveClock)
            return false;
        if (fullMoveCounter != other.fullMoveCounter)
            return false;
        if (hashKey != other.hashKey)
            return false;
        return true;
    }
    @Override
    public int hashCode() {
        return (int)hashKey;
    }

    /**
     * Return Zobrish hash value for the current position.
     * Everything except the move counters are included in the hash value.
     */
    /**
      * 현재 위치에 대한 Zobrish 해시 값을 반환합니다.
      * 이동 카운터를 제외한 모든 항목이 해시 값에 포함됩니다.
    */
    public final long zobristHash() {
        return hashKey;
    }

    /**
     * Decide if two positions are equal in the sense of the draw by repetition rule.
     * @return True if positions are equal, false otherwise.
     */
    /**
      * 반복 규칙에 의해 두 위치가 추첨의 의미에서 동일하면 결정하십시오.
      * @return 위치가 동일한 경우는 true, 그렇지 않은 경우는 false
    */
    final public boolean drawRuleEquals(Position other) {
        for (int i = 0; i < 64; i++) {
            if (squares[i] != other.squares[i])
                return false;
        }
        if (whiteMove != other.whiteMove)
            return false;
        if (castleMask != other.castleMask)
            return false;
        if (epSquare != other.epSquare)
            return false;
        return true;
    }

    public final void setWhiteMove(boolean whiteMove) {
        if (whiteMove != this.whiteMove) {
            hashKey ^= whiteHashKey;
            this.whiteMove = whiteMove;
        }
    }
    /** Return index in squares[] vector corresponding to (x,y). */
    /** (x, y)에 해당하는 squares [] 벡터의 인덱스를 반환합니다. */
    public final static int getSquare(int x, int y) {
        return y * 8 + x;
    }
    /** Return x position (file) corresponding to a square. */
    /** 사각형에 해당하는 x 위치 (파일)를 반환합니다. */
    public final static int getX(int square) {
        return square & 7;
    }
    /** Return y position (rank) corresponding to a square. */
         /** 사각형에 해당하는 y 위치 (순위)를 반환합니다. */
    public final static int getY(int square) {
        return square >> 3;
    }
    /** Return true if (x,y) is a dark square. */
    /** (x, y)가 어두운 사각형 인 경우 true를 반환합니다. */
    public final static boolean darkSquare(int x, int y) {
        return (x & 1) == (y & 1);
    }

    /** Return piece occuping a square. */
    /** 사각형을 차지하는 조각을 되 돌린다. */
    public final int getPiece(int square) {
        return squares[square];
    }
    /** Set a square to a piece value. */
    /** 사각형을 조각 값으로 설정합니다. */
    public final void setPiece(int square, int piece) {
    	// Update hash key
        // hash key 업데이트 
    	int oldPiece = squares[square];
        hashKey ^= psHashKeys[oldPiece][square];
        hashKey ^= psHashKeys[piece][square];
        
        // Update board
        // 판 업데이트 
        squares[square] = piece;

        // Update king position 
        // 왕의 위치 최신화
        if (piece == Piece.WKING) {
            wKingSq = square;
        } else if (piece == Piece.BKING) {
            bKingSq = square;
        }
    }

    /** Return true if white long castling right has not been lost. */
    /** 만약 흰색 긴 castling 권리가 손실되지 않은 경우 true를 반환합니다. */
    public final boolean a1Castle() {
        return (castleMask & (1 << A1_CASTLE)) != 0;
    }
    /** Return true if white short castling right has not been lost. */
    public final boolean h1Castle() {
        return (castleMask & (1 << H1_CASTLE)) != 0;
    }
    /** Return true if black long castling right has not been lost. */
    /** 하얀 짧은 짧은 성곽을 잃어 버리지 않았다면 true를 반환합니다. */
    public final boolean a8Castle() {
        return (castleMask & (1 << A8_CASTLE)) != 0;
    }
    /** Return true if black short castling right has not been lost. */
    /** 짧은 검은 색 성채를 잃어 버리지 않은 경우 true를 반환합니다. */
    public final boolean h8Castle() {
        return (castleMask & (1 << H8_CASTLE)) != 0;
    }
    /** Bitmask describing castling rights. */
    /** 성채를 묘사하는 비트 마스크. */
    public final int getCastleMask() {
        return castleMask;
    }
    public final void setCastleMask(int castleMask) {
        hashKey ^= castleHashKeys[this.castleMask];
        hashKey ^= castleHashKeys[castleMask];
        this.castleMask = castleMask;
    }

    /** En passant square, or -1 if no ep possible. */
    /** en passant square 또는 ep가 없으면 -1입니다. */
    public final int getEpSquare() {
        return epSquare;
    }
    public final void setEpSquare(int epSquare) {
        if (this.epSquare != epSquare) {
            hashKey ^= epHashKeys[(this.epSquare >= 0) ? getX(this.epSquare) + 1 : 0];
            hashKey ^= epHashKeys[(epSquare >= 0) ? getX(epSquare) + 1 : 0];
            this.epSquare = epSquare;
        }
    }


    public final int getKingSq(boolean whiteMove) {
        return whiteMove ? wKingSq : bKingSq;
    }

    /**
     * Count number of pieces of a certain type.
     */
    
    /**
     * 조각의 타입수를 센다.
     */
    public final int nPieces(int pType) {
        int ret = 0;
        for (int sq = 0; sq < 64; sq++) {
            if (squares[sq] == pType)
                ret++;
        }
        return ret;
    }

    /** Apply a move to the current position. */
    //현재 위치를 적용시킨다. 
    public final void makeMove(Move move, UndoInfo ui) {
        ui.capturedPiece = squares[move.to];
        ui.castleMask = castleMask;
        ui.epSquare = epSquare;
        ui.halfMoveClock = halfMoveClock;
        boolean wtm = whiteMove;
        
        int p = squares[move.from];
        int capP = squares[move.to];

        boolean nullMove = (move.from == 0) && (move.to == 0);
        
        if (nullMove || (capP != Piece.EMPTY) || (p == (wtm ? Piece.WPAWN : Piece.BPAWN))) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
        if (!wtm) {
            fullMoveCounter++;
        }

        // Handle castling
        // 말들의 위치를 조작한다,
        int king = wtm ? Piece.WKING : Piece.BKING;
        int k0 = move.from;
        if (p == king) {
            if (move.to == k0 + 2) { // O-O
                setPiece(k0 + 1, squares[k0 + 3]);
                setPiece(k0 + 3, Piece.EMPTY);
            } else if (move.to == k0 - 2) { // O-O-O
                setPiece(k0 - 1, squares[k0 - 4]);
                setPiece(k0 - 4, Piece.EMPTY);
            }
            if (wtm) {
                setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
                setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
            } else {
                setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
                setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
            }
        }
        if (!nullMove) {
        	int rook = wtm ? Piece.WROOK : Piece.BROOK;
        	if (p == rook) {
        		removeCastleRights(move.from);
        	}
        	int oRook = wtm ? Piece.BROOK : Piece.WROOK;
        	if (capP == oRook) {
        		removeCastleRights(move.to);
        	}
        }

        // Handle en passant and epSquare
        int prevEpSquare = epSquare;
        setEpSquare(-1);
        if (p == Piece.WPAWN) {
            if (move.to - move.from == 2 * 8) {
                int x = Position.getX(move.to);
                if (    ((x > 0) && (squares[move.to - 1] == Piece.BPAWN)) ||
                        ((x < 7) && (squares[move.to + 1] == Piece.BPAWN))) {
                    setEpSquare(move.from + 8);
                }
            } else if (move.to == prevEpSquare) {
                setPiece(move.to - 8, Piece.EMPTY);
            }
        } else if (p == Piece.BPAWN) {
            if (move.to - move.from == -2 * 8) {
                int x = Position.getX(move.to);
                if (    ((x > 0) && (squares[move.to - 1] == Piece.WPAWN)) ||
                        ((x < 7) && (squares[move.to + 1] == Piece.WPAWN))) {
                    setEpSquare(move.from - 8);
                }
            } else if (move.to == prevEpSquare) {
                setPiece(move.to + 8, Piece.EMPTY);
            }
        }
            
        // Perform move
        // 이동을 실행
        setPiece(move.from, Piece.EMPTY);
        
        // Handle promotion
        //전진하는 것을 다루다
        if (move.promoteTo != Piece.EMPTY) {
            setPiece(move.to, move.promoteTo);
        } else {
        	setPiece(move.to, p);
        }
        setWhiteMove(!wtm);
    }
    
    public final void unMakeMove(Move move, UndoInfo ui) {
        setWhiteMove(!whiteMove);
        int p = squares[move.to];
        setPiece(move.from, p);
        setPiece(move.to, ui.capturedPiece);
        setCastleMask(ui.castleMask);
        setEpSquare(ui.epSquare);
        halfMoveClock = ui.halfMoveClock;
        boolean wtm = whiteMove;
        if (move.promoteTo != Piece.EMPTY) {
        	p = wtm ? Piece.WPAWN : Piece.BPAWN;
            setPiece(move.from, p);
        }
        if (!wtm) {
            fullMoveCounter--;
        }
        
        // Handle castling
        // 왕의 이동을 다루다
        int king = wtm ? Piece.WKING : Piece.BKING;
        int k0 = move.from;
        if (p == king) {
            if (move.to == k0 + 2) { // O-O
                setPiece(k0 + 3, squares[k0 + 1]);
                setPiece(k0 + 1, Piece.EMPTY);
            } else if (move.to == k0 - 2) { // O-O-O
                setPiece(k0 - 4, squares[k0 - 1]);
                setPiece(k0 - 1, Piece.EMPTY);
            }
        }

        // Handle en passant
        if (move.to == epSquare) {
        	if (p == Piece.WPAWN) {
                setPiece(move.to - 8, Piece.BPAWN);
        	} else if (p == Piece.BPAWN) {
                setPiece(move.to + 8, Piece.WPAWN);
        	}
        }
    }
    
    private final void removeCastleRights(int square) {
        if (square == Position.getSquare(0, 0)) {
            setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
        } else if (square == Position.getSquare(7, 0)) {
            setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
        } else if (square == Position.getSquare(0, 7)) {
            setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
        } else if (square == Position.getSquare(7, 7)) {
            setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
        }
    }

    /* ------------- Hashing code ------------------ */
    /* ------------- 해시 코드 ------------------ */
 
    private static long[][] psHashKeys;    // [piece][square]
    private static long whiteHashKey;
    private static long[] castleHashKeys;  // [castleMask]
    private static long[] epHashKeys;      // [epFile + 1] (epFile==-1 for no ep)

    static {
        psHashKeys = new long[Piece.nPieceTypes][64];
        castleHashKeys = new long[16];
        epHashKeys = new long[9];
        int rndNo = 0;
        for (int p = 0; p < Piece.nPieceTypes; p++) {
            for (int sq = 0; sq < 64; sq++) {
                psHashKeys[p][sq] = getRandomHashVal(rndNo++);
            }
        }
        whiteHashKey = getRandomHashVal(rndNo++);
        for (int cm = 0; cm < castleHashKeys.length; cm++)
            castleHashKeys[cm] = getRandomHashVal(rndNo++);
        for (int f = 0; f < epHashKeys.length; f++)
            epHashKeys[f] = getRandomHashVal(rndNo++);
    }

    /**
     * Compute the Zobrist hash value non-incrementally. Only useful for test programs.
     */
    /**
      * Zobrist 해시 값을 비 점진적으로 계산합니다. 테스트 프로그램에만 유용합니다.
     */
    final long computeZobristHash() {
        long hash = 0;
        for (int sq = 0; sq < 64; sq++) {
        	int p = squares[sq];
            hash ^= psHashKeys[p][sq];
        }
        if (whiteMove)
            hash ^= whiteHashKey;
        hash ^= castleHashKeys[castleMask];
        hash ^= epHashKeys[(epSquare >= 0) ? getX(epSquare) + 1 : 0];
        return hash;
    }
    //get random value
    //random value를 얻는다
    private final static long getRandomHashVal(int rndNo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] input = new byte[4];
            for (int i = 0; i < 4; i++)
                input[i] = (byte)((rndNo >> (i * 8)) & 0xff);
            byte[] digest = md.digest(input);
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret ^= ((long)digest[i]) << (i * 8);
            }
            return ret;
        } catch (NoSuchAlgorithmException ex) {
            throw new UnsupportedOperationException("SHA-1 not available");
        }
    }

    /** Useful for debugging. */
    //디버깅을 유용화하게 한다.
    public final String toString() {
    	return TextIO.asciiBoard(this);
    }
}
