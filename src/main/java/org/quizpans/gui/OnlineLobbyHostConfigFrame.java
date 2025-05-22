package org.quizpans.gui;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.quizpans.services.GameService;
import org.quizpans.services.OnlineService;
import org.quizpans.online.model.PlayerInfo;
import org.quizpans.online.model.GameSettingsData;
import org.quizpans.online.model.LobbyStateData;
import org.quizpans.utils.AutoClosingAlerts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OnlineLobbyHostConfigFrame {

    private Stage stage;
    private BorderPane rootPane;
    private OnlineService onlineService;
    private String lobbyId;
    private Runnable onBackToLobbyChoice;

    private PasswordField passwordField;
    private TextField teamRedNameInput;
    private TextField teamBlueNameInput;
    private ComboBox<String> categoryComboBox;
    private Slider roundsSlider;
    private Label roundsValueLabel;
    private Slider answerTimeSlider;
    private Label answerTimeValueLabel;

    private TilePane waitingPlayersTilePane;
    private TilePane teamRedTilePane;
    private TilePane teamBlueTilePane;

    private ScrollPane waitingPlayersScrollPane;
    private ScrollPane teamRedScrollPane;
    private ScrollPane teamBlueScrollPane;


    private HBox quizMasterDisplayBox;
    private Label quizMasterNickLabel;
    private Button unassignQuizMasterButton;

    private Button removeFromRoleOrTeamButton;

    private Button saveConfigButton;
    private Button startGameButton;
    private Button leaveLobbyButton;

    private final ObjectProperty<PlayerInfo> currentQuizMaster = new SimpleObjectProperty<>(null);
    private final ObjectProperty<GameSettingsData> currentLobbySettings = new SimpleObjectProperty<>(new GameSettingsData());
    private Label titleLabel;

    private static final String MAIN_MENU_BUTTON_STYLE_CLASS = "main-menu-button";
    private PlayerInfo selectedPlayerForAction = null;
    private Node lastSelectedTile = null;

    private Image userIcon;


    public OnlineLobbyHostConfigFrame(Stage stage, OnlineService onlineService, String lobbyId, Runnable onBackToLobbyChoice) {
        this.stage = stage;
        this.onlineService = onlineService;
        this.lobbyId = lobbyId;
        this.onBackToLobbyChoice = onBackToLobbyChoice;
        loadResources();
        initializeView();
        setupBindingsAndListeners();
    }

    private void loadResources() {
        try (InputStream stream = getClass().getResourceAsStream("/user_icon.png")) {
            if (stream != null) {
                userIcon = new Image(stream);
            } else {
            }
        } catch (Exception e) {
        }
    }


    public String getLobbyId() {
        return lobbyId;
    }

    private void initializeView() {
        rootPane = new BorderPane();
        rootPane.setPadding(new Insets(10));
        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        rootPane.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        titleLabel = new Label("Panel Konfiguracji Lobby: " + lobbyId);
        titleLabel.getStyleClass().add("main-menu-title");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(10, 0, 20, 0));
        rootPane.setTop(titleLabel);

        VBox generalSettingsPanel = createGeneralSettingsPanel();
        ScrollPane generalSettingsScrollPane = wrapInScrollPane(generalSettingsPanel);

        VBox leftPanelContainer = new VBox(generalSettingsScrollPane);
        VBox.setVgrow(generalSettingsScrollPane, Priority.ALWAYS);
        leftPanelContainer.setPrefWidth(350);
        leftPanelContainer.setMinWidth(280);
        leftPanelContainer.setMaxWidth(450);
        BorderPane.setMargin(leftPanelContainer, new Insets(0, 10, 0, 0));
        rootPane.setLeft(leftPanelContainer);

        VBox participantManagementPanel = createParticipantManagementPanel();
        ScrollPane participantManagementScrollPane = wrapInScrollPane(participantManagementPanel);
        participantManagementScrollPane.setMinWidth(300);
        BorderPane.setMargin(participantManagementScrollPane, new Insets(0, 0, 0, 10));
        rootPane.setCenter(participantManagementScrollPane);

        saveConfigButton = new Button("Zapisz Zmiany");
        saveConfigButton.getStyleClass().addAll(MAIN_MENU_BUTTON_STYLE_CLASS, "button-config-save");
        saveConfigButton.setOnAction(e -> saveLobbyConfiguration());

        startGameButton = new Button("Rozpocznij Grę");
        startGameButton.getStyleClass().addAll(MAIN_MENU_BUTTON_STYLE_CLASS, "button-config-start");
        startGameButton.setDisable(true);
        startGameButton.setOnAction(e -> {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "startGame");
            message.put("lobbyId", this.lobbyId);
            onlineService.sendJsonMessage(message);
        });

        leaveLobbyButton = new Button("Opuść Lobby");
        leaveLobbyButton.getStyleClass().addAll(MAIN_MENU_BUTTON_STYLE_CLASS, "button-config-leave");
        leaveLobbyButton.setOnAction(e -> handleLeaveLobby());

        FlowPane bottomButtonFlowPane = new FlowPane(Orientation.HORIZONTAL, 15, 10);
        bottomButtonFlowPane.getChildren().addAll(leaveLobbyButton, saveConfigButton, startGameButton);
        bottomButtonFlowPane.setAlignment(Pos.CENTER);
        bottomButtonFlowPane.setPadding(new Insets(20, 10, 10, 10));
        rootPane.setBottom(bottomButtonFlowPane);
    }

    private ScrollPane wrapInScrollPane(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar-config-pane");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        return scrollPane;
    }

    private VBox createGeneralSettingsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("config-panel-translucent");
        panel.setAlignment(Pos.TOP_LEFT);

        Label settingsTitle = new Label("Ustawienia Główne");
        settingsTitle.getStyleClass().add("config-panel-title");
        VBox.setMargin(settingsTitle, new Insets(0,0,15,0));

        passwordField = new PasswordField();
        passwordField.setPromptText("Hasło (opcjonalnie)");

        categoryComboBox = new ComboBox<>();

        roundsSlider = new Slider(1, 18, 5);
        roundsValueLabel = new Label(String.format("%.0f", roundsSlider.getValue()));
        configureSlider(roundsSlider, 1, 0, true);

        answerTimeSlider = new Slider(10, 120, 30);
        answerTimeValueLabel = new Label(String.format("%.0fs", answerTimeSlider.getValue()));
        configureSlider(answerTimeSlider, 10, 1, true);

        Set<String> availableCategories = GameService.getAvailableCategories();
        categoryComboBox.setItems(FXCollections.observableArrayList(availableCategories.isEmpty() ? List.of("Brak kategorii") : availableCategories));
        if (!categoryComboBox.getItems().isEmpty()) {
            categoryComboBox.getSelectionModel().selectFirst();
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        grid.add(createSettingLabel("Hasło Lobby:"), 0, 0); grid.add(passwordField, 1, 0);
        grid.add(createSettingLabel("Kategoria Pytań:"), 0, 1); grid.add(categoryComboBox, 1, 1);

        HBox roundsBox = new HBox(10, roundsSlider, roundsValueLabel);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(roundsSlider, Priority.ALWAYS);
        grid.add(createSettingLabel("Liczba Rund:"), 0, 2); grid.add(roundsBox, 1, 2);

        HBox timeBox = new HBox(10, answerTimeSlider, answerTimeValueLabel);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(answerTimeSlider, Priority.ALWAYS);
        grid.add(createSettingLabel("Czas na Odpowiedź:"), 0, 3); grid.add(timeBox, 1, 3);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(160);
        col0.setHgrow(Priority.NEVER);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        applyModernFormFieldStyles(passwordField, categoryComboBox);
        roundsValueLabel.getStyleClass().add("slider-value-label-modern");
        answerTimeValueLabel.getStyleClass().add("slider-value-label-modern");

        panel.getChildren().addAll(settingsTitle, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        return panel;
    }

    private VBox createParticipantManagementPanel() {
        VBox mainPanelContainer = new VBox(15);
        mainPanelContainer.setPadding(new Insets(20));
        mainPanelContainer.getStyleClass().add("config-panel-translucent");
        mainPanelContainer.setAlignment(Pos.TOP_CENTER);
        mainPanelContainer.setMinWidth(Region.USE_PREF_SIZE);

        Label usersSettingsTitle = new Label("Ustawienia Użytkowników");
        usersSettingsTitle.getStyleClass().add("config-panel-title");
        VBox.setMargin(usersSettingsTitle, new Insets(0,0,12,0));


        quizMasterDisplayBox = new HBox(10);
        quizMasterDisplayBox.setAlignment(Pos.CENTER);
        quizMasterNickLabel = new Label("Prowadzący: (nieprzypisany)");
        quizMasterNickLabel.getStyleClass().add("quizmaster-label-modern");

        unassignQuizMasterButton = createTileActionButton("X", "ptab-remove");
        Tooltip.install(unassignQuizMasterButton, new Tooltip("Odwołaj Prowadzącego i przenieś do poczekalni"));
        unassignQuizMasterButton.setOnAction(e -> {
            PlayerInfo qm = currentQuizMaster.get();
            if (qm != null && onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "unassignParticipant");
                message.put("lobbyId", this.lobbyId);
                message.put("participantSessionId", qm.sessionId());
                onlineService.sendJsonMessage(message);
                clearAllTileSelections();
            }
        });
        unassignQuizMasterButton.setVisible(false);

        quizMasterDisplayBox.getChildren().addAll(quizMasterNickLabel, unassignQuizMasterButton);
        VBox.setMargin(quizMasterDisplayBox, new Insets(0,0,10,0));

        HBox columnsContainer = new HBox(10);
        columnsContainer.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);
        columnsContainer.setMaxWidth(Double.MAX_VALUE);

        teamBlueNameInput = new TextField("Niebiescy");
        applyModernFormFieldStyles(teamBlueNameInput);
        teamBlueNameInput.getStyleClass().add("team-name-input-modern");
        teamBlueTilePane = createPlayerTilePane();
        teamBlueScrollPane = wrapTilePaneInScrollPane(teamBlueTilePane);
        VBox teamBluePane = createTeamColumnVBox(null, teamBlueScrollPane, teamBlueNameInput, "Niebiescy");

        waitingPlayersTilePane = createPlayerTilePane();
        waitingPlayersScrollPane = wrapTilePaneInScrollPane(waitingPlayersTilePane);
        VBox waitingPlayersPane = createTeamColumnVBox("Oczekujący", waitingPlayersScrollPane, null, null);

        teamRedNameInput = new TextField("Czerwoni");
        applyModernFormFieldStyles(teamRedNameInput);
        teamRedNameInput.getStyleClass().add("team-name-input-modern");
        teamRedTilePane = createPlayerTilePane();
        teamRedScrollPane = wrapTilePaneInScrollPane(teamRedTilePane);
        VBox teamRedPane = createTeamColumnVBox(null, teamRedScrollPane, teamRedNameInput, "Czerwoni");

        HBox.setHgrow(teamBluePane, Priority.ALWAYS);
        teamBluePane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(waitingPlayersPane, Priority.ALWAYS);
        waitingPlayersPane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teamRedPane, Priority.ALWAYS);
        teamRedPane.setMaxWidth(Double.MAX_VALUE);

        columnsContainer.getChildren().addAll(teamBluePane, waitingPlayersPane, teamRedPane);

        removeFromRoleOrTeamButton = createActionButton("Usuń Wybranego Gracza (z Potwierdzeniem)", "button-remove-participant");
        removeFromRoleOrTeamButton.setOnAction(e -> removeSelectedParticipantWithConfirmation());
        FlowPane removeButtonFlowPane = new FlowPane(removeFromRoleOrTeamButton);
        removeButtonFlowPane.setAlignment(Pos.CENTER);
        VBox.setMargin(removeButtonFlowPane, new Insets(20,0,0,0));

        mainPanelContainer.getChildren().addAll(
                usersSettingsTitle,
                quizMasterDisplayBox,
                columnsContainer,
                removeButtonFlowPane
        );
        VBox.setVgrow(mainPanelContainer, Priority.ALWAYS);
        return mainPanelContainer;
    }

    private TilePane createPlayerTilePane() {
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setAlignment(Pos.TOP_LEFT);
        tilePane.setPrefColumns(1);
        tilePane.setTileAlignment(Pos.CENTER_LEFT);
        tilePane.getStyleClass().add("participant-tile-pane");
        return tilePane;
    }

    private ScrollPane wrapTilePaneInScrollPane(TilePane tilePane) {
        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinHeight(150);
        scrollPane.setPrefHeight(300);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.getStyleClass().add("participant-scroll-pane");
        return scrollPane;
    }

    private VBox createTeamColumnVBox(String staticTitle, ScrollPane playerScrollPane, TextField teamNameInputField, String defaultTeamNameIfApplicable) {
        VBox pane = new VBox(8);
        pane.setAlignment(Pos.TOP_CENTER);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("participant-list-pane-modern");
        pane.setMinWidth(180);
        pane.setPrefWidth(220);
        pane.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(pane, Priority.ALWAYS);

        Node titleElement;
        if (teamNameInputField != null) {
            if (teamNameInputField.getText().trim().isEmpty() && defaultTeamNameIfApplicable != null) {
                teamNameInputField.setText(defaultTeamNameIfApplicable);
            }
            titleElement = teamNameInputField;
            teamNameInputField.setAlignment(Pos.CENTER);
        } else {
            Label staticTitleLabel = createSectionTitleLabel(staticTitle);
            staticTitleLabel.setMaxWidth(Double.MAX_VALUE);
            staticTitleLabel.setAlignment(Pos.CENTER);
            titleElement = staticTitleLabel;
        }

        VBox.setVgrow(playerScrollPane, Priority.ALWAYS);
        pane.getChildren().addAll(titleElement, playerScrollPane);
        return pane;
    }

    private Node createPlayerTile(PlayerInfo playerInfo, String currentPaneType) {
        HBox tileLayout = new HBox(8);
        tileLayout.getStyleClass().add("player-tile");
        tileLayout.setUserData(playerInfo);
        tileLayout.setAlignment(Pos.CENTER_LEFT);
        tileLayout.setPadding(new Insets(5, 8, 5, 8));

        if (userIcon != null) {
            ImageView iconView = new ImageView(userIcon);
            iconView.setFitHeight(24);
            iconView.setFitWidth(24);
            iconView.getStyleClass().add("player-tile-icon");
            tileLayout.getChildren().add(iconView);
        } else {
            SVGPath defaultIconSvg = new SVGPath();
            defaultIconSvg.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
            defaultIconSvg.getStyleClass().add("player-tile-default-svg-icon");

            Pane iconPane = new Pane(defaultIconSvg);
            iconPane.setPrefSize(24, 24);
            iconPane.setMinSize(24,24);
            iconPane.setMaxSize(24,24);
            iconPane.getStyleClass().add("player-tile-icon-placeholder");
            tileLayout.getChildren().add(iconPane);
        }

        Label nickLabel = new Label(playerInfo.nickname());
        nickLabel.getStyleClass().add("player-tile-nick");
        nickLabel.setWrapText(true);
        HBox.setHgrow(nickLabel, Priority.ALWAYS);
        nickLabel.setMaxWidth(Double.MAX_VALUE);

        tileLayout.getChildren().add(nickLabel);

        HBox actionButtons = new HBox(5);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button assignToBlueBtn = createTileActionButton("N", "ptab-blue");
        Tooltip.install(assignToBlueBtn, new Tooltip("Do drużyny " + teamBlueNameInput.getText()));
        assignToBlueBtn.setOnAction(e -> handleAssignToTeamAction(playerInfo, teamBlueNameInput.getText()));

        Button assignAsQMBtm = createTileActionButton("P", "ptab-qm");
        Tooltip.install(assignAsQMBtm, new Tooltip("Ustaw jako Prowadzącego"));
        assignAsQMBtm.setOnAction(e -> handleAssignAsQuizMasterAction(playerInfo));

        Button assignToRedBtn = createTileActionButton("C", "ptab-red");
        Tooltip.install(assignToRedBtn, new Tooltip("Do drużyny " + teamRedNameInput.getText()));
        assignToRedBtn.setOnAction(e -> handleAssignToTeamAction(playerInfo, teamRedNameInput.getText()));

        Button unassignButton = createTileActionButton("X", "ptab-remove");
        Tooltip.install(unassignButton, new Tooltip("Cofnij do poczekalni"));
        unassignButton.setOnAction(e -> {
            if (onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "unassignParticipant");
                message.put("lobbyId", this.lobbyId);
                message.put("participantSessionId", playerInfo.sessionId());
                onlineService.sendJsonMessage(message);
                clearAllTileSelections();
            }
        });

        boolean isPlayerCurrentlyQuizMaster = currentQuizMaster.get() != null && currentQuizMaster.get().sessionId().equals(playerInfo.sessionId());

        if (!isPlayerCurrentlyQuizMaster) {
            switch (currentPaneType) {
                case "WAITING":
                    actionButtons.getChildren().addAll(assignToBlueBtn, assignAsQMBtm, assignToRedBtn);
                    break;
                case "TEAM_BLUE":
                case "TEAM_RED":
                    actionButtons.getChildren().add(unassignButton);
                    break;
            }
        }

        if (!actionButtons.getChildren().isEmpty()) {
            tileLayout.getChildren().add(actionButtons);
        }

        tileLayout.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                clearAllTileSelections();
                tileLayout.getStyleClass().add("player-tile-selected");
                selectedPlayerForAction = playerInfo;
                lastSelectedTile = tileLayout;
            }
        });
        return tileLayout;
    }


    private Button createTileActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("player-tile-action-button", styleClass);
        return button;
    }

    private void clearAllTileSelections() {
        if (lastSelectedTile != null) {
            lastSelectedTile.getStyleClass().remove("player-tile-selected");
        }

        selectedPlayerForAction = null;
        lastSelectedTile = null;
    }

    private Label createSettingLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("config-label-modern");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createSectionTitleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title-label-modern");
        return label;
    }

    private void configureSlider(Slider slider, double majorTick, int minorTick, boolean snap) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(majorTick);
        slider.setMinorTickCount(minorTick);
        slider.setSnapToTicks(snap);
        slider.getStyleClass().add("custom-slider-modern");
    }

    private void applyModernFormFieldStyles(Control... controls) {
        for (Control control : controls) {
            control.getStyleClass().add("config-form-control-modern");
            if (control instanceof Region) {
                ((Region)control).setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(control, Priority.ALWAYS);
            }
        }
    }

    private Button createActionButton(String text, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().add("action-button");
        for (String styleClass : styleClasses) {
            if (styleClass != null && !styleClass.isEmpty()) {
                button.getStyleClass().add(styleClass);
            }
        }
        return button;
    }

    private void setupBindingsAndListeners() {
        roundsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            roundsValueLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });
        answerTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            answerTimeValueLabel.setText(String.format("%.0fs", newVal.doubleValue()));
        });

        currentQuizMaster.addListener((obs, oldQm, newQm) -> {
            Platform.runLater(() -> {
                if (newQm != null) {
                    quizMasterNickLabel.setText("Prowadzący: " + newQm.nickname());
                    unassignQuizMasterButton.setVisible(true);
                } else {
                    quizMasterNickLabel.setText("Prowadzący: (nieprzypisany)");
                    unassignQuizMasterButton.setVisible(false);
                }
                updateAllPlayerDisplays();
            });
        });

        currentLobbySettings.addListener((obs, oldSettings, newSettings) -> {
            if (newSettings != null) {
                teamRedNameInput.setText(newSettings.teamRedName());
                teamBlueNameInput.setText(newSettings.teamBlueName());
                if (newSettings.category() != null && categoryComboBox.getItems().contains(newSettings.category())) {
                    categoryComboBox.setValue(newSettings.category());
                } else if (!categoryComboBox.getItems().isEmpty()){
                    categoryComboBox.getSelectionModel().selectFirst();
                }
                roundsSlider.setValue(newSettings.numberOfRounds());
                answerTimeSlider.setValue(newSettings.answerTime());
            } else {
                GameSettingsData defaults = new GameSettingsData();
                teamRedNameInput.setText(defaults.teamRedName());
                teamBlueNameInput.setText(defaults.teamBlueName());
                if (!categoryComboBox.getItems().isEmpty()) categoryComboBox.getSelectionModel().selectFirst();
                roundsSlider.setValue(defaults.numberOfRounds());
                answerTimeSlider.setValue(defaults.answerTime());
            }
        });
    }

    private void handleAssignAsQuizMasterAction(PlayerInfo player) {
        if (player == null) return;
        if (currentQuizMaster.get() != null && currentQuizMaster.get().sessionId().equals(player.sessionId())){
            AutoClosingAlerts.show(stage, Alert.AlertType.INFORMATION, "Informacja", null, player.nickname() + " już jest prowadzącym.", Duration.seconds(3));
            return;
        }
        if (onlineService != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "assignRole");
            message.put("lobbyId", this.lobbyId);
            message.put("participantSessionId", player.sessionId());
            message.put("role", "QUIZ_MASTER");
            onlineService.sendJsonMessage(message);
        }
        clearAllTileSelections();
    }

    private void handleAssignToTeamAction(PlayerInfo player, String teamName) {
        if (player == null) return;
        String targetTeamName = teamName.trim();
        if (targetTeamName.isEmpty()) {
            AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd", null, "Nazwa drużyny docelowej nie może być pusta.", Duration.seconds(3));
            return;
        }
        if (currentQuizMaster.get() != null && currentQuizMaster.get().sessionId().equals(player.sessionId())){
            AutoClosingAlerts.show(stage, Alert.AlertType.WARNING, "Błąd", null, "Prowadzący nie może być przypisany do drużyny.", Duration.seconds(3));
            return;
        }
        if (onlineService != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "assignRole");
            message.put("lobbyId", this.lobbyId);
            message.put("participantSessionId", player.sessionId());
            message.put("role", "PLAYER");
            message.put("targetTeamName", targetTeamName);
            onlineService.sendJsonMessage(message);
        }
        clearAllTileSelections();
    }

    private List<PlayerInfo> filterWaitingPlayers(List<PlayerInfo> rawWaitingPlayers, PlayerInfo qm, GameSettingsData gs, Map<String, List<PlayerInfo>> teams) {
        if (rawWaitingPlayers == null) {
            return new ArrayList<>();
        }
        if (qm == null && (gs == null || teams == null)) {
            return new ArrayList<>(rawWaitingPlayers);
        }

        return rawWaitingPlayers.stream()
                .filter(p -> {
                    if (qm != null && p.sessionId().equals(qm.sessionId())) {
                        return false;
                    }
                    if (gs != null && teams != null) {
                        List<PlayerInfo> teamBlue = teams.get(gs.teamBlueName());
                        if (teamBlue != null && teamBlue.stream().anyMatch(tp -> tp.sessionId().equals(p.sessionId()))) {
                            return false;
                        }
                        List<PlayerInfo> teamRed = teams.get(gs.teamRedName());
                        if (teamRed != null && teamRed.stream().anyMatch(tp -> tp.sessionId().equals(p.sessionId()))) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void updateAllPlayerDisplays() {
        Platform.runLater(() -> {
            if (onlineService != null && onlineService.currentlyHostedLobbyStateProperty().get() != null) {
                LobbyStateData clientLobbyState = onlineService.currentlyHostedLobbyStateProperty().get();

                PlayerInfo qm = clientLobbyState.getQuizMaster();
                GameSettingsData gs = clientLobbyState.getGameSettings();
                Map<String, List<PlayerInfo>> teams = clientLobbyState.getTeams();

                List<PlayerInfo> filteredWaitingPlayers = filterWaitingPlayers(clientLobbyState.getWaitingPlayers(), qm, gs, teams);
                updatePlayerTiles(waitingPlayersTilePane, filteredWaitingPlayers, "WAITING");

                if (gs != null && teams != null) {
                    updatePlayerTiles(teamBlueTilePane, teams.get(gs.teamBlueName()), "TEAM_BLUE");
                    updatePlayerTiles(teamRedTilePane, teams.get(gs.teamRedName()), "TEAM_RED");
                } else {
                    updatePlayerTiles(teamBlueTilePane, null, "TEAM_BLUE");
                    updatePlayerTiles(teamRedTilePane, null, "TEAM_RED");
                }
            } else {
                updatePlayerTiles(waitingPlayersTilePane, null, "WAITING");
                updatePlayerTiles(teamBlueTilePane, null, "TEAM_BLUE");
                updatePlayerTiles(teamRedTilePane, null, "TEAM_RED");
            }
            clearAllTileSelections();
        });
    }

    private void updatePlayerTiles(TilePane tilePane, List<PlayerInfo> players, String paneType) {
        tilePane.getChildren().clear();
        if (players != null) {
            for (PlayerInfo player : players) {
                Node playerTile = createPlayerTile(player, paneType);
                tilePane.getChildren().add(playerTile);
            }
        }
    }

    private void removeSelectedParticipantWithConfirmation() {
        PlayerInfo playerToRemove = selectedPlayerForAction;

        if (playerToRemove == null && currentQuizMaster.get() != null && quizMasterDisplayBox.lookup(".selected-quizmaster") != null) {

            playerToRemove = currentQuizMaster.get();
        }


        if (playerToRemove == null) {
            AutoClosingAlerts.show(stage, Alert.AlertType.WARNING, "Błąd", null, "Wybierz uczestnika do usunięcia, klikając na jego kafelek.", Duration.seconds(4));
            return;
        }

        final PlayerInfo finalPlayerToRemove = playerToRemove;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Potwierdzenie usunięcia");
        confirmation.setHeaderText("Czy na pewno chcesz całkowicie usunąć gracza " + finalPlayerToRemove.nickname() + " z lobby?");
        confirmation.setContentText("Tej operacji nie można cofnąć.");


        Optional<ButtonType> result = confirmation.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK){
            if (onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "removeParticipantFromLobby");
                message.put("lobbyId", this.lobbyId);
                message.put("participantSessionId", finalPlayerToRemove.sessionId());
                onlineService.sendJsonMessage(message);
                clearAllTileSelections();
            }
        }
    }


    private void saveLobbyConfiguration() {
        if (onlineService == null) return;

        String blueTeam = teamBlueNameInput.getText().trim();
        String redTeam = teamRedNameInput.getText().trim();

        if(blueTeam.isEmpty()) blueTeam = "Niebiescy";
        if(redTeam.isEmpty()) redTeam = "Czerwoni";
        if(blueTeam.equals(redTeam)) {
            AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd Nazw Drużyn", null, "Nazwy drużyn nie mogą być takie same.", Duration.seconds(4));
            return;
        }

        GameSettingsData newSettings = new GameSettingsData(
                categoryComboBox.getValue(),
                (int) answerTimeSlider.getValue(),
                (int) roundsSlider.getValue(),
                6,
                redTeam,
                blueTeam
        );

        Map<String, Object> message = new HashMap<>();
        message.put("action", "configureLobby");
        message.put("lobbyId", this.lobbyId);
        message.put("password", passwordField.getText());
        message.put("gameSettings", newSettings);

        onlineService.sendJsonMessage(message);
        AutoClosingAlerts.show(stage, Alert.AlertType.INFORMATION, "Konfiguracja", null, "Wysłano konfigurację lobby.", Duration.seconds(3));
    }

    private void handleLeaveLobby() {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Opuścić Lobby?");
        confirmationDialog.setHeaderText("Czy na pewno chcesz opuścić to lobby?");

        Stage alertStage = (Stage) confirmationDialog.getDialogPane().getScene().getWindow();
        if (stage != null && !stage.getIcons().isEmpty()) {
            alertStage.getIcons().addAll(stage.getIcons());
        }

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "leaveLobby");
                message.put("lobbyId", this.lobbyId);
                onlineService.sendJsonMessage(message);
                onlineService.clearActiveHostConfigFrame();
                onlineService.currentlyHostedLobbyStateProperty().set(null);
            }
            if (onBackToLobbyChoice != null) {
                onBackToLobbyChoice.run();
            }
        }
    }

    public Parent getView() {
        return rootPane;
    }

    public void show() {
        if (stage.getScene() == null) {
            Scene scene = new Scene(rootPane, 1280, 800);
            addStylesToScene(scene);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(rootPane);
            addStylesToScene(stage.getScene());
        }
        titleLabel.setText("Panel Konfiguracji Lobby: " + this.lobbyId);
        stage.setTitle("Panel Konfiguracji Lobby: " + this.lobbyId);

        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setMaximized(true);
        stage.show();
    }

    private void addStylesToScene(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {System.err.println("Error loading CSS for OnlineLobbyHostConfigFrame: " + e.getMessage());}
    }

    public void updateFullLobbyState(LobbyStateData clientLobbyState) {
        if (clientLobbyState == null) {
            return;
        }
        if (!clientLobbyState.getId().equals(this.lobbyId)) {
            return;
        }
        Platform.runLater(() -> {
            titleLabel.setText("Panel Konfiguracji Lobby: " + clientLobbyState.getName());
            if(stage.isShowing()) {
                stage.setTitle("Panel Konfiguracji Lobby: " + clientLobbyState.getName());
            }

            GameSettingsData gs = clientLobbyState.getGameSettings();
            if(gs == null) gs = new GameSettingsData();
            currentLobbySettings.set(gs);

            passwordField.setText(clientLobbyState.getPassword() != null ? clientLobbyState.getPassword() : "");

            PlayerInfo qm = clientLobbyState.getQuizMaster();
            currentQuizMaster.set(qm);

            Map<String, List<PlayerInfo>> teams = clientLobbyState.getTeams();
            List<PlayerInfo> filteredWaitingPlayers = filterWaitingPlayers(clientLobbyState.getWaitingPlayers(), qm, gs, teams);

            updatePlayerTiles(waitingPlayersTilePane, filteredWaitingPlayers, "WAITING");

            if(teams != null) {
                updatePlayerTiles(teamBlueTilePane, teams.get(gs.teamBlueName()), "TEAM_BLUE");
                updatePlayerTiles(teamRedTilePane, teams.get(gs.teamRedName()), "TEAM_RED");
            } else {
                updatePlayerTiles(teamBlueTilePane, null, "TEAM_BLUE");
                updatePlayerTiles(teamRedTilePane, null, "TEAM_RED");
            }
            clearAllTileSelections();

            boolean canStart = false;
            if (teams != null && gs != null) {
                List<PlayerInfo> teamRedList = teams.get(gs.teamRedName());
                List<PlayerInfo> teamBlueList = teams.get(gs.teamBlueName());

                canStart = (teamBlueList != null && !teamBlueList.isEmpty()) &&
                        (teamRedList != null && !teamRedList.isEmpty()) &&
                        qm != null;
            }
            startGameButton.setDisable(!canStart);
        });
    }
}