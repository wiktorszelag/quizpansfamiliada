package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.OverrunStyle;
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
import org.quizpans.utils.TextNormalizer;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class GameFrame {
    private final Stage stage;
    private GameService gameService;
    private final BorderPane[] answerPanes = new BorderPane[6];
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
    private final List<String> team1Members;
    private final List<String> team2Members;
    private int team1PlayerIndex = 0;
    private int team2PlayerIndex = 0;
    private Label currentPlayerLabel;

    private static final String HIDDEN_ANSWER_PLACEHOLDER = "■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■";

    private int roundPoints = 0;
    private Timeline timer;
    private int timeLeft;
    private boolean isTeam1Turn;
    private int team1Score = 0;
    private int team2Score = 0;
    private boolean hasControl = false;
    private int revealedAnswers = 0;
    private boolean stealOpportunity = false;
    private boolean firstAnswerInRound = true;
    private String firstTeamAnswer = null;
    private int firstTeamAnswerPosition = -1;
    private boolean gameFinished = false;
    private final Set<String> usedQuestions = new HashSet<>();
    private ProgressIndicator loadingIndicator;
    private int team1Errors = 0;
    private int team2Errors = 0;
    private boolean timerWasRunningBeforeAlert = false;
    private int currentRoundNumber = 1;
    private final int MAX_ROUNDS = 6;

    public GameFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, boolean isTeam1Turn, List<String> team1Members, List<String> team2Members) {
        this.stage = new Stage();
        this.selectedCategory = selectedCategory;
        this.initialAnswerTime = answerTime;
        this.timeLeft = answerTime;
        this.isTeam1Turn = isTeam1Turn;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = (team1Members != null && !team1Members.isEmpty()) ? new ArrayList<>(team1Members) : Collections.singletonList("Gracz 1 (D1)");
        this.team2Members = (team2Members != null && !team2Members.isEmpty()) ? new ArrayList<>(team2Members) : Collections.singletonList("Gracz 1 (D2)");

        this.gameService = new GameService(selectedCategory);
        this.team1Label = createTeamLabel(team1Name);
        this.team2Label = createTeamLabel(team2Name);
        this.team1TotalLabel = createTotalScoreLabel();
        this.team2TotalLabel = createTotalScoreLabel();
        this.team1ErrorsPanel = createErrorsPanel(true);
        this.team2ErrorsPanel = createErrorsPanel(false);
        initializeFrame();
        initUI();
        updateTeamLabels();
        // --- ZMIANA: Usunięto wywołanie stąd ---
        // setInitialPlayerIndexForRound();
        // --- Ustawienie gracza 1 na start Rundy 1 ---
        this.team1PlayerIndex = 0;
        this.team2PlayerIndex = 0;
        // --- Koniec zmiany ---
        updateCurrentPlayerLabel();
        startTimer();
        usedQuestions.add(TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion()));
    }

    private VBox createErrorsPanel(boolean isLeftPanel) {
        VBox panel = new VBox(30);
        panel.setPadding(new Insets(20));

        for (int i = 0; i < 3; i++) {
            Label errorLabel = new Label("");
            errorLabel.getStyleClass().add("error-circle-empty");
            errorLabel.setMinSize(95, 95);
            errorLabel.setAlignment(Pos.CENTER);
            panel.getChildren().add(errorLabel);
        }
        panel.getStyleClass().add(isLeftPanel ? "error-panel-left" : "error-panel-right");

        return panel;
    }

    private void updateErrorsPanel(VBox panel, int errors) {
        if (panel == null || panel.getChildren().isEmpty()) {
            System.err.println("Błąd: Próba aktualizacji niezainicjalizowanego lub pustego panelu błędów.");
            return;
        }
        int errorsToDisplay = Math.min(errors, panel.getChildren().size());

        for (int i = 0; i < panel.getChildren().size(); i++) {
            if (!(panel.getChildren().get(i) instanceof Label)) continue;

            Label label = (Label) panel.getChildren().get(i);
            boolean shouldBeFilled = i < errorsToDisplay;
            boolean wasFilled = label.getStyleClass().contains("error-circle");

            if (shouldBeFilled) {
                if (!wasFilled) {
                    label.getStyleClass().remove("error-circle-empty");
                    label.getStyleClass().add("error-circle");
                    label.setText("X");

                    if (i == errorsToDisplay - 1) {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), label);
                        ft.setFromValue(0);
                        ft.setToValue(1);
                        ft.play();

                        label.setScaleX(1.3);
                        label.setScaleY(1.3);
                        ScaleTransition st = new ScaleTransition(Duration.millis(300), label);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                    }
                }
            } else {
                if (wasFilled) {
                    label.getStyleClass().remove("error-circle");
                    label.getStyleClass().add("error-circle-empty");
                    label.setText("");
                }
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
            team1Label.getStyleClass().removeAll("team-inactive", "team-active");
            team1Label.getStyleClass().add("team-active");
            team2Label.getStyleClass().removeAll("team-inactive", "team-active");
            team2Label.getStyleClass().add("team-inactive");

        } else {
            team1Label.setTextFill(Color.WHITE);
            team1Label.setEffect(new DropShadow(5, Color.BLACK));
            team2Label.setTextFill(Color.GOLD);
            team2Label.setEffect(new DropShadow(10, Color.GOLD));
            team1Label.getStyleClass().removeAll("team-inactive", "team-active");
            team1Label.getStyleClass().add("team-inactive");
            team2Label.getStyleClass().removeAll("team-inactive", "team-active");
            team2Label.getStyleClass().add("team-active");
        }
        team1TotalLabel.setText(String.valueOf(team1Score));
        team2TotalLabel.setText(String.valueOf(team2Score));
    }

    private void updateCurrentPlayerLabel() {
        String currentPlayerName;
        if (isTeam1Turn) {
            if (!team1Members.isEmpty()) {
                currentPlayerName = team1Members.get(team1PlayerIndex);
            } else {
                currentPlayerName = team1Name;
            }
        } else {
            if (!team2Members.isEmpty()) {
                currentPlayerName = team2Members.get(team2PlayerIndex);
            } else {
                currentPlayerName = team2Name;
            }
        }
        if (currentPlayerLabel != null) {
            currentPlayerLabel.setText("Odpowiada teraz: " + currentPlayerName);
        }
    }

    // --- ZMIANA: Metoda ustawia indeksy dla OBU drużyn ---
    private void setInitialPlayerIndexForRound() {
        int baseIndex = currentRoundNumber - 1;

        if (!team1Members.isEmpty()) {
            team1PlayerIndex = baseIndex % team1Members.size();
        } else {
            team1PlayerIndex = 0;
        }

        if (!team2Members.isEmpty()) {
            team2PlayerIndex = baseIndex % team2Members.size();
        } else {
            team2PlayerIndex = 0;
        }
        System.out.println("Setting initial indices for Round " + currentRoundNumber + ": T1=" + team1PlayerIndex + ", T2=" + team2PlayerIndex);
    }
    // --- KONIEC ZMIANY ---


    private void initializeFrame() {
        stage.setTitle("Familiada - Nowoczesna Wersja");
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
            System.err.println("Nie można załadować ikony aplikacji: " + e.getMessage());
        }
    }

    private String getFormattedQuestion() {
        if (gameService == null || gameService.getCurrentQuestion() == null) {
            return "Błąd ładowania pytania...";
        }
        String baseQuestion = gameService.getCurrentQuestion();
        return "Pytanie numer " + currentRoundNumber + ": " + baseQuestion + (baseQuestion.endsWith("?") ? "" : "?");
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

        VBox topPanel = new VBox(15);
        topPanel.setAlignment(Pos.CENTER);

        questionLabel.setText(getFormattedQuestion());
        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setEffect(new DropShadow(15, Color.BLACK));
        questionLabel.setWrapText(true);
        questionLabel.setAlignment(Pos.CENTER);

        HBox timerBox = new HBox(25);
        timerBox.setAlignment(Pos.CENTER);

        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        updateTimerDisplay();

        roundPointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        roundPointsLabel.setTextFill(Color.GOLD);
        roundPointsLabel.setEffect(new DropShadow(10, Color.BLACK));

        timerBox.getChildren().addAll(timerLabel, roundPointsLabel);
        topPanel.getChildren().addAll(questionLabel, timerBox);
        mainPane.setTop(topPanel);
        BorderPane.setMargin(topPanel, new Insets(20, 0, 20, 0));

        GridPane centerContainer = new GridPane();
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setHgap(15);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        col1.setHalignment(HPos.CENTER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        col2.setHalignment(HPos.CENTER);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(15);
        col3.setHalignment(HPos.CENTER);

        centerContainer.getColumnConstraints().addAll(col1, col2, col3);

        StackPane leftErrorPane = new StackPane(team1ErrorsPanel);
        leftErrorPane.setAlignment(Pos.CENTER);

        StackPane rightErrorPane = new StackPane(team2ErrorsPanel);
        rightErrorPane.setAlignment(Pos.CENTER);

        VBox answersPanel = new VBox(15);
        answersPanel.setAlignment(Pos.CENTER);
        answersPanel.setPadding(new Insets(20));
        answersPanel.getStyleClass().add("answer-panel");

        for (int i = 0; i < 6; i++) {
            answerPanes[i] = createAnswerPane(i + 1);
            answersPanel.getChildren().add(answerPanes[i]);
        }

        centerContainer.add(leftErrorPane, 0, 0);
        centerContainer.add(answersPanel, 1, 0);
        centerContainer.add(rightErrorPane, 2, 0);

        mainPane.setCenter(centerContainer);

        VBox bottomContainer = new VBox(15);
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(20, 0, 20, 0));

        currentPlayerLabel = new Label("Odpowiada teraz: ...");
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        currentPlayerLabel.setTextFill(Color.WHITE);
        currentPlayerLabel.setEffect(new DropShadow(5, Color.BLACK));

        HBox inputAndScoresBox = new HBox(25);
        inputAndScoresBox.setAlignment(Pos.CENTER);

        VBox team1Box = new VBox(10, team1Label, team1TotalLabel);
        team1Box.setAlignment(Pos.CENTER_LEFT);
        team1Box.setPadding(new Insets(0, 0, 0, 30));

        answerField.setPrefWidth(450);
        answerField.setPrefHeight(60);
        answerField.setFont(Font.font("Arial", 24));
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        answerField.setOnAction(e -> processAnswer());

        VBox team2Box = new VBox(10, team2Label, team2TotalLabel);
        team2Box.setAlignment(Pos.CENTER_RIGHT);
        team2Box.setPadding(new Insets(0, 30, 0, 0));

        inputAndScoresBox.getChildren().addAll(team1Box, answerField, team2Box);
        HBox.setHgrow(team1Box, Priority.ALWAYS);
        HBox.setHgrow(answerField, Priority.NEVER);
        HBox.setHgrow(team2Box, Priority.ALWAYS);

        bottomContainer.getChildren().addAll(currentPlayerLabel, inputAndScoresBox);

        mainPane.setBottom(bottomContainer);

        Scene scene = new Scene(mainPane);
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: /styles.css. " + e.getMessage());
        }
        stage.setScene(scene);
    }


    private BorderPane createAnswerPane(int number) {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("answer-pane");

        Label placeholderLabel = new Label(number + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().add("answer-text-label");
        placeholderLabel.getStyleClass().add("hidden");

        pane.setCenter(placeholderLabel);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);

        FadeTransition ft = new FadeTransition(Duration.millis(500), pane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return pane;
    }

    private void resetAnswerPane(int index) {
        BorderPane pane = answerPanes[index];
        pane.getStyleClass().remove("answer-revealed");

        Label placeholderLabel = new Label((index + 1) + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().clear();
        placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");

        pane.setCenter(placeholderLabel);
        pane.setRight(null);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);

        FadeTransition ft = new FadeTransition(Duration.millis(500), pane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        timeLeft = initialAnswerTime;
        updateTimerDisplay();

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (gameFinished) {
                        timer.stop();
                        return;
                    }
                    timeLeft--;
                    updateTimerDisplay();
                    if (timeLeft <= 0) {
                        timer.stop();
                        answerField.setDisable(true);
                        Platform.runLater(this::handleTimeOut);
                    }
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        timerWasRunningBeforeAlert = true;
    }

    private void updateTimerDisplay() {
        timerLabel.setText("Czas: " + timeLeft + "s");
        timerLabel.getStyleClass().removeAll("timer-critical", "timer-normal");
        if (timeLeft <= 10) {
            timerLabel.getStyleClass().add("timer-critical");
        } else {
            timerLabel.getStyleClass().add("timer-normal");
        }
    }

    private void processAnswer() {
        if (gameFinished || answerField.isDisabled()) return;

        String userAnswer = answerField.getText().trim();
        answerField.clear();

        advanceCurrentPlayerIndex();

        if (userAnswer.isEmpty()) {
            showInfo("Pusta odpowiedź! Liczymy jako błąd.");
            registerError();
        } else {
            Optional<String> correctAnswerOpt = gameService.checkAnswer(userAnswer);
            if (correctAnswerOpt.isPresent()) {
                handleCorrectAnswer(correctAnswerOpt.get());
            } else {
                registerError();
            }
        }
    }

    private void advanceCurrentPlayerIndex() {
        if (isTeam1Turn) {
            if (team1Members.size() > 1) {
                team1PlayerIndex = (team1PlayerIndex + 1) % team1Members.size();
                System.out.println("Advanced T1 index to: " + team1PlayerIndex + " (after action)");
            }
        } else {
            if (team2Members.size() > 1) {
                team2PlayerIndex = (team2PlayerIndex + 1) % team2Members.size();
                System.out.println("Advanced T2 index to: " + team2PlayerIndex + " (after action)");
            }
        }
    }

    private void continueTurnForCurrentTeam() {
        updateCurrentPlayerLabel();
        resetTimer();
    }

    private void handleCorrectAnswer(String correctAnswerBaseForm) {
        if (gameFinished) return;

        int points = gameService.getPoints(correctAnswerBaseForm);
        int position = gameService.getAnswerPosition(correctAnswerBaseForm);

        if (answerPanes[position].getStyleClass().contains("answer-revealed")) {
            showInfo("Ta odpowiedź została już odkryta!");
            continueTurnForCurrentTeam();
            return;
        }

        boolean wasStealing = stealOpportunity;
        stealOpportunity = false;

        revealAnswer(correctAnswerBaseForm, points, position);
        roundPoints += points;
        roundPointsLabel.setText("Pkt: " + roundPoints);
        revealedAnswers++;

        if (wasStealing) {
            if (isTeam1Turn) team1Score += roundPoints;
            else team2Score += roundPoints;
            showInfo("Przejęcie udane! Punkty (" + roundPoints + ") dla drużyny " + getCurrentTeamName() + "!", false);
            endRound(false);
            return;
        }

        if (firstAnswerInRound) {
            firstAnswerInRound = false;
            firstTeamAnswer = correctAnswerBaseForm;
            firstTeamAnswerPosition = position;

            if (position != 0) {
                showInfo("Dobra odpowiedź! Ale czy najlepsza? Szansa dla drużyny " + getOpponentTeamName() + "!");
                switchTeam();
            } else {
                hasControl = true;
                resetErrorsAndPanels();
                showInfo("Najlepsza odpowiedź! Drużyna " + getCurrentTeamName() + " ma kontrolę!");
                continueTurnForCurrentTeam();
            }
        } else if (!hasControl) {
            if (firstTeamAnswerPosition == -1) {
                hasControl = true;
                resetErrorsAndPanels();
                showInfo("Poprawna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę!");
                continueTurnForCurrentTeam();
            } else {
                if (position < firstTeamAnswerPosition) {
                    hasControl = true;
                    resetErrorsAndPanels();
                    showInfo("Świetna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę!");
                    continueTurnForCurrentTeam();
                } else {
                    showInfo("Poprawna odpowiedź, ale nie lepsza. Kontrola dla " + getOpponentTeamName() + ".");
                    isTeam1Turn = !isTeam1Turn;
                    hasControl = true;
                    resetErrorsAndPanels();
                    updateTeamLabels();
                    updateCurrentPlayerLabel();
                    resetTimer();
                }
            }
        } else {
            if (revealedAnswers == 6) {
                showInfo("Wszystkie odpowiedzi odkryte! Runda dla drużyny " + getCurrentTeamName() + "!", false);
                endRound(true);
            } else {
                continueTurnForCurrentTeam();
            }
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private void revealAnswer(String answerBaseForm, int points, int position) {
        String originalAnswer = gameService.getOriginalAnswer(answerBaseForm);
        String answerToDisplay = capitalizeFirstLetter(originalAnswer);

        BorderPane pane = answerPanes[position];
        pane.getStyleClass().add("answer-revealed");

        Label answerTextLabel = new Label((position + 1) + ". " + answerToDisplay);
        answerTextLabel.getStyleClass().clear();
        answerTextLabel.getStyleClass().addAll("answer-text-label", "revealed");

        Label pointsLabel = new Label(points + " pkt");
        pointsLabel.getStyleClass().clear();
        pointsLabel.getStyleClass().addAll("points-label", "revealed");

        pane.setCenter(answerTextLabel);
        pane.setRight(pointsLabel);
        BorderPane.setAlignment(answerTextLabel, Pos.CENTER_LEFT);
        BorderPane.setAlignment(pointsLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(pointsLabel, new Insets(0, 10, 0, 10));

        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.play();
    }

    private void registerError() {
        if (gameFinished) return;

        boolean hadControlBeforeError = hasControl;
        boolean isStealing = stealOpportunity;
        stealOpportunity = false;

        int currentErrors;
        VBox panelToUpdate;
        boolean shouldDisplayErrorX = hadControlBeforeError || isStealing;

        if (isTeam1Turn) {
            team1Errors++;
            currentErrors = team1Errors;
            panelToUpdate = team1ErrorsPanel;
        } else {
            team2Errors++;
            currentErrors = team2Errors;
            panelToUpdate = team2ErrorsPanel;
        }

        if (shouldDisplayErrorX) {
            updateErrorsPanel(panelToUpdate, currentErrors);
        }

        if (isStealing) {
            showInfo("Błędna odpowiedź przy próbie przejęcia! Punkty ("+ roundPoints +") wędrują do drużyny " + getOpponentTeamName() + "!", false);
            awardPointsToOpponent();
            endRound(false);
        } else if (hadControlBeforeError) {
            if (currentErrors >= 3) {
                showInfo("Trzeci błąd! Szansa na przejęcie dla drużyny " + getOpponentTeamName() + "!", false);
                giveStealOpportunity();
            } else {
                showInfo("Błąd! Pozostało prób: " + (3 - currentErrors));
                continueTurnForCurrentTeam();
            }
        } else {
            if (!firstAnswerInRound) {
                if (firstTeamAnswerPosition == -1) {
                    showInfo("Błędna odpowiedź! Szansa dla drużyny " + getOpponentTeamName() + ".");
                    switchTeam();
                } else {
                    showInfo("Błędna odpowiedź, nie udało się przejąć. Kontrola dla drużyny " + getOpponentTeamName() + ".");
                    isTeam1Turn = !isTeam1Turn;
                    hasControl = true;
                    resetErrorsAndPanels();
                    updateTeamLabels();
                    updateCurrentPlayerLabel();
                    resetTimer();
                }
            } else {
                firstAnswerInRound = false;
                firstTeamAnswerPosition = -1;
                showInfo("Błędna odpowiedź! Szansa dla drużyny " + getOpponentTeamName() + ".");
                switchTeam();
            }
        }
    }

    private void giveStealOpportunity() {
        hasControl = false;
        stealOpportunity = true;
        if (timer != null) {
            timer.stop();
        }

        showInfo("Uwaga! Drużyna " + getOpponentTeamName() + " ma jedną szansę na przejęcie wszystkich punktów!", false);

        Timeline pause = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (gameFinished) return;
            isTeam1Turn = !isTeam1Turn;
            updateTeamLabels();
            updateCurrentPlayerLabel();

            answerField.setPromptText("Jedna odpowiedź, aby przejąć punkty!");
            resetTimer();
        }));
        pause.play();
    }

    private void resetErrorsAndPanels() {
        team1Errors = 0;
        team2Errors = 0;
        updateErrorsPanel(team1ErrorsPanel, 0);
        updateErrorsPanel(team2ErrorsPanel, 0);
    }

    private void handleTimeOut() {
        if (gameFinished) return;

        System.out.println("Timeout occurred for team: " + getCurrentTeamName());
        answerField.setDisable(true);

        advanceCurrentPlayerIndex();

        VBox panelToUpdate;
        int errorsAfterTimeout;
        boolean hadControlBeforeTimeout = hasControl;
        boolean wasStealing = stealOpportunity;
        stealOpportunity = false;

        if (isTeam1Turn) {
            team1Errors++;
            errorsAfterTimeout = team1Errors;
            panelToUpdate = team1ErrorsPanel;
        } else {
            team2Errors++;
            errorsAfterTimeout = team2Errors;
            panelToUpdate = team2ErrorsPanel;
        }

        if (hadControlBeforeTimeout || wasStealing) {
            updateErrorsPanel(panelToUpdate, errorsAfterTimeout);
        }

        String message = "Czas minął! ";
        Runnable nextAction = null;

        if (wasStealing) {
            System.out.println("Timeout during steal attempt.");
            message += "Próba przejęcia nieudana. Punkty ("+ roundPoints +") wędrują do drużyny " + getOpponentTeamName() + "!";
            nextAction = () -> {
                awardPointsToOpponent();
                endRound(false);
            };
        } else if (hadControlBeforeTimeout) {
            System.out.println("Timeout while team had control. Errors: " + errorsAfterTimeout);
            if (errorsAfterTimeout >= 3) {
                message += "Trzeci błąd! Szansa na przejęcie dla drużyny " + getOpponentTeamName() + "!";
                nextAction = this::giveStealOpportunity;
            } else {
                message += "Kolejka przechodzi do następnego gracza w tej samej drużynie.";
                nextAction = this::continueTurnForCurrentTeam;
            }
        } else {
            System.out.println("Timeout while team did not have control.");
            if (!firstAnswerInRound) {
                if (firstTeamAnswerPosition == -1) {
                    message += "Szansa dla drużyny " + getOpponentTeamName() + ".";
                    nextAction = this::switchTeam;
                } else {
                    message += "Nie udało się przejąć kontroli. Kontrola dla drużyny " + getOpponentTeamName() + ".";
                    nextAction = () -> {
                        isTeam1Turn = !isTeam1Turn;
                        hasControl = true;
                        resetErrorsAndPanels();
                        updateTeamLabels();
                        updateCurrentPlayerLabel();
                        resetTimer();
                    };
                }
            } else {
                firstAnswerInRound = false;
                firstTeamAnswerPosition = -1;
                message += "Kolejka przechodzi do drużyny " + getOpponentTeamName() + ".";
                nextAction = this::switchTeam;
            }
        }

        showInfo(message, false);

        final Runnable finalNextAction = nextAction;
        if (finalNextAction != null) {
            finalNextAction.run();
        }
    }

    private void endRound(boolean pointsAwardedToCurrentTeam) {
        if (!gameFinished) {
            gameFinished = true;
            answerField.setDisable(true);
            if (timer != null) {
                timer.stop();
            }
            timerWasRunningBeforeAlert = false;

            if (pointsAwardedToCurrentTeam) {
                if (isTeam1Turn) team1Score += roundPoints;
                else team2Score += roundPoints;
            }

            updateTeamLabels();
            Platform.runLater(this::showEndOfRoundDialog);
        }
    }

    private void showEndOfRoundDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Koniec rundy " + currentRoundNumber + "!");
        alert.setHeaderText("Runda " + currentRoundNumber + " zakończona! Aktualny wynik:");

        Label content = new Label(
                team1Name + ": " + team1Score + " pkt\n" +
                        team2Name + ": " + team2Score + " pkt\n\n" +
                        (currentRoundNumber < MAX_ROUNDS ? "Trwa ładowanie kolejnego pytania..." : "Gra zakończona!")
        );
        content.setFont(Font.font("Arial", 20));
        content.setWrapText(true);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setStyle("-fx-progress-color: gold;");
        loadingIndicator.setPrefSize(50, 50);
        loadingIndicator.setVisible(currentRoundNumber < MAX_ROUNDS);

        VBox alertContent = new VBox(20, content, loadingIndicator);
        alertContent.setAlignment(Pos.CENTER);
        alertContent.setPadding(new Insets(20));

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów dla alertu: " + e.getMessage());
        }
        dialogPane.setContent(alertContent);
        dialogPane.setPrefSize(500, 300);

        new Thread(() -> {
            if (currentRoundNumber < MAX_ROUNDS) {
                loadNewQuestion();
            }
            Platform.runLater(() -> {
                alert.close();
                prepareNewRound();
            });
        }).start();

        alert.show();
    }

    private void awardPointsToOpponent() {
        if (isTeam1Turn) {
            team2Score += roundPoints;
        } else {
            team1Score += roundPoints;
        }
        updateTeamLabels();
    }

    private void loadNewQuestion() {
        System.out.println("Ładowanie nowego pytania...");
        String newQuestion;
        int attempts = 0;
        final int MAX_ATTEMPTS_LOAD = 10;

        do {
            try {
                gameService = new GameService(selectedCategory);
                newQuestion = gameService.getCurrentQuestion();
            } catch (RuntimeException e) {
                System.err.println("Krytyczny błąd podczas ładowania pytania z bazy: " + e.getMessage());
                gameService = null;
                newQuestion = null;
                break;
            }

            attempts++;
            if (newQuestion == null) {
                System.err.println("Błąd: Nie udało się załadować pytania z bazy dla kategorii: " + selectedCategory + " (próba " + attempts + ")");
                if(attempts >= MAX_ATTEMPTS_LOAD) break;
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }

            String normalizedNewQuestion = TextNormalizer.normalizeToBaseForm(newQuestion);

            if (!usedQuestions.contains(normalizedNewQuestion)) {
                usedQuestions.add(normalizedNewQuestion);
                System.out.println("Załadowano nowe pytanie: " + newQuestion);
                return;
            }

            System.out.println("Pytanie '" + newQuestion + "' już było. Losuję kolejne... (próba " + attempts + ")");

        } while (attempts < MAX_ATTEMPTS_LOAD);

        if (gameService == null || gameService.getCurrentQuestion() == null || usedQuestions.contains(TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion())) ) {
            System.err.println("Nie udało się znaleźć nowego, nieużywanego pytania po " + MAX_ATTEMPTS_LOAD + " próbach.");
            if (gameService != null) {
                try {
                    java.lang.reflect.Field questionField = GameService.class.getDeclaredField("currentQuestion");
                    questionField.setAccessible(true);
                    questionField.set(gameService, null);
                } catch (Exception e) {
                    System.err.println("Nie można ustawić currentQuestion na null przez refleksję.");
                    gameService = null;
                }
            }
        } else {
            System.out.println("Załadowano nowe pytanie (po pętli): " + gameService.getCurrentQuestion());
        }
    }

    private void prepareNewRound() {
        currentRoundNumber++;

        if (currentRoundNumber > MAX_ROUNDS || gameService == null || gameService.getCurrentQuestion() == null) {
            if(currentRoundNumber > MAX_ROUNDS) {
                System.out.println("Osiągnięto maksymalną liczbę rund (" + MAX_ROUNDS + "). Koniec gry.");
            } else {
                System.err.println("Nie można przygotować nowej rundy - brak pytania lub błąd ładowania.");
            }
            gameFinished = true;
            Platform.runLater(this::showGameEndDialog);
            return;
        }

        System.out.println("Przygotowywanie nowej rundy " + currentRoundNumber + "...");
        gameFinished = false;
        roundPoints = 0;
        roundPointsLabel.setText("Pkt: 0");
        revealedAnswers = 0;
        hasControl = false;
        stealOpportunity = false;
        firstAnswerInRound = true;
        firstTeamAnswer = null;
        firstTeamAnswerPosition = -1;

        resetErrorsAndPanels();

        isTeam1Turn = !isTeam1Turn;
        updateTeamLabels();
        // --- ZMIANA: Ustaw indeksy startowe dla obu drużyn ZANIM pokażesz gracza ---
        setInitialPlayerIndexForRound();
        updateCurrentPlayerLabel();
        // --- KONIEC ZMIANY ---

        questionLabel.setText(getFormattedQuestion());

        for (int i = 0; i < 6; i++) {
            resetAnswerPane(i);
        }

        answerField.setDisable(false);
        answerField.requestFocus();
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        resetTimer();

        System.out.println("Nowa runda (" + currentRoundNumber + "/" + MAX_ROUNDS + ") rozpoczęta. Zaczyna drużyna: " + getCurrentTeamName());
    }

    private void showGameEndDialog() {
        gameFinished = true;
        answerField.setDisable(true);
        if (timer != null) {
            timer.stop();
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Koniec Gry!");

        String winnerText;
        if (team1Score > team2Score) {
            winnerText = "Wygrywa drużyna " + team1Name + "!";
        } else if (team2Score > team1Score) {
            winnerText = "Wygrywa drużyna " + team2Name + "!";
        } else {
            winnerText = "Remis!";
        }

        int roundsPlayed = Math.min(currentRoundNumber -1, MAX_ROUNDS);
        if (currentRoundNumber > MAX_ROUNDS) roundsPlayed = MAX_ROUNDS;

        alert.setHeaderText("Koniec gry po " + roundsPlayed + " rundach!\n" + winnerText);
        alert.setContentText(
                "Wynik końcowy:\n" +
                        team1Name + ": " + team1Score + " pkt\n" +
                        team2Name + ": " + team2Score + " pkt"
        );

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów dla alertu końca gry: " + e.getMessage());
        }
        dialogPane.setPrefSize(500, 300);

        ButtonType closeButton = new ButtonType("Zamknij grę", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(closeButton);

        alert.showAndWait();
        stage.close();
    }

    private void resetTimer() {
        if (gameFinished) return;

        if (timer != null) {
            timer.stop();
        }
        timeLeft = initialAnswerTime;
        updateTimerDisplay();

        answerField.setDisable(false);
        answerField.requestFocus();

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (gameFinished) {
                        timer.stop();
                        return;
                    }
                    timeLeft--;
                    updateTimerDisplay();
                    if (timeLeft <= 0) {
                        timer.stop();
                        answerField.setDisable(true);
                        Platform.runLater(this::handleTimeOut);
                    }
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        timerWasRunningBeforeAlert = true;
    }

    private void switchTeam() {
        isTeam1Turn = !isTeam1Turn;
        updateTeamLabels();
        updateCurrentPlayerLabel();
        System.out.println("Zmiana tury. Teraz odpowiada: " + getCurrentTeamName() + " Gracz index: " + (isTeam1Turn ? team1PlayerIndex : team2PlayerIndex));
        answerField.setDisable(false);
        answerField.requestFocus();
        resetTimer();
    }

    private String getCurrentTeamName() {
        return isTeam1Turn ? team1Name : team2Name;
    }

    private String getOpponentTeamName() {
        return isTeam1Turn ? team2Name : team1Name;
    }

    private void showInfo(String message) {
        showInfo(message, true);
    }

    private void showInfo(String message, boolean resetTimerAfter) {
        if (gameFinished && currentRoundNumber > MAX_ROUNDS) return;

        boolean wasTimerRunning = (timer != null && timer.getStatus() == Timeline.Status.RUNNING);
        if (wasTimerRunning) {
            timer.stop();
            System.out.println("Timer zatrzymany przez alert: " + message);
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Informacja");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów dla alertu informacyjnego: " + e.getMessage());
        }

        alert.showAndWait();
    }

    private void showCriticalError(String message) {
        if (timer != null && timer.getStatus() == Timeline.Status.RUNNING) {
            timer.stop();
            System.out.println("Timer zatrzymany przez błąd krytyczny.");
        }
        gameFinished = true;
        answerField.setDisable(true);

        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd krytyczny");
        alert.setHeaderText("Wystąpił poważny błąd!");
        alert.setContentText(message + "\n\nAplikacja może wymagać ponownego uruchomienia.");
        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów dla alertu błędu: " + e.getMessage());
        }

        if (Platform.isFxApplicationThread()) {
            alert.showAndWait();
        } else {
            Platform.runLater(() -> alert.showAndWait()); // Ensure lambda is used
        }
    }

    public void show() {
        stage.show();
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else {
            System.err.println("Nie można wykonać animacji FadeTransition - scena lub root jest null.");
        }
    }
}