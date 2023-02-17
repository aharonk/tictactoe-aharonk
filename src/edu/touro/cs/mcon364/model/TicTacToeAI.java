package edu.touro.cs.mcon364.model;

import java.awt.*;

public interface TicTacToeAI {
    Point calculateMove();

    void submitMoveToAI(Point loc);
}
