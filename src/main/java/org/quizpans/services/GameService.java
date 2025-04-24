package org.quizpans.services;

import org.quizpans.config.DatabaseConfig;
import org.quizpans.utils.SynonymManager;
import org.quizpans.utils.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.sql.*;
import java.util.*;

public class GameService {
    private final Map<String, Integer> answers = new LinkedHashMap<>();
    private final Map<String, Integer> pointsMap = new HashMap<>();
    private final Map<String, String> synonymMap = new HashMap<>();
    private final String category;
    private String currentQuestion;

    public GameService(String category) {
        this.category = category;
        loadQuestion();
    }

    public void loadQuestion() {
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
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
        synonymMap.clear();
        for (int i = 1; i <= 6; i++) {
            String answer = rs.getString("odpowiedz" + i);
            if (answer != null) {
                int points = rs.getInt("punkty" + i);
                String baseForm = TextNormalizer.normalizeToBaseForm(answer);
                answers.put(baseForm, 7 - i);
                pointsMap.put(baseForm, points);
                loadSynonyms(answer, baseForm);
            }
        }
    }

    private void loadSynonyms(String originalAnswer, String baseForm) {
        if (!synonymMap.containsKey(baseForm)) {
            List<String> synonyms = SynonymManager.findSynonymsFor(originalAnswer);
            synonyms.forEach(syn -> {
                String processedSyn = TextNormalizer.normalizeToBaseForm(syn);
                if (!processedSyn.equals(baseForm)) {
                    synonymMap.put(processedSyn, baseForm);
                }
            });
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        String processedInput = TextNormalizer.normalizeToBaseForm(userAnswer);
        Optional<String> directMatch = answers.keySet().stream()
                .filter(answer -> isMatch(processedInput, answer))
                .findFirst();
        if (directMatch.isPresent()) {
            return directMatch;
        }
        String synonymMatch = synonymMap.get(processedInput);
        return Optional.ofNullable(synonymMatch);
    }

    private boolean isMatch(String userInput, String correctAnswer) {
        return userInput.equals(correctAnswer) || new LevenshteinDistance().apply(userInput, correctAnswer) <= 2;
    }

    public int getAnswerPosition(String answer) {
        return 6 - answers.get(answer);
    }

    public int getPoints(String answer) {
        return pointsMap.get(answer);
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public String getCategory() {
        return category;
    }
}