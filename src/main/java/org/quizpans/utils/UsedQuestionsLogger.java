package org.quizpans.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UsedQuestionsLogger {

    private static final String FILE_NAME = "questions.txt";
    private static Path filePath;

    static {
        try {
            String runningDir = System.getProperty("user.dir");
            filePath = Paths.get(runningDir, FILE_NAME);

            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("Utworzono plik do logowania użytych pytań: " + filePath.toAbsolutePath());
            } else {
                System.out.println("Plik do logowania użytych pytań istnieje: " + filePath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Nie można zainicjalizować ścieżki do pliku '" + FILE_NAME + "': " + e.getMessage());
            filePath = null;
        }
    }

    public static synchronized void addUsedQuestionId(int questionId) {
        if (filePath == null) {
            System.err.println("Ścieżka do pliku logowania nie jest ustawiona. Nie można zapisać ID pytania.");
            return;
        }
        if (questionId <= 0) return;

        try {
            Set<Integer> existingIds = loadUsedQuestionIdsFromFile();
            if (existingIds.contains(questionId)) {
                return;
            }
        } catch (IOException e) {
            System.err.println("Nie można było odczytać istniejących ID, błąd zostanie zignorowany, nastąpi próba zapisu: " + e.getMessage());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            writer.write(String.valueOf(questionId));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisywania ID pytania " + questionId + " do pliku '" + FILE_NAME + "': " + e.getMessage());
        }
    }

    public static Set<Integer> loadUsedQuestionIdsFromFile() throws IOException {
        if (filePath == null || !Files.exists(filePath)) {
            return Collections.emptySet();
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && line.matches("\\d+"))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
        }
    }

    public static void setFilePath(String customPath) {
        try {
            filePath = Paths.get(customPath);
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            System.out.println("Ścieżka pliku logowania ustawiona na: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Nie można ustawić niestandardowej ścieżki pliku logowania: " + e.getMessage());
            filePath = null;
        }
    }

    public static synchronized void clearUsedQuestionsLog() {
        if (filePath == null) {
            System.err.println("Ścieżka do pliku logowania nie jest ustawiona. Nie można wyczyścić logu.");
            return;
        }
        try {
            Files.write(filePath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Wyczyszczono plik użytych pytań: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Błąd podczas czyszczenia pliku użytych pytań '" + FILE_NAME + "': " + e.getMessage());
        }
    }
}