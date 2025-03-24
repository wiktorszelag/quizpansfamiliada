package quizpans.gui;

import quizpans.services.GameService;
import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private final GameService gameService;
    private final JLabel[] answerLabels = new JLabel[6];
    private final JTextField answerField = new JTextField(20);
    private final JLabel questionLabel = new JLabel(" ", SwingConstants.CENTER);

    public GameFrame(String selectedCategory) {
        this.gameService = new GameService(selectedCategory);
        initializeFrame();
        initUI();
        setupAnswerField();
    }

    private void initializeFrame() {
        setTitle("Familiada");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel pytania
        JPanel questionPanel = new JPanel(new BorderLayout());
        questionLabel.setFont(new Font("Arial", Font.BOLD, 28));
        questionLabel.setText("Pytanie: " + gameService.getCurrentQuestion());
        questionPanel.add(questionLabel, BorderLayout.CENTER);

        // Panel odpowiedzi
        JPanel answersPanel = new JPanel(new GridLayout(6, 1));
        for (int i = 0; i < 6; i++) {
            answerLabels[i] = createAnswerLabel(i + 1);
            answersPanel.add(answerLabels[i]);
        }

        // Panel wejściowy
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(answerField, BorderLayout.CENTER);

        mainPanel.add(questionPanel, BorderLayout.NORTH);
        mainPanel.add(answersPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JLabel createAnswerLabel(int number) {
        JLabel label = new JLabel(number + ". ****************************************", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        return label;
    }

    private void setupAnswerField() {
        answerField.addActionListener(e -> processAnswer());
    }

    private void processAnswer() {
        String userAnswer = answerField.getText().trim();
        answerField.setText("");

        if (userAnswer.isEmpty()) {
            showWarning("Minus szansa", "Błąd");
            return;
        }

        gameService.checkAnswer(userAnswer).ifPresentOrElse(
                correctAnswer -> updateUI(correctAnswer),
                () -> showWarning("Minus szansa", "Zła odpowiedź")
        );
    }

    private void updateUI(String correctAnswer) {
        int position = gameService.getAnswerPosition(correctAnswer);
        int points = gameService.getPoints(correctAnswer);
        answerLabels[position].setText((position + 1) + ". " + correctAnswer + " (" + points + " pkt)");
    }

    private void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }
}