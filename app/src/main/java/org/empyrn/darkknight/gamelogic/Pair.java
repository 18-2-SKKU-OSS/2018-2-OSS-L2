package org.empyrn.darkknight.gamelogic;

/**
 * A small helper class that makes it possible to return two values from a function.
 * @author petero
 */

// Pair 형태를 다루기 위한 헬퍼 템플릿 클래스.
// C++ STL의 Pair 객체와 완전히 동일함.
public final class Pair<T1, T2> {
    public final T1 first;
    public final T2 second;
    
    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}
