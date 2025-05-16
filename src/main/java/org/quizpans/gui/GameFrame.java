package org.quizpans.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
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
import org.quizpans.utils.AutoClosingAlerts;
import org.quizpans.services.GameService;
import org.quizpans.utils.TextNormalizer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class GameFrame {
    private Stage stage;
    private GameService gameService;
    private BorderPane gameContentPane;
    private StackPane rootStackPane;
    private VBox pauseMenuPane;
    private boolean uiInitialized = false;
    private boolean isPaused = false;

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
    private Label currentPlayerLabel = new Label("Odpowiada teraz: ...");

    private final int initialAnswerTime;
    private final String selectedCategory;
    private final String team1Name;
    private final String team2Name;
    private final List<String> team1Members;
    private final List<String> team2Members;
    private int team1PlayerIndex = 0;
    private int team2PlayerIndex = 0;
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
    private int team1Errors = 0;
    private int team2Errors = 0;
    private int currentRoundNumber = 1;
    private final int MAX_ROUNDS = 6;
    private boolean timerWasRunningBeforeAlert = false;
    private int roundPoints = 0;
    private Set<String> revealedAnswerBaseFormsInRound = new HashSet<>();

    private Map<String, List<Integer>> playerCorrectAnswersPerRound = new LinkedHashMap<>();
    private Set<String> allPlayerNamesForStats = new LinkedHashSet<>();

    private static final String HIDDEN_ANSWER_PLACEHOLDER = "■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■";

    public GameFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, boolean isTeam1Turn, List<String> team1Members, List<String> team2Members, Stage stage, GameService existingGameService) {
        this.stage = stage;
        this.selectedCategory = selectedCategory;
        this.initialAnswerTime = answerTime;
        this.timeLeft = answerTime;
        this.isTeam1Turn = isTeam1Turn;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = (team1Members != null && !team1Members.isEmpty()) ? new ArrayList<>(team1Members) : Collections.singletonList(team1Name);
        this.team2Members = (team2Members != null && !team2Members.isEmpty()) ? new ArrayList<>(team2Members) : Collections.singletonList(team2Name);
        this.gameService = existingGameService;

        this.team1Label = createTeamLabel(this.team1Name);
        this.team2Label = createTeamLabel(this.team2Name);
        this.team1TotalLabel = createTotalScoreLabel();
        this.team2TotalLabel = createTotalScoreLabel();
        this.team1ErrorsPanel = createErrorsPanel(true);
        this.team2ErrorsPanel = createErrorsPanel(false);

        if (this.gameService == null) {
            try {
                this.gameService = new GameService(selectedCategory);
            } catch (RuntimeException e) {
                showCriticalError("Nie udało się załadować danych gry przy inicjalizacji: " + e.getMessage());
            }
        } else if (this.gameService.getCurrentQuestion() == null && !gameFinished) {
            try {
                this.gameService.loadQuestion();
            } catch (RuntimeException e) {
                if (!gameFinished) {
                    showCriticalError("Nie udało się załadować danych gry przy ponownym ładowaniu: " + e.getMessage());
                }
            }
        }
    }

    public void initializeGameContent() {
        if (!uiInitialized) {
            initUI();
        }
        setFrameProperties();
        applyStylesheets();
        setInitialPlayerIndexForRound();
        updateTeamLabels();
        updateCurrentPlayerLabel();

        if (gameService != null && gameService.getCurrentQuestion() != null) {
            String normalizedQuestion = TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion());
            if (!usedQuestions.contains(normalizedQuestion)) {
                usedQuestions.add(normalizedQuestion);
            }
            prepareNewRoundVisuals();
            startTimer();
        } else {
            if (uiInitialized && !gameFinished) {
                Platform.runLater(this::showEndOfRoundOrGameScreen);
            } else if (uiInitialized && gameFinished) {

            }
            else {
                showCriticalError("Nie udało się załadować danych gry (pytanie początkowe). Gra nie może być kontynuowana.");
            }
        }
    }

    private void setFrameProperties() {
        stage.setTitle("QuizPans - Rozgrywka");
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream != null) {
                if (stage.getIcons().isEmpty()) {
                    stage.getIcons().add(new Image(logoStream));
                }
                logoStream.close();
            }
        } catch (Exception e) { System.err.println("Błąd ładowania ikony aplikacji: " + e.getMessage()); }
    }

    private void applyStylesheets() {
        Scene scene = stage.getScene();
        if (scene != null) {
            try {
                String cssPath = getClass().getResource("/styles.css").toExternalForm();
                if (cssPath != null && !scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().add(cssPath);
                }
            } catch (Exception e) { System.err.println("Błąd ładowania arkusza stylów: " + e.getMessage());}
        }
    }

    public Parent getRootPane() {
        if (rootStackPane == null) {
            initUI();
        }
        return rootStackPane;
    }

    private void initUI() {
        if (uiInitialized) { return; }

        gameContentPane = new BorderPane();
        LinearGradient gradient = new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#1a2a6c")),new Stop(1,Color.web("#b21f1f")));
        gameContentPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        gameContentPane.setPadding(new Insets(20));

        VBox topPanel = new VBox(15); topPanel.setAlignment(Pos.CENTER);
        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36)); questionLabel.setTextFill(Color.WHITE);
        questionLabel.setEffect(new DropShadow(15, Color.BLACK)); questionLabel.setWrapText(true); questionLabel.setAlignment(Pos.CENTER);
        if (gameService != null && gameService.getCurrentQuestion() != null) {
            questionLabel.setText(getFormattedQuestion());
        } else {
            questionLabel.setText("Ładowanie pytania...");
        }

        HBox timerBox = new HBox(25); timerBox.setAlignment(Pos.CENTER);
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        updateTimerDisplay();
        roundPointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32)); roundPointsLabel.setTextFill(Color.GOLD);
        roundPointsLabel.setEffect(new DropShadow(10, Color.BLACK));
        timerBox.getChildren().addAll(timerLabel, roundPointsLabel);
        topPanel.getChildren().addAll(questionLabel, timerBox);
        gameContentPane.setTop(topPanel); BorderPane.setMargin(topPanel, new Insets(20, 0, 20, 0));

        GridPane centerContainer = new GridPane(); centerContainer.setAlignment(Pos.CENTER); centerContainer.setHgap(15);
        ColumnConstraints col1=new ColumnConstraints(); col1.setPercentWidth(10); col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2=new ColumnConstraints(); col2.setPercentWidth(80); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3=new ColumnConstraints(); col3.setPercentWidth(10); col3.setHalignment(HPos.CENTER);
        centerContainer.getColumnConstraints().addAll(col1, col2, col3);
        StackPane leftErrorPane = new StackPane(team1ErrorsPanel); leftErrorPane.setAlignment(Pos.CENTER);
        StackPane rightErrorPane = new StackPane(team2ErrorsPanel); rightErrorPane.setAlignment(Pos.CENTER);
        VBox answersPanel = new VBox(10); answersPanel.setAlignment(Pos.CENTER); // Zmniejszono nieco VBox spacing
        answersPanel.setPadding(new Insets(10)); answersPanel.getStyleClass().add("answer-panel"); // Zmniejszono nieco padding
        for (int i = 0; i < 6; i++) { answerPanes[i] = createAnswerPane(i + 1); answersPanel.getChildren().add(answerPanes[i]); }
        centerContainer.add(leftErrorPane, 0, 0); centerContainer.add(answersPanel, 1, 0); centerContainer.add(rightErrorPane, 2, 0);
        gameContentPane.setCenter(centerContainer);

        VBox bottomContainer = new VBox(15); bottomContainer.setAlignment(Pos.CENTER); bottomContainer.setPadding(new Insets(20, 0, 20, 0));
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22)); currentPlayerLabel.setTextFill(Color.WHITE);
        currentPlayerLabel.setEffect(new DropShadow(5, Color.BLACK));
        HBox inputAndScoresBox = new HBox(25); inputAndScoresBox.setAlignment(Pos.CENTER);
        VBox team1Box = new VBox(10, team1Label, team1TotalLabel); team1Box.setAlignment(Pos.CENTER_LEFT); team1Box.setPadding(new Insets(0,0,0,30));
        answerField.setPrefWidth(450); answerField.setPrefHeight(60); answerField.setFont(Font.font("Arial", 24));
        answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter"); answerField.setOnAction(e -> processAnswer());
        VBox team2Box = new VBox(10, team2Label, team2TotalLabel); team2Box.setAlignment(Pos.CENTER_RIGHT); team2Box.setPadding(new Insets(0,30,0,0));
        inputAndScoresBox.getChildren().addAll(team1Box, answerField, team2Box);
        HBox.setHgrow(team1Box, Priority.ALWAYS); HBox.setHgrow(answerField, Priority.NEVER); HBox.setHgrow(team2Box, Priority.ALWAYS);
        bottomContainer.getChildren().addAll(currentPlayerLabel, inputAndScoresBox);
        gameContentPane.setBottom(bottomContainer);

        Button pauseButton = new Button();
        try (InputStream pauseIconStream = getClass().getResourceAsStream("/pause_icon.png")) {
            if (pauseIconStream != null) {
                ImageView pauseImageView = new ImageView(new Image(pauseIconStream));
                pauseImageView.setFitHeight(32);
                pauseImageView.setFitWidth(32);
                pauseButton.setGraphic(pauseImageView);
            } else {
                pauseButton.setText("||");
                pauseButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            }
        } catch (Exception e) {
            pauseButton.setText("||");
            pauseButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            System.err.println("Nie udało się załadować ikony pauzy: " + e.getMessage());
        }
        pauseButton.getStyleClass().add("pause-button");
        pauseButton.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 25px; -fx-padding: 10px;");
        pauseButton.setOnAction(e -> togglePauseMenu());

        StackPane.setAlignment(pauseButton, Pos.TOP_RIGHT);
        StackPane.setMargin(pauseButton, new Insets(15, 15, 0, 0));

        pauseMenuPane = createPauseMenu();
        pauseMenuPane.setVisible(false);

        rootStackPane = new StackPane(gameContentPane, pauseMenuPane, pauseButton);
        pauseButton.visibleProperty().bind(pauseMenuPane.visibleProperty().not());

        uiInitialized = true;
    }

    private VBox createPauseMenu() {
        VBox menu = new VBox(20);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(50));
        menu.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-background-radius: 20px;");
        menu.setMaxSize(400, 300);

        Label pauseTitle = new Label("Pauza");
        pauseTitle.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        pauseTitle.setTextFill(Color.GOLD);

        Button resumeButton = new Button("Wznów Grę");
        resumeButton.getStyleClass().add("main-menu-button");
        resumeButton.setOnAction(e -> togglePauseMenu());

        Button mainMenuButton = new Button("Menu Główne");
        mainMenuButton.getStyleClass().add("main-menu-button");
        mainMenuButton.setOnAction(e -> returnToSetupScreen());

        Button exitAppButton = new Button("Zakończ Aplikację");
        exitAppButton.getStyleClass().add("main-menu-button");
        String originalStyle = resumeButton.getStyle() != null ? resumeButton.getStyle() : "";
        exitAppButton.setStyle(originalStyle + "-fx-background-color: #F44336;");
        exitAppButton.setOnAction(e -> Platform.exit());

        menu.getChildren().addAll(pauseTitle, resumeButton, mainMenuButton, exitAppButton);
        return menu;
    }

    private void togglePauseMenu() {
        isPaused = !isPaused;
        if (isPaused) {
            if (timer != null && timer.getStatus() == Animation.Status.RUNNING) {
                timer.pause();
                timerWasRunningBeforeAlert = true;
            }
            answerField.setDisable(true);
            pauseMenuPane.setVisible(true);
            gameContentPane.setEffect(new GaussianBlur(10));
            pauseMenuPane.toFront();

        } else {
            pauseMenuPane.setVisible(false);
            gameContentPane.setEffect(null);
            answerField.setDisable(gameFinished);
            if (timerWasRunningBeforeAlert && timer != null && !gameFinished) {
                timer.play();
            }
            timerWasRunningBeforeAlert = false;
        }
    }

    private VBox createErrorsPanel(boolean isLeftPanel) {
        VBox panel = new VBox(30); panel.setPadding(new Insets(20));
        for (int i = 0; i < 3; i++) {
            Label errorLabel = new Label(""); errorLabel.getStyleClass().add("error-circle-empty");
            errorLabel.setMinSize(95, 95); errorLabel.setAlignment(Pos.CENTER);
            panel.getChildren().add(errorLabel);
        }
        panel.getStyleClass().add(isLeftPanel ? "error-panel-left" : "error-panel-right");
        return panel;
    }

    private void updateErrorsPanel(VBox panel, int errors) {
        if (panel == null || panel.getChildren().isEmpty()) { return; }
        int errorsToDisplay = Math.min(errors, panel.getChildren().size());
        for (int i = 0; i < panel.getChildren().size(); i++) {
            if (!(panel.getChildren().get(i) instanceof Label)) continue;
            Label label = (Label) panel.getChildren().get(i);
            boolean shouldBeFilled = i < errorsToDisplay; boolean wasFilled = label.getStyleClass().contains("error-circle");
            if (shouldBeFilled) {
                if (!wasFilled) {
                    label.getStyleClass().remove("error-circle-empty"); label.getStyleClass().add("error-circle"); label.setText("X");
                    if (i == errorsToDisplay - 1) {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), label); ft.setFromValue(0); ft.setToValue(1); ft.play();
                        label.setScaleX(1.3); label.setScaleY(1.3);
                        ScaleTransition st = new ScaleTransition(Duration.millis(300), label); st.setToX(1.0); st.setToY(1.0); st.play();
                    }
                }
            } else {
                if (wasFilled) { label.getStyleClass().remove("error-circle"); label.getStyleClass().add("error-circle-empty"); label.setText(""); }
            }
        }
    }

    private Label createTeamLabel(String teamNameText) {
        Label label = new Label(teamNameText); label.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        label.setTextFill(Color.WHITE); label.setEffect(new DropShadow(5, Color.BLACK)); return label;
    }

    private Label createTotalScoreLabel() {
        Label label = new Label("0"); label.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        label.setTextFill(Color.GOLD); label.setEffect(new DropShadow(5, Color.BLACK)); return label;
    }

    private void updateTeamLabels() {
        if (team1Label == null || team2Label == null || team1TotalLabel == null || team2TotalLabel == null) { return; }
        if (isTeam1Turn) {
            team1Label.setTextFill(Color.GOLD); team1Label.setEffect(new DropShadow(10, Color.GOLD));
            team2Label.setTextFill(Color.WHITE); team2Label.setEffect(new DropShadow(5, Color.BLACK));
            team1Label.getStyleClass().setAll("team-label","team-active");
            team2Label.getStyleClass().setAll("team-label","team-inactive");
        } else {
            team1Label.setTextFill(Color.WHITE); team1Label.setEffect(new DropShadow(5, Color.BLACK));
            team2Label.setTextFill(Color.GOLD); team2Label.setEffect(new DropShadow(10, Color.GOLD));
            team1Label.getStyleClass().setAll("team-label","team-inactive");
            team2Label.getStyleClass().setAll("team-label","team-active");
        }
        team1TotalLabel.setText(String.valueOf(team1Score)); team2TotalLabel.setText(String.valueOf(team2Score));
    }

    private void updateCurrentPlayerLabel() {
        if (currentPlayerLabel == null) { return; }
        String currentPlayerNameText = getCurrentPlayerNameForStats();
        if (currentPlayerNameText != null) {
            currentPlayerLabel.setText("Odpowiada teraz: " + currentPlayerNameText);
        } else {
            currentPlayerLabel.setText("Odpowiada teraz: ...");
        }
    }

    private String getCurrentPlayerNameForStats() {
        try {
            if (isTeam1Turn) {
                return team1Members.isEmpty() || team1PlayerIndex < 0 || team1PlayerIndex >= team1Members.size() ? team1Name : team1Members.get(team1PlayerIndex);
            } else {
                return team2Members.isEmpty() || team2PlayerIndex < 0 || team2PlayerIndex >= team2Members.size() ? team2Name : team2Members.get(team2PlayerIndex);
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Błąd indeksu przy pobieraniu nazwy gracza: T1 idx=" + team1PlayerIndex + ", T2 idx=" + team2PlayerIndex);
            return isTeam1Turn ? team1Name : team2Name;
        }
    }

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
    }

    private String getFormattedQuestion() {
        if (gameService == null || gameService.getCurrentQuestion() == null) return "Błąd ładowania pytania...";
        String baseQuestion = gameService.getCurrentQuestion();
        return "Pytanie numer " + currentRoundNumber + ": " + baseQuestion + (baseQuestion.endsWith("?") ? "" : "?");
    }

    private BorderPane createAnswerPane(int number) {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("answer-pane");
        pane.setPrefHeight(70); // Zwiększona preferowana wysokość wiersza
        pane.setPadding(new Insets(10, 15, 10, 15)); // Dodany wewnętrzny padding

        Label placeholderLabel = new Label(number + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 24)); // Zwiększona czcionka
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");
        placeholderLabel.setMaxHeight(Double.MAX_VALUE);

        pane.setCenter(placeholderLabel);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
        return pane;
    }

    private void resetAnswerPane(int index) {
        if (index < 0 || index >= answerPanes.length || answerPanes[index] == null) { return; }
        BorderPane pane = answerPanes[index];
        pane.getStyleClass().remove("answer-revealed");
        pane.setPrefHeight(70); // Upewnij się, że wysokość jest spójna
        pane.setPadding(new Insets(10, 15, 10, 15)); // Upewnij się, że padding jest spójny

        Label placeholderLabel = new Label((index + 1) + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 24)); // Spójna czcionka
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().clear();
        placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");
        placeholderLabel.setMaxHeight(Double.MAX_VALUE);

        pane.setCenter(placeholderLabel);
        pane.setRight(null);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
    }

    private void startTimer() {
        if (isPaused || gameFinished) { if(timer != null) timer.stop(); return; }
        if (timer != null) timer.stop();
        timeLeft = initialAnswerTime; updateTimerDisplay();
        timer = new Timeline( new KeyFrame(Duration.seconds(1), e -> {
            if (isPaused || gameFinished) { if(timer != null) timer.stop(); return; }
            timeLeft--; updateTimerDisplay();
            if (timeLeft <= 0) {
                if(timer != null) timer.stop();
                if(answerField != null) answerField.setDisable(true);
                Platform.runLater(this::handleTimeOut);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE); timer.play();
        timerWasRunningBeforeAlert = true;
    }

    private void updateTimerDisplay() {
        if (timerLabel == null) { return; }
        timerLabel.setText("Czas: " + timeLeft + "s");
        timerLabel.getStyleClass().removeAll("timer-critical", "timer-normal", "timer-label");
        timerLabel.getStyleClass().add("timer-label");
        timerLabel.getStyleClass().add(timeLeft <= 10 ? "timer-critical" : "timer-normal");
    }

    private void processAnswer() {
        if (isPaused || gameFinished || answerField == null || answerField.isDisabled()) {
            return;
        }
        String userAnswer = answerField.getText().trim();

        if (userAnswer.isEmpty()) {
            showInfo("Pusta odpowiedź! Liczymy jako błąd.");
            registerError();
            return;
        }
        if (gameService == null) { showCriticalError("Błąd wewnętrzny gry (serwis gry niedostępny)."); return; }

        Optional<String> correctAnswerOpt = gameService.checkAnswer(userAnswer);
        answerField.clear();

        if (correctAnswerOpt.isPresent()) {
            handleCorrectAnswer(correctAnswerOpt.get());
        } else {
            registerError();
        }
    }
    private void advancePlayerInCurrentTeam() {
        if (isTeam1Turn) {
            if (!team1Members.isEmpty()) {
                team1PlayerIndex = (team1PlayerIndex + 1) % team1Members.size();
            }
        } else {
            if (!team2Members.isEmpty()) {
                team2PlayerIndex = (team2PlayerIndex + 1) % team2Members.size();
            }
        }
        updateCurrentPlayerLabel();
    }

    private void continueTurnForCurrentTeam() {
        advancePlayerInCurrentTeam();
        resetTimer();
    }

    private void handleCorrectAnswer(String correctAnswerBaseForm) {
        if (isPaused || gameFinished || gameService == null) return;

        int points = gameService.getPoints(correctAnswerBaseForm);
        int position = gameService.getAnswerPosition(correctAnswerBaseForm);

        if (position < 0 || position >= answerPanes.length || answerPanes[position] == null || answerPanes[position].getStyleClass().contains("answer-revealed")) {
            showInfo("Ta odpowiedź została już odkryta lub jest nieprawidłowa pozycja!");
            resetTimer();
            return;
        }

        String currentPlayerName = getCurrentPlayerNameForStats();
        if (currentPlayerName != null) {
            allPlayerNamesForStats.add(currentPlayerName);
            playerCorrectAnswersPerRound.putIfAbsent(currentPlayerName, new ArrayList<>());
            List<Integer> scores = playerCorrectAnswersPerRound.get(currentPlayerName);
            while (scores.size() < currentRoundNumber) {
                scores.add(0);
            }
            scores.set(currentRoundNumber - 1, scores.get(currentRoundNumber - 1) + 1);
        }

        boolean wasStealing = stealOpportunity;
        stealOpportunity = false;

        revealAnswer(correctAnswerBaseForm, points, position);
        roundPoints += points;
        if(roundPointsLabel != null) roundPointsLabel.setText("Pkt: " + roundPoints);
        revealedAnswers++;

        if (wasStealing) {
            if (isTeam1Turn) team1Score += roundPoints; else team2Score += roundPoints;
            updateTeamLabels();
            showInfo("Przejęcie udane! Punkty (" + roundPoints + ") dla drużyny " + getCurrentTeamName() + "!", false);
            advancePlayerInCurrentTeam();
            endRound();
            return;
        }

        if (firstAnswerInRound) {
            firstAnswerInRound = false;
            firstTeamAnswer = correctAnswerBaseForm;
            firstTeamAnswerPosition = position;

            if (position != 0) {
                showInfo("Dobra odpowiedź! Ale czy najlepsza? Szansa dla drużyny " + getOpponentTeamName() + "!", false);
                advancePlayerInCurrentTeam();
                switchTeam();
            } else {
                hasControl = true;
                resetErrorsAndPanels();
                showInfo("Najlepsza odpowiedź! Drużyna " + getCurrentTeamName() + " ma kontrolę!", false);
                continueTurnForCurrentTeam();
            }
        } else if (!hasControl) {
            if (firstTeamAnswerPosition == -1) {
                hasControl = true; resetErrorsAndPanels();
                showInfo("Poprawna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę (fallback)!", false);
                continueTurnForCurrentTeam();
            } else {
                if (position < firstTeamAnswerPosition) {
                    hasControl = true;
                    resetErrorsAndPanels();
                    showInfo("Świetna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę!", false);
                    continueTurnForCurrentTeam();
                } else {
                    showInfo("Poprawna odpowiedź, ale nie lepsza niż drużyny " + getOpponentTeamName() + ". Kontrola dla " + getOpponentTeamName() + ".", false);
                    advancePlayerInCurrentTeam();
                    isTeam1Turn = !isTeam1Turn;
                    hasControl = true;
                    resetErrorsAndPanels();
                    updateTeamLabels();
                    updateCurrentPlayerLabel();
                    resetTimer();
                }
            }
        } else {
            if (revealedAnswers == gameService.getTotalAnswersCount() || revealedAnswers >= answerPanes.length) {
                if (isTeam1Turn) team1Score += roundPoints; else team2Score += roundPoints;
                updateTeamLabels();
                showInfo("Wszystkie odpowiedzi odkryte! Runda dla drużyny " + getCurrentTeamName() + "!", false);
                advancePlayerInCurrentTeam();
                endRound();
            } else {
                continueTurnForCurrentTeam();
            }
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    private void revealAnswer(String answerBaseForm, int points, int position) {
        if (gameService == null || position < 0 || position >= answerPanes.length || answerPanes[position] == null) {
            return;
        }
        revealedAnswerBaseFormsInRound.add(answerBaseForm);
        String originalAnswer = gameService.getOriginalAnswer(answerBaseForm);
        String answerToDisplay = capitalizeFirstLetter(originalAnswer);

        BorderPane pane = answerPanes[position];
        pane.getStyleClass().add("answer-revealed");
        pane.setPrefHeight(70); // Spójna wysokość
        pane.setPadding(new Insets(10, 15, 10, 15)); // Spójny padding

        Label answerTextLabel = new Label((position + 1) + ". " + answerToDisplay);
        answerTextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26)); // Zwiększona czcionka dla odkrytej odpowiedzi
        answerTextLabel.getStyleClass().setAll("answer-text-label", "revealed");
        answerTextLabel.setMaxHeight(Double.MAX_VALUE);

        Label pointsLabel = new Label(points + " pkt");
        pointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24)); // Zwiększona czcionka dla punktów
        pointsLabel.getStyleClass().setAll("points-label", "revealed");
        pointsLabel.setMaxHeight(Double.MAX_VALUE);

        pane.setCenter(answerTextLabel);
        pane.setRight(pointsLabel);
        BorderPane.setAlignment(answerTextLabel, Pos.CENTER_LEFT);
        BorderPane.setAlignment(pointsLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(pointsLabel, new Insets(0, 10, 0, 10));

        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    private void registerError() {
        if (isPaused || gameFinished) return;

        boolean hadControlBeforeError = hasControl;
        boolean isStealing = stealOpportunity;
        stealOpportunity = false;

        int currentTeamErrors;
        VBox panelToUpdate;

        if (isTeam1Turn) {
            team1Errors++;
            currentTeamErrors = team1Errors;
            panelToUpdate = team1ErrorsPanel;
        } else {
            team2Errors++;
            currentTeamErrors = team2Errors;
            panelToUpdate = team2ErrorsPanel;
        }

        if ((hadControlBeforeError || isStealing) && panelToUpdate != null) {
            updateErrorsPanel(panelToUpdate, currentTeamErrors);
        }

        if (isStealing) {
            showInfo("Błędna odpowiedź przy próbie przejęcia! Punkty ("+ roundPoints +") wędrują do drużyny " + getOpponentTeamName() + "!", false);
            advancePlayerInCurrentTeam();
            awardPointsToOpponent();
            endRound();
        } else if (hadControlBeforeError) {
            if (currentTeamErrors >= 3) {
                showInfo("Trzeci błąd! Szansa na przejęcie dla drużyny " + getOpponentTeamName() + "!", false);
                advancePlayerInCurrentTeam();
                giveStealOpportunity();
            } else {
                showInfo("Błąd! Pozostało prób: " + (3 - currentTeamErrors));
                continueTurnForCurrentTeam();
            }
        } else {
            advancePlayerInCurrentTeam();
            if (!firstAnswerInRound) {
                if (firstTeamAnswerPosition == -1) {
                    showInfo("Błędna odpowiedź! Szansa dla drużyny " + getOpponentTeamName() + ".");
                    switchTeam();
                } else {
                    showInfo("Błędna odpowiedź. Nie udało się przejąć kontroli. Kontrola dla drużyny " + getOpponentTeamName() + ".");
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
        hasControl = false; stealOpportunity = true;
        if (timer != null) timer.stop();
        timerWasRunningBeforeAlert = false;
        String stealingTeamName = getOpponentTeamName();
        showInfo("Uwaga! Drużyna " + stealingTeamName + " ma jedną szansę na przejęcie wszystkich punktów!", false);

        Timeline pause = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (isPaused || gameFinished) return;
            isTeam1Turn = !isTeam1Turn;
            updateTeamLabels();
            updateCurrentPlayerLabel();
            if(answerField != null) {
                answerField.setDisable(false);
                answerField.setPromptText("Jedna odpowiedź, aby przejąć punkty!");
                answerField.requestFocus();
            }
            resetTimer();
        }));
        pause.play();
    }

    private void resetErrorsAndPanels() {
        team1Errors = 0; team2Errors = 0;
        if(team1ErrorsPanel != null) updateErrorsPanel(team1ErrorsPanel, 0);
        if(team2ErrorsPanel != null) updateErrorsPanel(team2ErrorsPanel, 0);
    }

    private void handleTimeOut() {
        if (isPaused || gameFinished) return;
        if(answerField != null) answerField.setDisable(true);

        VBox panelToUpdate; int errorsAfterTimeout;
        boolean hadControlBeforeTimeout = hasControl;
        boolean wasStealing = stealOpportunity;
        stealOpportunity = false;

        if (isTeam1Turn) {
            team1Errors++; errorsAfterTimeout = team1Errors; panelToUpdate = team1ErrorsPanel;
        } else {
            team2Errors++; errorsAfterTimeout = team2Errors; panelToUpdate = team2ErrorsPanel;
        }

        if ((hadControlBeforeTimeout || wasStealing) && panelToUpdate != null) {
            updateErrorsPanel(panelToUpdate, errorsAfterTimeout);
        }

        String message = "Czas minął! "; Runnable nextAction = null;

        if (wasStealing) {
            message += "Próba przejęcia nieudana. Punkty ("+ roundPoints +") wędrują do drużyny " + getOpponentTeamName() + "!";
            advancePlayerInCurrentTeam();
            nextAction = () -> { awardPointsToOpponent(); endRound(); };
        } else if (hadControlBeforeTimeout) {
            if (errorsAfterTimeout >= 3) {
                message += "Trzeci błąd z powodu czasu! Szansa na przejęcie dla drużyny " + getOpponentTeamName() + "!";
                advancePlayerInCurrentTeam();
                nextAction = this::giveStealOpportunity;
            } else {
                message += "Kolejka przechodzi do następnego gracza w tej samej drużynie.";
                nextAction = this::continueTurnForCurrentTeam;
            }
        } else {
            advancePlayerInCurrentTeam();
            if (!firstAnswerInRound && firstTeamAnswerPosition != -1) {
                message += "Czas minął przy próbie przejęcia kontroli. Kontrola dla drużyny " + getOpponentTeamName() + ".";
                nextAction = () -> {
                    isTeam1Turn = !isTeam1Turn;
                    hasControl = true;
                    resetErrorsAndPanels();
                    updateTeamLabels();
                    updateCurrentPlayerLabel();
                    resetTimer();
                };
            } else {
                firstAnswerInRound = false;
                firstTeamAnswerPosition = -1;
                message += "Kolejka przechodzi do drużyny " + getOpponentTeamName() + ".";
                nextAction = this::switchTeam;
            }
        }
        showInfo(message, false);
        if (nextAction != null) Platform.runLater(nextAction);
    }

    private void endRound() {
        if (!gameFinished) {
            gameFinished = true;
            if(answerField != null) answerField.setDisable(true);
            if (timer != null) timer.stop();
            timerWasRunningBeforeAlert = false;
            updateTeamLabels();
            Platform.runLater(this::showEndOfRoundOrGameScreen);
        }
    }

    private void showEndOfRoundOrGameScreen() {
        boolean isFinalRound = (currentRoundNumber >= MAX_ROUNDS);
        boolean noMoreQuestionsAvailable = (gameService != null && gameService.getCurrentQuestion() == null && currentRoundNumber < MAX_ROUNDS && !isFinalRound);

        if (isFinalRound || noMoreQuestionsAvailable) {
            Map<String, List<Integer>> finalPlayerStats = new LinkedHashMap<>(playerCorrectAnswersPerRound);
            Runnable playAgainAction = this::returnToSetupScreen;
            Runnable exitAction = Platform::exit;

            EndGameFrame endGameScreen = new EndGameFrame(
                    this.stage,
                    this.team1Name,
                    this.team2Name,
                    this.team1Score,
                    this.team2Score,
                    new ArrayList<>(this.team1Members),
                    new ArrayList<>(this.team2Members),
                    finalPlayerStats,
                    playAgainAction,
                    exitAction
            );
            endGameScreen.show();
        } else {
            Set<String> revealedCopy = new HashSet<>(this.revealedAnswerBaseFormsInRound);
            Map<String, List<Integer>> statsCopy = new LinkedHashMap<>();
            for(Map.Entry<String, List<Integer>> entry : playerCorrectAnswersPerRound.entrySet()){
                statsCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            NextRoundFrame nextRoundScreen = new NextRoundFrame(
                    this.stage,
                    () -> {
                        GameFrame.this.prepareNewRound();
                        if (!gameFinished && GameFrame.this.rootStackPane != null && GameFrame.this.stage.getScene() != null) {
                            Parent gameRootPane = GameFrame.this.getRootPane();
                            GameFrame.this.stage.getScene().setRoot(gameRootPane);
                            FadeTransition fadeInGame = new FadeTransition(Duration.millis(300), gameRootPane);
                            fadeInGame.setFromValue(0.0);
                            fadeInGame.setToValue(1.0);
                            fadeInGame.play();
                        } else if (!gameFinished){
                            showCriticalError("Nie można wrócić do ekranu gry.");
                        }
                    },
                    this.gameService,
                    revealedCopy,
                    statsCopy,
                    this.currentRoundNumber,
                    this.team1Name,
                    this.team2Name,
                    new ArrayList<>(this.team1Members),
                    new ArrayList<>(this.team2Members),
                    this.team1Score,
                    this.team2Score
            );
            nextRoundScreen.show();
        }
    }

    private void awardPointsToOpponent() {
        if (isTeam1Turn) team2Score += roundPoints; else team1Score += roundPoints;
        updateTeamLabels();
    }

    private boolean loadNewQuestionForRound() {
        if (gameService == null) {
            try {
                gameService = new GameService(selectedCategory);
            } catch (Exception e) {
                if(!gameFinished) showCriticalError("Krytyczny błąd: Nie można utworzyć serwisu gry przy ładowaniu nowego pytania.");
                return false;
            }
        }

        int attempts = 0; final int MAX_ATTEMPTS_LOAD = 10;
        boolean questionLoadedSuccessfully = false;

        do {
            String newQuestion = null;
            try {
                gameService.loadQuestion();
                newQuestion = gameService.getCurrentQuestion();
            } catch (RuntimeException e) {
                newQuestion = null;
                System.err.println("Próba " + (attempts + 1) + " ładowania pytania nie powiodła się: " + e.getMessage());
                if (attempts >= MAX_ATTEMPTS_LOAD - 1) {
                    gameService.setCurrentQuestionToNull();
                    if(!gameFinished && stage.isShowing()){

                    }
                    return false;
                }
            }
            attempts++;

            if (newQuestion == null) {
                if (attempts >= MAX_ATTEMPTS_LOAD) {
                    gameService.setCurrentQuestionToNull();
                    if(!gameFinished && stage.isShowing()){

                    }
                    break;
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                continue;
            }

            String normalizedNewQuestion = TextNormalizer.normalizeToBaseForm(newQuestion);
            if (!usedQuestions.contains(normalizedNewQuestion)) {
                questionLoadedSuccessfully = true;
                return true;
            } else {
                System.out.println("Pytanie '" + newQuestion.substring(0, Math.min(30,newQuestion.length())) + "...' było już użyte. Próba: " + attempts);
            }
        } while (attempts < MAX_ATTEMPTS_LOAD);

        if (!questionLoadedSuccessfully) {
            gameService.setCurrentQuestionToNull();
            if(!gameFinished && stage.isShowing()){
                Platform.runLater(() -> showCriticalError("Nie udało się znaleźć unikalnego pytania po " + MAX_ATTEMPTS_LOAD + " próbach."));
            }
        }
        return questionLoadedSuccessfully;
    }

    private void returnToSetupScreen() {
        if (timer != null) timer.stop();
        isPaused = false;
        gameFinished = true;
        if(answerField != null) answerField.setDisable(true);

        playerCorrectAnswersPerRound.clear();
        allPlayerNamesForStats.clear();
        usedQuestions.clear();
        currentRoundNumber = 1;
        team1Score = 0;
        team2Score = 0;

        if (gameService != null) {
            gameService.setCurrentQuestionToNull();
        }

        TeamSetupFrame setupFrame = new TeamSetupFrame(stage, () -> {
            MainMenuFrame mainMenu = new MainMenuFrame(stage);
            if (stage.getScene() == null) {
                Scene newSceneForMenu = new Scene(mainMenu.getRootPane());
                try {
                    String cssPath = getClass().getResource("/styles.css").toExternalForm();
                    if (cssPath != null) newSceneForMenu.getStylesheets().add(cssPath);
                } catch (Exception e) {System.err.println("Błąd ładowania CSS dla MainMenuFrame: " + e.getMessage());}
                stage.setScene(newSceneForMenu);
            } else {
                stage.getScene().setRoot(mainMenu.getRootPane());
            }
            mainMenu.show();
        });
        Parent setupRoot = setupFrame.getRootPane();

        if (setupRoot != null && stage.getScene() != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                stage.getScene().setRoot(setupRoot);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), setupRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } else if (setupRoot != null) {
            Scene newScene = new Scene(setupRoot);
            try {
                String cssPath = getClass().getResource("/styles.css").toExternalForm();
                if (cssPath != null) newScene.getStylesheets().add(cssPath);
            } catch (Exception e) {System.err.println("Błąd ładowania CSS dla TeamSetupFrame: " + e.getMessage());}
            stage.setScene(newScene);
            stage.setMaximized(true);
            stage.show();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), setupRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
        else {
            Platform.exit();
        }
    }

    private void prepareNewRoundVisuals() {
        if (questionLabel == null) { return; }

        if (gameService == null || gameService.getCurrentQuestion() == null) {
            questionLabel.setText("Brak dostępnych pytań...");
            for (BorderPane pane : answerPanes) if (pane != null) pane.setVisible(false);
            if (!gameFinished) {
                Platform.runLater(this::showEndOfRoundOrGameScreen);
            }
            return;
        }

        questionLabel.setText(getFormattedQuestion());
        int totalAnswersForQuestion = gameService.getTotalAnswersCount();

        for (int i = 0; i < answerPanes.length; i++) {
            if (answerPanes[i] == null) continue;
            if (i < totalAnswersForQuestion) {
                answerPanes[i].setVisible(true);
                resetAnswerPane(i);
            } else {
                answerPanes[i].setVisible(false);
            }
        }
    }

    private void prepareNewRound() {
        revealedAnswerBaseFormsInRound.clear();

        if (currentRoundNumber >= MAX_ROUNDS) {
            if (!gameFinished) {
                Platform.runLater(this::showEndOfRoundOrGameScreen);
            }
            return;
        }

        boolean newQuestionLoaded = loadNewQuestionForRound();

        if (!newQuestionLoaded || gameService.getCurrentQuestion() == null) {
            if (!gameFinished) {
                Platform.runLater(this::showEndOfRoundOrGameScreen);
            }
            return;
        }

        currentRoundNumber++;

        gameFinished = false;
        isPaused = false;
        roundPoints = 0;
        if (roundPointsLabel != null) roundPointsLabel.setText("Pkt: 0");
        revealedAnswers = 0;
        hasControl = false;
        stealOpportunity = false;
        firstAnswerInRound = true;
        firstTeamAnswer = null;
        firstTeamAnswerPosition = -1;

        resetErrorsAndPanels();
        isTeam1Turn = !isTeam1Turn;
        setInitialPlayerIndexForRound();
        updateTeamLabels();
        updateCurrentPlayerLabel();

        if (gameService != null && gameService.getCurrentQuestion() != null) {
            String normalizedQuestion = TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion());
            usedQuestions.add(normalizedQuestion);
        }

        prepareNewRoundVisuals();

        if(answerField != null) {
            answerField.setDisable(false);
            answerField.requestFocus();
            answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        }
        resetTimer();
    }

    private void resetTimer() {
        if (isPaused || gameFinished) {
            if(timer != null) timer.stop();
            return;
        }
        if (timer != null) timer.stop();
        timeLeft = initialAnswerTime; updateTimerDisplay();
        if (answerField != null && !gameFinished) {
            answerField.setDisable(false);
            answerField.requestFocus();
        } else if (answerField != null) {
            answerField.setDisable(true);
        }

        if (!gameFinished) {
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (isPaused || gameFinished) { if(timer != null) timer.stop(); return; }
                timeLeft--; updateTimerDisplay();
                if (timeLeft <= 0) {
                    if(timer != null) timer.stop();
                    if(answerField != null) answerField.setDisable(true);
                    Platform.runLater(this::handleTimeOut);
                }
            }));
            timer.setCycleCount(Timeline.INDEFINITE); timer.play();
            timerWasRunningBeforeAlert = true;
        } else {
            timerWasRunningBeforeAlert = false;
        }
    }

    private void switchTeam() {
        isTeam1Turn = !isTeam1Turn;
        updateTeamLabels();
        updateCurrentPlayerLabel();
        resetTimer();
    }

    private String getCurrentTeamName() { return isTeam1Turn ? team1Name : team2Name; }
    private String getOpponentTeamName() { return isTeam1Turn ? team2Name : team1Name; }

    private void showInfo(String message) { showInfo(message, true); }

    private void showInfo(String message, boolean shouldResetOrContinueTimerAfter) {
        showInfo(message, shouldResetOrContinueTimerAfter, Duration.seconds(10));
    }

    private void showInfo(String message, boolean shouldResetOrContinueTimerAfter, Duration autoCloseDuration) {
        if (isPaused) return;

        boolean isEndOfRoundOrGameRelated = message.toLowerCase().contains("koniec rundy") ||
                message.toLowerCase().contains("koniec gry") ||
                message.toLowerCase().contains("wygrywa drużyna") ||
                message.toLowerCase().contains("remis") ||
                message.toLowerCase().contains("przejęcie udane") ||
                message.toLowerCase().contains("wędrują do drużyny");

        if (gameFinished && !isEndOfRoundOrGameRelated) {
            return;
        }

        boolean localTimerWasRunning = (timer != null && timer.getStatus() == Animation.Status.RUNNING);
        if (localTimerWasRunning) {
            timer.stop();
        }

        AutoClosingAlerts.show(
                stage,
                AlertType.INFORMATION,
                "Informacja",
                null,
                message,
                autoCloseDuration
        );

        if (!gameFinished) {
            if (shouldResetOrContinueTimerAfter) {
                if (timerWasRunningBeforeAlert) {
                    resetTimer();
                }
            }
        }
    }

    private void showCriticalError(String message) {
        if (timer != null) timer.stop();
        isPaused = true;
        gameFinished = true;
        if(answerField != null) answerField.setDisable(true);

        Platform.runLater(() -> {
            ButtonType returnButton = new ButtonType("Wróć do ustawień", ButtonBar.ButtonData.BACK_PREVIOUS);
            ButtonType closeButton = new ButtonType("Zamknij aplikację", ButtonBar.ButtonData.CANCEL_CLOSE);

            Optional<ButtonType> result = AutoClosingAlerts.show(
                    stage,
                    AlertType.ERROR,
                    "Błąd krytyczny",
                    "Wystąpił poważny błąd!",
                    message + "\n\nAplikacja może wymagać ponownego uruchomienia lub powrotu do ustawień.",
                    Duration.seconds(30),
                    returnButton, closeButton
            );

            if (result.isPresent() && result.get() == returnButton) {
                returnToSetupScreen();
            } else {
                Platform.exit();
            }
        });
    }

    public void show() {
        if (stage != null) {
            if (rootStackPane == null || !uiInitialized) {
                initUI();
                initializeGameContent();
            }

            Scene currentScene = stage.getScene();
            if (currentScene == null) {
                Scene newScene = new Scene(rootStackPane, stage.getWidth(), stage.getHeight());
                stage.setScene(newScene);
                applyStylesheets();
            } else if (currentScene.getRoot() != rootStackPane) {
                currentScene.setRoot(rootStackPane);
                applyStylesheets();
            } else {
                applyStylesheets();
            }
            stage.setMaximized(true);
            stage.show();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
        }
    }
}