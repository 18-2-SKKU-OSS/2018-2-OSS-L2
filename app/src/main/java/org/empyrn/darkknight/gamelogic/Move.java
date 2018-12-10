package org.empyrn.darkknight.gamelogic;

/**
 *
 * @author petero
 */
public class Move {
    // 체스판은 8x8 규격이며, 이를 2차원 좌표형태 대신 1차원 스트링형태로 기록한다.
    
    /** From square, 0-63. */
    public int from;
    /** To square, 0-63. */
    public int to;
    /** Promotion piece. */
    public int promoteTo;

    /** Create a move object. */
    // Move 객체 생성자
    public Move(int from, int to, int promoteTo) {
        this.from = from;
        this.to = to;
        this.promoteTo = promoteTo;
    }
    // Move 객체 복사 생성자
    public Move(Move m) {
        this.from = m.from;
        this.to = m.to;
        this.promoteTo = m.promoteTo;
    }
    
    // Get anonymous object from extern,
    // Valuate the two move position is eqaul.
    // 외부에서 임의의 객체를 받아서 두 객체의 mooe position 이 같은지 평가한다.
    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            // This means that this two objects are not same class' object
            // 두 객체가 서로 다른 객체이므로 평가할 수 없음.
            // 에러 반환 대신 false 반환.
            return false;
        Move other = (Move)o;
        if (from != other.from)
            return false;
        if (to != other.to)
            return false;
        if (promoteTo != other.promoteTo)
            return false;
        return true;
    }
    @Override
    public int hashCode() {
        return (from * 64 + to) * 16 + promoteTo;
    }

    /** Useful for debugging. */
    public final String toString() {
    	return TextIO.moveToUCIString(this);
    }
}
