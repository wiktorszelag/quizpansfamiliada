package org.quizpans.gui.onlinegame;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.quizpans.gui.MainMenuFrame;
import org.quizpans.online.model.LobbyStateData;
import org.quizpans.online.model.PlayerInfo;
import org.quizpans.online.model.GameSettingsData;
import org.quizpans.services.OnlineService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineGameFrame {
    private Stage stage;
    private OnlineService onlineService;
    private LobbyStateData lobbyState;
    private String clientSessionId;
    private GameSettingsData gameSettings;

    private BorderPane gameContentPane;
    private StackPane rootStackPane;
    private VBox pauseMenuPane;
    private boolean uiInitialized = false;
    private boolean isPaused = false;

    private final BorderPane[] answerPanes = new BorderPane[6];

    private final Label questionLabel = new Label("Oczekiwanie na pytanie od serwera...");
    private final Label roundInfoLabel = new Label("Przygotowanie rundy...");
    private final Label timerLabel = new Label();
    private final Label roundPointsLabel = new Label("Pkt: 0");
    private Label team1Label;
    private Label team2Label;
    private Label team1TotalLabel;
    private Label team2TotalLabel;
    private VBox team1ErrorsPanel;
    private VBox team2ErrorsPanel;
    private Label currentPlayerLabel = new Label("Odpowiada teraz: Oczekiwanie...");

    private int timeLeftForDisplay = 30;
    private boolean gameFinished = false;

    private int team1PlayerIndex = 0;
    private int team2PlayerIndex = 0;
    private boolean isOnlineTeam1Turn = true;
    private List<PlayerInfo> team1PlayersList = new ArrayList<>();
    private List<PlayerInfo> team2PlayersList = new ArrayList<>();

    private static final String HIDDEN_ANSWER_PLACEHOLDER = "■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■";

    public OnlineGameFrame(Stage stage, OnlineService onlineService, LobbyStateData initialLobbyState, String clientSessionId) {
        this.stage = stage;
        this.onlineService = onlineService;
        this.lobbyState = initialLobbyState;
        this.clientSessionId = clientSessionId;

        if (initialLobbyState != null) {
            this.gameSettings = initialLobbyState.getGameSettings();
            if (this.gameSettings != null) {
                if (this.gameSettings.answerTime() > 0) {
                    this.timeLeftForDisplay = this.gameSettings.answerTime();
                }
                if (initialLobbyState.getTeams() != null) {
                    String blueTeamName = this.gameSettings.teamBlueName() != null ? this.gameSettings.teamBlueName() : "Niebiescy";
                    String redTeamName = this.gameSettings.teamRedName() != null ? this.gameSettings.teamRedName() : "Czerwoni";
                    this.team1PlayersList = new ArrayList<>(initialLobbyState.getTeams().getOrDefault(blueTeamName, new ArrayList<>()));
                    this.team2PlayersList = new ArrayList<>(initialLobbyState.getTeams().getOrDefault(redTeamName, new ArrayList<>()));
                }
            }
        } else {
            this.gameSettings = new GameSettingsData();
        }

        this.team1Label = createTeamLabel(gameSettings != null ? gameSettings.teamBlueName() : "Niebiescy");
        this.team2Label = createTeamLabel(gameSettings != null ? gameSettings.teamRedName() : "Czerwoni");
        this.team1TotalLabel = createTotalScoreLabel();
        this.team2TotalLabel = createTotalScoreLabel();
        this.team1ErrorsPanel = createErrorsPanel(true);
        this.team2ErrorsPanel = createErrorsPanel(false);

        initializeGameContent();

        if (this.onlineService != null && this.lobbyState != null) {
            this.onlineService.currentlyHostedLobbyStateProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getId() != null && this.lobbyState != null && newVal.getId().equals(this.lobbyState.getId())) {
                    updateUIBasedOnServerState(newVal);
                }
            });
            updateUIBasedOnServerState(this.lobbyState);
            initializeTurnLogic(this.lobbyState);
        }
    }

    private void initializeTurnLogic(LobbyStateData currentLobbyState) {
        if (currentLobbyState == null) return;

        this.isOnlineTeam1Turn = currentLobbyState.isTeam1Turn();

        int currentRound = currentLobbyState.getCurrentRoundNumber();
        if (currentRound <= 0 && currentLobbyState.getGameSettings() != null && currentLobbyState.getGameSettings().numberOfRounds() > 0) {
            currentRound = 1;
        }

        setInitialPlayerIndexForOnlineRound(currentRound);
        updateCurrentPlayerDisplay();
        updateTeamHighlights(this.isOnlineTeam1Turn);
    }

    private void setInitialPlayerIndexForOnlineRound(int roundNumber) {
        int baseIndex = Math.max(0, roundNumber - 1);
        if (!team1PlayersList.isEmpty()) {
            team1PlayerIndex = baseIndex % team1PlayersList.size();
        } else {
            team1PlayerIndex = 0;
        }
        if (!team2PlayersList.isEmpty()) {
            team2PlayerIndex = baseIndex % team2PlayersList.size();
        } else {
            team2PlayerIndex = 0;
        }
    }

    private void updateCurrentPlayerDisplay() {
        String currentPlayerNameText = "Oczekiwanie...";
        String currentTurnPlayerSessionId = this.lobbyState != null ? this.lobbyState.getCurrentPlayerSessionId() : null;

        if (currentTurnPlayerSessionId != null) {
            PlayerInfo player = findGlobalPlayerBySessionId(currentTurnPlayerSessionId);
            if (player != null) {
                currentPlayerNameText = player.nickname();
            } else {
                currentPlayerNameText = "Nieznany gracz";
            }
        } else {
            if (isOnlineTeam1Turn) {
                if (!team1PlayersList.isEmpty() && team1PlayerIndex >=0 && team1PlayerIndex < team1PlayersList.size()) {
                    currentPlayerNameText = team1PlayersList.get(team1PlayerIndex).nickname();
                } else if (gameSettings != null) {
                    currentPlayerNameText = gameSettings.teamBlueName();
                }
            } else {
                if (!team2PlayersList.isEmpty() && team2PlayerIndex >=0 && team2PlayerIndex < team2PlayersList.size()) {
                    currentPlayerNameText = team2PlayersList.get(team2PlayerIndex).nickname();
                } else if (gameSettings != null) {
                    currentPlayerNameText = gameSettings.teamRedName();
                }
            }
        }
        currentPlayerLabel.setText("Odpowiada teraz: " + currentPlayerNameText);
    }

    public void initializeGameContent() {
        if (!uiInitialized) {
            initUI();
        }
        setFrameProperties();
        applyStylesheets();
    }

    public void updateUIBasedOnServerState(LobbyStateData newLobbyState) {
        if (newLobbyState == null) {
            Platform.runLater(() -> {
                questionLabel.setText("Błąd: Brak danych lobby.");
                roundInfoLabel.setText("Błąd danych");
            });
            return;
        }

        this.lobbyState = newLobbyState;
        GameSettingsData newGameSettings = newLobbyState.getGameSettings();

        if (newGameSettings != null) {
            this.gameSettings = newGameSettings;
            if (this.gameSettings.answerTime() > 0) {
                if(newLobbyState.getCurrentAnswerTimeRemaining() >= 0) {
                    this.timeLeftForDisplay = newLobbyState.getCurrentAnswerTimeRemaining();
                } else {
                    this.timeLeftForDisplay = this.gameSettings.answerTime();
                }
            }
            if (newLobbyState.getTeams() != null) {
                String blueTeamName = this.gameSettings.teamBlueName() != null ? this.gameSettings.teamBlueName() : "Niebiescy";
                String redTeamName = this.gameSettings.teamRedName() != null ? this.gameSettings.teamRedName() : "Czerwoni";
                this.team1PlayersList = new ArrayList<>(newLobbyState.getTeams().getOrDefault(blueTeamName, new ArrayList<>()));
                this.team2PlayersList = new ArrayList<>(newLobbyState.getTeams().getOrDefault(redTeamName, new ArrayList<>()));
            }
        } else {
            this.gameSettings = new GameSettingsData();
        }

        Platform.runLater(() -> {
            if (team1Label != null && gameSettings != null) team1Label.setText(gameSettings.teamBlueName());
            if (team2Label != null && gameSettings != null) team2Label.setText(gameSettings.teamRedName());

            if (newLobbyState.getCurrentQuestionText() != null && !newLobbyState.getCurrentQuestionText().isEmpty()) {
                if (newLobbyState.getCurrentQuestionText().startsWith("Koniec gry!")) {
                    questionLabel.setText(newLobbyState.getCurrentQuestionText());
                    gameFinished = true;
                } else {
                    questionLabel.setText(newLobbyState.getCurrentQuestionText());
                    gameFinished = false;
                }
            } else {
                questionLabel.setText("Oczekiwanie na pytanie...");
                gameFinished = false;
            }

            if (newLobbyState.getTotalRounds() > 0) {
                roundInfoLabel.setText("Runda: " + newLobbyState.getCurrentRoundNumber() + " / " + newLobbyState.getTotalRounds());
            } else if (newLobbyState.getCurrentRoundNumber() > 0) {
                roundInfoLabel.setText("Runda: " + newLobbyState.getCurrentRoundNumber());
            } else {
                roundInfoLabel.setText("Przygotowanie rundy...");
            }

            if (team1TotalLabel != null) team1TotalLabel.setText(String.valueOf(newLobbyState.getTeam1Score()));
            if (team2TotalLabel != null) team2TotalLabel.setText(String.valueOf(newLobbyState.getTeam2Score()));

            updateErrorsPanel(team1ErrorsPanel, newLobbyState.getTeam1Errors());
            updateErrorsPanel(team2ErrorsPanel, newLobbyState.getTeam2Errors());

            if (roundPointsLabel != null) roundPointsLabel.setText("Pkt: " + newLobbyState.getCurrentRoundPoints());

            this.isOnlineTeam1Turn = newLobbyState.isTeam1Turn();

            updateCurrentPlayerDisplay();
            updateAnswerPanes(newLobbyState.getRevealedAnswersData());
            updateTeamHighlights(this.isOnlineTeam1Turn);
            updateTimerDisplay();

            if(gameFinished) {
                togglePauseMenu();
                if(pauseMenuPane != null && pauseMenuPane.getChildren().size() > 1 && pauseMenuPane.getChildren().get(1) instanceof Button) {
                    ((Button)pauseMenuPane.getChildren().get(1)).setText("Zakończono");
                    ((Button)pauseMenuPane.getChildren().get(1)).setDisable(true);
                }
            }
        });
    }

    private PlayerInfo findGlobalPlayerBySessionId(String sessionId) {
        if (lobbyState == null || sessionId == null) return null;

        if (lobbyState.getTeams() != null) {
            for (List<PlayerInfo> teamList : lobbyState.getTeams().values()) {
                if (teamList != null) {
                    for (PlayerInfo player : teamList) {
                        if (sessionId.equals(player.sessionId())) {
                            return player;
                        }
                    }
                }
            }
        }
        if (lobbyState.getWaitingPlayers() != null) {
            for (PlayerInfo player : lobbyState.getWaitingPlayers()) {
                if (sessionId.equals(player.sessionId())) {
                    return player;
                }
            }
        }
        if (lobbyState.getQuizMaster() != null && sessionId.equals(lobbyState.getQuizMaster().sessionId())) {
            return lobbyState.getQuizMaster();
        }
        return null;
    }

    private void updateTeamHighlights(boolean isTeam1Active) {
        if (team1Label == null || team2Label == null) return;
        if (isTeam1Active) {
            team1Label.setTextFill(Color.GOLD);
            team1Label.setEffect(new DropShadow(10, Color.GOLD));
            team2Label.setTextFill(Color.WHITE);
            team2Label.setEffect(new DropShadow(5, Color.BLACK));
            team1Label.getStyleClass().setAll("team-label","team-active");
            team2Label.getStyleClass().setAll("team-label","team-inactive");
        } else {
            team1Label.setTextFill(Color.WHITE);
            team1Label.setEffect(new DropShadow(5, Color.BLACK));
            team2Label.setTextFill(Color.GOLD);
            team2Label.setEffect(new DropShadow(10, Color.GOLD));
            team1Label.getStyleClass().setAll("team-label","team-inactive");
            team2Label.getStyleClass().setAll("team-label","team-active");
        }
    }

    private void updateAnswerPanes(List<Map<String, Object>> currentAnswersData) {
        for (int i = 0; i < answerPanes.length; i++) {
            resetAnswerPane(i);
        }
        if (currentAnswersData == null) return;

        int actualAnswersCount = 0;
        for (Map<String, Object> answerData : currentAnswersData) {
            Object textObj = answerData.get("text");
            if (textObj instanceof String && !((String)textObj).isEmpty()){
                actualAnswersCount++;
            }
        }

        for (int i = 0; i < answerPanes.length; i++) {
            if(answerPanes[i] != null) {
                answerPanes[i].setVisible(i < actualAnswersCount);
            }
        }

        for (Map<String, Object> answerData : currentAnswersData) {
            Object textObj = answerData.get("text");
            Object pointsObj = answerData.get("points");
            Object revealedObj = answerData.get("isRevealed");
            Object positionObj = answerData.get("position");

            if (textObj instanceof String && pointsObj instanceof Number && revealedObj instanceof Boolean && positionObj instanceof Number) {
                String text = (String) textObj;
                int points = ((Number) pointsObj).intValue();
                boolean isRevealed = (Boolean) revealedObj;
                int position = ((Number) positionObj).intValue();

                if (position >= 0 && position < answerPanes.length && answerPanes[position] != null) {
                    answerPanes[position].setVisible(true);
                    if (isRevealed) {
                        revealAnswerOnBoard(text, points, position);
                    }
                }
            }
        }
    }

    private void revealAnswerOnBoard(String answerText, int points, int position) {
        if (position < 0 || position >= answerPanes.length || answerPanes[position] == null) {
            return;
        }
        BorderPane pane = answerPanes[position];
        pane.getStyleClass().add("answer-revealed");

        Label answerTextLabel = new Label((position + 1) + ". " + answerText);
        answerTextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        answerTextLabel.getStyleClass().setAll("answer-text-label", "revealed");
        answerTextLabel.setMaxHeight(Double.MAX_VALUE);

        Label pointsLabel = new Label(points + " pkt");
        pointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
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

    private void resetAnswerPane(int index) {
        if (index < 0 || index >= answerPanes.length || answerPanes[index] == null) { return; }
        BorderPane pane = answerPanes[index];
        pane.getStyleClass().remove("answer-revealed");

        Label placeholderLabel = new Label((index + 1) + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 24));
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().setAll("answer-text-label", "hidden");
        placeholderLabel.setMaxHeight(Double.MAX_VALUE);

        pane.setCenter(placeholderLabel);
        pane.setRight(null);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
        pane.setVisible(false);
    }

    private void setFrameProperties() {
        stage.setTitle("QuizPans - Rozgrywka Online");
        try (InputStream logoStream = getClass().getResourceAsStream("/logo.png")) {
            if (logoStream != null) {
                if (stage.getIcons().isEmpty()) {
                    stage.getIcons().add(new Image(logoStream));
                }
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

    public Parent getView() {
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

        VBox topPanel = new VBox(10);
        topPanel.setAlignment(Pos.CENTER);

        roundInfoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        roundInfoLabel.setTextFill(Color.LIGHTCYAN);
        roundInfoLabel.setEffect(new DropShadow(5, Color.BLACK));

        questionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setEffect(new DropShadow(15, Color.BLACK)); questionLabel.setWrapText(true); questionLabel.setAlignment(Pos.CENTER);

        HBox timerBox = new HBox(25); timerBox.setAlignment(Pos.CENTER);
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        updateTimerDisplay();
        roundPointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30)); roundPointsLabel.setTextFill(Color.GOLD);
        roundPointsLabel.setEffect(new DropShadow(10, Color.BLACK));
        timerBox.getChildren().addAll(timerLabel, roundPointsLabel);

        topPanel.getChildren().addAll(roundInfoLabel, questionLabel, timerBox);
        gameContentPane.setTop(topPanel); BorderPane.setMargin(topPanel, new Insets(15, 0, 15, 0));

        GridPane centerContainer = new GridPane(); centerContainer.setAlignment(Pos.CENTER); centerContainer.setHgap(15);
        ColumnConstraints col1=new ColumnConstraints(); col1.setPercentWidth(10); col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2=new ColumnConstraints(); col2.setPercentWidth(80); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3=new ColumnConstraints(); col3.setPercentWidth(10); col3.setHalignment(HPos.CENTER);
        centerContainer.getColumnConstraints().addAll(col1, col2, col3);
        StackPane leftErrorPane = new StackPane(team1ErrorsPanel); leftErrorPane.setAlignment(Pos.CENTER);
        StackPane rightErrorPane = new StackPane(team2ErrorsPanel); rightErrorPane.setAlignment(Pos.CENTER);
        VBox answersPanel = new VBox(10); answersPanel.setAlignment(Pos.CENTER);
        answersPanel.setPadding(new Insets(10)); answersPanel.getStyleClass().add("answer-panel");
        for (int i = 0; i < 6; i++) { answerPanes[i] = createAnswerPane(i + 1); answersPanel.getChildren().add(answerPanes[i]); }
        centerContainer.add(leftErrorPane, 0, 0); centerContainer.add(answersPanel, 1, 0); centerContainer.add(rightErrorPane, 2, 0);
        gameContentPane.setCenter(centerContainer);

        VBox bottomContainer = new VBox(15); bottomContainer.setAlignment(Pos.CENTER); bottomContainer.setPadding(new Insets(15, 0, 15, 0));
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20)); currentPlayerLabel.setTextFill(Color.WHITE);
        currentPlayerLabel.setEffect(new DropShadow(5, Color.BLACK));
        HBox scoresBox = new HBox(25); scoresBox.setAlignment(Pos.CENTER);
        VBox team1Box = new VBox(10, team1Label, team1TotalLabel); team1Box.setAlignment(Pos.CENTER_LEFT); team1Box.setPadding(new Insets(0,0,0,30));
        VBox team2Box = new VBox(10, team2Label, team2TotalLabel); team2Box.setAlignment(Pos.CENTER_RIGHT); team2Box.setPadding(new Insets(0,30,0,0));
        scoresBox.getChildren().addAll(team1Box, team2Box);
        HBox.setHgrow(team1Box, Priority.ALWAYS); HBox.setHgrow(team2Box, Priority.ALWAYS);
        bottomContainer.getChildren().addAll(currentPlayerLabel, scoresBox);
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

        Button mainMenuButton = new Button("Wyjdź z Lobby");
        mainMenuButton.getStyleClass().add("main-menu-button");
        mainMenuButton.setOnAction(e -> {
            if (onlineService != null && lobbyState != null) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("action", "leaveLobby");
                msg.put("lobbyId", lobbyState.getId());
                onlineService.sendJsonMessage(msg);
                onlineService.disconnect();

                MainMenuFrame mainMenu = new MainMenuFrame(stage);
                Parent mainMenuRoot = mainMenu.getRootPane();
                if (stage.getScene() != null && mainMenuRoot != null) {
                    stage.getScene().setRoot(mainMenuRoot);
                } else if (mainMenuRoot != null) {
                    Scene scene = new Scene(mainMenuRoot);
                    stage.setScene(scene);
                }
                mainMenu.show();
            }
        });

        menu.getChildren().addAll(pauseTitle, resumeButton, mainMenuButton);
        return menu;
    }

    private void togglePauseMenu() {
        isPaused = !isPaused;
        if (isPaused) {
            pauseMenuPane.setVisible(true);
            gameContentPane.setEffect(new GaussianBlur(10));
            pauseMenuPane.toFront();
        } else {
            pauseMenuPane.setVisible(false);
            gameContentPane.setEffect(null);
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

    private void updateTimerDisplay() {
        if (timerLabel == null) { return; }
        timerLabel.setText("Czas: " + timeLeftForDisplay + "s");
        timerLabel.getStyleClass().removeAll("timer-critical", "timer-normal", "timer-label");
        timerLabel.getStyleClass().add("timer-label");
        timerLabel.getStyleClass().add(timeLeftForDisplay <= 10 ? "timer-critical" : "timer-normal");
    }

    private BorderPane createAnswerPane(int number) {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("answer-pane");
        pane.setPrefHeight(70);
        pane.setPadding(new Insets(10, 15, 10, 15));

        Label placeholderLabel = new Label(number + ". " + HIDDEN_ANSWER_PLACEHOLDER);
        placeholderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 24));
        placeholderLabel.setTextOverrun(OverrunStyle.CLIP);
        placeholderLabel.getStyleClass().addAll("answer-text-label", "hidden");
        placeholderLabel.setMaxHeight(Double.MAX_VALUE);

        pane.setCenter(placeholderLabel);
        BorderPane.setAlignment(placeholderLabel, Pos.CENTER_LEFT);
        return pane;
    }

    public void show() {
        if (stage != null) {
            if (rootStackPane == null || !uiInitialized) {
                initUI();
            }

            Scene currentScene = stage.getScene();
            Parent newRoot = rootStackPane;

            if (currentScene == null) {
                Scene scene = new Scene(newRoot);
                applyStylesheets();
                stage.setScene(scene);
            } else if (currentScene.getRoot() != newRoot) {
                currentScene.setRoot(newRoot);
                applyStylesheets();
            } else {
                applyStylesheets();
            }

            stage.setMaximized(true);
            stage.show();

            if (newRoot != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), newRoot);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();
            }
        }
    }
}