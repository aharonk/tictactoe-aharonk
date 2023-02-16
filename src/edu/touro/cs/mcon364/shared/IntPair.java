package edu.touro.cs.mcon364.shared;

import java.io.Serializable;

public class IntPair implements Serializable {
    public final int val1, val2;

    public IntPair(int val1, int val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntPair intPair = (IntPair) o;
        return val1 == intPair.val1 && val2 == intPair.val2;
    }

    @Override
    public int hashCode() {
        int hash = 31 * 31 + val1;
        return hash * 31 + val2;
    }
}