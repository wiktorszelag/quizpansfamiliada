package org.quizpans.utils;

import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class TextNormalizer {
    private static LemmatizerME lemmatizer = null; // Zmienione na non-final
    private static boolean lemmatizerInitialized = false; // Flaga statusu inicjalizacji

    private static final Map<String, String> normalizationCacheSingleString = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> normalizationCacheTokenList = new ConcurrentHashMap<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "ach", "aj", "albo", "ależ", "aż", "bardziej", "bardzo", "bez", "bo", "bowiem", "by", "byli", "bym", "byś", "był", "była", "było",
            "ci", "cię", "ciebie", "co", "coś", "czy", "czyli", "daleko", "dla", "dlaczego", "do", "dobrze", "dokąd", "dość", "dużo", "dwa", "dwaj", "dwie",
            "dzisiaj", "dziś", "gdy", "gdyby", "gdyż", "gdzie", "go", "i", "ich", "ile", "im", "inny", "ja", "ją", "jak", "jakaś", "jakby", "jaki", "jakiś",
            "jakie", "jako", "jakoś", "je", "jeden", "jedna", "jedno", "jego", "jej", "jemu", "jeszcze", "jest", "jestem", "jeśli", "jeżeli", "już",
            "każdy", "kiedy", "kierunku", "kto", "ktoś", "która", "które", "którego", "której", "który", "których", "którym", "którzy", "ku",
            "lecz", "lub", "ma", "mają", "mam", "mi", "mną", "mnie", "moi", "musi", "mój", "moja", "moje", "my",
            "na", "nad", "nam", "nami", "nas", "nasi", "nasz", "nasza", "nasze", "natychmiast", "nawet", "nią", "nic", "nich", "nie", "niego", "niej", "niemu", "nigdy", "nim", "nimi", "niż", "no",
            "o", "obok", "od", "około", "on", "ona", "one", "oni", "ono", "oraz", "oto",
            "pan", "po", "pod", "podczas", "pomimo", "ponad", "ponieważ", "przed", "przede", "przedtem", "przez", "przy",
            "razie", "roku", "również",
            "się", "skąd", "sobie", "sobą", "sposób", "swoje", "są",
            "ta", "tak", "taka", "taki", "takie", "także", "tam", "te", "tego", "tej", "temu", "ten", "teraz", "też", "to", "tobą", "tobie", "trzy", "tu", "tutaj", "twoi", "twój", "twoja", "twoje", "ty", "tym", "tys",
            "w", "wam", "wami", "was", "wasi", "wasz", "wasza", "wasze", "we", "więc", "wszyscy", "wszystkich", "wszystkie", "wszystkim", "wszystko", "wtedy",
            "z", "za", "żaden", "żadna", "żadne", "żadnych", "że", "żeby"
    );

    static {
        try (InputStream modelStream = TextNormalizer.class.getClassLoader().getResourceAsStream("opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin")) {
            if (modelStream == null) {
                System.err.println("Krytyczny błąd TextNormalizer: Nie można znaleźć pliku modelu lematyzera: opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin.");
            } else {
                LemmatizerModel model = new LemmatizerModel(modelStream);
                lemmatizer = new LemmatizerME(model);
                lemmatizerInitialized = true;
                System.out.println("TextNormalizer: Lemmatizer OpenNLP załadowany poprawnie.");
            }
        } catch (Throwable e) { // Łapiemy Throwable, aby złapać też błędy inicjalizacji OpenNLP
            System.err.println("Krytyczny błąd podczas inicjalizacji lematyzera OpenNLP w TextNormalizer: " + e.getMessage());
            e.printStackTrace();
        }

        if (!SpellCheckerService.isInitialized()) {
            System.err.println("Ostrzeżenie TextNormalizer: SpellCheckerService nie został poprawnie zainicjalizowany.");
        }
    }

    public static boolean isLemmatizerInitialized() {
        return lemmatizerInitialized;
    }


    private static List<String> lemmatizeAndClean(String text, boolean removeStopWords) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String cleanedTextForTokens = text.trim().toLowerCase()
                .replace("ł", "l").replace("ś", "s").replace("ż", "z")
                .replace("ź", "z").replace("ć", "c").replace("ń", "n")
                .replace("ą", "a").replace("ę", "e").replace("ó", "o")
                .replaceAll("\\s+", " ").trim();

        String[] rawTokens = SimpleTokenizer.INSTANCE.tokenize(cleanedTextForTokens);
        List<String> spellingCorrectedTokens = new ArrayList<>();

        for (String rawToken : rawTokens) {
            if (rawToken.isEmpty()) continue;
            String correctedToken = SpellCheckerService.correctWord(rawToken.replaceAll("[^a-z0-9żźćńółęąśŻŹĆŃÓŁĘĄŚ]", "")); // Zachowaj polskie znaki
            spellingCorrectedTokens.add(correctedToken);
        }

        if (spellingCorrectedTokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> finalTokens = new ArrayList<>();
        if (lemmatizerInitialized && lemmatizer != null) {
            try {
                for (String tokenToLemmatize : spellingCorrectedTokens) {
                    if (tokenToLemmatize.isEmpty()) continue;

                    String[] singleTokenArray = { tokenToLemmatize };
                    // Tagi POS nie są tu używane, ale metoda ich wymaga; można przekazać puste
                    String[] tags = new String[singleTokenArray.length];
                    String[] lemmas = lemmatizer.lemmatize(singleTokenArray, tags);

                    // Sprawdź, czy wynik lematyzacji nie jest "O" (oznaczenie OpenNLP dla nieznanych/błędnych)
                    // i czy nie jest pusty
                    String currentLemma = (lemmas != null && lemmas.length > 0 && lemmas[0] != null && !lemmas[0].equals("O")) ?
                            lemmas[0].replaceAll("[^a-z0-9żźćńółęąśŻŹĆŃÓŁĘĄŚ]", "") : "";

                    if (!currentLemma.isEmpty()) {
                        if (removeStopWords && STOP_WORDS.contains(currentLemma.toLowerCase())) {
                            continue;
                        }
                        finalTokens.add(currentLemma);
                    } else if (!tokenToLemmatize.isEmpty()){ // Jeśli lema pusta, ale token nie, użyj tokenu
                        String cleanedCorrectedToken = tokenToLemmatize.replaceAll("[^a-z0-9żźćńółęąśŻŹĆŃÓŁĘĄŚ]", "");
                        if (!cleanedCorrectedToken.isEmpty()) {
                            if (removeStopWords && STOP_WORDS.contains(cleanedCorrectedToken.toLowerCase())) {
                                continue;
                            }
                            finalTokens.add(cleanedCorrectedToken);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd podczas lematyzacji w TextNormalizer (lemmatizer.lemmatize): " + e.getMessage() + " dla tekstu: " + text + ". Używanie tokenów bez lematyzacji.");
                // W przypadku błędu, użyj tokenów po korekcie pisowni i podstawowym czyszczeniu jako fallback
                finalTokens.clear();
                for (String token : spellingCorrectedTokens) {
                    String cleanedToken = token.replaceAll("[^a-z0-9żźćńółęąśŻŹĆŃÓŁĘĄŚ]", "");
                    if (!cleanedToken.isEmpty()) {
                        if (removeStopWords && STOP_WORDS.contains(cleanedToken.toLowerCase())) continue;
                        finalTokens.add(cleanedToken);
                    }
                }
            }
        } else {
            System.err.println("TextNormalizer: Lemmatizer nie jest zainicjalizowany. Używanie tokenów bez lematyzacji.");
            for (String token : spellingCorrectedTokens) {
                String cleanedToken = token.replaceAll("[^a-z0-9żźćńółęąśŻŹĆŃÓŁĘĄŚ]", "");
                if (!cleanedToken.isEmpty()) {
                    if (removeStopWords && STOP_WORDS.contains(cleanedToken.toLowerCase())) continue;
                    finalTokens.add(cleanedToken);
                }
            }
        }
        return finalTokens.stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
    }

    public static String normalizeToBaseForm(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String cacheKey = text.trim() + "_stopwords:true_spellcheck:LT_lemmatizer:" + lemmatizerInitialized;
        return normalizationCacheSingleString.computeIfAbsent(cacheKey, key -> {
            List<String> lemmas = lemmatizeAndClean(text.trim(), true);
            return String.join("", lemmas);
        });
    }

    public static List<String> getLemmatizedTokens(String text, boolean removeStopWords) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cacheKey = text.trim() + "_stopwords:" + removeStopWords + "_spellcheck:LT_lemmatizer:" + lemmatizerInitialized;
        return normalizationCacheTokenList.computeIfAbsent(cacheKey, k ->
                lemmatizeAndClean(text.trim(), removeStopWords)
        );
    }

    public static void clearCache() {
        normalizationCacheSingleString.clear();
        normalizationCacheTokenList.clear();
    }
}