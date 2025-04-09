package org.quizpans.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SynonymManager {
    private static final String THESAURUS_PATH = "/thesaurus.txt";
    private static final Map<String, List<String>> synonymDictionary = new ConcurrentHashMap<>();
    private static boolean isLoaded = false;

    static {
        loadThesaurus();
    }

    private static synchronized void loadThesaurus() {
        if (isLoaded) return;
        try (InputStream is = SynonymManager.class.getResourceAsStream(THESAURUS_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processDictionaryLine(line);
            }
            isLoaded = true;
        } catch (Exception e) {
            System.err.println("Błąd ładowania tezaurusa: " + e.getMessage());
        }
    }

    private static void processDictionaryLine(String line) {
        line = line.split("#")[0].trim().replaceAll("^;+|;+$", "");
        if (line.isEmpty()) return;
        String[] parts = line.split(";");
        if (parts.length < 2) return;
        String mainWord = TextNormalizer.normalizeToBaseForm(parts[0].trim());
        List<String> synonyms = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String synonym = parts[i].trim();
            if (!synonym.isEmpty()) {
                String normalizedSynonym = TextNormalizer.normalizeToBaseForm(synonym);
                if (!normalizedSynonym.equals(mainWord)) {
                    synonyms.add(normalizedSynonym);
                }
            }
        }
        if (!synonyms.isEmpty()) {
            synonymDictionary.put(mainWord, Collections.unmodifiableList(synonyms));
        }
    }

    public static List<String> findSynonymsFor(String word) {
        if (!isLoaded) {
            throw new IllegalStateException("Słownik synonimów nie został załadowany");
        }
        String target = TextNormalizer.normalizeToBaseForm(word);
        return synonymDictionary.getOrDefault(target, Collections.emptyList());
    }
}