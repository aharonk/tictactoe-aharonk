package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.model.TicTacToeModel.CellValue;

import java.awt.*;
import java.io.*;

public class HardTicTacToeAI implements TicTacToeAI, Serializable {
    private final TicTacToeModel model;
    private transient CellValue[][] board; // Because getting it every time is a lot of extra operations
    private transient CellValue myTeam;

    private transient ScorePair[] colScores;
    private transient ScorePair[] rowScores;
    private transient ScorePair[] diagScores;

    @Serial
    private static final long serialVersionUID = 44L;

    public HardTicTacToeAI(TicTacToeModel m) {
        model = m;
        init();
    }

    //setup
    private void init() {
        board = model.getBoard();
        myTeam = model.getAiTeam();

        colScores = new ScorePair[3];
        rowScores = new ScorePair[3];
        diagScores = new ScorePair[2];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                addToLineScore(colScores, x, board[x][y]);
                addToLineScore(rowScores, y, board[x][y]);
                if (x == y) {
                    addToLineScore(diagScores, 0, board[x][y]);
                }
                if (x == 2 - y) {
                    addToLineScore(diagScores, 1, board[x][y]);
                }
            }
        }
    }

    private void addToLineScore(ScorePair[] line, int index, CellValue v) {
        if (line[index] == null) {
            line[index] = ScorePair.empty();
        }

        line[index].increment(v);
    }

    // gameplay
    @Override
    public void submitMoveToAI(Point loc) {
        int x = loc.x, y = loc.y;
        CellValue player = model.previousPlayer().other();
        board[x][y] = player;

        colScores[x].increment(player);
        rowScores[y].increment(player);
        if (x == y) {
            diagScores[0].increment(player);
        }
        if (x == 2 - y) {
            diagScores[1].increment(player);
        }
    }

    /**
     * Logic is modified from https://onlinelibrary.wiley.com/doi/epdf/10.1207/s15516709cog1704_3, pg. 536
     *
     * @return the requested move location
     */
    @Override
    public Point calculateMove() {
        Point block = null; // Only need to keep track of one. If there's more than one, we've lost either way.

        // region 1. Win
        // columns
        LineStats colResults = calculateWinBlockAndFork(colScores);
        int[] winBlockStats = colResults.winBlockScores;
        for (int y = 0; y < 3; y++) {
            if (winBlockStats[0] != -1 && board[winBlockStats[0]][y] == CellValue.NONE) {
                return new Point(winBlockStats[0], y);
            }

            if (winBlockStats[1] != -1 && board[winBlockStats[1]][y] == CellValue.NONE) {
                block = new Point(winBlockStats[1], y);
            }
        }

        // rows
        LineStats rowResults = calculateWinBlockAndFork(rowScores);
        winBlockStats = rowResults.winBlockScores;
        for (int x = 0; x < 3; x++) {
            if (winBlockStats[0] != -1 && board[x][winBlockStats[0]] == CellValue.NONE) {
                return new Point(x, winBlockStats[0]);
            }

            if (winBlockStats[1] != -1 && board[x][winBlockStats[1]] == CellValue.NONE) {
                block = new Point(x, winBlockStats[1]);
            }
        }

        // diagonals
        LineStats diagResults = calculateWinBlockAndFork(diagScores);
        winBlockStats = diagResults.winBlockScores;
        for (int i = 0; i < 3; i++) {
            if (winBlockStats[0] == 0 && board[i][i] == CellValue.NONE) {
                return new Point(i, i);
            }
            if (winBlockStats[0] == 1 && board[i][2 - i] == CellValue.NONE) {
                return new Point(i, 2 - i);
            }

            if (winBlockStats[1] == 0 && board[i][i] == CellValue.NONE) {
                block = new Point(i, i);
            }

            if (winBlockStats[1] == 1 && board[i][2 - i] == CellValue.NONE) {
                block = new Point(i, 2 - i);
            }
        }
        //endregion

        // region 2. Block Win
        if (block != null) {
            return block; // make this random for replayability?
        }
        //endregion

        Point forkBlock = null; // Only need to keep track of one. If there's more than one, we've lost either way.
        Point locationToMakeTwoInARow = null; // Also need only one; it's just to stave off a fork.

        // region 3. Fork
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                // Fork score for a space is the number of lines that intersect on a
                // space that have only one space claimed, and it's by the AI
                int myForkScore = colResults.myForkScores[col]
                        + rowResults.myForkScores[row]
                        + (col == row ? diagResults.myForkScores[0] : 0)
                        + (col == 2 - row ? diagResults.myForkScores[1] : 0);

                if (board[col][row] == CellValue.NONE) {
                    Point possibleLocation = new Point(col, row);

                    if (myForkScore > 1) {
                        return possibleLocation;
                    }
                    if (myForkScore > 0) {
                        if (locationToMakeTwoInARow == null ||
                                Math.abs(possibleLocation.x - possibleLocation.y) != 1) {
                            locationToMakeTwoInARow = possibleLocation;
                        }
                    }
                }

                int theirForkScore = colResults.theirForkScores[col]
                        + rowResults.theirForkScores[row]
                        + (col == row ? diagResults.theirForkScores[0] : 0)
                        + (col == 2 - row ? diagResults.theirForkScores[1] : 0);
                if (theirForkScore > 1 && board[col][row] == CellValue.NONE) {
                    forkBlock = new Point(col, row);
                }
            }
        }
        //endregion

        // region 4. Block fork
        if (forkBlock != null) {
            if (locationToMakeTwoInARow != null) {
                return locationToMakeTwoInARow; // make this random for replayability?
            }

            return forkBlock;
        }
        //endregion

        // region 5. Center
        if (board[1][1] == CellValue.NONE) {
            return new Point(1, 1);
        }
        //endregion

        Point emptyCorner = null;

        //region 6. Opposite corner
        int[] cornerPoints = new int[]{0, 2};

        for (int x : cornerPoints) {
            for (int y : cornerPoints) {
                if (board[x][y] == myTeam.other() && board[2 - x][2 - y] == CellValue.NONE) {
                    return new Point(2 - x, 2 - y);
                }

                if (board[x][y] == CellValue.NONE) {
                    emptyCorner = new Point(x, y);
                }
            }
        }
        //endregion

        //region 7. Empty corner
        if (emptyCorner != null) {
            return emptyCorner; // make this random for replayability?
        }
        //endregion

        //region 8. Empty side
        for (Point loc : new Point[]{new Point(0, 1), new Point(1, 0), new Point(2, 1), new Point(1, 2)}) {
            if (board[loc.x][loc.y] == CellValue.NONE) {
                return loc; // make this random for replayability?
            }
        }
        //endregion

        throw new IllegalStateException("Can't return move while board is full.");
    }

    private LineStats calculateWinBlockAndFork(ScorePair[] line) {
        int[] lineScores = new int[]{-1, -1}, canFork = new int[line.length], theyCanFork = new int[line.length];

        for (int i = 0; i < line.length; i++) {
            if (line[i].getValue(myTeam) == 2) {
                lineScores[0] = i;
            } else if (line[i].getValue(myTeam.other()) == 2) {
                lineScores[1] = i;
            }


            if (line[i].total() == 1) {
                if (line[i].getValue(myTeam) == 1) {
                    canFork[i] = 1;
                } else if (line[i].getValue(myTeam.other()) == 1) {
                    theyCanFork[i] = 1;
                }
            }
        }

        return new LineStats(lineScores, canFork, theyCanFork);
    }

    // built-in
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        init();
    }

    // helpers
    public static class ScorePair implements Serializable {
        private int x, o;

        public ScorePair(int x, int o) {
            this.x = x;
            this.o = o;
        }

        public static ScorePair empty() {
            return new ScorePair(0, 0);
        }

        public void increment(CellValue c) {
            switch (c) {
                case X -> x++;
                case O -> o++;
                default -> {}
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

    private static class LineStats {
        // winBlockScores[0] is number of spaces in the line controlled by the AI,
        // [1] is the number controlled by the other player
        public final int[] winBlockScores, myForkScores, theirForkScores;

        public LineStats(int[] scores, int[] mine, int[] theirs) {
            winBlockScores = scores;
            myForkScores = mine;
            theirForkScores = theirs;
        }
    }
}
