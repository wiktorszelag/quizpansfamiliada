package quizpans.utils;

public class TextNormalizer {
    public static String normalize(String text) {
        return text.toLowerCase()
                .replace("ł", "l")
                .replace("ś", "s")
                .replace("ż", "z")
                .replace("ź", "z")
                .replace("ć", "c")
                .replace("ń", "n")
                .replace("ą", "a")
                .replace("ę", "e")
                .replace("ó", "o")
                .replace(" ", "");
    }
}