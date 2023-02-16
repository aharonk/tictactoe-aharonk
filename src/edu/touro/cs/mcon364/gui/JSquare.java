package edu.touro.cs.mcon364.gui;

import edu.touro.cs.mcon364.shared.IntPair;

import javax.swing.*;
import java.io.Serializable;

public class JSquare extends JButton implements Serializable {
    public IntPair p;
    public JSquare(IntPair p) {
        this.p = p;
    }
}
