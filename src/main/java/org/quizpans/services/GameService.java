package org.quizpans.services;

import org.quizpans.config.DatabaseConfig;
import org.quizpans.utils.SynonymManager;
import org.quizpans.utils.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.sql.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameService {
    private final Map<String, Integer> answers = new LinkedHashMap<>();
    private final Map<String, Integer> pointsMap = new HashMap<>();
    private final Map<String, String> synonymMap = new HashMap<>();
    private final Map<String, String> baseFormToOriginalMap = new HashMap<>();
    private final String category;
    private String currentQuestion;

    public GameService(String category) {
        this.category = category;
        loadQuestion();
    }

    public void loadQuestion() {
        answers.clear();
        pointsMap.clear();
        synonymMap.clear();
        baseFormToOriginalMap.clear();
        currentQuestion = null;

        String sql = "SELECT * FROM `" + category + "` ORDER BY RAND() LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                currentQuestion = rs.getString("pytanie");
                loadAnswers(rs);
            } else {
                currentQuestion = null;
                throw new RuntimeException("Brak pytań w bazie danych dla kategorii: " + category);
            }
        } catch (SQLException e) {
            currentQuestion = null;
            throw new RuntimeException("Błąd SQL podczas ładowania pytania: " + e.getMessage(), e);
        } catch (Exception e) {
            currentQuestion = null;
            throw new RuntimeException("Inny błąd podczas ładowania pytania: " + e.getMessage(), e);
        }
    }

    private void loadAnswers(ResultSet rs) throws SQLException {
        for (int i = 1; i <= 6; i++) {
            String answer = rs.getString("odpowiedz" + i);
            if (answer != null && !answer.trim().isEmpty()) {
                int points = rs.getInt("punkty" + i);
                String baseForm = TextNormalizer.normalizeToBaseForm(answer);
                answers.put(baseForm, 7 - i);
                pointsMap.put(baseForm, points);
                baseFormToOriginalMap.put(baseForm, answer.trim());
                loadSynonyms(answer.trim(), baseForm);
            }
        }
    }

    private void loadSynonyms(String originalAnswer, String baseForm) {
        if (!synonymMap.containsValue(baseForm)) {
            List<String> synonyms = SynonymManager.findSynonymsFor(originalAnswer);
            synonyms.forEach(syn -> {
                String processedSyn = TextNormalizer.normalizeToBaseForm(syn);
                if (!processedSyn.equals(baseForm) && !synonymMap.containsKey(processedSyn)) {
                    synonymMap.put(processedSyn, baseForm);
                }
            });
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        String processedInput = TextNormalizer.normalizeToBaseForm(userAnswer);

        if (answers.containsKey(processedInput)) {
            return Optional.of(processedInput);
        }

        if (synonymMap.containsKey(processedInput)) {
            return Optional.of(synonymMap.get(processedInput));
        }

        for (String correctAnswerBaseForm : answers.keySet()) {
            if (isMatchWithLevenshtein(processedInput, correctAnswerBaseForm)) {
                return Optional.of(correctAnswerBaseForm);
            }
        }
        return Optional.empty();
    }

    private boolean isMatchWithLevenshtein(String userInput, String correctAnswer) {
        int threshold = Math.min(2, correctAnswer.length() / 4);
        return new LevenshteinDistance().apply(userInput, correctAnswer) <= threshold;
    }

    public int getAnswerPosition(String answerBaseForm) {
        return 6 - answers.getOrDefault(answerBaseForm, 6);
    }

    public int getPoints(String answerBaseForm) {
        return pointsMap.getOrDefault(answerBaseForm, 0);
    }

    public String getOriginalAnswer(String baseForm) {
        return baseFormToOriginalMap.getOrDefault(baseForm, baseForm);
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalAnswersCount() {
        return answers.size();
    }

    public void setCurrentQuestionToNull() {
        this.currentQuestion = null;
        this.answers.clear();
        this.pointsMap.clear();
        this.synonymMap.clear();
        this.baseFormToOriginalMap.clear();
    }

    public static class AnswerData {
        public final String baseForm;
        public final String originalText;
        public final int points;
        public final int displayOrderIndex;

        public AnswerData(String baseForm, String originalText, int points, int displayOrderIndex) {
            this.baseForm = baseForm;
            this.originalText = originalText;
            this.points = points;
            this.displayOrderIndex = displayOrderIndex;
        }

        public String getBaseForm() { return baseForm; }
        public String getOriginalText() { return originalText; }
        public int getPoints() { return points; }
        public int getDisplayOrderIndex() { return displayOrderIndex; }
    }

    public List<AnswerData> getAllAnswersForCurrentQuestion() {
        List<AnswerData> allAnswersData = new ArrayList<>();
        if (answers.isEmpty()) {
            return allAnswersData;
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(answers.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int displayIndex = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String baseForm = entry.getKey();
            String originalText = baseFormToOriginalMap.getOrDefault(baseForm, "Brak tekstu");
            int points = pointsMap.getOrDefault(baseForm, 0);
            allAnswersData.add(new AnswerData(baseForm, originalText, points, displayIndex++));
        }
        return allAnswersData;
    }
}