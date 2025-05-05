package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.services.GameService;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class TeamSelectionFrame {
    private final Stage stage;
    private final String selectedCategory;
    private final int answerTime;
    private final String team1Name;
    private final String team2Name;
    private final List<String> team1Members;
    private final List<String> team2Members;
    private GameService gameService;
    private boolean team1Starts;

    // NOWE FLAGI do synchronizacji
    private volatile boolean animationComplete = false;
    private volatile boolean loadingComplete = false;

    public TeamSelectionFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, List<String> team1Members, List<String> team2Members) {
        this.stage = new Stage();
        this.selectedCategory = selectedCategory;
        this.answerTime = answerTime;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = team1Members;
        this.team2Members = team2Members;
        initializeFrame();
        initUI();

        new Thread(this::initializeGameService).start();
    }

    private void initializeGameService() {
        try {
            this.gameService = new GameService(selectedCategory);
            loadingComplete = true; // Ustaw flagę zakończenia ładowania
            Platform.runLater(this::attemptProceedToGame); // Spróbuj przejść dalej (z wątku UI)
        } catch (Exception e) {
            System.err.println("Krytyczny błąd inicjalizacji GameService: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Błąd krytyczny");
                alert.setHeaderText("Nie można załadować danych gry!");
                alert.setContentText("Wystąpił błąd podczas ładowania pytań lub usługi gry.\nSprawdź połączenie z bazą danych i konfigurację.\n\n" + e.getMessage());
                alert.showAndWait();
                stage.close();
            });
        }
    }


    private void initializeFrame() {
        stage.setTitle("Familiada - Wybór drużyny rozpoczynającej");
        stage.setMaximized(true);
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream != null) {
                stage.getIcons().add(new Image(logoStream));
                logoStream.close();
            } else {
                System.err.println("Nie można załadować ikony aplikacji: /logo.png");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować ikony: " + e.getMessage());
        }
    }

    private void initUI() {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        VBox mainPane = new VBox(20);
        mainPane.setAlignment(Pos.CENTER);
        mainPane.setPadding(new Insets(30));
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("PRZYGOTOWYWANIE GRY");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));

        VBox loadingPanel = new VBox(15);
        loadingPanel.setAlignment(Pos.CENTER);
        loadingPanel.setPadding(new Insets(30));
        loadingPanel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.2),
                new CornerRadii(15),
                Insets.EMPTY
        )));

        Label loadingLabel = new Label("Ładowanie gry, przygotowanie pytań...");
        loadingLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        loadingLabel.setTextFill(Color.WHITE);

        Label startingLabel = new Label();
        startingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        startingLabel.setTextFill(Color.GOLD);
        startingLabel.setEffect(new DropShadow(5, Color.BLACK));
        startingLabel.setVisible(false);

        Label teamLabel = new Label();
        teamLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        teamLabel.setTextFill(Color.GOLD);
        teamLabel.setEffect(new DropShadow(15, Color.BLACK));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.setStyle("-fx-progress-color: gold;");

        loadingPanel.getChildren().addAll(
                loadingLabel,
                startingLabel,
                teamLabel,
                progressIndicator
        );

        mainPane.getChildren().addAll(
                titleLabel,
                loadingPanel
        );

        startSelectionAnimation(startingLabel, teamLabel); // Rozpocznij animację

        Scene scene = new Scene(mainPane);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }
        stage.setScene(scene);
    }

    // ZMODYFIKOWANA METODA startSelectionAnimation
    private void startSelectionAnimation(Label startingLabel, Label teamLabel) {
        Random random = new Random();
        this.team1Starts = random.nextBoolean();

        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(200), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(300), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(400), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(500), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(600), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(700), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(800), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(900), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(1000), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(1100), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(1200), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(1300), e -> teamLabel.setText(team1Name)),
                new KeyFrame(Duration.millis(1400), e -> teamLabel.setText(team2Name)),
                new KeyFrame(Duration.millis(1500), e -> {
                    String selectedTeam = team1Starts ? team1Name : team2Name;
                    teamLabel.setText(selectedTeam);
                    startingLabel.setText("Rozpoczyna drużyna:");
                    startingLabel.setVisible(true);

                    FadeTransition ft = new FadeTransition(Duration.millis(300), teamLabel);
                    ft.setFromValue(0.5);
                    ft.setToValue(1.0);
                    ft.setCycleCount(3);
                    ft.setAutoReverse(true);
                    ft.play();
                })
        );
        // ZMIANA: Ustawienie akcji po zakończeniu animacji
        animation.setOnFinished(event -> {
            animationComplete = true; // Ustaw flagę zakończenia animacji
            attemptProceedToGame(); // Spróbuj przejść dalej
        });

        animation.play(); // Uruchom animację
    }

    // NOWA METODA do próby przejścia do gry
    private synchronized void attemptProceedToGame() {
        // Sprawdź, czy obie flagi są true i czy gameService istnieje
        if (animationComplete && loadingComplete && gameService != null && gameService.getCurrentQuestion() != null) {
            stage.close();
            GameFrame gameFrame = new GameFrame(selectedCategory, answerTime, team1Name, team2Name, team1Starts, this.team1Members, this.team2Members);
            gameFrame.show();
        }
        // Jeśli któraś flaga jest false, nic nie rób - druga operacja (ładowanie lub animacja) musi się zakończyć
        // i ona również wywoła tę metodę.
    }

    // Usunięto starą metodę startGame() - jej logika jest teraz w attemptProceedToGame()

    public void show() {
        stage.show();
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}