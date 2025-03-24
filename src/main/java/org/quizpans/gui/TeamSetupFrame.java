package quizpans.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TeamSetupFrame extends JFrame {
    private JPanel mainPanel;
    private JPanel settingsPanel;
    private JComboBox<Integer> teamSizeComboBox;
    private JComboBox<String> categoryComboBox;
    private JSpinner answerTimeSpinner;
    private JPanel team1MembersPanel;
    private JPanel team2MembersPanel;
    private JTextField team1Field;
    private JTextField team2Field;

    public TeamSetupFrame() {
        initializeFrame();
        createMainPanel();
        createSettingsPanel();
        showMainPanel();
    }

    private void initializeFrame() {
        setTitle("Ustawienia drużyn");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void createMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel dla drużyny 1
        JPanel team1Panel = new JPanel(new BorderLayout());
        team1Field = new JTextField(20);
        team1MembersPanel = createTeamPanel("Członkowie drużyny 1");
        team1Panel.add(new JLabel("Nazwa drużyny 1:"), BorderLayout.NORTH);
        team1Panel.add(team1Field, BorderLayout.CENTER);
        team1Panel.add(team1MembersPanel, BorderLayout.SOUTH);

        // Panel dla drużyny 2
        JPanel team2Panel = new JPanel(new BorderLayout());
        team2Field = new JTextField(20);
        team2MembersPanel = createTeamPanel("Członkowie drużyny 2");
        team2Panel.add(new JLabel("Nazwa drużyny 2:"), BorderLayout.NORTH);
        team2Panel.add(team2Field, BorderLayout.CENTER);
        team2Panel.add(team2MembersPanel, BorderLayout.SOUTH);

        // Przyciski
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton startButton = new JButton("Rozpocznij grę");
        JButton settingsButton = new JButton("Ustawienia");
        buttonPanel.add(startButton);
        buttonPanel.add(settingsButton);

        mainPanel.add(team1Panel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(team2Panel);
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);

        startButton.addActionListener(e -> startGame());
        settingsButton.addActionListener(e -> showSettingsPanel());
    }

    private void createSettingsPanel() {
        settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Ustawienia
        teamSizeComboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        categoryComboBox = new JComboBox<>(new String[]{"Testowa1", "Testowa2"});
        answerTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 120, 5));

        JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = new JButton("Wróć");
        backButton.addActionListener(e -> {
            updateTeamMembers();
            showMainPanel();
        });

        addSettingComponent("Liczba osób w drużynie:", teamSizeComboBox);
        addSettingComponent("Wybierz kategorię:", categoryComboBox);
        addSettingComponent("Czas na odpowiedź (sekundy):", answerTimeSpinner);

        settingsPanel.add(Box.createVerticalGlue());
        backButtonPanel.add(backButton);
        settingsPanel.add(backButtonPanel);
    }

    private void addSettingComponent(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);
        settingsPanel.add(panel);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private JPanel createTeamPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void updateTeamMembers() {
        int teamSize = (int) teamSizeComboBox.getSelectedItem();
        updateTeamPanel(team1MembersPanel, teamSize);
        updateTeamPanel(team2MembersPanel, teamSize);
    }

    private void updateTeamPanel(JPanel panel, int size) {
        panel.removeAll();
        for (int i = 0; i < size; i++) {
            panel.add(new JLabel("Gracz " + (i + 1) + ":"));
            panel.add(new JTextField(20));
            if (i < size - 1) {
                panel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void startGame() {
        String team1Name = team1Field.getText().trim();
        String team2Name = team2Field.getText().trim();

        if (team1Name.isEmpty() || team2Name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Proszę wypełnić nazwy drużyn", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> team1Members = getMembers(team1MembersPanel);
        List<String> team2Members = getMembers(team2MembersPanel);

        if (team1Members != null && team2Members != null) {
            dispose();
            new GameFrame((String) categoryComboBox.getSelectedItem()).setVisible(true);
        }
    }

    private List<String> getMembers(JPanel panel) {
        List<String> members = new ArrayList<>();
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JTextField) {
                String name = ((JTextField) comp).getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Wprowadź nazwy wszystkich graczy", "Błąd", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                members.add(name);
            }
        }
        return members;
    }

    private void showMainPanel() {
        getContentPane().removeAll();
        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showSettingsPanel() {
        getContentPane().removeAll();
        add(settingsPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TeamSetupFrame().setVisible(true);
        });
    }
}