package edu.touro.cs.mcon364.model;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import edu.touro.cs.mcon364.shared.IntPair;

public class TicTacToeModel implements Serializable {
    private transient CellValue[][] board = new CellValue[3][3];
    private boolean XTurn;

    // Used for O(1) win checks
    private final ScorePair[] colScores = new ScorePair[3];
    private final ScorePair[] rowScores = new ScorePair[3];
    private final ScorePair[] diagScores = new ScorePair[2];
    private transient int moveCount;

    private transient TicTacToeAI ai;
    private CellValue aiTeam = null;

    @Serial
    private static final long serialVersionUID = 43L;

    // Since TicTacToe was designed to be played on a 3x3 grid, all values are hardcoded.
    public TicTacToeModel() {
        init();
    }

    // initialization
    public void newGame() {
        init();
    }

    private void init() {
        // set up game
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                board[x][y] = CellValue.NONE;
            }
        }

        XTurn = true;

        // set up scoring
        for (int i = 0; i < 3; i++) {
            colScores[i] = ScorePair.empty();
        }

        for (int i = 0; i < 3; i++) {
            rowScores[i] = ScorePair.empty();
        }

        for (int i = 0; i < 2; i++) {
            diagScores[i] = ScorePair.empty();
        }

        moveCount = 0;
    }

    public void startGame(GameType gt) {
        if (gt == GameType.COMPUTER) {
            aiTeam = (new Random().nextBoolean()) ? CellValue.X : CellValue.O;
            ai = new HardTicTacToeAI(this);
        }
    }

    // getters
    protected CellValue[][] getBoard() {
        CellValue[][] boardCopy = new CellValue[3][3];

        for (int i = 0; i < 3; i++) {
            System.arraycopy(board[i], 0, boardCopy[i], 0, 3);
        }

        return boardCopy;
    }

    public CellValue getAiTeam() {
        return aiTeam;
    }

    public CellValue previousPlayer() {
        return XTurn ? CellValue.O : CellValue.X;
    }

    protected CellValue getSpace(int x, int y) {
        return board[x][y];
    }

    // move methods
    public MoveResult makeMove(IntPair p) throws IllegalArgumentException, IndexOutOfBoundsException {
        int x = p.val1, y = p.val2;
        if (x < 0 || x > 2 || y < 0 || y > 2) {
            throw new IndexOutOfBoundsException();
        }

        if (board[x][y] != CellValue.NONE) {
            throw new IllegalArgumentException();
        }

        CellValue currentPlayer = XTurn ? CellValue.X : CellValue.O;

        board[x][y] = currentPlayer;
        if (ai != null) {
            ai.submitMoveToAI(new IntPair(x, y));
        }
        XTurn = !XTurn;

        return scoreAndCheckWin(x, y, currentPlayer);
    }

    // Each time a move is made, update the scores for that player in that row, column, and diagonal
    // (if applicable), and check whether there is a win along each of those lines.
    // Adapted from https://stackoverflow.com/a/1610176
    private MoveResult scoreAndCheckWin(int x, int y, CellValue player) {

        MoveResult res = new MoveResult(x, y);

        // Add a point for the player along the column the move was in.
        if ((colScores[x].increment(player)) == 3) {
            res.affectedLines.add(new IntPair[]{new IntPair(x, 0), new IntPair(x, 1), new IntPair(x, 2)});
        }

        // And in the row.
        if ((rowScores[y].increment(player)) == 3) {
            res.affectedLines.add(new IntPair[]{new IntPair(0, y), new IntPair(1, y), new IntPair(2, y)});
        }

        // Diagonal if x == y
        if (x == y && (diagScores[0].increment(player)) == 3) {
            res.affectedLines.add(new IntPair[]{new IntPair(0, 0), new IntPair(1, 1), new IntPair(2, 2)});
        }

        // Anti-diagonal if (2,0), (1,1) or (0,2)
        if (x == 2 - y && (diagScores[1].increment(player)) == 3) {
            res.affectedLines.add(new IntPair[]{new IntPair(0, 2), new IntPair(1, 1), new IntPair(2, 0)});
        }

        if (!res.affectedLines.isEmpty()) {
            res.resultingState = XTurn ? MoveResult.GameState.O_WIN : MoveResult.GameState.X_WIN;
        } else if ((++moveCount) == 9) {
            res.resultingState = MoveResult.GameState.DRAW;
        }

        return res;
    }

    public MoveResult aiMove() {
        try {
            return makeMove(ai.calculateMove());
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new IllegalStateException("Something went wrong with the AI.");
        }
    }

    // serialization
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                s.writeObject(board[x][y]);
            }
        }

        if (ai != null) {
            s.writeObject(ai);
        }
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        moveCount = 0;
        board = new CellValue[3][3];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                board[x][y] = (CellValue) s.readObject();

                if (board[x][y] != CellValue.NONE) {
                    moveCount++;
                }
            }
        }

        if (aiTeam != null) {
            ai = (TicTacToeAI) s.readObject();
        }
    }

    // classes
    public enum GameType {
        HUMAN,
        COMPUTER,
    }

    public enum CellValue {
        NONE(""),
        X("X"),
        O("O");

        private final String repr;

        CellValue(String s) {
            repr = s;
        }

        public String getRepr() {
            return repr;
        }

        public CellValue other() {
            return switch (this) {
                case X -> O;
                case O -> X;
                case NONE -> NONE;
            };
        }
    }

    public static class MoveResult {
        public enum GameState {
            CONTINUE,
            DRAW,
            X_WIN,
            O_WIN,
        }

        public GameState resultingState;
        public final int moveX, moveY;
        public final ArrayList<IntPair[]> affectedLines;

        public MoveResult(int x, int y) {
            moveX = x;
            moveY = y;
            resultingState = GameState.CONTINUE;
            affectedLines = new ArrayList<>();
        }
    }
}
