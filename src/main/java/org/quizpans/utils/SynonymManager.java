package org.quizpans.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet; // Dodany import
import java.util.List;
import java.util.Map;
import java.util.Set;
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
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processDictionaryLine(line);
            }
            isLoaded = true;
        } catch (Exception e) {
            System.err.println("Błąd ładowania tezaurusa: " + e.getMessage());
            isLoaded = false;
        }
    }

    private static void processDictionaryLine(String line) {
        String[] lineParts = line.split("#", 2);
        String relevantPart = lineParts[0].trim();
        relevantPart = relevantPart.replaceAll("^;+|;+$", "");

        if (relevantPart.isEmpty()) return;
        String[] parts = relevantPart.split(";");
        if (parts.length < 1) return;

        String rawMainWord = parts[0].trim();
        if (rawMainWord.isEmpty()) return;

        String mainWordNormalized = TextNormalizer.normalizeToBaseForm(rawMainWord);
        if (mainWordNormalized.isEmpty()) return;

        List<String> synonyms = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String synonym = parts[i].trim();
            if (!synonym.isEmpty()) {
                synonyms.add(synonym);
            }
        }

        if (!synonymDictionary.containsKey(mainWordNormalized)) {
            synonymDictionary.put(mainWordNormalized, Collections.unmodifiableList(synonyms));
        } else {
            List<String> existingSynonyms = new ArrayList<>(synonymDictionary.get(mainWordNormalized));
            Set<String> uniqueSynonyms = new HashSet<>(existingSynonyms); // Tutaj był używany HashSet
            uniqueSynonyms.addAll(synonyms);
            synonymDictionary.put(mainWordNormalized, Collections.unmodifiableList(new ArrayList<>(uniqueSynonyms)));
        }
    }

    public static List<String> findSynonymsFor(String word) {
        if (!isLoaded) {
            System.err.println("Słownik synonimów nie został załadowany lub wystąpił błąd podczas ładowania.");
            return Collections.emptyList();
        }
        String normalizedWord = TextNormalizer.normalizeToBaseForm(word);
        return synonymDictionary.getOrDefault(normalizedWord, Collections.emptyList());
    }

    public static Set<String> getKnownWords() {
        if (!isLoaded) {
            loadThesaurus();
        }
        return Collections.unmodifiableSet(synonymDictionary.keySet());
    }

    public static boolean isLoaded() {
        return isLoaded;
    }
}