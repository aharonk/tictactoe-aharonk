package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.shared.IntPair;

public interface TicTacToeAI {
    IntPair calculateMove();

    void submitMoveToAI(IntPair loc);
}
