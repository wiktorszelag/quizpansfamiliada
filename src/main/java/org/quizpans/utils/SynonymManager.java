package org.quizpans.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

        InputStream initialStreamTest = SynonymManager.class.getResourceAsStream(THESAURUS_PATH);
        if (initialStreamTest == null) {
            System.err.println("SynonymManager: KRYTYCZNY BŁĄD - Nie znaleziono pliku tezaurusa pod ścieżką: " + THESAURUS_PATH);
            isLoaded = false;
            return;
        } else {
            try {
                initialStreamTest.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        try (InputStream is = SynonymManager.class.getResourceAsStream(THESAURUS_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            System.out.println("SynonymManager: Rozpoczęto ładowanie tezaurusa z: " + THESAURUS_PATH);
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber % 500 == 0) {
                    System.out.println("SynonymManager: Przetwarzanie linii " + lineNumber);
                }
                processDictionaryLine(line);
            }
            isLoaded = true;
            System.out.println("SynonymManager: Tezaurus załadowany poprawnie. Przetworzono linii: " + lineNumber);
        } catch (NullPointerException npe) {
            System.err.println("SynonymManager: NullPointerException podczas próby otwarcia strumienia lub czytnika dla " + THESAURUS_PATH + ".");
            npe.printStackTrace();
            isLoaded = false;
        }
        catch (Exception e) {
            System.err.println("SynonymManager: Błąd podczas ładowania tezaurusa z " + THESAURUS_PATH + ": " + e.getMessage());
            e.printStackTrace();
            isLoaded = false;
        }
    }

    private static void processDictionaryLine(String line) {
        String[] lineParts = line.split("#", 2);
        String relevantPart = lineParts[0].trim();
        relevantPart = relevantPart.replaceAll("^;+|;+$", "");

        if (relevantPart.isEmpty()) return;

        String[] parts = relevantPart.split(";", -1);
        if (parts.length < 1) return;

        String mainWordKey = parts[0].trim(); // Klucz jest teraz oryginalnym słowem po trim()

        if (mainWordKey.isEmpty()) {
            System.err.println("SynonymManager: Puste słowo kluczowe w linii: " + line);
            return;
        }

        List<String> synonyms = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String synonym = parts[i].trim();
            if (!synonym.isEmpty()) {
                synonyms.add(synonym); // Synonimy też są dodawane "tak jak są"
            }
        }

        if (!synonymDictionary.containsKey(mainWordKey)) {
            synonymDictionary.put(mainWordKey, Collections.unmodifiableList(synonyms));
        } else {
            List<String> existingSynonyms = new ArrayList<>(synonymDictionary.get(mainWordKey));
            Set<String> uniqueSynonyms = new HashSet<>(existingSynonyms);
            uniqueSynonyms.addAll(synonyms);
            synonymDictionary.put(mainWordKey, Collections.unmodifiableList(new ArrayList<>(uniqueSynonyms)));
        }
    }

    public static List<String> findSynonymsFor(String word) {
        if (!isLoaded) {
            System.err.println("SynonymManager: Słownik synonimów nie załadowany. Próba dla: " + word);
            return Collections.emptyList();
        }
        // Kluczem jest teraz słowo po trim() i lowercase(), aby pasowało do kluczy w tezaurusie.
        // Można też zostawić oryginalne word, jeśli tezaurus ma zachowaną wielkość liter.
        // Dla spójności, jeśli klucze w tezaurusie są z małej litery, to tu też.
        String searchKey = word.trim().toLowerCase(); // Lub inna prosta normalizacja klucza wyszukiwania

        // Alternatywnie, jeśli chcesz, aby wyszukiwanie synonimów nadal używało
        // znormalizowanej formy słowa wejściowego:
        // String normalizedSearchKey = TextNormalizer.normalizeToBaseForm(word);
        // return synonymDictionary.getOrDefault(normalizedSearchKey, Collections.emptyList());
        // Ale wtedy klucze w synonymDictionary MUSZĄ być też znormalizowane (co właśnie usunęliśmy)

        // Ta wersja używa searchKey (np. lowercase trim)
        return synonymDictionary.getOrDefault(searchKey, Collections.emptyList());
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