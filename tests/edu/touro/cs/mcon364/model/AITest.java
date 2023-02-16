package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.model.TicTacToeModel.MoveResult.GameState;
import edu.touro.cs.mcon364.shared.IntPair;

import static edu.touro.cs.mcon364.model.TicTacToeModel.MoveResult.GameState.*;
import static org.junit.jupiter.api.Assertions.*;

class AITest {
    private TicTacToeModel model;

    private GameState naiveMove() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (model.getSpace(x, y) == TicTacToeModel.CellValue.NONE) {
                    return model.makeMove(new IntPair(x, y)).resultingState;
                }
            }
        }
        return null;
    }

    @org.junit.jupiter.api.Test
    void testAIInteracts() {
        model = new TicTacToeModel();
        model.startGame(TicTacToeModel.GameType.COMPUTER);

        if (model.getAiTeam() == TicTacToeModel.CellValue.X) {
            model.aiMove();
        }

        GameState gs = GameState.CONTINUE;

        while (gs == GameState.CONTINUE) {
            if (model.getAiTeam() == model.previousPlayer().other()) {
                gs = model.aiMove().resultingState;
            } else {
                gs = naiveMove();
            }
        }

        assertEquals((model.getAiTeam() == TicTacToeModel.CellValue.X ? X_WIN : O_WIN), gs);
    }
}
