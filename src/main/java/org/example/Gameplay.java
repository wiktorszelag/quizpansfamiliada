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

public class Gameplay extends JFrame {
    private final JLabel[] odpowiedziLabels = new JLabel[6];
    private final JTextField odpowiedzField = new JTextField(20);
    private final JLabel pytanieLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Map<String, Integer> odpowiedzi = new LinkedHashMap<>();
    private final Map<String, Integer> punktyOdpowiedzi = new HashMap<>();
    private final List<String> wprowadzoneOdpowiedzi = new ArrayList<>();

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
                        odpowiedzi.put(odpowiedz.toLowerCase(), 7 - i);
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

            String[] tokens = tokenizer.tokenize(odpowiedzUzytkownika);
            String[] lematyzowanaOdpowiedz = lemmatizer.lemmatize(tokens, new String[tokens.length]);

            for (String poprawnaOdpowiedz : odpowiedzi.keySet()) {
                String[] poprawneTokeny = tokenizer.tokenize(poprawnaOdpowiedz);
                String[] lematyzowanaPoprawnaOdpowiedz = lemmatizer.lemmatize(poprawneTokeny, new String[poprawneTokeny.length]);

                if (Arrays.equals(lematyzowanaOdpowiedz, lematyzowanaPoprawnaOdpowiedz)) {
                    return poprawnaOdpowiedz;
                }

                LevenshteinDistance levenshtein = new LevenshteinDistance();
                int distance = levenshtein.apply(String.join(" ", lematyzowanaOdpowiedz), String.join(" ", lematyzowanaPoprawnaOdpowiedz));
                if (distance <= 2 && Math.abs(lematyzowanaOdpowiedz.length - lematyzowanaPoprawnaOdpowiedz.length) <= 2) {
                    return poprawnaOdpowiedz;
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
}