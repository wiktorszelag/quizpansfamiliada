package org.example;

import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Gameplay extends JFrame {
    private final JLabel[] odpowiedziLabels = new JLabel[6];
    private final JTextField odpowiedzField = new JTextField(20);
    private final JLabel pytanieLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Map<String, Integer> odpowiedzi = new LinkedHashMap<>();
    private final Map<String, Integer> punktyOdpowiedzi = new HashMap<>();
    private final List<String> wprowadzoneOdpowiedzi = new ArrayList<>();

    // Lista słów nieistotnych (stop words)
    private final Set<String> stopWords = Set.of("i", "lub", "czy", "a", "w", "na", "do", "się");

    public Gameplay(String selectedCategory) {
        setTitle("Familiada");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        zaladujPytanieIOdpowiedzi(selectedCategory);

        odpowiedzField.addActionListener(e -> dodajOdpowiedz());
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel odpowiedziPanel = new JPanel(new GridLayout(6, 1));
        JPanel questionPanel = new JPanel(new BorderLayout());

        pytanieLabel.setFont(new Font("Arial", Font.BOLD, 28));
        questionPanel.add(pytanieLabel, BorderLayout.NORTH);
        panel.add(questionPanel, BorderLayout.NORTH);

        for (int i = 0; i < 6; i++) {
            odpowiedziLabels[i] = new JLabel((i + 1) + ". ****************************************", SwingConstants.CENTER);
            odpowiedziLabels[i].setFont(new Font("Arial", Font.BOLD, 24));
            odpowiedziPanel.add(odpowiedziLabels[i]);
        }
        panel.add(odpowiedziPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(odpowiedzField, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        add(panel);
    }

    private void zaladujPytanieIOdpowiedzi(String selectedCategory) {
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + selectedCategory + " ORDER BY RAND() LIMIT 1")) {

            if (rs.next()) {
                pytanieLabel.setText("Pytanie: " + rs.getString("pytanie"));
                odpowiedzi.clear();
                punktyOdpowiedzi.clear();

                for (int i = 1; i <= 6; i++) {
                    String odpowiedz = rs.getString("odpowiedz" + i);
                    if (odpowiedz != null) {
                        int punkty = rs.getInt("punkty" + i);
                        odpowiedzi.put(odpowiedz.toLowerCase(), 7 - i); // Zachowujemy oryginalną odpowiedź
                        punktyOdpowiedzi.put(odpowiedz.toLowerCase(), punkty);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Brak pytań w wybranej kategorii.", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Błąd podczas pobierania pytania z bazy danych: " + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void dodajOdpowiedz() {
        String odpowiedzUzytkownika = odpowiedzField.getText().trim().toLowerCase();
        odpowiedzField.setText("");
        if (odpowiedzUzytkownika.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Minus szansa", "Zle", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String doceloweSlowo = czyOdpowiedzPoprawna(odpowiedzUzytkownika);
        if (doceloweSlowo != null) {
            wprowadzoneOdpowiedzi.add(doceloweSlowo);
            aktualizujOdpowiedzi();
        } else {
            JOptionPane.showMessageDialog(this, "Minus szansaa", "Zle", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String czyOdpowiedzPoprawna(String odpowiedzUzytkownika) {
        try (InputStream modelIn = getClass().getResourceAsStream("/opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin")) {
            if (modelIn == null) {
                throw new IllegalStateException("Plik opennlp-pl-ud-pdb-lemmas-1.2-2.5.0.bin nie został znaleziony!");
            }

            LemmatizerModel model = new LemmatizerModel(modelIn);
            LemmatizerME lemmatizer = new LemmatizerME(model);
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

            // Normalizacja odpowiedzi użytkownika (usuwanie spacji i znaków polskich)
            String znormalizowanaOdpowiedzUzytkownika = normalizujTekst(odpowiedzUzytkownika);

            // Tokenizacja i lematyzacja odpowiedzi użytkownika
            String[] tokens = tokenizer.tokenize(znormalizowanaOdpowiedzUzytkownika);
            String[] lematyzowanaOdpowiedz = lemmatizer.lemmatize(tokens, new String[tokens.length]);

            // Filtrowanie słów nieistotnych
            List<String> przefiltrowaneSlowaUzytkownika = Arrays.stream(lematyzowanaOdpowiedz)
                    .filter(slowo -> !stopWords.contains(slowo))
                    .collect(Collectors.toList());

            for (String poprawnaOdpowiedz : odpowiedzi.keySet()) {
                // Normalizacja poprawnej odpowiedzi (usuwanie spacji i znaków polskich)
                String znormalizowanaPoprawnaOdpowiedz = normalizujTekst(poprawnaOdpowiedz);

                // Tokenizacja i lematyzacja poprawnej odpowiedzi
                String[] poprawneTokeny = tokenizer.tokenize(znormalizowanaPoprawnaOdpowiedz);
                String[] lematyzowanaPoprawnaOdpowiedz = lemmatizer.lemmatize(poprawneTokeny, new String[poprawneTokeny.length]);

                // Filtrowanie słów nieistotnych
                List<String> przefiltrowaneSlowaOczekiwane = Arrays.stream(lematyzowanaPoprawnaOdpowiedz)
                        .filter(slowo -> !stopWords.contains(slowo))
                        .collect(Collectors.toList());

                // Porównanie zbiorów słów
                if (new HashSet<>(przefiltrowaneSlowaUzytkownika).equals(new HashSet<>(przefiltrowaneSlowaOczekiwane))) {
                    return poprawnaOdpowiedz; // Zwracamy oryginalną odpowiedź (ze spacjami)
                }

                // Tolerancja dla błędów ortograficznych (tylko dla pojedynczych słów)
                if (przefiltrowaneSlowaUzytkownika.size() == 2 && przefiltrowaneSlowaOczekiwane.size() == 2) {
                    LevenshteinDistance levenshtein = new LevenshteinDistance();
                    int distance = levenshtein.apply(przefiltrowaneSlowaUzytkownika.get(0), przefiltrowaneSlowaOczekiwane.get(0));
                    if (distance <= 2) { // Zwiększona tolerancja
                        return poprawnaOdpowiedz; // Zwracamy oryginalną odpowiedź (ze spacjami)
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void aktualizujOdpowiedzi() {
        wprowadzoneOdpowiedzi.forEach(odpowiedz -> {
            if (odpowiedzi.containsKey(odpowiedz)) {
                int index = 6 - odpowiedzi.get(odpowiedz);
                odpowiedziLabels[index].setText((index + 1) + ". " + odpowiedz + " (" + punktyOdpowiedzi.get(odpowiedz) + " pkt)");
            }
        });
    }

    // Metoda do normalizacji tekstu (usuwanie znaków polskich i spacji)
    private String normalizujTekst(String tekst) {
        if (tekst == null) {
            return "";
        }

        // Zamiana polskich znaków na ich odpowiedniki bez znaków diakrytycznych
        tekst = tekst.toLowerCase()
                .replace("ł", "l")
                .replace("ś", "s")
                .replace("ż", "z")
                .replace("ź", "z")
                .replace("ć", "c")
                .replace("ń", "n")
                .replace("ą", "a")
                .replace("ę", "e")
                .replace("ó", "o");

        // Usuwanie spacji
        tekst = tekst.replace(" ", "");

        return tekst;
    }
}