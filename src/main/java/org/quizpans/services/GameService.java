package quizpans.services;

import quizpans.config.DatabaseConfig;
import quizpans.utils.NLPProcessor;
import quizpans.utils.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.sql.*;
import java.util.*;

public class GameService {
    private final Map<String, Integer> answers = new LinkedHashMap<>();
    private final Map<String, Integer> pointsMap = new HashMap<>();
    private final Set<String> foundAnswers = new LinkedHashSet<>();
    private final String category;
    private String currentQuestion;
    private final NLPProcessor nlpProcessor = new NLPProcessor();

    public GameService(String category) {
        this.category = category;
        loadQuestion();
    }

    private void loadQuestion() {
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + category + " ORDER BY RAND() LIMIT 1")) {

            if (rs.next()) {
                currentQuestion = rs.getString("pytanie");
                loadAnswers(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd ładowania pytania: " + e.getMessage());
        }
    }

    private void loadAnswers(ResultSet rs) throws SQLException {
        answers.clear();
        pointsMap.clear();
        for (int i = 1; i <= 6; i++) {
            String answer = rs.getString("odpowiedz" + i);
            if (answer != null) {
                int points = rs.getInt("punkty" + i);
                String key = answer.toLowerCase();
                answers.put(key, 7 - i);
                pointsMap.put(key, points);
            }
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        String processedInput = processInput(userAnswer);
        return answers.keySet().stream()
                .filter(answer -> isMatch(processedInput, processInput(answer)))
                .findFirst();
    }

    private String processInput(String text) {
        return String.join("", nlpProcessor.processText(
                TextNormalizer.normalize(text)
        ));
    }

    private boolean isMatch(String userInput, String correctAnswer) {
        return userInput.equals(correctAnswer) ||
                isTypoMatch(userInput, correctAnswer);
    }

    private boolean isTypoMatch(String input, String correct) {
        LevenshteinDistance ld = new LevenshteinDistance();
        return ld.apply(input, correct) <= 2;
    }

    public int getAnswerPosition(String answer) {
        return 6 - answers.get(answer.toLowerCase());
    }

    public int getPoints(String answer) {
        return pointsMap.get(answer.toLowerCase());
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }
}