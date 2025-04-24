package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import org.quizpans.utils.TextNormalizer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class GameFrame {
    private final Stage stage;
    private GameService gameService;
    private final Label[] answerLabels = new Label[6];
    private final TextField answerField = new TextField();
    private final Label questionLabel = new Label();
    private final Label timerLabel = new Label();
    private final Label roundPointsLabel = new Label("Pkt: 0");
    private final Label team1Label;
    private final Label team2Label;
    private final Label team1TotalLabel;
    private final Label team2TotalLabel;
    private final VBox team1ErrorsPanel;
    private final VBox team2ErrorsPanel;
    private final int initialAnswerTime;
    private final String selectedCategory;
    private final String team1Name;
    private final String team2Name;

    private int roundPoints = 0;
    private Timeline timer;
    private int timeLeft;
    private boolean isTeam1Turn;
    private int team1Score = 0;
    private int team2Score = 0;
    private boolean hasControl = false;
    private int revealedAnswers = 0;
    private int currentTeamErrors = 0;
    private boolean stealOpportunity = false;
    private int lastRevealedPosition = -1;
    private boolean firstAnswerInRound = true;
    private String firstTeamAnswer = null;
    private int firstTeamAnswerPosition = -1;
    private int firstTeamAnswerPoints = 0;
    private boolean gameFinished = false;
    private final Set<String> usedQuestions = new HashSet<>();
    private ProgressIndicator loadingIndicator;
    private int team1Errors = 0;
    private int team2Errors = 0;

    public GameFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, boolean isTeam1Turn) {
        this.stage = new Stage();
        this.selectedCategory = selectedCategory;
        this.initialAnswerTime = answerTime;
        this.timeLeft = answerTime;
        this.isTeam1Turn = isTeam1Turn;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.gameService = new GameService(selectedCategory);
        this.team1Label = createTeamLabel(team1Name);
        this.team2Label = createTeamLabel(team2Name);
        this.team1TotalLabel = createTotalScoreLabel();
        this.team2TotalLabel = createTotalScoreLabel();
        this.team1ErrorsPanel = createErrorsPanel(true);  // Lewy panel
        this.team2ErrorsPanel = createErrorsPanel(false); // Prawy panel
        initializeFrame();
        initUI();
        updateTeamLabels();
        startTimer();
        usedQuestions.add(TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion()));
    }


    private VBox createErrorsPanel(boolean isLeftPanel) {
        VBox panel = new VBox(25);
        panel.setAlignment(isLeftPanel ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add(isLeftPanel ? "error-panel-left" : "error-panel-right");

        for (int i = 0; i < 3; i++) {
            Label errorLabel = new Label("");
            errorLabel.getStyleClass().add("error-circle-empty");
            errorLabel.setMinSize(70, 70);
            errorLabel.setAlignment(Pos.CENTER);
            panel.getChildren().add(errorLabel);
        }

        return panel;
    }

    private void updateErrorsPanel(VBox panel, int errors) {
        for (int i = 0; i < panel.getChildren().size(); i++) {
            Label label = (Label) panel.getChildren().get(i);
            if (i < errors) {
                label.setText("X");
                label.getStyleClass().remove("error-circle-empty");
                label.getStyleClass().add("error-circle");

                // Animacja dla nowo dodanego błędu
                if (i == errors - 1) {
                    FadeTransition ft = new FadeTransition(Duration.millis(300), label);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();

                    label.setScaleX(1.3);
                    label.setScaleY(1.3);
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(300), label);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                }
            } else {
                label.setText("");
                label.getStyleClass().remove("error-circle");
                label.getStyleClass().add("error-circle-empty");
            }
        }
    }

    private Label createTeamLabel(String teamName) {
        Label label = new Label(teamName);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        label.setTextFill(Color.WHITE);
        label.setEffect(new DropShadow(5, Color.BLACK));
        return label;
    }

    private Label createTotalScoreLabel() {
        Label label = new Label("0");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        label.setTextFill(Color.GOLD);
        label.setEffect(new DropShadow(5, Color.BLACK));
        return label;
    }

    private void updateTeamLabels() {
        if (isTeam1Turn) {
            team1Label.setTextFill(Color.GOLD);
            team1Label.setEffect(new DropShadow(10, Color.GOLD));
            team2Label.setTextFill(Color.WHITE);
            team2Label.setEffect(new DropShadow(5, Color.BLACK));
        } else {
            team1Label.setTextFill(Color.WHITE);
            team1Label.setEffect(new DropShadow(5, Color.BLACK));
            team2Label.setTextFill(Color.GOLD);
            team2Label.setEffect(new DropShadow(10, Color.GOLD));
        }
        team1TotalLabel.setText(String.valueOf(team1Score));
        team2TotalLabel.setText(String.valueOf(team2Score));
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
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        BorderPane mainPane = new BorderPane();
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(20));

        // Górny panel
        VBox topPanel = new VBox(15);
        topPanel.setAlignment(Pos.CENTER);

        ImageView logo = new ImageView();
        try {
            logo.setImage(new Image(getClass().getResourceAsStream("/logo_small.png")));
            logo.setFitHeight(90);
            logo.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Nie można załadować logo: " + e.getMessage());
        }

        questionLabel.setText(gameService.getCurrentQuestion() + (gameService.getCurrentQuestion().endsWith("?") ? "" : "?"));
        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setEffect(new DropShadow(15, Color.BLACK));

        HBox timerBox = new HBox(25);
        timerBox.setAlignment(Pos.CENTER);

        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        updateTimerDisplay();

        roundPointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        roundPointsLabel.setTextFill(Color.GOLD);
        roundPointsLabel.setEffect(new DropShadow(10, Color.BLACK));

        timerBox.getChildren().addAll(timerLabel, roundPointsLabel);
        topPanel.getChildren().addAll(logo, questionLabel, timerBox);
        mainPane.setTop(topPanel);

        // Główny kontener z panelami błędów i odpowiedziami
        HBox centerContainer = new HBox();
        centerContainer.setAlignment(Pos.CENTER);

        // Lewy panel błędów - drużyna 1
        StackPane leftErrorPane = new StackPane(team1ErrorsPanel);
        leftErrorPane.setPadding(new Insets(0, 30, 0, 30));
        leftErrorPane.setAlignment(Pos.CENTER_LEFT);

        // Prawy panel błędów - drużyna 2
        StackPane rightErrorPane = new StackPane(team2ErrorsPanel);
        rightErrorPane.setPadding(new Insets(0, 30, 0, 30));
        rightErrorPane.setAlignment(Pos.CENTER_RIGHT);

        // Panel odpowiedzi
        VBox answersPanel = new VBox(20);
        answersPanel.setAlignment(Pos.CENTER);
        answersPanel.setPadding(new Insets(25));
        answersPanel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.25),
                new CornerRadii(20),
                Insets.EMPTY
        )));
        answersPanel.setPrefWidth(800);

        for (int i = 0; i < 6; i++) {
            answerLabels[i] = createAnswerLabel(i + 1);
            answersPanel.getChildren().add(answerLabels[i]);
        }

        team1ErrorsPanel.setPrefWidth(180);
        team2ErrorsPanel.setPrefWidth(180);

        centerContainer.getChildren().addAll(leftErrorPane, answersPanel, rightErrorPane);

        HBox.setHgrow(leftErrorPane, Priority.ALWAYS);
        HBox.setHgrow(answersPanel, Priority.NEVER);
        HBox.setHgrow(rightErrorPane, Priority.ALWAYS);

        mainPane.setCenter(centerContainer);

        // Dolny panel
        HBox inputContainer = new HBox(25);
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setPadding(new Insets(30, 0, 0, 0));

        VBox team1Box = new VBox(10, team1Label, team1TotalLabel);
        team1Box.setAlignment(Pos.CENTER_LEFT);
        team1Box.setPadding(new Insets(0, 0, 0, 30));

        answerField.setPrefWidth(450);
        answerField.setPrefHeight(60);
        answerField.setFont(Font.font("Arial", 24));
        answerField.setStyle("-fx-background-radius: 15; -fx-border-radius: 15;");
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        answerField.setOnAction(e -> processAnswer());

        VBox team2Box = new VBox(10, team2Label, team2TotalLabel);
        team2Box.setAlignment(Pos.CENTER_RIGHT);
        team2Box.setPadding(new Insets(0, 30, 0, 0));

        inputContainer.getChildren().addAll(team1Box, answerField, team2Box);
        HBox.setHgrow(team1Box, Priority.ALWAYS);
        HBox.setHgrow(team2Box, Priority.ALWAYS);
        mainPane.setBottom(inputContainer);

        Scene scene = new Scene(mainPane);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }
        stage.setScene(scene);
    }

    private Label createAnswerLabel(int number) {
        Label label = new Label(number + ". ⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        label.setTextFill(Color.WHITE);
        label.setEffect(new DropShadow(10, Color.BLACK));
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPrefWidth(750);
        label.setPadding(new Insets(15, 25, 15, 25));
        label.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.4),
                new CornerRadii(15),
                Insets.EMPTY
        )));

        FadeTransition ft = new FadeTransition(Duration.millis(500), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return label;
    }

    private void resetAnswerLabel(int index) {
        answerLabels[index].setText((index + 1) + ". ⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌⛌");
        answerLabels[index].setTextFill(Color.WHITE);
        answerLabels[index].setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.4),
                new CornerRadii(15),
                Insets.EMPTY
        )));
        answerLabels[index].setEffect(new DropShadow(10, Color.BLACK));

        FadeTransition ft = new FadeTransition(Duration.millis(500), answerLabels[index]);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    timeLeft--;
                    updateTimerDisplay();
                    if (timeLeft <= 0) {
                        timer.stop();
                        answerField.setDisable(true);
                        handleTimeOut();
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
            timerLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,0,0,0.8), 10, 0, 0, 0);");
        } else {
            timerLabel.setTextFill(Color.GOLD);
            timerLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");
        }
    }

    private void processAnswer() {
        if (gameFinished) return;

        String userAnswer = answerField.getText().trim();
        answerField.clear();

        if (userAnswer.isEmpty()) {
            registerError();
            return;
        }

        Optional<String> correctAnswer = gameService.checkAnswer(userAnswer);
        if (correctAnswer.isPresent()) {
            handleCorrectAnswer(correctAnswer.get());
        } else {
            registerError();
        }
    }

    private void handleCorrectAnswer(String correctAnswer) {
        int points = gameService.getPoints(correctAnswer);
        int position = gameService.getAnswerPosition(correctAnswer);

        if (firstAnswerInRound) {
            firstAnswerInRound = false;
            firstTeamAnswer = correctAnswer;
            firstTeamAnswerPosition = position;
            firstTeamAnswerPoints = points;

            revealAnswer(correctAnswer, points, position);
            lastRevealedPosition = position;

            if (position != 0) {
                showInfo("Drużyna " + getOpponentTeamName() + " może spróbować przejąć kontrolę!");
                switchTeam();
            } else {
                hasControl = true;
                currentTeamErrors = 0;
                roundPoints += points;
                roundPointsLabel.setText("Pkt: " + roundPoints);
                revealedAnswers++;
            }
            return;
        }

        if (!hasControl && position < firstTeamAnswerPosition) {
            hasControl = true;
            currentTeamErrors = 0;
            showInfo("Drużyna " + (isTeam1Turn ? team1Label.getText() : team2Label.getText()) + " przejęła kontrolę!");

            revealAnswer(correctAnswer, points, position);
            roundPoints = firstTeamAnswerPoints + points;
            roundPointsLabel.setText("Pkt: " + roundPoints);
            revealedAnswers += 2;

            resetTimer();
            return;
        }

        revealAnswer(correctAnswer, points, position);
        roundPoints += points;
        roundPointsLabel.setText("Pkt: " + roundPoints);

        if (!hasControl) {
            hasControl = true;
            currentTeamErrors = 0;
        }

        revealedAnswers++;
        resetTimer();

        if (revealedAnswers == 6) {
            endRound();
        }
    }

    private void revealAnswer(String answer, int points, int position) {
        answerLabels[position].setText((position + 1) + ". " + answer + " (" + points + " pkt)");
        answerLabels[position].setTextFill(Color.GOLD);

        FadeTransition ft = new FadeTransition(Duration.millis(300), answerLabels[position]);
        ft.setFromValue(0.5);
        ft.setToValue(1);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void registerError() {
        currentTeamErrors++;

        if (isTeam1Turn) {
            team1Errors = Math.min(team1Errors + 1, 3);
            updateErrorsPanel(team1ErrorsPanel, team1Errors);
        } else {
            team2Errors = Math.min(team2Errors + 1, 3);
            updateErrorsPanel(team2ErrorsPanel, team2Errors);
        }

        if (stealOpportunity) {
            if (currentTeamErrors >= 1) {
                if (!isTeam1Turn) {
                    team1Score += roundPoints;
                } else {
                    team2Score += roundPoints;
                }
                endGame();
            }
            return;
        }

        if (hasControl) {
            if (currentTeamErrors >= 3) {
                giveStealOpportunity();
            }
        } else {
            switchTeam();
        }
    }

    private void giveStealOpportunity() {
        stealOpportunity = true;
        answerField.setDisable(true);
        showInfo("Szansa na przejęcie dla drużyny " + getOpponentTeamName() + "\nTylko jedna próba!");

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    switchTeam();
                    answerField.setDisable(false);
                    answerField.requestFocus();
                    answerField.setPromptText("Podaj jedną poprawną odpowiedź");
                })
        );
        timeline.play();
    }

    private void handleTimeOut() {
        if (hasControl) {
            giveStealOpportunity();
        } else {
            switchTeam();
        }
    }

    private void endRound() {
        if (!stealOpportunity && hasControl) {
            if (isTeam1Turn) {
                team1Score += roundPoints;
            } else {
                team2Score += roundPoints;
            }
        }

        updateTeamLabels();
        endGame();
    }

    private void endGame() {
        gameFinished = true;
        answerField.setDisable(true);
        if (timer != null) {
            timer.stop();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Koniec rundy!");
        alert.setHeaderText(null);

        Label content = new Label("Przygotowywanie kolejnego pytania...\n\n" +
                team1Name + ": " + team1Score + " pkt\n" +
                team2Name + ": " + team2Score + " pkt\n\n" +
                "Następna runda zaczyna się za chwilę!");
        content.setFont(Font.font("Arial", 20));

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setStyle("-fx-progress-color: gold;");
        loadingIndicator.setPrefSize(50, 50);

        VBox alertContent = new VBox(20, content, loadingIndicator);
        alertContent.setAlignment(Pos.CENTER);
        alertContent.setPadding(new Insets(20));

        alert.getDialogPane().setContent(alertContent);
        alert.getDialogPane().setPrefSize(500, 300);

        new Thread(() -> {
            loadNewQuestion();

            Platform.runLater(() -> {
                alert.close();
                prepareNewRound();
            });
        }).start();

        alert.showAndWait();
    }

    private void loadNewQuestion() {
        String newQuestion;
        do {
            gameService = new GameService(selectedCategory);
            newQuestion = gameService.getCurrentQuestion();
        } while (usedQuestions.contains(TextNormalizer.normalizeToBaseForm(newQuestion)));

        usedQuestions.add(TextNormalizer.normalizeToBaseForm(newQuestion));
    }

    private void prepareNewRound() {
        gameFinished = false;
        roundPoints = 0;
        roundPointsLabel.setText("Pkt: 0");
        revealedAnswers = 0;
        hasControl = false;
        currentTeamErrors = 0;
        stealOpportunity = false;
        lastRevealedPosition = -1;
        firstAnswerInRound = true;
        firstTeamAnswer = null;
        firstTeamAnswerPosition = -1;
        firstTeamAnswerPoints = 0;
        team1Errors = 0;
        team2Errors = 0;
        updateErrorsPanel(team1ErrorsPanel, 0);
        updateErrorsPanel(team2ErrorsPanel, 0);

        isTeam1Turn = !isTeam1Turn;
        questionLabel.setText(gameService.getCurrentQuestion() + (gameService.getCurrentQuestion().endsWith("?") ? "" : "?"));

        for (int i = 0; i < 6; i++) {
            resetAnswerLabel(i);
        }

        updateTeamLabels();
        resetTimer();
        answerField.setDisable(false);
        answerField.requestFocus();
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
    }

    private void resetTimer() {
        timeLeft = initialAnswerTime;
        updateTimerDisplay();
        startTimer();
    }

    private void switchTeam() {
        isTeam1Turn = !isTeam1Turn;
        currentTeamErrors = 0;
        answerField.setDisable(false);
        answerField.requestFocus();
        updateTeamLabels();
        resetTimer();
    }

    private String getOpponentTeamName() {
        return isTeam1Turn ? team2Label.getText().split(":")[0] : team1Label.getText().split(":")[0];
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacja");
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
        FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}