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
                System.out.println("Nie znaleziono nowych pytań dla kategorii: " + category + ". Próba wylosowania spośród wszystkich pytań.");
                questionLoaded = tryLoadQuestion(conn, Collections.emptySet(), false);
                if (questionLoaded) {
                    System.out.println("Wylosowano pytanie spośród wszystkich (ignorując listę użytych), ponieważ pula nowych pytań mogła się wyczerpać.");
                }
            }

            if (!questionLoaded) {
                currentQuestion = null;
                System.err.println("Krytyczny błąd: Brak jakichkolwiek pytań dla kategorii: " + category);
                throw new RuntimeException("Brak pytań dla kategorii: " + category);
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
        String fetchIdsSqlBase = "SELECT id FROM Pytania WHERE kategoria = ?";
        List<Object> paramsForFetchIds = new ArrayList<>();
        paramsForFetchIds.add(this.category);

        if (excludeModeActive && !idsToExclude.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(idsToExclude.size(), "?"));
            fetchIdsSqlBase += " AND id NOT IN (" + placeholders + ")";
            paramsForFetchIds.addAll(idsToExclude);
        }

        try (PreparedStatement pstmtIds = conn.prepareStatement(fetchIdsSqlBase)) {
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
                String baseFormSingleString = TextNormalizer.normalizeToBaseForm(originalAnswerTrimmed);
                if (baseFormSingleString.isEmpty()) continue;

                answers.put(baseFormSingleString, 7 - i);
                pointsMap.put(baseFormSingleString, points);
                baseFormToOriginalMap.put(baseFormSingleString, originalAnswerTrimmed);

                List<String> answerTokens = TextNormalizer.getLemmatizedTokens(originalAnswerTrimmed, true);
                if (answerTokens.size() >= MIN_WORDS_FOR_KEYWORD_LOGIC) {
                    answerKeyToCombinedKeywords.putIfAbsent(baseFormSingleString, new HashSet<>());
                    answerKeyToCombinedKeywords.get(baseFormSingleString).addAll(answerTokens);
                }
            }
        }
        for (Map.Entry<String, String> entry : baseFormToOriginalMap.entrySet()) {
            loadSynonyms(entry.getValue(), entry.getKey());
        }
    }

    private void loadSynonyms(String originalAnswerText, String originalAnswerBaseKey) {
        List<String> rawSynonyms = SynonymManager.findSynonymsFor(originalAnswerText);
        for (String syn : rawSynonyms) {
            String trimmedSyn = syn.trim();
            if (trimmedSyn.isEmpty()) continue;

            String processedSynSingleString = TextNormalizer.normalizeToBaseForm(trimmedSyn);
            if (processedSynSingleString.isEmpty() || processedSynSingleString.equals(originalAnswerBaseKey)) {
                continue;
            }

            boolean isSynonymEqualToAnotherMainAnswer = false;
            if (baseFormToOriginalMap.containsKey(processedSynSingleString) && !processedSynSingleString.equals(originalAnswerBaseKey)) {
                isSynonymEqualToAnotherMainAnswer = true;
            }

            if (!isSynonymEqualToAnotherMainAnswer) {
                if (!synonymMap.containsKey(processedSynSingleString)) {
                    synonymMap.put(processedSynSingleString, originalAnswerBaseKey);
                }

                List<String> synonymTokens = TextNormalizer.getLemmatizedTokens(trimmedSyn, true);
                if (synonymTokens.size() >= MIN_WORDS_FOR_KEYWORD_LOGIC) {
                    if (answerKeyToCombinedKeywords.containsKey(originalAnswerBaseKey)) {
                        answerKeyToCombinedKeywords.get(originalAnswerBaseKey).addAll(synonymTokens);
                    } else {
                        answerKeyToCombinedKeywords.putIfAbsent(originalAnswerBaseKey, new HashSet<>());
                        answerKeyToCombinedKeywords.get(originalAnswerBaseKey).addAll(synonymTokens);
                    }
                }
            }
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) return Optional.empty();

        String originalUserAnswerForComparison = userAnswer;
        String processedInputSingleString = TextNormalizer.normalizeToBaseForm(originalUserAnswerForComparison);
        List<String> processedUserTokens = TextNormalizer.getLemmatizedTokens(originalUserAnswerForComparison, true);

        if (processedInputSingleString.isEmpty() && (processedUserTokens == null || processedUserTokens.isEmpty())) return Optional.empty();

        if (answers.containsKey(processedInputSingleString)) {
            return Optional.of(processedInputSingleString);
        }

        String synonymMatchResult = synonymMap.get(processedInputSingleString);
        if (synonymMatchResult != null && answers.containsKey(synonymMatchResult)) {
            return Optional.of(synonymMatchResult);
        }

        String bestFuzzyMatchKey = null;
        double highestOverallConfidence = 0.0;

        JaroWinklerSimilarity jwSimilarity = new JaroWinklerSimilarity();
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        Set<String> inputNGramsSingleString = getCharacterNGrams(processedInputSingleString, N_GRAM_SIZE);
        int inputSingleStringLength = processedInputSingleString.length();

        for (Map.Entry<String, String> correctAnswerEntry : baseFormToOriginalMap.entrySet()) {
            String correctAnswerKeySingleString = correctAnswerEntry.getKey();
            String originalCorrectAnswerText = correctAnswerEntry.getValue();

            if (correctAnswerKeySingleString.isEmpty()) continue;

            List<String> correctFormattedTokens = TextNormalizer.getLemmatizedTokens(originalCorrectAnswerText, true);

            double currentCombinedConfidence = 0.0;
            double keywordScoreContribution = 0.0;
            boolean strongPartialKeywordMatch = false;

            if (answerKeyToCombinedKeywords.containsKey(correctAnswerKeySingleString)) {
                Set<String> expectedKeywords = answerKeyToCombinedKeywords.get(correctAnswerKeySingleString);
                if (expectedKeywords != null && !expectedKeywords.isEmpty() &&
                        processedUserTokens != null && !processedUserTokens.isEmpty()) {

                    Set<String> userKeywordSet = new HashSet<>(processedUserTokens);
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
                int ld = levenshteinDistance.apply(processedInputSingleString, correctAnswerKeySingleString);
                int adaptiveLevThreshold = calculateAdaptiveLevenshteinThreshold(correctAnswerKeySingleString.length());
                if (ld <= adaptiveLevThreshold) {
                    double levSimString = 0.0;
                    int maxLengthString = Math.max(inputSingleStringLength, correctAnswerKeySingleString.length());
                    if (maxLengthString > 0) {
                        levSimString = 1.0 - ((double) ld / maxLengthString);
                    } else if (ld == 0) {
                        levSimString = 1.0;
                    }

                    double jwScoreString = (inputSingleStringLength >= 1 && correctAnswerKeySingleString.length() >= 1) ?
                            jwSimilarity.apply(processedInputSingleString, correctAnswerKeySingleString) : 0.0;

                    Set<String> correctNGramsSingleStringLocal = getCharacterNGrams(correctAnswerKeySingleString, N_GRAM_SIZE);
                    double jaccardScoreTrigramString = calculateJaccardSimilarity(inputNGramsSingleString, correctNGramsSingleStringLocal);

                    double tokenSetJaccardScore = 0.0;
                    if ((processedUserTokens != null && !processedUserTokens.isEmpty()) || (correctFormattedTokens != null && !correctFormattedTokens.isEmpty())) {
                        tokenSetJaccardScore = calculateJaccardSimilarity(
                                processedUserTokens != null ? new HashSet<>(processedUserTokens) : new HashSet<>(),
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
                bestFuzzyMatchKey = correctAnswerKeySingleString;
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

        if (processedUserTokens != null && processedUserTokens.size() == 1 && (bestFuzzyMatchKey == null || highestOverallConfidence < effectiveThreshold )) {
            String userSingleWord = processedInputSingleString;

            for (Map.Entry<String, String> correctAnswerEntry : baseFormToOriginalMap.entrySet()) {
                String correctAnswerKey = correctAnswerEntry.getKey();
                String originalCorrectText = correctAnswerEntry.getValue();
                List<String> correctTokensForFallback = TextNormalizer.getLemmatizedTokens(originalCorrectText, true);

                if (correctTokensForFallback != null && correctTokensForFallback.size() == 1) {
                    String correctSingleWordKey = correctAnswerKey;
                    int ldFallback = levenshteinDistance.apply(userSingleWord, correctSingleWordKey);
                    int allowedDistanceFallback = 1;
                    if (userSingleWord.length() >= 5 || correctSingleWordKey.length() >= 5) {
                        allowedDistanceFallback = 2;
                    }
                    if (userSingleWord.length() >= 8 || correctSingleWordKey.length() >= 8) {
                        allowedDistanceFallback = Math.min(3, (int) Math.max(1, correctSingleWordKey.length() * 0.3));
                    }

                    if (ldFallback <= allowedDistanceFallback) {
                        double jwFallback = jwSimilarity.apply(userSingleWord, correctSingleWordKey);
                        if (jwFallback >= FALLBACK_SINGLE_WORD_JARO_WINKLER_THRESHOLD) {
                            return Optional.of(correctSingleWordKey);
                        }
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

    public int getAnswerPosition(String answerBaseForm) { return 6 - answers.getOrDefault(answerBaseForm, 0); }
    public int getPoints(String answerBaseForm) { return pointsMap.getOrDefault(answerBaseForm, 0); }
    public String getOriginalAnswer(String baseForm) { return baseFormToOriginalMap.getOrDefault(baseForm, baseForm); }
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
            String baseForm = entry.getKey();
            String originalText = baseFormToOriginalMap.getOrDefault(baseForm, baseForm);
            int points = pointsMap.getOrDefault(baseForm, 0);
            allAnswersData.add(new AnswerData(baseForm, originalText, points, displayIndex++));
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