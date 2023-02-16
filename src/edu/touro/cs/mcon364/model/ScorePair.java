package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.model.TicTacToeModel.CellValue;

import java.io.Serializable;

public class ScorePair implements Serializable {
    private int x, o;

    public ScorePair(int x, int o) {
        this.x = x;
        this.o = o;
    }

    public static ScorePair empty() {
        return new ScorePair(0, 0);
    }

    /**
     * Increments a score.
     *
     * @param c The player to increment
     * @return The score after incrementing
     */
    public int increment(CellValue c) {
        switch (c) {
            case X:
                x++;
                return x;
            case O:
                o++;
                return o;
            default:
                return -1;
        }
    }

    public int getValue(CellValue c) {
        return switch (c) {
            case X -> x;
            case O -> o;
            default -> -1;
        };
    }

    public int total() {
        return x + o;
    }
}
