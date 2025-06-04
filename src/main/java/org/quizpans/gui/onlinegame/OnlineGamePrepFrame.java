package org.quizpans.gui.onlinegame;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.online.model.LobbyStateData;
import org.quizpans.online.model.PlayerInfo;
import org.quizpans.online.model.GameSettingsData;
import org.quizpans.services.OnlineService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineGamePrepFrame {
    private Stage stage;
    private BorderPane mainPane;
    private final LobbyStateData lobbyState;
    private final OnlineService onlineService;
    private final String clientSessionId;
    private Button actionButton;
    private boolean waitingForServerAfterClick = false;
    private boolean isPlayerCurrentlyWaiting = false;
    private Label roundsInfoLabel;

    public OnlineGamePrepFrame(Stage stage, LobbyStateData lobbyState, OnlineService onlineService, String clientSessionId) {
        this.stage = stage;
        this.lobbyState = lobbyState;
        this.onlineService = onlineService;
        this.clientSessionId = clientSessionId;
        initializeFrameContent();
    }

    private void initializeFrameContent() {
        initUI();
        setFrameProperties();
        applyStylesheetsToMainPane();

        if (lobbyState != null && lobbyState.getHostSessionId() != null && lobbyState.getHostSessionId().equals(clientSessionId)) {
            actionButton.setText("Rozpocznij Grę");
            actionButton.setDisable(false);
            actionButton.setOnAction(e -> signalGameStart());
            this.isPlayerCurrentlyWaiting = false;
        } else {
            actionButton.setText("Oczekiwanie na hosta...");
            actionButton.setDisable(true);
            this.isPlayerCurrentlyWaiting = true;
        }
    }

    private void signalGameStart() {
        if (onlineService != null && lobbyState != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "startGame");
            message.put("lobbyId", lobbyState.getId());

            onlineService.setActiveGamePrepFrame(this);
            onlineService.sendJsonMessage(message);

            actionButton.setText("Oczekiwanie na serwer...");
            actionButton.setDisable(true);
            this.waitingForServerAfterClick = true;
        }
    }

    public boolean isPlayerWaitingForGameStart() {
        return isPlayerCurrentlyWaiting;
    }

    public void proceedToOnlineGame() {
        Platform.runLater(() -> {
            if (actionButton != null) {
                actionButton.setText("Gra się rozpoczyna!");
                actionButton.setDisable(true);
            }
            if (onlineService != null && onlineService.getActiveGamePrepFrame() == this) {
                onlineService.clearActiveGamePrepFrame();
            }

            OnlineGameFrame onlineGameFrame = new OnlineGameFrame(stage, onlineService, lobbyState, clientSessionId);
            onlineGameFrame.show();
        });
    }

    public boolean isWaitingForServerAfterStartSignal() {
        return waitingForServerAfterClick;
    }

    public String getLobbyIdInternal() {
        return this.lobbyState != null ? this.lobbyState.getId() : null;
    }

    public Parent getView() {
        return mainPane;
    }

    private void setApplicationIcon() {
        try (InputStream logoStream = getClass().getResourceAsStream("/logo.png")) {
            if (logoStream == null) {
                return;
            }
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                return;
            }
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
        } catch (Exception e) {
        }
    }

    private void setFrameProperties() {
        stage.setTitle("QuizPans - Przygotowanie do Gry Online");
        setApplicationIcon();
    }

    private void applyStylesheetsToMainPane() {
        if (mainPane == null) return;
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && mainPane.getStylesheets().isEmpty()) {
                if (stage.getScene() != null && !stage.getScene().getStylesheets().isEmpty()) {
                    mainPane.getStylesheets().addAll(stage.getScene().getStylesheets());
                } else {
                    mainPane.getStylesheets().add(cssPath);
                }
            } else if (cssPath != null && !mainPane.getStylesheets().contains(cssPath)) {
                mainPane.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
        }
    }

    private void initUI() {
        mainPane = new BorderPane();
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(30));

        Label titleUiLabel = new Label("PRZYGOTOWANIE DO GRY");
        titleUiLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleUiLabel.setTextFill(Color.WHITE);
        titleUiLabel.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.7)));
        BorderPane.setAlignment(titleUiLabel, Pos.CENTER);
        BorderPane.setMargin(titleUiLabel, new Insets(10, 0, 30, 0));
        mainPane.setTop(titleUiLabel);

        GameSettingsData settings = lobbyState != null ? lobbyState.getGameSettings() : new GameSettingsData();
        Map<String, List<PlayerInfo>> teamsMap = lobbyState != null ? lobbyState.getTeams() : new HashMap<>();

        String team1Name = settings != null && settings.teamBlueName() != null ? settings.teamBlueName() : "Niebiescy";
        List<PlayerInfo> team1Players = teamsMap.getOrDefault(team1Name, new ArrayList<>());

        String team2Name = settings != null && settings.teamRedName() != null ? settings.teamRedName() : "Czerwoni";
        List<PlayerInfo> team2Players = teamsMap.getOrDefault(team2Name, new ArrayList<>());

        VBox team1InfoPanel = createTeamInfoPanel(team1Name, team1Players);
        mainPane.setLeft(team1InfoPanel);
        BorderPane.setMargin(team1InfoPanel, new Insets(0, 20, 0, 20));

        VBox team2InfoPanel = createTeamInfoPanel(team2Name, team2Players);
        mainPane.setRight(team2InfoPanel);
        BorderPane.setMargin(team2InfoPanel, new Insets(0, 20, 0, 20));

        Label quizMasterLabel = new Label();
        if (lobbyState != null && lobbyState.getQuizMaster() != null) {
            quizMasterLabel.setText("Prowadzący: " + lobbyState.getQuizMaster().nickname());
            quizMasterLabel.setTextFill(Color.GOLD);
        } else {
            quizMasterLabel.setText("Grę prowadzi komputer");
            quizMasterLabel.setTextFill(Color.LIGHTSKYBLUE);
        }
        quizMasterLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        quizMasterLabel.setEffect(new DropShadow(5, Color.BLACK));
        VBox.setMargin(quizMasterLabel, new Insets(10,0,15,0));

        roundsInfoLabel = new Label();
        roundsInfoLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        roundsInfoLabel.setTextFill(Color.WHITE);
        roundsInfoLabel.setEffect(new DropShadow(3, Color.BLACK));
        if (settings != null) {
            roundsInfoLabel.setText("Liczba rund: " + settings.numberOfRounds());
        } else {
            roundsInfoLabel.setText("Liczba rund: (nieustalono)");
        }
        VBox.setMargin(roundsInfoLabel, new Insets(0,0,20,0));


        VBox centerContent = new VBox(10, quizMasterLabel, roundsInfoLabel);
        centerContent.setAlignment(Pos.CENTER);
        mainPane.setCenter(centerContent);

        actionButton = new Button("Oczekiwanie...");
        actionButton.getStyleClass().add("main-menu-button");
        actionButton.setDisable(true);
        BorderPane.setAlignment(actionButton, Pos.CENTER);
        BorderPane.setMargin(actionButton, new Insets(30, 0, 20, 0));
        mainPane.setBottom(actionButton);
    }

    private VBox createTeamInfoPanel(String teamNameText, List<PlayerInfo> membersList) {
        VBox teamPanel = new VBox(12);
        teamPanel.setPadding(new Insets(20));
        teamPanel.setAlignment(Pos.TOP_CENTER);
        teamPanel.getStyleClass().add("team-setup-panel");
        teamPanel.setPrefWidth(320);
        teamPanel.setMinWidth(280);
        teamPanel.setMaxWidth(400);
        teamPanel.setMinHeight(300);

        Label nameLabel = new Label(teamNameText);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        nameLabel.setTextFill(Color.GOLD);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setWrapText(true);
        nameLabel.setEffect(new DropShadow(8, Color.rgb(0,0,0, 0.8)));

        Rectangle separator = new Rectangle(200, 2, Color.rgb(218, 165, 32, 0.6));
        separator.setArcWidth(5);
        separator.setArcHeight(5);
        VBox.setMargin(separator, new Insets(10, 0, 15, 0));

        Label membersTitleLabel = new Label("Kolejność Graczy:");
        membersTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        membersTitleLabel.setTextFill(Color.WHITE);
        VBox.setMargin(membersTitleLabel, new Insets(8, 0, 8, 0));

        teamPanel.getChildren().addAll(nameLabel, separator, membersTitleLabel);

        VBox membersListVBox = new VBox(8);
        membersListVBox.setAlignment(Pos.CENTER_LEFT);
        membersListVBox.setPadding(new Insets(5, 0, 0, 25));

        if (membersList != null && !membersList.isEmpty()) {
            for (int i = 0; i < membersList.size(); i++) {
                if (i >= 6) break;
                PlayerInfo member = membersList.get(i);
                Label memberLabel = new Label((i + 1) + ". " + member.nickname());
                memberLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 17));
                memberLabel.setTextFill(Color.WHITE);
                memberLabel.setWrapText(true);
                membersListVBox.getChildren().add(memberLabel);
            }
        } else {
            Label noMembersLabel = new Label("(Brak graczy w drużynie)");
            noMembersLabel.setFont(Font.font("Segoe UI", FontWeight.LIGHT, 15));
            noMembersLabel.setTextFill(Color.LIGHTGRAY);
            membersListVBox.getChildren().add(noMembersLabel);
        }

        ScrollPane membersScrollPane = new ScrollPane(membersListVBox);
        membersScrollPane.setFitToWidth(true);
        membersScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        membersScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        membersScrollPane.getStyleClass().add("no-scroll-bar");
        membersScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(membersScrollPane, Priority.ALWAYS);

        teamPanel.getChildren().add(membersScrollPane);
        return teamPanel;
    }

    public void show() {
        if (onlineService != null) {
            onlineService.setActiveGamePrepFrame(this);
        }

        if (stage.getScene() == null) {
            Scene scene = new Scene(mainPane);
            applyStylesheetsToMainPane();
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(mainPane);
            applyStylesheetsToMainPane();
        }
        stage.setMaximized(true);
        stage.show();
        if (mainPane != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(400), mainPane);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}