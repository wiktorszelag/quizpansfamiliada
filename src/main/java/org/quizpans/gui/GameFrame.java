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
import javafx.scene.image.Image;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GameFrame {
    private Stage stage;
    private GameService gameService;
    private BorderPane mainPane;
    private boolean uiInitialized = false;

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
    private ProgressIndicator loadingIndicator;
    private int team1Errors = 0;
    private int team2Errors = 0;
    private int currentRoundNumber = 1;
    private final int MAX_ROUNDS = 6;
    private boolean timerWasRunningBeforeAlert = false;
    private int roundPoints = 0;

    private static final String HIDDEN_ANSWER_PLACEHOLDER = "■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■";

    public GameFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, boolean isTeam1Turn, List<String> team1Members, List<String> team2Members, Stage stage, GameService existingGameService) {
        this.stage = stage;
        this.selectedCategory = selectedCategory;
        this.initialAnswerTime = answerTime;
        this.timeLeft = answerTime;
        this.isTeam1Turn = isTeam1Turn;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = (team1Members != null && !team1Members.isEmpty()) ? new ArrayList<>(team1Members) : Collections.singletonList("Gracz 1 (D1)");
        this.team2Members = (team2Members != null && !team2Members.isEmpty()) ? new ArrayList<>(team2Members) : Collections.singletonList("Gracz 1 (D2)");
        this.gameService = existingGameService;

        this.team1Label = createTeamLabel(team1Name);
        this.team2Label = createTeamLabel(team2Name);
        this.team1TotalLabel = createTotalScoreLabel();
        this.team2TotalLabel = createTotalScoreLabel();
        this.team1ErrorsPanel = createErrorsPanel(true);
        this.team2ErrorsPanel = createErrorsPanel(false);

        if (this.gameService == null) {
            this.gameService = new GameService(selectedCategory);
            try {
                this.gameService.loadQuestion();
            } catch (RuntimeException e) {
            }
        } else if (this.gameService.getCurrentQuestion() == null) {
            try {
                this.gameService.loadQuestion();
            } catch (RuntimeException e) {
            }
        }
    }

    public void initializeGameContent() {
        if (!uiInitialized) {
            initUI();
            uiInitialized = true;
        }
        setFrameProperties();
        applyStylesheets();
        setInitialPlayerIndexForRound();
        updateTeamLabels();
        updateCurrentPlayerLabel();

        if (gameService != null && gameService.getCurrentQuestion() != null) {
            if (!usedQuestions.contains(TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion()))) {
                usedQuestions.add(TextNormalizer.normalizeToBaseForm(gameService.getCurrentQuestion()));
            }
            prepareNewRoundVisuals();
            startTimer();
        } else {
            showCriticalError("Nie udało się załadować danych gry (pytanie początkowe). Gra nie może być kontynuowana.");
        }
    }

    private void setFrameProperties() {
        stage.setTitle("Familiada - Nowoczesna Wersja");
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream != null) {
                stage.getIcons().add(new Image(logoStream));
                logoStream.close();
            }
        } catch (Exception e) {  }
    }

    private void applyStylesheets() {
        if (mainPane != null && mainPane.getScene() != null) {
            try {
                String cssPath = getClass().getResource("/styles.css").toExternalForm();
                if (!mainPane.getScene().getStylesheets().contains(cssPath)) {
                    mainPane.getScene().getStylesheets().add(cssPath);
                }
            } catch (Exception e) { }
        }
    }

    public Parent getRootPane() {
        if (mainPane == null) {
            initUI();
            uiInitialized = true;
        }
        return mainPane;
    }

    private void initUI() {
        if (uiInitialized) { return; }
        mainPane = new BorderPane();
        LinearGradient gradient = new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#1a2a6c")),new Stop(1,Color.web("#b21f1f")));
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(20));

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
        mainPane.setTop(topPanel); BorderPane.setMargin(topPanel, new Insets(20, 0, 20, 0));

        GridPane centerContainer = new GridPane(); centerContainer.setAlignment(Pos.CENTER); centerContainer.setHgap(15);
        ColumnConstraints col1=new ColumnConstraints(); col1.setPercentWidth(15); col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2=new ColumnConstraints(); col2.setPercentWidth(70); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3=new ColumnConstraints(); col3.setPercentWidth(15); col3.setHalignment(HPos.CENTER);
        centerContainer.getColumnConstraints().addAll(col1, col2, col3);
        StackPane leftErrorPane = new StackPane(team1ErrorsPanel); leftErrorPane.setAlignment(Pos.CENTER);
        StackPane rightErrorPane = new StackPane(team2ErrorsPanel); rightErrorPane.setAlignment(Pos.CENTER);
        VBox answersPanel = new VBox(15); answersPanel.setAlignment(Pos.CENTER);
        answersPanel.setPadding(new Insets(20)); answersPanel.getStyleClass().add("answer-panel");
        for (int i = 0; i < 6; i++) { answerPanes[i] = createAnswerPane(i + 1); answersPanel.getChildren().add(answerPanes[i]); }
        centerContainer.add(leftErrorPane, 0, 0); centerContainer.add(answersPanel, 1, 0); centerContainer.add(rightErrorPane, 2, 0);
        mainPane.setCenter(centerContainer);

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
        mainPane.setBottom(bottomContainer);
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
        String currentPlayerNameText;
        if (isTeam1Turn) {
            currentPlayerNameText = team1Members.isEmpty() ? team1Name : team1Members.get(team1PlayerIndex);
        } else {
            currentPlayerNameText = team2Members.isEmpty() ? team2Name : team2Members.get(team2PlayerIndex);
        }
        currentPlayerLabel.setText("Odpowiada teraz: " + currentPlayerNameText);
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
        BorderPane pane = new BorderPane(); pane.getStyleClass().add("answer-pane");
        Label placeholderLabel = new Label(number + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP); placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");
        pane.setCenter(placeholderLabel); BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
        return pane;
    }

    private void resetAnswerPane(int index) {
        if (index < 0 || index >= answerPanes.length || answerPanes[index] == null) { return; }
        BorderPane pane = answerPanes[index]; pane.getStyleClass().remove("answer-revealed");
        Label placeholderLabel = new Label((index + 1) + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP); placeholderLabel.getStyleClass().clear(); placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");
        pane.setCenter(placeholderLabel); pane.setRight(null); BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
    }

    private void startTimer() {
        if (gameFinished) { if(timer != null) timer.stop(); return; }
        if (timer != null) timer.stop();
        timeLeft = initialAnswerTime; updateTimerDisplay();
        timer = new Timeline( new KeyFrame(Duration.seconds(1), e -> {
            if (gameFinished) { if(timer != null) timer.stop(); return; }
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
        if (gameFinished || answerField == null || answerField.isDisabled()) {
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
        if (gameFinished || gameService == null) return;

        int points = gameService.getPoints(correctAnswerBaseForm);
        int position = gameService.getAnswerPosition(correctAnswerBaseForm);

        if (position < 0 || position >= answerPanes.length || answerPanes[position] == null || answerPanes[position].getStyleClass().contains("answer-revealed")) {
            showInfo("Ta odpowiedź została już odkryta lub jest nieprawidłowa pozycja!");
            resetTimer();
            return;
        }

        boolean wasStealing = stealOpportunity;
        stealOpportunity = false;

        revealAnswer(correctAnswerBaseForm, points, position);
        roundPoints += points;
        if(roundPointsLabel != null) roundPointsLabel.setText("Pkt: " + roundPoints);
        revealedAnswers++;

        if (wasStealing) {
            if (isTeam1Turn) team1Score += roundPoints; else team2Score += roundPoints;
            showInfo("Przejęcie udane! Punkty (" + roundPoints + ") dla drużyny " + getCurrentTeamName() + "!", false);
            advancePlayerInCurrentTeam();
            endRound(false);
            return;
        }

        if (firstAnswerInRound) {
            firstAnswerInRound = false;
            firstTeamAnswer = correctAnswerBaseForm;
            firstTeamAnswerPosition = position;

            if (position != 0) {
                showInfo("Dobra odpowiedź! Ale czy najlepsza? Szansa dla drużyny " + getOpponentTeamName() + "!");
                advancePlayerInCurrentTeam();
                switchTeam();
            } else {
                hasControl = true;
                resetErrorsAndPanels();
                showInfo("Najlepsza odpowiedź! Drużyna " + getCurrentTeamName() + " ma kontrolę!");
                continueTurnForCurrentTeam();
            }
        } else if (!hasControl) {
            if (firstTeamAnswerPosition == -1) {
                hasControl = true; resetErrorsAndPanels();
                showInfo("Poprawna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę (fallback)!");
                continueTurnForCurrentTeam();
            } else {
                if (position < firstTeamAnswerPosition) {
                    hasControl = true;
                    resetErrorsAndPanels();
                    showInfo("Świetna odpowiedź! Drużyna " + getCurrentTeamName() + " przejmuje kontrolę!");
                    continueTurnForCurrentTeam();
                } else {
                    showInfo("Poprawna odpowiedź, ale nie lepsza niż drużyny " + getOpponentTeamName() + ". Kontrola dla " + getOpponentTeamName() + ".");
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
            if (revealedAnswers == gameService.getTotalAnswersCount() || revealedAnswers == answerPanes.length) {
                showInfo("Wszystkie odpowiedzi odkryte! Runda dla drużyny " + getCurrentTeamName() + "!", false);
                advancePlayerInCurrentTeam();
                endRound(true);
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
        String originalAnswer = gameService.getOriginalAnswer(answerBaseForm); String answerToDisplay = capitalizeFirstLetter(originalAnswer);
        BorderPane pane = answerPanes[position]; pane.getStyleClass().add("answer-revealed");
        Label answerTextLabel = new Label((position + 1) + ". " + answerToDisplay);
        answerTextLabel.getStyleClass().setAll("answer-text-label", "revealed");
        Label pointsLabel = new Label(points + " pkt");
        pointsLabel.getStyleClass().setAll("points-label", "revealed");
        pane.setCenter(answerTextLabel); pane.setRight(pointsLabel);
        BorderPane.setAlignment(answerTextLabel, Pos.CENTER_LEFT); BorderPane.setAlignment(pointsLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(pointsLabel, new Insets(0, 10, 0, 10));
        FadeTransition ft = new FadeTransition(Duration.millis(400), pane); ft.setFromValue(0.3); ft.setToValue(1.0); ft.play();
    }

    private void registerError() {
        if (gameFinished) return;

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
            endRound(false);
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
            if (gameFinished) return;
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
        if (gameFinished) return;
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
            nextAction = () -> { awardPointsToOpponent(); endRound(false); };
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

    private void endRound(boolean pointsAwardedToCurrentTeamItself) {
        if (!gameFinished) {
            gameFinished = true;
            if(answerField != null) answerField.setDisable(true);
            if (timer != null) timer.stop();
            timerWasRunningBeforeAlert = false;

            if (pointsAwardedToCurrentTeamItself) {
                if (isTeam1Turn) team1Score += roundPoints; else team2Score += roundPoints;
            }
            updateTeamLabels();
            Platform.runLater(this::showEndOfRoundDialog);
        }
    }

    private void showEndOfRoundDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Koniec rundy " + currentRoundNumber + "!");
        alert.setHeaderText("Runda " + currentRoundNumber + " zakończona! Aktualny wynik:");

        boolean noMoreQuestionsAvailable = (gameService == null);
        if (gameService != null && gameService instanceof org.quizpans.services.GameService) {
        }

        boolean maxRoundsReached = (currentRoundNumber >= MAX_ROUNDS);
        boolean gameCanContinue = !maxRoundsReached && !noMoreQuestionsAvailable;

        String roundEndMessage;
        if (maxRoundsReached) {
            roundEndMessage = "Osiągnięto maksymalną liczbę rund. Koniec gry!";
        } else if (noMoreQuestionsAvailable && gameService != null) {
            roundEndMessage = "Brak więcej unikalnych pytań. Koniec gry.";
        } else {
            roundEndMessage = "Trwa ładowanie kolejnego pytania...";
        }

        Label content = new Label(team1Name + ": " + team1Score + " pkt\n" +
                team2Name + ": " + team2Score + " pkt\n\n" +
                roundEndMessage);
        content.setFont(Font.font("Arial", 20)); content.setWrapText(true);

        loadingIndicator = new ProgressIndicator(); loadingIndicator.setStyle("-fx-progress-color: gold;");
        loadingIndicator.setPrefSize(50, 50); loadingIndicator.setVisible(gameCanContinue);

        VBox alertContent = new VBox(20, content, loadingIndicator); alertContent.setAlignment(Pos.CENTER); alertContent.setPadding(new Insets(20));
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) { dialogPane.getStylesheets().add(cssPath); dialogPane.getStyleClass().add("custom-alert"); }
        } catch (Exception e) {  }
        dialogPane.setContent(alertContent); dialogPane.setPrefSize(500, 300);

        if (gameCanContinue) {
            new Thread(() -> {
                Platform.runLater(() -> {
                    alert.close();
                    prepareNewRound();
                });
            }).start();
        } else {
            alert.setOnCloseRequest(e -> Platform.runLater(this::showGameEndDialog));
        }
        alert.showAndWait();
    }

    private void awardPointsToOpponent() {
        if (isTeam1Turn) team2Score += roundPoints; else team1Score += roundPoints;
        updateTeamLabels();
    }

    private boolean loadNewQuestionForRound() {
        if (gameService == null) {
            gameService = new GameService(selectedCategory);
        }

        GameService tempGameService = this.gameService;
        int attempts = 0; final int MAX_ATTEMPTS_LOAD = 10; String newQuestion = null;
        boolean questionLoadedSuccessfully = false;

        do {
            try {
                tempGameService.loadQuestion();
                newQuestion = tempGameService.getCurrentQuestion();
            } catch (RuntimeException e) {
                newQuestion = null;
                break;
            }
            attempts++;

            if (newQuestion == null) {
                if(attempts >= MAX_ATTEMPTS_LOAD) break;
                try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                continue;
            }

            String normalizedNewQuestion = TextNormalizer.normalizeToBaseForm(newQuestion);
            if (!usedQuestions.contains(normalizedNewQuestion)) {
                usedQuestions.add(normalizedNewQuestion);
                questionLoadedSuccessfully = true;
                return true;
            }
        } while (attempts < MAX_ATTEMPTS_LOAD);

        if (!questionLoadedSuccessfully) {
            if (this.gameService != null) this.gameService.setCurrentQuestionToNull();
            return false;
        }
        return false;
    }

    private void returnToSetupScreen() {
        if (timer != null) timer.stop();
        gameFinished = true;
        if(answerField != null) answerField.setDisable(true);

        TeamSetupFrame setupFrame = new TeamSetupFrame(stage);
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
                newScene.getStylesheets().add(cssPath);
            } catch (Exception e) {}
            stage.setScene(newScene);
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
            questionLabel.setText("Brak dostępnych pytań do wyświetlenia...");
            for (BorderPane pane : answerPanes) if (pane != null) pane.setVisible(false);
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
        if (currentRoundNumber >= MAX_ROUNDS) {
            if (!gameFinished) showGameEndDialog();
            return;
        }

        boolean newQuestionLoaded = loadNewQuestionForRound();

        if (!newQuestionLoaded) {
            if (!gameFinished) showGameEndDialog();
            return;
        }

        currentRoundNumber++;

        gameFinished = false;
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

        prepareNewRoundVisuals();

        if(answerField != null) {
            answerField.setDisable(false);
            answerField.requestFocus();
            answerField.setPromptText("Wpisz odpowiedź i naciśnij Enter");
        }
        resetTimer();
    }

    private void showGameEndDialog() {
        if (gameFinished && currentRoundNumber > MAX_ROUNDS +1) {
            return;
        }
        gameFinished = true;
        if(answerField != null) answerField.setDisable(true);
        if (timer != null) timer.stop();
        timerWasRunningBeforeAlert = false;

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Koniec Gry!");
        String winnerText;
        if (team1Score > team2Score) winnerText = "Wygrywa drużyna " + team1Name + "!";
        else if (team2Score > team1Score) winnerText = "Wygrywa drużyna " + team2Name + "!";
        else winnerText = "Remis!";

        int roundsActuallyPlayed = currentRoundNumber;
        if ( (gameService == null || gameService.getCurrentQuestion() == null) && currentRoundNumber > 1 && !loadNewQuestionForRound()) {
            roundsActuallyPlayed = currentRoundNumber -1;
        }
        roundsActuallyPlayed = Math.min(roundsActuallyPlayed, MAX_ROUNDS);
        if (roundsActuallyPlayed == 0 && (team1Score > 0 || team2Score > 0)) roundsActuallyPlayed =1;

        alert.setHeaderText("Koniec gry po " + roundsActuallyPlayed + " rundach!\n" + winnerText);
        alert.setContentText("Wynik końcowy:\n" + team1Name + ": " + team1Score + " pkt\n" + team2Name + ": " + team2Score + " pkt");
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) { dialogPane.getStylesheets().add(cssPath); dialogPane.getStyleClass().add("custom-alert"); }
        } catch (Exception e) {}
        dialogPane.setPrefSize(500, 300);

        ButtonType closeButton = new ButtonType("Zamknij grę", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType newGameButton = new ButtonType("Nowa gra", ButtonBar.ButtonData.YES);
        alert.getButtonTypes().setAll(newGameButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == newGameButton) {
            returnToSetupScreen();
        } else {
            Platform.exit();
        }
    }

    private void resetTimer() {
        if (gameFinished && currentRoundNumber > MAX_ROUNDS) {
            if(timer != null) timer.stop();
            return;
        }
        if (timer != null) timer.stop();
        timeLeft = initialAnswerTime; updateTimerDisplay();
        if (answerField != null) {
            answerField.setDisable(false);
            answerField.requestFocus();
        }
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (gameFinished && currentRoundNumber > MAX_ROUNDS) { if(timer != null) timer.stop(); return; }
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
        boolean isEndOfRoundOrGameRelated = message.toLowerCase().contains("koniec rundy") ||
                message.toLowerCase().contains("koniec gry") ||
                message.toLowerCase().contains("wygrywa drużyna") ||
                message.toLowerCase().contains("remis") ||
                message.toLowerCase().contains("przejęcie udane") ||
                message.toLowerCase().contains("wędrują do drużyny");

        if (gameFinished && !isEndOfRoundOrGameRelated && currentRoundNumber > MAX_ROUNDS) {
            return;
        }

        boolean localTimerWasRunning = (timer != null && timer.getStatus() == Timeline.Status.RUNNING);
        if (localTimerWasRunning) {
            timer.stop();
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Informacja"); alert.setHeaderText(null); alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) { dialogPane.getStylesheets().add(cssPath); dialogPane.getStyleClass().add("custom-alert"); }
        } catch (Exception e) {  }

        alert.showAndWait();

        if (!gameFinished || currentRoundNumber <= MAX_ROUNDS) {
            if (shouldResetOrContinueTimerAfter) {
                if (timerWasRunningBeforeAlert && localTimerWasRunning) {
                    resetTimer();
                } else if (timerWasRunningBeforeAlert) {
                    resetTimer();
                }
            }
        }
        timerWasRunningBeforeAlert = localTimerWasRunning;
    }

    private void showCriticalError(String message) {
        if (timer != null && timer.getStatus() == Timeline.Status.RUNNING) timer.stop();
        timerWasRunningBeforeAlert = false;
        gameFinished = true;
        if(answerField != null) answerField.setDisable(true);

        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd krytyczny"); alert.setHeaderText("Wystąpił poważny błąd!");
        alert.setContentText(message + "\n\nAplikacja może wymagać ponownego uruchomienia lub powrotu do ustawień.");
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) { dialogPane.getStylesheets().add(cssPath); dialogPane.getStyleClass().add("custom-alert"); }
        } catch (Exception e) { }

        ButtonType returnButton = new ButtonType("Wróć do ustawień", ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType closeButton = new ButtonType("Zamknij aplikację", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(returnButton, closeButton);

        Runnable actionAfterAlert = () -> {
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == returnButton) {
                returnToSetupScreen();
            } else {
                Platform.exit();
            }
        };

        if (Platform.isFxApplicationThread()) {
            actionAfterAlert.run();
        } else {
            Platform.runLater(actionAfterAlert);
        }
    }

    public void show() {
        if (stage != null) {
            if (mainPane == null) {
                initializeGameContent();
            }
            if (stage.getScene() == null) {
                Scene scene = new Scene(mainPane);
                stage.setScene(scene);
                applyStylesheets();
            } else if (stage.getScene().getRoot() != mainPane) {
                stage.getScene().setRoot(mainPane);
                applyStylesheets();
            }

            stage.show();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
        }
    }
}