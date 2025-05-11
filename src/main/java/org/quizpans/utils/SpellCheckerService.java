package org.quizpans.utils;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Polish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SpellCheckerService {

    private static JLanguageTool langTool;
    private static boolean isInitialized = false;

    static {
        try {
            langTool = new JLanguageTool(new Polish());
            // Można wyłączyć niektóre reguły, jeśli powodują problemy, np. gramatyczne,
            // jeśli interesuje nas tylko korekta pisowni.
            // Przykład: langTool.disableRule("MORFOLOGIK_RULE_PL_PL");
            isInitialized = true;
            System.out.println("LanguageTool initialized successfully for Polish.");
        } catch (Exception e) {
            System.err.println("Krytyczny błąd podczas inicjalizacji LanguageTool: " + e.getMessage());
            e.printStackTrace();
            isInitialized = false;
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Próbuje skorygować pisownię pojedynczego słowa.
     * Zwraca najlepszą sugestię lub oryginalne słowo, jeśli brak dobrej sugestii.
     * @param word Słowo do sprawdzenia
     * @return Skorygowane słowo lub oryginalne słowo
     */
    public static String correctWord(String word) {
        if (!isInitialized || word == null || word.trim().isEmpty()) {
            return word;
        }

        String trimmedWord = word.trim();
        // LanguageTool działa najlepiej na zdaniach, ale dla pojedynczych słów też można próbować.
        // Czasem warto dodać kropkę, aby wymusić traktowanie jako koniec zdania.
        // Jednak dla pojedynczych tokenów, samo słowo powinno wystarczyć.

        try {
            List<RuleMatch> matches = langTool.check(trimmedWord);
            for (RuleMatch match : matches) {
                // Interesują nas głównie błędy typowo pisowniowe
                // Można filtrować po ID reguł, jeśli znamy te, które odpowiadają za literówki
                // np. match.getRule().isDictionaryBasedSpellingRule()
                if (!match.getSuggestedReplacements().isEmpty()) {
                    // Bierzemy pierwszą sugestię jako najlepszą.
                    // Można by tu dodać logikę wyboru "najlepszej" sugestii, jeśli jest ich wiele.
                    String suggestion = match.getSuggestedReplacements().get(0);

                    // Prosta heurystyka: jeśli sugestia nie zmienia znacząco długości
                    // i nie jest zbyt odległa (LanguageTool sam to ocenia)
                    if (suggestion != null && !suggestion.isEmpty()) {
                        // System.out.println("DEBUG SpellCorrect (LT): '" + trimmedWord + "' -> '" + suggestion + "' (Rule: " + match.getRule().getId() + ")");
                        return suggestion;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas sprawdzania pisowni z LanguageTool dla słowa '" + trimmedWord + "': " + e.getMessage());
        }
        return trimmedWord; // Zwróć oryginalne słowo, jeśli brak poprawek
    }

    /**
     * Próbuje skorygować pisownię w całej frazie, poprawiając poszczególne słowa.
     * @param phrase Fraza do sprawdzenia
     * @return Fraza z potencjalnie skorygowanymi słowami
     */
    public static String correctPhrase(String phrase) {
        if (!isInitialized || phrase == null || phrase.trim().isEmpty()) {
            return phrase;
        }
        // Prosta tokenizacja po spacjach. LanguageTool lepiej radzi sobie z całymi zdaniami,
        // ale jeśli chcemy korygować "na zewnątrz" TextNormalizer, to tak można.
        // Alternatywnie, można by przekazać całą odpowiedź użytkownika do langTool.check()
        // i próbować złożyć wynik z poprawek.

        // Na razie skupmy się na korekcie poszczególnych słów składowych, jeśli to konieczne.
        // Lub po prostu przekażemy całą frazę do LanguageTool i weźmiemy pierwszą sensowną poprawkę,
        // jeśli LanguageTool sugeruje zmianę całej frazy lub jej fragmentu.

        try {
            List<RuleMatch> matches = langTool.check(phrase);
            // Zastosowanie poprawek jest bardziej skomplikowane dla całych zdań,
            // bo pozycje błędów się zmieniają. Prostsze jest poprawianie token po tokenie.
            // Dla uproszczenia, na razie ta metoda nie będzie robić złożonej korekty fraz,
            // a skupimy się na integracji correctWord w TextNormalizer.
            // Jeśli chcemy pełną korektę frazy, trzeba by iterować po matches i stosować je ostrożnie.

            // Poniżej bardzo uproszczone podejście, które może nie być idealne dla wszystkich przypadków.
            // Jeśli jest tylko jeden błąd i jedna sugestia, można ją zastosować.
            if (matches.size() == 1) {
                RuleMatch match = matches.get(0);
                if (!match.getSuggestedReplacements().isEmpty()) {
                    String suggestedReplacement = match.getSuggestedReplacements().get(0);
                    // Prosta zamiana, jeśli błąd dotyczy całej frazy lub jej znaczącej części
                    if (match.getToPos() - match.getFromPos() > phrase.length() / 2 || matches.size() ==1) {
                        // System.out.println("DEBUG SpellCorrect Phrase (LT): '" + phrase + "' -> '" + suggestedReplacement + "' (Rule: " + match.getRule().getId() + ")");
                        // return suggestedReplacement; // To może być zbyt agresywne
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas sprawdzania pisowni z LanguageTool dla frazy '" + phrase + "': " + e.getMessage());
        }


        // Jeśli nie ma globalnej korekty frazy, poprawiamy słowo po słowie
        String[] words = phrase.split("\\s+");
        return java.util.Arrays.stream(words)
                .map(SpellCheckerService::correctWord) // Użyj metody correctWord dla każdego tokenu
                .collect(Collectors.joining(" "));
    }
}