package org.quizpans.utils;

import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.tokenize.SimpleTokenizer;
// Usuniemy import dla Apache Commons Text Levenshtein, jeśli nie jest już potrzebny gdzie indziej w tej klasie
// import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
// Usuniemy import HashSet, jeśli nie jest już tu potrzebny (był dla spellingDictionary)
// import java.util.HashSet;

public class TextNormalizer {
    private static final LemmatizerME lemmatizer;
    private static final Map<String, String> normalizationCacheSingleString = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> normalizationCacheTokenList = new ConcurrentHashMap<>();
    // Usunięto: private static Set<String> spellingDictionary;

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
                throw new RuntimeException("Nie można znaleźć pliku modelu lematyzera: opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin.");
            }
            LemmatizerModel model = new LemmatizerModel(modelStream);
            lemmatizer = new LemmatizerME(model);
        } catch (Exception e) {
            System.err.println("Krytyczny błąd podczas inicjalizacji lematyzera OpenNLP: " + e.getMessage());
            throw new RuntimeException("Błąd inicjalizacji lematyzera", e);
        }
        // Inicjalizacja SpellCheckerService (jego blok statyczny zostanie wywołany przy pierwszym użyciu)
        if (!SpellCheckerService.isInitialized()) {
            System.err.println("Ostrzeżenie: SpellCheckerService nie został poprawnie zainicjalizowany.");
        }
    }

    // Usunięto metodę correctSpellingForToken, ponieważ użyjemy SpellCheckerService

    private static List<String> lemmatizeAndClean(String text, boolean removeStopWords) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String cleanedTextForTokens = text.trim().toLowerCase()
                .replace("ł", "l").replace("ś", "s").replace("ż", "z")
                .replace("ź", "z").replace("ć", "c").replace("ń", "n")
                .replace("ą", "a").replace("ę", "e").replace("ó", "o")
                // Usuwamy znaki interpunkcyjne dopiero po potencjalnej korekcie pisowni całych fraz,
                // ale przed tokenizacją dla lematyzera, jeśli LanguageTool nie poprawił interpunkcji.
                // Na razie zostawiamy tak, LanguageTool powinien radzić sobie z interpunkcją.
                .replaceAll("\\s+", " ").trim();


        // Korekta pisowni całej frazy lub słowo po słowie przez SpellCheckerService
        // Dla uproszczenia, na razie poprawiamy słowo po słowie po tokenizacji.
        // Lepsze byłoby przekazanie całej frazy do SpellCheckerService.correctPhrase,
        // ale integracja tego jest bardziej złożona, jeśli chcemy precyzyjnie zamieniać fragmenty.

        String[] rawTokens = SimpleTokenizer.INSTANCE.tokenize(cleanedTextForTokens);
        List<String> spellingCorrectedTokens = new ArrayList<>();

        for (String rawToken : rawTokens) {
            if (rawToken.isEmpty()) continue;
            // Popraw pisownię każdego tokenu
            String correctedToken = SpellCheckerService.correctWord(rawToken.replaceAll("[^a-z0-9]", "")); // Czyścimy token przed korektą
            spellingCorrectedTokens.add(correctedToken);
        }

        if (spellingCorrectedTokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> finalLemmas = new ArrayList<>();
        for (String tokenToLemmatize : spellingCorrectedTokens) {
            if (tokenToLemmatize.isEmpty()) continue;

            // Lematyzacja pojedynczego, potencjalnie skorygowanego tokenu
            String[] singleTokenArray = { tokenToLemmatize };
            String[] tags = new String[singleTokenArray.length];
            String[] lemmas = lemmatizer.lemmatize(singleTokenArray, tags);

            String currentLemma = (lemmas != null && lemmas.length > 0 && lemmas[0] != null) ?
                    lemmas[0].replaceAll("[^a-z0-9]", "") : "";

            if (!currentLemma.isEmpty() && !currentLemma.equalsIgnoreCase("o")) {
                if (removeStopWords && STOP_WORDS.contains(currentLemma.toLowerCase())) {
                    continue;
                }
                finalLemmas.add(currentLemma);
            } else if (!tokenToLemmatize.isEmpty() && !tokenToLemmatize.equalsIgnoreCase("o") && currentLemma.isEmpty()){
                // Jeśli lematyzacja zawiodła lub dała pusty wynik, a skorygowany token nie jest pusty
                String cleanedCorrectedToken = tokenToLemmatize.replaceAll("[^a-z0-9]", "");
                if (!cleanedCorrectedToken.isEmpty()) {
                    if (removeStopWords && STOP_WORDS.contains(cleanedCorrectedToken.toLowerCase())) {
                        continue;
                    }
                    finalLemmas.add(cleanedCorrectedToken);
                }
            }
        }

        return finalLemmas.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    public static String normalizeToBaseForm(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        // Klucz keszowania może wymagać aktualizacji, jeśli logika spellcheckingu się zmienia
        return normalizationCacheSingleString.computeIfAbsent(text.trim() + "_stopwords:true_spellcheck:LT", key -> {
            List<String> lemmas = lemmatizeAndClean(text.trim(), true);
            return String.join("", lemmas);
        });
    }

    public static List<String> getLemmatizedTokens(String text, boolean removeStopWords) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cacheKey = text.trim() + "_stopwords:" + removeStopWords + "_spellcheck:LT";
        return normalizationCacheTokenList.computeIfAbsent(cacheKey, k ->
                lemmatizeAndClean(text.trim(), removeStopWords)
        );
    }

    public static void clearCache() {
        normalizationCacheSingleString.clear();
        normalizationCacheTokenList.clear();
    }
}