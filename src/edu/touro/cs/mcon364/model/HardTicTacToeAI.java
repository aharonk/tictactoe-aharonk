package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.model.TicTacToeModel.CellValue;
import edu.touro.cs.mcon364.shared.IntPair;

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

    private void init() {
        board = model.getBoard();
        myTeam = model.getAiTeam();

        colScores = new ScorePair[3];
        rowScores = new ScorePair[3];
        diagScores = new ScorePair[2];

        // Deep copy
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

    @Override
    public void submitMoveToAI(IntPair loc) {
        int x = loc.val1, y = loc.val2;
        CellValue player = model.previousPlayer().other();
        board[x][y] = player;

        colScores[x].increment(player);
        rowScores[y].increment(player);
        if (x == y) {
            diagScores[0].increment(player);
        }
        if (x == 2-y) {
            diagScores[1].increment(player);
        }
    }

    /**
     * Logic is modified from https://onlinelibrary.wiley.com/doi/epdf/10.1207/s15516709cog1704_3, pg. 536
     *
     * @return the requested move location
     */
    @Override
    public IntPair calculateMove() {
        IntPair block = null;

        // region 1. Win
        // columns
        LineStats colResults = calculateWinBlockAndFork(colScores);
        IntPair winBlockStats = colResults.winBlockScores;
        for (int y = 0; y < 3; y++) {
            if (winBlockStats.val1 != -1 && board[winBlockStats.val1][y] == CellValue.NONE) {
                return new IntPair(winBlockStats.val1, y);
            }

            if (winBlockStats.val2 != -1 && board[winBlockStats.val2][y] == CellValue.NONE) {
                block = new IntPair(winBlockStats.val2, y);
            }
        }

        // rows
        LineStats rowResults = calculateWinBlockAndFork(rowScores);
        winBlockStats = rowResults.winBlockScores;
        for (int x = 0; x < 3; x++) {
            if (winBlockStats.val1 != -1 && board[x][winBlockStats.val1] == CellValue.NONE) {
                return new IntPair(x, winBlockStats.val1);
            }

            if (winBlockStats.val2 != -1 && board[x][winBlockStats.val2] == CellValue.NONE) {
                block = new IntPair(x, winBlockStats.val2);
            }
        }

        // diagonals
        LineStats diagResults = calculateWinBlockAndFork(diagScores);
        winBlockStats = diagResults.winBlockScores;
        for (int i = 0; i < 3; i++) {
            if (winBlockStats.val1 == 0 && board[i][i] == CellValue.NONE) {
                return new IntPair(i, i);
            }
            if (winBlockStats.val1 == 1 && board[i][2 - i] == CellValue.NONE) {
                return new IntPair(i, 2 - i);
            }

            if (winBlockStats.val2 == 0 && board[i][i] == CellValue.NONE) {
                block = new IntPair(i, i);
            }

            if (winBlockStats.val2 == 1 && board[i][2 - i] == CellValue.NONE) {
                block = new IntPair(i, 2 - i);
            }
        }
        //endregion

        // region 2. Block Win
        if (block != null) {
            return block; //todo random?
        }
        //endregion

        IntPair forkBlock = null; // Only need to keep track of one. If there's more than one, we've lost either way.
        IntPair locationToMakeTwoInARow = null; // Also need only one. Just stave off a fork.

        // region 3. Fork
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                int myForkScore = colResults.myForkScores[col]
                        + rowResults.myForkScores[row]
                        + (col == row ? diagResults.myForkScores[0] : 0)
                        + (col == 2 - row ? diagResults.myForkScores[1] : 0);
                if (board[col][row] == CellValue.NONE) {
                    IntPair possibleLocation = new IntPair(col, row);

                    if (myForkScore > 1) {
                        return possibleLocation;
                    }
                    if (myForkScore > 0) {
                        if (locationToMakeTwoInARow == null ||
                                Math.abs(possibleLocation.val1 - possibleLocation.val2) != 1) {
                            locationToMakeTwoInARow = possibleLocation;
                        }
                    }
                }

                int theirForkScore = colResults.theirForkScores[col]
                        + rowResults.theirForkScores[row]
                        + (col == row ? diagResults.theirForkScores[0] : 0)
                        + (col == 2 - row ? diagResults.theirForkScores[1] : 0);
                if (theirForkScore > 1 && board[col][row] == CellValue.NONE) {
                    forkBlock = new IntPair(col, row);
                }
            }
        }
        //endregion

        // region 4. Block fork
        if (forkBlock != null) {
            if (locationToMakeTwoInARow != null) {
                return locationToMakeTwoInARow; //todo random?
            }

            return forkBlock;
        }
        //endregion

        // region 5. Center
        if (board[1][1] == CellValue.NONE) {
            return new IntPair(1, 1);
        }
        //endregion

        IntPair emptyCorner = null;

        //region 6. Opposite corner
        int[] cornerLocs = new int[]{0, 2};

        for (int x : cornerLocs) {
            for (int y : cornerLocs) {
                if (board[x][y] == myTeam.other() && board[2 - x][2 - y] == CellValue.NONE) {
                    return new IntPair(2 - x, 2 - y);
                }

                if (board[x][y] == CellValue.NONE) {
                    emptyCorner = new IntPair(x, y);
                }
            }
        }
        //endregion

        //region 7. Empty corner
        if (emptyCorner != null) {
            return emptyCorner; //todo random?
        }
        //endregion

        //region 8. Empty side
        for (IntPair loc : new IntPair[]{new IntPair(0, 1), new IntPair(1, 0), new IntPair(2, 1), new IntPair(1, 2)}) {
            if (board[loc.val1][loc.val2] == CellValue.NONE) {
                return loc; //todo random?
            }
        }
        //endregion

        throw new IllegalStateException("Can't return move while board is full.");
    }

    private void addToLineScore(ScorePair[] line, int index, CellValue v) {
        if (line[index] == null) {
            line[index] = ScorePair.empty();
        }

        line[index].increment(v);
    }

    private LineStats calculateWinBlockAndFork(ScorePair[] line) {
        int myWin = -1, theirWin = -1;
        int[] canFork = new int[line.length], theyCanFork = new int[line.length];

        for (int i = 0; i < line.length; i++) {
            if (line[i].getValue(myTeam) == 2) {
                myWin = i;
            } else if (line[i].getValue(myTeam.other()) == 2) {
                theirWin = i;
            }


            if (line[i].total() == 1) {
                if (line[i].getValue(myTeam) == 1) {
                    canFork[i] = 1;
                } else if (line[i].getValue(myTeam.other()) == 1) {
                    theyCanFork[i] = 1;
                }
            }
        }

        return new LineStats(new IntPair(myWin, theirWin), canFork, theyCanFork);
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        init();
    }

    private static class LineStats {
        public final IntPair winBlockScores;
        public final int[] myForkScores, theirForkScores;

        public LineStats(IntPair ip, int[] mine, int[] theirs) {
            winBlockScores = ip;
            myForkScores = mine;
            theirForkScores = theirs;
        }
    }
}
