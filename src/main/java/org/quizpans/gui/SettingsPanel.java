package org.quizpans.gui;

import javax.swing.*;

public class SettingsPanel extends JPanel {
    public SettingsPanel(JComboBox<Integer> teamSizeComboBox,
                         JComboBox<String> categoryComboBox,
                         JSpinner answerTimeSpinner) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initializeComponents(teamSizeComboBox, categoryComboBox, answerTimeSpinner);
    }

    private void initializeComponents(JComboBox<Integer> teamSizeComboBox,
                                      JComboBox<String> categoryComboBox,
                                      JSpinner answerTimeSpinner) {
        // Implementacja panelu ustawień
    }
}