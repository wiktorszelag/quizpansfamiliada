package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

public class GameFrame {
    private final Stage stage;
    private final GameService gameService;
    private final Label[] answerLabels = new Label[6];
    private final TextField answerField = new TextField();
    private final Label questionLabel = new Label();
    private final Label timerLabel = new Label();
    private final Label roundPointsLabel = new Label("Pkt: 0");
    private final Label team1Label;
    private final Label team2Label;
    private int roundPoints = 0;
    private Timeline timer;
    private int timeLeft;

    public GameFrame(String selectedCategory, int answerTime, String team1Name, String team2Name) {
        stage = new Stage();
        this.gameService = new GameService(selectedCategory);
        this.timeLeft = answerTime;
        this.team1Label = createTeamLabel(team1Name);
        this.team2Label = createTeamLabel(team2Name);
        initializeFrame();
        initUI();
        startTimer();
    }

    private Label createTeamLabel(String teamName) {
        Label label = new Label(teamName);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        label.setTextFill(Color.WHITE);
        label.setEffect(new DropShadow(5, Color.BLACK));
        return label;
    }

    private void initializeFrame() {
        stage.setTitle("Familiada - Nowoczesna Wersja");
        stage.setMaximized(true);
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception e) {
            System.err.println("Nie można załadować ikony: " + e.getMessage());
        }
    }

    private void initUI() {
        // Gradient tła
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        BorderPane mainPane = new BorderPane();
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(20));

        // Górny panel z pytaniem
        VBox topPanel = new VBox(10);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(0, 0, 30, 0));

        // Logo
        ImageView logo = new ImageView();
        try {
            logo.setImage(new Image(getClass().getResourceAsStream("/logo_small.png")));
            logo.setFitHeight(80);
            logo.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Nie można załadować logo: " + e.getMessage());
        }

        // Panel pytania
        VBox questionPanel = new VBox(10);
        questionPanel.setAlignment(Pos.CENTER);

        // Formatowanie pytania
        String question = gameService.getCurrentQuestion();
        if (!question.endsWith("?")) {
            question += "?";
        }
        questionLabel.setText(question);
        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setEffect(new DropShadow(10, Color.BLACK));

        // Timer i punkty rundy
        HBox timerBox = new HBox(20);
        timerBox.setAlignment(Pos.CENTER);

        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        timerLabel.setTextFill(Color.GOLD);
        timerLabel.setEffect(new DropShadow(5, Color.BLACK));
        updateTimerDisplay();

        roundPointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        roundPointsLabel.setTextFill(Color.GOLD);
        roundPointsLabel.setEffect(new DropShadow(5, Color.BLACK));

        timerBox.getChildren().addAll(timerLabel, roundPointsLabel);
        questionPanel.getChildren().addAll(questionLabel, timerBox);

        topPanel.getChildren().addAll(logo, questionPanel);
        mainPane.setTop(topPanel);

        // Panel odpowiedzi
        VBox answersPanel = new VBox(15);
        answersPanel.setAlignment(Pos.CENTER);
        answersPanel.setPadding(new Insets(20));
        answersPanel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.2),
                new CornerRadii(15),
                Insets.EMPTY
        )));

        for (int i = 0; i < 6; i++) {
            answerLabels[i] = createAnswerLabel(i + 1);
            answersPanel.getChildren().add(answerLabels[i]);
        }

        // Panel wprowadzania odpowiedzi z nazwami drużyn
        HBox inputContainer = new HBox();
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setPadding(new Insets(30, 0, 0, 0));

        // Lewa drużyna - przy lewej krawędzi
        StackPane leftTeamPane = new StackPane();
        leftTeamPane.setAlignment(Pos.CENTER_LEFT);
        leftTeamPane.setPadding(new Insets(0, 0, 0, 20));
        leftTeamPane.getChildren().add(team1Label);

        // Pole wprowadzania odpowiedzi
        answerField.setPrefWidth(400);
        answerField.setPrefHeight(50);
        answerField.setFont(Font.font("Arial", 20));
        answerField.setStyle("-fx-background-radius: 15; -fx-border-radius: 15;");
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        answerField.setOnAction(e -> processAnswer());

        // Prawa drużyna - przy prawej krawędzi
        StackPane rightTeamPane = new StackPane();
        rightTeamPane.setAlignment(Pos.CENTER_RIGHT);
        rightTeamPane.setPadding(new Insets(0, 20, 0, 0));
        rightTeamPane.getChildren().add(team2Label);

        inputContainer.getChildren().addAll(
                leftTeamPane,
                answerField,
                rightTeamPane
        );

        // Ustawienie szerokości elementów
        HBox.setHgrow(leftTeamPane, Priority.ALWAYS);
        HBox.setHgrow(rightTeamPane, Priority.ALWAYS);

        mainPane.setCenter(answersPanel);
        mainPane.setBottom(inputContainer);

        Scene scene = new Scene(mainPane);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }
        stage.setScene(scene);
    }

    private void startTimer() {
        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    timeLeft--;
                    updateTimerDisplay();
                    if (timeLeft <= 0) {
                        timer.stop();
                        answerField.setDisable(true);
                        showWarning("Czas upłynął!", "Koniec czasu");
                    }
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerDisplay() {
        timerLabel.setText("Czas: " + timeLeft + "s");
        if (timeLeft <= 10) {
            timerLabel.setTextFill(Color.RED);
        } else {
            timerLabel.setTextFill(Color.GOLD);
        }
    }

    private void updateRoundPoints(int points) {
        roundPoints += points;
        roundPointsLabel.setText("Pkt: " + roundPoints);
    }

    private Label createAnswerLabel(int number) {
        Label label = new Label(number + ". ********************************");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        label.setTextFill(Color.WHITE);
        label.setEffect(new DropShadow(5, Color.BLACK));
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPrefWidth(600);
        label.setPadding(new Insets(10, 20, 10, 20));
        label.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.3),
                new CornerRadii(10),
                Insets.EMPTY
        )));

        FadeTransition ft = new FadeTransition(Duration.millis(500), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return label;
    }

    private void processAnswer() {
        String userAnswer = answerField.getText().trim();
        answerField.clear();

        if (userAnswer.isEmpty()) {
            showWarning("Minus szansa", "Błąd");
            return;
        }

        gameService.checkAnswer(userAnswer).ifPresentOrElse(
                this::updateUI,
                () -> showWarning("Minus szansa", "Zła odpowiedź")
        );
    }

    private void updateUI(String correctAnswer) {
        int position = gameService.getAnswerPosition(correctAnswer);
        int points = gameService.getPoints(correctAnswer);

        updateRoundPoints(points);

        answerLabels[position].setText((position + 1) + ". " + correctAnswer + " (" + points + " pkt)");
        answerLabels[position].setTextFill(Color.GOLD);

        FadeTransition ft = new FadeTransition(Duration.millis(300), answerLabels[position]);
        ft.setFromValue(0.5);
        ft.setToValue(1);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void showWarning(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów alertu: " + e.getMessage());
        }

        alert.showAndWait();
    }

    public void show() {
        stage.show();
    }
}