package org.quizpans;

import org.quizpans.gui.TeamSetupFrame;
import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TeamSetupFrame frame = new TeamSetupFrame();
            frame.setVisible(true);
        });
    }
}