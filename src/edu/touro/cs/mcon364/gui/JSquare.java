package edu.touro.cs.mcon364.gui;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class JSquare extends JButton implements Serializable {
    public Point p;
    public JSquare(Point p) {
        this.p = p;
    }
}
