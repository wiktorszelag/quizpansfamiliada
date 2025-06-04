package org.quizpans.services;

import org.quizpans.config.DatabaseConfig;
import org.quizpans.utils.SynonymManager;
import org.quizpans.utils.TextNormalizer;
import org.quizpans.utils.UsedQuestionsLogger;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GameService {
    private final Map<String, Integer> answers = new LinkedHashMap<>();
    private final Map<String, Integer> pointsMap = new HashMap<>();
    private final Map<String, String> synonymMap = new HashMap<>();
    private final Map<String, String> baseFormToOriginalMap = new HashMap<>();
    private final Map<String, Set<String>> answerKeyToCombinedKeywords = new HashMap<>();
    private final String category;
    private String currentQuestion;
    private int currentQuestionId = -1;

    private static final int N_GRAM_SIZE = 3;
    private static final int MIN_WORDS_FOR_KEYWORD_LOGIC = 2;
    private static final double MIN_KEYWORD_QUALITY_THRESHOLD = 0.35;

    private static final double WEIGHT_LEVENSHTEIN_SIMILARITY = 0.25;
    private static final double WEIGHT_JARO_WINKLER = 0.15;
    private static final double WEIGHT_JACCARD_TRIGRAM_STRING = 0.0;
    private static final double WEIGHT_JACCARD_TOKEN_SET = 0.25;
    private static final double WEIGHT_KEYWORD_SCORE = 0.35;

    private static final double MIN_ACCEPTABLE_COMBINED_SCORE = 0.58;
    private static final double MIN_ACCEPTABLE_PHRASE_SCORE = 0.60;
    private static final double FALLBACK_SINGLE_WORD_JARO_WINKLER_THRESHOLD = 0.85;
    private static final double STRONG_PARTIAL_KEYWORD_CONFIDENCE = 0.75;
    private static final double MIN_COVERAGE_FOR_STRONG_PARTIAL = 0.40;


    public GameService(String category) {
        this.category = category;
        try {
            loadQuestion();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static Set<String> getCharacterNGrams(String text, int n) {
        Set<String> nGrams = new HashSet<>();
        if (text == null || text.length() < n) return nGrams;
        for (int i = 0; i <= text.length() - n; i++) nGrams.add(text.substring(i, i + n));
        return nGrams;
    }

    private static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1 == null || set2 == null) return 0.0;
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        if (union.isEmpty()) return 1.0;
        return (double) intersection.size() / union.size();
    }

    public void loadQuestion() {
        answers.clear();
        pointsMap.clear();
        synonymMap.clear();
        baseFormToOriginalMap.clear();
        answerKeyToCombinedKeywords.clear();
        currentQuestion = null;
        currentQuestionId = -1;

        Set<Integer> usedIds = Collections.emptySet();
        try {
            usedIds = UsedQuestionsLogger.loadUsedQuestionIdsFromFile();
        } catch (IOException e) {
            System.err.println("Nie udało się wczytać użytych ID pytań z pliku: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword())) {
            boolean questionLoaded = tryLoadQuestion(conn, usedIds, true);

            if (!questionLoaded) {
                String categoryInfo = (this.category == null) ? "wszystkich kategorii (MIX)" : "kategorii: " + this.category;
                System.out.println("Nie znaleziono nowych pytań dla " + categoryInfo + ". Próba wylosowania spośród wszystkich pytań dla wybranego zakresu.");
                questionLoaded = tryLoadQuestion(conn, Collections.emptySet(), false);
                if (questionLoaded) {
                    System.out.println("Wylosowano pytanie spośród wszystkich (ignorując listę użytych) dla zakresu: " + categoryInfo + ", ponieważ pula nowych pytań mogła się wyczerpać.");
                }
            }

            if (!questionLoaded) {
                currentQuestion = null;
                String errorCategoryInfo = (this.category == null) ? "żadnej kategorii (MIX)" : "kategorii: " + this.category;
                System.err.println("Krytyczny błąd: Brak jakichkolwiek pytań dla " + errorCategoryInfo);
                throw new RuntimeException("Brak pytań dla " + errorCategoryInfo);
            }

        } catch (SQLException e) {
            currentQuestion = null;
            System.err.println("Błąd SQL podczas ładowania pytania: " + e.getMessage());
            throw new RuntimeException("Błąd SQL podczas ładowania pytania.", e);
        } catch (Exception e) {
            currentQuestion = null;
            System.err.println("Inny błąd podczas ładowania pytania: " + e.getMessage());
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw new RuntimeException("Inny błąd podczas ładowania pytania.", e);
        }
    }

    private boolean tryLoadQuestion(Connection conn, Set<Integer> idsToExclude, boolean excludeModeActive) throws SQLException {
        List<Integer> availableQuestionIds = new ArrayList<>();
        StringBuilder fetchIdsSqlBuilder = new StringBuilder("SELECT id FROM Pytania");
        List<Object> paramsForFetchIds = new ArrayList<>();
        boolean hasWhereClause = false;

        if (this.category != null) { // Jeśli kategoria to MIX, this.category będzie null
            fetchIdsSqlBuilder.append(" WHERE kategoria = ?");
            paramsForFetchIds.add(this.category);
            hasWhereClause = true;
        }

        if (excludeModeActive && !idsToExclude.isEmpty()) {
            if (!hasWhereClause) {
                fetchIdsSqlBuilder.append(" WHERE");
            } else {
                fetchIdsSqlBuilder.append(" AND");
            }
            String placeholders = String.join(",", Collections.nCopies(idsToExclude.size(), "?"));
            fetchIdsSqlBuilder.append(" id NOT IN (").append(placeholders).append(")");
            paramsForFetchIds.addAll(idsToExclude);
        }

        try (PreparedStatement pstmtIds = conn.prepareStatement(fetchIdsSqlBuilder.toString())) {
            for (int i = 0; i < paramsForFetchIds.size(); i++) {
                pstmtIds.setObject(i + 1, paramsForFetchIds.get(i));
            }
            try (ResultSet rsIds = pstmtIds.executeQuery()) {
                while (rsIds.next()) {
                    availableQuestionIds.add(rsIds.getInt("id"));
                }
            }
        }

        if (availableQuestionIds.isEmpty()) {
            return false;
        }

        int randomId = availableQuestionIds.get(new Random().nextInt(availableQuestionIds.size()));

        String selectSql = "SELECT * FROM Pytania WHERE id = ?";
        try (PreparedStatement pstmtSelect = conn.prepareStatement(selectSql)) {
            pstmtSelect.setInt(1, randomId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    currentQuestion = rs.getString("pytanie");
                    currentQuestionId = randomId;

                    if (excludeModeActive || !idsToExclude.contains(currentQuestionId)) {
                        UsedQuestionsLogger.addUsedQuestionId(currentQuestionId);
                    }

                    loadAnswers(rs);
                    return true;
                } else {
                    System.err.println("Błąd krytyczny: Nie znaleziono pytania o ID " + randomId + ", które powinno istnieć.");
                    return false;
                }
            }
        }
    }


    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    private void loadAnswers(ResultSet rs) throws SQLException {
        for (int i = 1; i <= 6; i++) {
            String answer = rs.getString("odpowiedz" + i);
            if (answer != null && !answer.trim().isEmpty()) {
                int points = rs.getInt("punkty" + i);
                String originalAnswerTrimmed = answer.trim();
                String simpleKey = originalAnswerTrimmed.toLowerCase();

                if (simpleKey.isEmpty()) continue;

                answers.put(simpleKey, 7 - i);
                pointsMap.put(simpleKey, points);
                baseFormToOriginalMap.put(simpleKey, originalAnswerTrimmed);

                List<String> answerTokens = TextNormalizer.getLemmatizedTokens(originalAnswerTrimmed, true);
                if (answerTokens.size() >= MIN_WORDS_FOR_KEYWORD_LOGIC) {
                    answerKeyToCombinedKeywords.putIfAbsent(simpleKey, new HashSet<>());
                    answerKeyToCombinedKeywords.get(simpleKey).addAll(answerTokens);
                }
            }
        }
        for (Map.Entry<String, String> entry : baseFormToOriginalMap.entrySet()) {
            loadSynonyms(entry.getValue(), entry.getKey());
        }
    }

    private void loadSynonyms(String originalAnswerText, String originalAnswerSimpleKey) {
        List<String> rawSynonyms = SynonymManager.findSynonymsFor(originalAnswerText);
        for (String syn : rawSynonyms) {
            String trimmedSyn = syn.trim();
            if (trimmedSyn.isEmpty()) continue;

            String simpleSynonymKey = trimmedSyn.toLowerCase();
            if (simpleSynonymKey.isEmpty() || simpleSynonymKey.equals(originalAnswerSimpleKey)) {
                continue;
            }

            boolean isSynonymEqualToAnotherMainAnswer = false;
            if (baseFormToOriginalMap.containsKey(simpleSynonymKey) && !simpleSynonymKey.equals(originalAnswerSimpleKey)) {
                isSynonymEqualToAnotherMainAnswer = true;
            }

            if (!isSynonymEqualToAnotherMainAnswer) {
                if (!synonymMap.containsKey(simpleSynonymKey)) {
                    synonymMap.put(simpleSynonymKey, originalAnswerSimpleKey);
                }

                List<String> synonymTokens = TextNormalizer.getLemmatizedTokens(trimmedSyn, true);
                if (synonymTokens.size() >= MIN_WORDS_FOR_KEYWORD_LOGIC) {
                    if (answerKeyToCombinedKeywords.containsKey(originalAnswerSimpleKey)) {
                        answerKeyToCombinedKeywords.get(originalAnswerSimpleKey).addAll(synonymTokens);
                    } else {
                        answerKeyToCombinedKeywords.putIfAbsent(originalAnswerSimpleKey, new HashSet<>());
                        answerKeyToCombinedKeywords.get(originalAnswerSimpleKey).addAll(synonymTokens);
                    }
                }
            }
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) return Optional.empty();

        String processedUserInputSimple = userAnswer.trim().toLowerCase();

        if (processedUserInputSimple.isEmpty()) return Optional.empty();

        if (answers.containsKey(processedUserInputSimple)) {
            return Optional.of(processedUserInputSimple);
        }

        String synonymMapsToKey = synonymMap.get(processedUserInputSimple);
        if (synonymMapsToKey != null && answers.containsKey(synonymMapsToKey)) {
            return Optional.of(synonymMapsToKey);
        }

        String originalUserAnswerForComparison = userAnswer;
        String processedInputDeepNormalized = TextNormalizer.normalizeToBaseForm(originalUserAnswerForComparison);
        List<String> processedUserTokensDeepNormalized = TextNormalizer.getLemmatizedTokens(originalUserAnswerForComparison, true);


        String bestFuzzyMatchKey = null;
        double highestOverallConfidence = 0.0;

        JaroWinklerSimilarity jwSimilarity = new JaroWinklerSimilarity();
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        Set<String> inputNGramsSingleString = getCharacterNGrams(processedInputDeepNormalized, N_GRAM_SIZE);
        int inputSingleStringLength = processedInputDeepNormalized.length();

        for (Map.Entry<String, String> correctAnswerEntry : baseFormToOriginalMap.entrySet()) {
            String correctAnswerSimpleKey = correctAnswerEntry.getKey();
            String originalCorrectAnswerText = correctAnswerEntry.getValue();

            String correctDeepNormalized = TextNormalizer.normalizeToBaseForm(originalCorrectAnswerText);
            if (correctDeepNormalized.isEmpty()) continue;


            List<String> correctFormattedTokens = TextNormalizer.getLemmatizedTokens(originalCorrectAnswerText, true);

            double currentCombinedConfidence = 0.0;
            double keywordScoreContribution = 0.0;
            boolean strongPartialKeywordMatch = false;

            if (answerKeyToCombinedKeywords.containsKey(correctAnswerSimpleKey)) {
                Set<String> expectedKeywords = answerKeyToCombinedKeywords.get(correctAnswerSimpleKey);
                if (expectedKeywords != null && !expectedKeywords.isEmpty() &&
                        processedUserTokensDeepNormalized != null && !processedUserTokensDeepNormalized.isEmpty()) {

                    Set<String> userKeywordSet = new HashSet<>(processedUserTokensDeepNormalized);
                    double tempKeywordScore = 0.0;

                    if (expectedKeywords.containsAll(userKeywordSet) && !userKeywordSet.isEmpty()) {
                        double coverage = (double) userKeywordSet.size() / expectedKeywords.size();
                        tempKeywordScore = 0.70 + (0.30 * coverage);
                        if (coverage >= MIN_COVERAGE_FOR_STRONG_PARTIAL && tempKeywordScore >= MIN_KEYWORD_QUALITY_THRESHOLD) {
                            strongPartialKeywordMatch = true;
                        }
                    } else {
                        Set<String> matchedKeywords = new HashSet<>(userKeywordSet);
                        matchedKeywords.retainAll(expectedKeywords);
                        if (!matchedKeywords.isEmpty()) {
                            tempKeywordScore = calculateJaccardSimilarity(userKeywordSet, expectedKeywords);
                        }
                    }

                    if (tempKeywordScore >= MIN_KEYWORD_QUALITY_THRESHOLD) {
                        keywordScoreContribution = tempKeywordScore;
                    }
                }
            }

            if (strongPartialKeywordMatch) {
                currentCombinedConfidence = STRONG_PARTIAL_KEYWORD_CONFIDENCE;
            } else {
                int ld = levenshteinDistance.apply(processedInputDeepNormalized, correctDeepNormalized);
                int adaptiveLevThreshold = calculateAdaptiveLevenshteinThreshold(correctDeepNormalized.length());
                if (ld <= adaptiveLevThreshold) {
                    double levSimString = 0.0;
                    int maxLengthString = Math.max(inputSingleStringLength, correctDeepNormalized.length());
                    if (maxLengthString > 0) {
                        levSimString = 1.0 - ((double) ld / maxLengthString);
                    } else if (ld == 0) {
                        levSimString = 1.0;
                    }

                    double jwScoreString = (inputSingleStringLength >= 1 && correctDeepNormalized.length() >= 1) ?
                            jwSimilarity.apply(processedInputDeepNormalized, correctDeepNormalized) : 0.0;

                    Set<String> correctNGramsSingleStringLocal = getCharacterNGrams(correctDeepNormalized, N_GRAM_SIZE);
                    double jaccardScoreTrigramString = calculateJaccardSimilarity(inputNGramsSingleString, correctNGramsSingleStringLocal);

                    double tokenSetJaccardScore = 0.0;
                    if ((processedUserTokensDeepNormalized != null && !processedUserTokensDeepNormalized.isEmpty()) || (correctFormattedTokens != null && !correctFormattedTokens.isEmpty())) {
                        tokenSetJaccardScore = calculateJaccardSimilarity(
                                processedUserTokensDeepNormalized != null ? new HashSet<>(processedUserTokensDeepNormalized) : new HashSet<>(),
                                correctFormattedTokens != null ? new HashSet<>(correctFormattedTokens) : new HashSet<>()
                        );
                    }

                    currentCombinedConfidence = (WEIGHT_LEVENSHTEIN_SIMILARITY * levSimString) +
                            (WEIGHT_JARO_WINKLER * jwScoreString) +
                            (WEIGHT_JACCARD_TRIGRAM_STRING * jaccardScoreTrigramString) +
                            (WEIGHT_JACCARD_TOKEN_SET * tokenSetJaccardScore) +
                            (WEIGHT_KEYWORD_SCORE * keywordScoreContribution);
                }
            }

            if (currentCombinedConfidence > highestOverallConfidence) {
                highestOverallConfidence = currentCombinedConfidence;
                bestFuzzyMatchKey = correctAnswerSimpleKey;
            }
        }

        double effectiveThreshold = MIN_ACCEPTABLE_COMBINED_SCORE;
        boolean isInputUserPhrase = originalUserAnswerForComparison.trim().contains(" ");
        boolean isBestMatchConsideredPhrase = false;
        if (bestFuzzyMatchKey != null && answerKeyToCombinedKeywords.containsKey(bestFuzzyMatchKey)) {
            Set<String> keywordsForBestMatch = answerKeyToCombinedKeywords.get(bestFuzzyMatchKey);
            if (keywordsForBestMatch != null && !keywordsForBestMatch.isEmpty()) {
                isBestMatchConsideredPhrase = true;
            }
        }
        if (isInputUserPhrase || isBestMatchConsideredPhrase) {
            effectiveThreshold = MIN_ACCEPTABLE_PHRASE_SCORE;
        }

        if (bestFuzzyMatchKey != null && highestOverallConfidence >= effectiveThreshold) {
            return Optional.of(bestFuzzyMatchKey);
        }

        if (processedUserInputSimple.split("\\s+").length == 1 && (bestFuzzyMatchKey == null || highestOverallConfidence < effectiveThreshold)) {
            for (Map.Entry<String, String> correctAnswerEntry : baseFormToOriginalMap.entrySet()) {
                String correctAnswerSimpleKey = correctAnswerEntry.getKey();
                if (correctAnswerSimpleKey.split("\\s+").length == 1) {
                    double jwFallback = jwSimilarity.apply(processedUserInputSimple, correctAnswerSimpleKey);
                    if (jwFallback >= FALLBACK_SINGLE_WORD_JARO_WINKLER_THRESHOLD) {
                        return Optional.of(correctAnswerSimpleKey);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private int calculateAdaptiveLevenshteinThreshold(int correctAnswerLength) {
        if (correctAnswerLength <= 1) return 0;
        if (correctAnswerLength <= 3) return 1;
        if (correctAnswerLength <= 5) return 1;
        if (correctAnswerLength <= 7) return 2;
        if (correctAnswerLength <= 10) return 3;
        return Math.min(4, (int) Math.ceil(correctAnswerLength * 0.30));
    }

    public int getAnswerPosition(String answerKey) { return 6 - answers.getOrDefault(answerKey, 0); }
    public int getPoints(String answerKey) { return pointsMap.getOrDefault(answerKey, 0); }
    public String getOriginalAnswer(String key) { return baseFormToOriginalMap.getOrDefault(key, key); }
    public String getCurrentQuestion() { return currentQuestion; }
    public int getCurrentQuestionId() { return currentQuestionId; }
    public String getCategory() { return category; }
    public int getTotalAnswersCount() {
        return (int) answers.keySet().stream().filter(key -> pointsMap.getOrDefault(key, 0) > 0).count();
    }
    public void setCurrentQuestionToNull() { currentQuestion = null; currentQuestionId = -1; answers.clear(); pointsMap.clear(); synonymMap.clear(); baseFormToOriginalMap.clear(); answerKeyToCombinedKeywords.clear();}

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
        if (currentQuestion == null || answers.isEmpty()) return allAnswersData;

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(answers.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int displayIndex = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String simpleKey = entry.getKey();
            String originalText = baseFormToOriginalMap.getOrDefault(simpleKey, simpleKey);
            int points = pointsMap.getOrDefault(simpleKey, 0);
            allAnswersData.add(new AnswerData(simpleKey, originalText, points, displayIndex++));
        }
        return allAnswersData;
    }

    public static Set<String> getAvailableCategories() {
        Set<String> categories = new TreeSet<>();
        String sql = "SELECT DISTINCT kategoria FROM Pytania ORDER BY kategoria ASC";
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String categoryName = rs.getString("kategoria");
                if(categoryName != null && !categoryName.trim().isEmpty()){
                    categories.add(categoryName.trim());
                }
            }
        } catch (SQLException e) { System.err.println("Błąd SQL podczas pobierania kategorii: " + e.getMessage());
        } catch (Exception e) { System.err.println("Nieoczekiwany błąd podczas pobierania kategorii: " + e.getMessage()); }
        return categories;
    }
}