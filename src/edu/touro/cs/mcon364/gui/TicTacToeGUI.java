package edu.touro.cs.mcon364.gui;

import edu.touro.cs.mcon364.model.TicTacToeModel;
import edu.touro.cs.mcon364.model.TicTacToeModel.MoveResult;
import edu.touro.cs.mcon364.model.TicTacToeModel.MoveResult.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import static edu.touro.cs.mcon364.model.TicTacToeModel.GameType.COMPUTER;
import static edu.touro.cs.mcon364.model.TicTacToeModel.GameType.HUMAN;

public class TicTacToeGUI extends JFrame implements Serializable {
    private TicTacToeModel model;
    private transient JSquare[][] board;
    private transient JLabel currTurn;
    private final JCheckBox aiCheckBox;
    private transient final JButton save, restore;

    // static fields aren't serialized
    private static final int WIDTH = 317, HEIGHT = 295;
    private static final Dimension BUTTON_DIMENSIONS = new Dimension(50, 50);
    private static final String TURN_BUFFER = "   ", TURN_LABEL = "'s turn.";

    @Serial
    private static final long serialVersionUID = 42L;

    public TicTacToeGUI() {
        model = new TicTacToeModel();
        board = new JSquare[3][3];

        setTitle("Tic Tac Toe");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Makes the JFrame appear in the center of the screen.
        setLocationRelativeTo(null);

        // Set up save bar
        JPanel savePanel = new JPanel(new BorderLayout());

        save = new JButton("Save");
        save.addActionListener(new SaveListener(this));
        save.setEnabled(false);

        restore = new JButton("Restore");
        restore.addActionListener(e -> loadSave());
        restore.setEnabled(false);

        JPanel padding = new JPanel();
        padding.setPreferredSize(new Dimension(0, 1));

        savePanel.add(save, BorderLayout.WEST);
        savePanel.add(restore, BorderLayout.EAST);
        savePanel.add(padding, BorderLayout.SOUTH);
        savePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));

        // Set up game board
        JPanel boardPanel = new JPanel(new FlowLayout());
        MoveListener ml = new MoveListener();

        for (int x = 0; x < 3; x++) {
            FlowLayout fl = new FlowLayout();
            fl.setHgap(15);
            JPanel row = new JPanel(fl);
            for (int y = 0; y < 3; y++) {
                JSquare b = new JSquare(new Point(x, y));
                b.setPreferredSize(BUTTON_DIMENSIONS);
                b.addActionListener(ml);
                b.setEnabled(false);
                board[x][y] = b;
                row.add(b);
            }

            boardPanel.add(row);
        }


        // Set up info panel
        JPanel bottomInfo = new JPanel(new BorderLayout(5, 0));

        currTurn = new JLabel("Begin a game.");
        bottomInfo.add(currTurn, BorderLayout.WEST);

        aiCheckBox = new JCheckBox("Computer Opponent");
        bottomInfo.add(aiCheckBox, BorderLayout.CENTER);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new NewGameListener());
        bottomInfo.add(newGame, BorderLayout.EAST);

        bottomInfo.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));


        // Compile
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(savePanel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(bottomInfo, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        mainPanel.setVisible(true);

        setVisible(true);
    }

    // initialization
    private void loadSave() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("save.bin"))) {
            TicTacToeGUI tttg = (TicTacToeGUI) ois.readObject();
            model = tttg.model;

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    board[x][y].setText(tttg.board[x][y].getText());
                }
            }

            currTurn.setText(tttg.currTurn.getText());
            aiCheckBox.setSelected(tttg.aiCheckBox.isSelected());

        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    this, "Restore failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // gameplay
    private void processMoveResult(MoveResult mv) {
        TicTacToeModel.CellValue previousPlayer = model.previousPlayer();

        board[mv.moveX][mv.moveY].setText(previousPlayer.getRepr());
        currTurn.setText(TURN_BUFFER + previousPlayer.other().getRepr() + TURN_LABEL);

        if (mv.resultingState == GameState.CONTINUE) {
            if (aiCheckBox.isSelected() && model.getAiTeam() == previousPlayer.other()) {
                processMoveResult(model.aiMove());
            }
            return;
        }

        if (mv.resultingState != GameState.DRAW) {
            // Show winning lines
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    board[i][j].setEnabled(false);
                }
            }

            for (Point[] line : mv.affectedLines) {
                for (Point p : line) {
                    board[p.x][p.y].setEnabled(true);
                }
            }
        }

        // If the game isn't continuing, it has ended
        String msg = switch (mv.resultingState) {
            case X_WIN -> "X won the game.";
            case O_WIN -> "O won the game.";
            case DRAW -> "The game is a draw.";
            case CONTINUE -> throw new IllegalStateException(); // The game didn't end if it's continuing
        };
        currTurn.setText(TURN_BUFFER + msg);
        aiCheckBox.setEnabled(true);
        save.setEnabled(false);
        restore.setEnabled(false);
    }

    // listeners
    private class MoveListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JSquare s = (JSquare) e.getSource();

            MoveResult moveResult;

            try {
                moveResult = model.makeMove(s.p);
            } catch (IllegalArgumentException ex) {
                // No move is performed. This catch will be tripped often,
                // for example if the player clicks on an already claimed button.
                return;
            }

            processMoveResult(moveResult);
        }
    }

    private class NewGameListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            model.newGame();

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    board[x][y].setText("");
                    board[x][y].setEnabled(true);
                }
            }

            model.startGame(aiCheckBox.isSelected() ? COMPUTER : HUMAN);
            aiCheckBox.setEnabled(false);
            save.setEnabled(true);
            restore.setEnabled(true);

            currTurn.setText(TURN_BUFFER + 'X' + TURN_LABEL);

            if (model.getAiTeam() == TicTacToeModel.CellValue.X) {
                processMoveResult(model.aiMove());
            }
        }
    }

    private static class SaveListener implements ActionListener {
        private final TicTacToeGUI tttg;

        public SaveListener(TicTacToeGUI tttg) {
            this.tttg = tttg;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("save.bin"))) {
                oos.writeObject(tttg);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(tttg, "Save failed!", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                s.writeObject(board[x][y]);
            }
        }

        s.writeObject(aiCheckBox.isSelected());
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        board = new JSquare[3][3];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                board[x][y] = (JSquare) s.readObject();
            }
        }

        currTurn = new JLabel(TURN_BUFFER + model.previousPlayer().other().getRepr() + TURN_LABEL);
    }
}
