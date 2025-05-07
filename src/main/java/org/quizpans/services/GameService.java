package org.quizpans.services;

import org.quizpans.config.DatabaseConfig;
import org.quizpans.utils.SynonymManager;
import org.quizpans.utils.TextNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.sql.*;
import java.util.*;

public class GameService {
    private final Map<String, Integer> answers = new LinkedHashMap<>(); // Przechowuje znormalizowaną odpowiedź i jej pozycję (dla kolejności)
    private final Map<String, Integer> pointsMap = new HashMap<>(); // Przechowuje znormalizowaną odpowiedź i punkty
    private final Map<String, String> synonymMap = new HashMap<>(); // Przechowuje znormalizowany synonim i jego mapowanie na znormalizowaną odpowiedź bazową
    private final Map<String, String> baseFormToOriginalMap = new HashMap<>(); // Przechowuje znormalizowaną odpowiedź i jej oryginalną formę
    private final String category;
    private String currentQuestion;

    public GameService(String category) {
        this.category = category;
        // System.out.println("GameService: Tworzenie instancji dla kategorii: " + category);
        loadQuestion();
    }

    public void loadQuestion() {
        // System.out.println("GameService: Próba załadowania pytania dla kategorii: " + category);
        answers.clear();
        pointsMap.clear();
        synonymMap.clear();
        baseFormToOriginalMap.clear();
        currentQuestion = null;

        String sql = "SELECT * FROM `" + category + "` ORDER BY RAND() LIMIT 1";
        // System.out.println("GameService: Wykonuję SQL: " + sql);

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                currentQuestion = rs.getString("pytanie");
                // System.out.println("GameService: Pytanie załadowane: " + currentQuestion.substring(0, Math.min(50, currentQuestion.length())) +"...");
                loadAnswers(rs);
            } else {
                // System.err.println("GameService: Brak pytań w bazie danych dla kategorii: " + category);
                currentQuestion = null; // Upewnij się, że jest null, jeśli nie ma pytań
                // Rozważ rzucenie dedykowanego wyjątku, np. NoQuestionsFoundException
                throw new RuntimeException("Brak pytań w bazie danych dla kategorii: " + category);
            }
        } catch (SQLException e) {
            // System.err.println("GameService: Błąd SQL podczas ładowania pytania dla kategorii '" + category + "': " + e.getMessage());
            // e.printStackTrace();
            currentQuestion = null; // Upewnij się, że jest null w przypadku błędu
            throw new RuntimeException("Błąd SQL podczas ładowania pytania: " + e.getMessage(), e);
        } catch (Exception e) { // Łapanie ogólniejszego wyjątku na wypadek innych problemów
            // System.err.println("GameService: Inny błąd podczas ładowania pytania dla kategorii '" + category + "': " + e.getMessage());
            // e.printStackTrace();
            currentQuestion = null;
            throw new RuntimeException("Inny błąd podczas ładowania pytania: " + e.getMessage(), e);
        }
    }

    private void loadAnswers(ResultSet rs) throws SQLException {
        for (int i = 1; i <= 6; i++) { // Zakładając maksymalnie 6 odpowiedzi
            String answer = rs.getString("odpowiedz" + i);
            if (answer != null && !answer.trim().isEmpty()) {
                int points = rs.getInt("punkty" + i);
                String baseForm = TextNormalizer.normalizeToBaseForm(answer); // Normalizuj odpowiedź
                answers.put(baseForm, 7 - i); // Kluczem jest znormalizowana forma, wartość to np. pozycja
                pointsMap.put(baseForm, points);
                baseFormToOriginalMap.put(baseForm, answer.trim()); // Przechowaj oryginalną formę
                loadSynonyms(answer.trim(), baseForm); // Ładuj synonimy dla oryginalnej odpowiedzi, mapuj na baseForm
            }
        }
        // System.out.println("GameService: Załadowano " + answers.size() + " odpowiedzi.");
    }

    private void loadSynonyms(String originalAnswer, String baseForm) {
        // Upewnij się, że synonimy są ładowane tylko raz dla danej odpowiedzi bazowej
        if (!synonymMap.containsValue(baseForm)) { // Sprawdzenie, czy dla baseForm już załadowano synonimy
            List<String> synonyms = SynonymManager.findSynonymsFor(originalAnswer);
            synonyms.forEach(syn -> {
                String processedSyn = TextNormalizer.normalizeToBaseForm(syn);
                // Dodaj synonim tylko jeśli nie jest taki sam jak forma bazowa odpowiedzi
                // i jeśli jeszcze nie istnieje jako klucz (aby uniknąć nadpisywania, jeśli różne synonimy normalizują się tak samo)
                if (!processedSyn.equals(baseForm) && !synonymMap.containsKey(processedSyn)) {
                    synonymMap.put(processedSyn, baseForm); // Mapuj znormalizowany synonim na znormalizowaną formę bazową
                }
            });
        }
    }

    public Optional<String> checkAnswer(String userAnswer) {
        String processedInput = TextNormalizer.normalizeToBaseForm(userAnswer);

        // 1. Bezpośrednie dopasowanie do znormalizowanej odpowiedzi
        if (answers.containsKey(processedInput)) {
            return Optional.of(processedInput);
        }

        // 2. Sprawdzenie synonimów
        if (synonymMap.containsKey(processedInput)) {
            return Optional.of(synonymMap.get(processedInput)); // Zwróć znormalizowaną formę bazową, na którą mapuje synonim
        }

        // 3. Sprawdzenie z odległością Levenshteina (opcjonalne, jeśli powyższe zawiodą)
        // Iteruj po kluczach (znormalizowanych odpowiedziach) w `answers`
        for (String correctAnswerBaseForm : answers.keySet()) {
            if (isMatchWithLevenshtein(processedInput, correctAnswerBaseForm)) {
                return Optional.of(correctAnswerBaseForm);
            }
        }
        // Można też sprawdzić Levenshteina dla synonimów, jeśli to konieczne, ale może to spowolnić
        // for (String synonymBaseForm : synonymMap.keySet()) {
        //     if (isMatchWithLevenshtein(processedInput, synonymBaseForm)) {
        //         return Optional.of(synonymMap.get(synonymBaseForm));
        //     }
        // }


        return Optional.empty(); // Brak dopasowania
    }

    // Oddzielna metoda dla Levenshteina, aby była jasność
    private boolean isMatchWithLevenshtein(String userInput, String correctAnswer) {
        // Odległość Levenshteina; próg można dostosować
        // Dla krótkich odpowiedzi próg 1-2 jest zwykle ok. Dla dłuższych można zwiększyć.
        // Warto też uwzględnić długość stringów przy określaniu progu.
        int threshold = Math.min(2, correctAnswer.length() / 4); // np. max 2 błędy lub 25% długości słowa
        return new LevenshteinDistance().apply(userInput, correctAnswer) <= threshold;
    }


    public int getAnswerPosition(String answerBaseForm) {
        // Odpowiedź powinna być już znormalizowaną formą bazową
        return 6 - answers.getOrDefault(answerBaseForm, 6); // Jeśli nie ma, zwróć domyślną pozycję (lub obsłuż błąd)
    }

    public int getPoints(String answerBaseForm) {
        // Odpowiedź powinna być już znormalizowaną formą bazową
        return pointsMap.getOrDefault(answerBaseForm, 0);
    }

    public String getOriginalAnswer(String baseForm) {
        // Zwraca oryginalną, nieprzetworzoną odpowiedź na podstawie formy bazowej
        return baseFormToOriginalMap.getOrDefault(baseForm, baseForm); // Jeśli nie ma, zwróć samą formę bazową
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

    public void setCurrentQuestionToNull() { // Używane np. przy błędzie ładowania, aby "zresetować" stan
        // System.out.println("GameService: Ustawiam currentQuestion na null i czyszczę odpowiedzi.");
        this.currentQuestion = null;
        this.answers.clear();
        this.pointsMap.clear();
        this.synonymMap.clear();
        this.baseFormToOriginalMap.clear();
    }
}