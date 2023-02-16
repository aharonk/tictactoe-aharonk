package edu.touro.cs.mcon364.model;

import edu.touro.cs.mcon364.shared.IntPair;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class ModelSerializationTest {
    @org.junit.jupiter.api.Test
    void testSave() {
        TicTacToeModel model = new TicTacToeModel();
        model.startGame(TicTacToeModel.GameType.HUMAN);

        for (int i = 0; i < 3; i++) {
            model.makeMove(new IntPair(0, i));
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("testSave.bin"))) {
            oos.writeObject(model);

            BufferedReader br = new BufferedReader(new FileReader("testSave.bin"));
            assertNotNull(br.readLine());
            br.close();
        } catch (IOException ex) {
            assert false;
        }
    }

    @org.junit.jupiter.api.Test
    void testLoad() {
        TicTacToeModel model = new TicTacToeModel();
        model.startGame(TicTacToeModel.GameType.HUMAN);

        for (int i = 0; i < 3; i++) {
            model.makeMove(new IntPair(0, i));
        }

        model.makeMove(new IntPair(1, 0));
        TicTacToeModel.CellValue prevPlayer = model.previousPlayer();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("testSave.bin"))) {
            model = (TicTacToeModel) ois.readObject();

            assertNotEquals(prevPlayer, model.previousPlayer());
            TicTacToeModel.CellValue[][] board = model.getBoard();

            for (int i = 0; i < 3; i++) {
                assertNotEquals(TicTacToeModel.CellValue.NONE, board[0][i]);
            }

            assertEquals(TicTacToeModel.CellValue.NONE, board[1][0]);

        } catch (IOException | ClassNotFoundException e) {
            assert false;
        }
    }
}
