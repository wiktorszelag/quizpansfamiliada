package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static JPanel mainPanel, settingsPanel;
    private static JFrame teamSetupFrame;
    private static JComboBox<Integer> teamSizeComboBox;
    private static JComboBox<String> categoryComboBox;
    private static JSpinner answerTimeSpinner;

    public static void main(String[] args) {
        teamSetupFrame = new JFrame("Ustawienia druzyn");
        teamSetupFrame.setSize(600, 400);
        teamSetupFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        teamSetupFrame.setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JTextField team1Field = new JTextField(20);
        JTextField team2Field = new JTextField(20);
        JPanel team1MembersPanel = createTeamPanel("Czlonkowie druzyny 1");
        JPanel team2MembersPanel = createTeamPanel("Czlonkowie druzyny 2");

        mainPanel.add(new JLabel("Nazwa druzyny 1:"));
        mainPanel.add(team1Field);
        mainPanel.add(team1MembersPanel);
        mainPanel.add(new JLabel("Nazwa druzyny 2:"));
        mainPanel.add(team2Field);
        mainPanel.add(team2MembersPanel);

        JButton startButton = new JButton("Rozpocznij gre");
        JButton settingsButton = new JButton("Ustawienia");
        mainPanel.add(startButton);
        mainPanel.add(settingsButton);

        teamSetupFrame.add(mainPanel);
        teamSetupFrame.setVisible(true);

        generujPolaGraczy(1, team1MembersPanel, team2MembersPanel);

        startButton.addActionListener(e -> rozpocznijGre(team1Field, team2Field, team1MembersPanel, team2MembersPanel));
        settingsButton.addActionListener(e -> pokazUstawienia());

        settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));

        teamSizeComboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        categoryComboBox = new JComboBox<>(new String[]{"Testowa1", "Testowa2"});
        answerTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 120, 5));
        JButton backButton = new JButton("Wroc");

        settingsPanel.add(new JLabel("Liczba osobb w druzynie:"));
        settingsPanel.add(teamSizeComboBox);
        settingsPanel.add(new JLabel("Wybierz kategorie:"));
        settingsPanel.add(categoryComboBox);
        settingsPanel.add(new JLabel("Czas na odpowiedz (sekundy):"));
        settingsPanel.add(answerTimeSpinner);
        settingsPanel.add(backButton);

        backButton.addActionListener(e -> {
            pokazGlownyPanel();
            generujPolaGraczy((int) teamSizeComboBox.getSelectedItem(), team1MembersPanel, team2MembersPanel);
        });
    }

    private static JPanel createTeamPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private static void rozpocznijGre(JTextField team1Field, JTextField team2Field, JPanel team1MembersPanel, JPanel team2MembersPanel) {
        String team1Name = team1Field.getText().trim();
        String team2Name = team2Field.getText().trim();

        if (team1Name.isEmpty() || team2Name.isEmpty()) {
            JOptionPane.showMessageDialog(teamSetupFrame, "Prosze wypelnic nazwy druzyn.", "Blad", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> team1Members = pobierzCzlonkow(team1MembersPanel);
        List<String> team2Members = pobierzCzlonkow(team2MembersPanel);

        if (team1Members == null || team2Members == null) return;

        teamSetupFrame.dispose();
        new Gameplay((String) categoryComboBox.getSelectedItem()).setVisible(true);
    }

    private static void pokazUstawienia() {
        teamSetupFrame.setContentPane(settingsPanel);
        teamSetupFrame.revalidate();
        teamSetupFrame.repaint();
    }

    private static void pokazGlownyPanel() {
        teamSetupFrame.setContentPane(mainPanel);
        teamSetupFrame.revalidate();
        teamSetupFrame.repaint();
    }

    private static void generujPolaGraczy(int teamSize, JPanel team1MembersPanel, JPanel team2MembersPanel) {
        team1MembersPanel.removeAll();
        team2MembersPanel.removeAll();
        for (int i = 0; i < teamSize; i++) {
            team1MembersPanel.add(new JLabel("Gracz " + (i + 1) + ":"));
            team1MembersPanel.add(new JTextField(20));
            team2MembersPanel.add(new JLabel("Gracz " + (i + 1) + ":"));
            team2MembersPanel.add(new JTextField(20));
        }
        team1MembersPanel.revalidate();
        team1MembersPanel.repaint();
        team2MembersPanel.revalidate();
        team2MembersPanel.repaint();
    }

    private static List<String> pobierzCzlonkow(JPanel membersPanel) {
        List<String> members = new ArrayList<>();
        for (Component component : membersPanel.getComponents()) {
            if (component instanceof JTextField) {
                String text = ((JTextField) component).getText().trim();
                if (text.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Wprowadz nazwy wszystkich graczy", "Blad", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                members.add(text);
            }
        }
        return members;
    }
}