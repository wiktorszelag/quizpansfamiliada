package org.quizpans.utils;

import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TextNormalizer {
    private static final LemmatizerME lemmatizer;
    private static final Map<String, String> normalizationCache = new ConcurrentHashMap<>();

    static {
        try (InputStream modelStream = TextNormalizer.class.getClassLoader().getResourceAsStream("opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin")) {
            lemmatizer = new LemmatizerME(new LemmatizerModel(modelStream));
        } catch (Exception e) {
            throw new RuntimeException("Błąd inicjalizacji lematyzera", e);
        }
    }

    public static String normalizeToBaseForm(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return normalizationCache.computeIfAbsent(text, key -> {
            String cleaned = text.toLowerCase()
                    .replace("ł", "l").replace("ś", "s").replace("ż", "z")
                    .replace("ź", "z").replace("ć", "c").replace("ń", "n")
                    .replace("ą", "a").replace("ę", "e").replace("ó", "o")
                    .replaceAll("[^a-z]", "");
            String[] tokens = SimpleTokenizer.INSTANCE.tokenize(cleaned);
            String[] lemmas = lemmatizer.lemmatize(tokens, new String[tokens.length]);
            return Arrays.stream(lemmas)
                    .filter(lemma -> !lemma.isEmpty())
                    .collect(Collectors.joining());
        });
    }

    public static void clearCache() {
        normalizationCache.clear();
    }
}