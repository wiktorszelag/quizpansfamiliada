package org.quizpans.gui.onlinegame;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;

import org.quizpans.online.model.ParticipantRole;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class OnlineLobbyHostConfigFrame {

    private Stage stage;
    private BorderPane rootPane;
    private OnlineService onlineService;
    private String lobbyId;
    private Runnable onBackToLobbyChoice;
    private String clientSessionId;

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
    private Button banPlayerButton;
    private Button startGameButton;
    private Button leaveLobbyButton;

    private final ObjectProperty<PlayerInfo> currentQuizMaster = new SimpleObjectProperty<>(null);
    private final ObjectProperty<GameSettingsData> currentLobbySettings = new SimpleObjectProperty<>(new GameSettingsData());
    private Label titleLabel;

    private static final String MAIN_MENU_BUTTON_STYLE_CLASS = "main-menu-button";
    private PlayerInfo selectedPlayerForAction = null;
    private Node lastSelectedTile = null;

    private Image userIcon;
    private String lastKnownPassword = "";
    private String lastKnownTeamBlueName = "Niebiescy";
    private String lastKnownTeamRedName = "Czerwoni";
    private String lastKnownCategory = null;
    private int lastKnownRounds = 5;
    private int lastKnownAnswerTime = 30;


    private static final int MAX_TEAM_NAME_LENGTH = 14;
    private static final String MIX_CATEGORY_IDENTIFIER = "MIX (Wszystkie Kategorie)";
    private boolean waitingForGameStartConfirmation = false;

    private Task<GameService> categoryPreloadTask;
    private GameService preloadedGameService;
    private Label categoryLoadingStatusLabel;
    private boolean isCategoryPreloadingSuccessful = false;

    private PauseTransition debounceTimer;
    private static final long DEBOUNCE_DELAY_MS = 750;
    private boolean uiUpdatingFromServer = false;


    public OnlineLobbyHostConfigFrame(Stage stage, OnlineService onlineService, String lobbyId, Runnable onBackToLobbyChoice) {
        this.stage = stage;
        this.onlineService = onlineService;
        this.lobbyId = lobbyId;
        this.onBackToLobbyChoice = onBackToLobbyChoice;
        if (this.onlineService != null) {
            this.clientSessionId = this.onlineService.getClientSessionId();
        }
        this.debounceTimer = new PauseTransition(Duration.millis(DEBOUNCE_DELAY_MS));
        this.debounceTimer.setOnFinished(event -> actualSaveLobbyConfiguration());

        loadResources();
        initializeView();
        setupListenersAndBindings();

        LobbyStateData initialLobbyState = onlineService.currentlyHostedLobbyStateProperty().get();
        if (initialLobbyState != null) {
            updateFieldsFromLobbyState(initialLobbyState, true);
            updateAllPlayerDisplays(initialLobbyState);
        } else {
            GameSettingsData defaultGs = new GameSettingsData();
            lastKnownCategory = defaultGs.category() == null ? MIX_CATEGORY_IDENTIFIER : defaultGs.category();
            lastKnownRounds = defaultGs.numberOfRounds();
            lastKnownAnswerTime = defaultGs.answerTime();
            lastKnownPassword = "";
            lastKnownTeamBlueName = defaultGs.teamBlueName();
            lastKnownTeamRedName = defaultGs.teamRedName();
        }
        if (categoryComboBox.getValue() != null) {
            preloadCategoryData(categoryComboBox.getValue());
        } else if (!categoryComboBox.getItems().isEmpty()) {
            preloadCategoryData(categoryComboBox.getItems().get(0));
        } else {
            isCategoryPreloadingSuccessful = false;
        }
        updateStartGameButtonState();
    }

    private void loadResources() {
        try (InputStream stream = getClass().getResourceAsStream("/user_icon.png")) {
            if (stream != null) {
                userIcon = new Image(stream);
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania user_icon.png: " + e.getMessage());
        }
    }

    public String getLobbyId() {
        return lobbyId;
    }

    private TextFormatter<String> createTeamNameTextFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() > MAX_TEAM_NAME_LENGTH) {
                return null;
            }
            return change;
        };
        return new TextFormatter<>(filter);
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

        startGameButton = new Button("Rozpocznij Grę");
        startGameButton.getStyleClass().addAll(MAIN_MENU_BUTTON_STYLE_CLASS, "button-config-start");
        startGameButton.setDisable(true);

        startGameButton.setOnAction(e -> {
            if (onlineService != null && lobbyId != null && isCategoryPreloadingSuccessful) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "startGame");
                message.put("lobbyId", this.lobbyId);
                onlineService.sendJsonMessage(message);
                waitingForGameStartConfirmation = true;
                startGameButton.setDisable(true);
                startGameButton.setText("Uruchamianie...");
            } else if (!isCategoryPreloadingSuccessful) {
                AutoClosingAlerts.show(stage, Alert.AlertType.WARNING, "Błąd Kategorii", "Nie można rozpocząć gry.", "Wybrana kategoria nie została poprawnie załadowana lub jest w trakcie ładowania.", Duration.seconds(5));
            }
        });

        leaveLobbyButton = new Button("Opuść Lobby");
        leaveLobbyButton.getStyleClass().addAll(MAIN_MENU_BUTTON_STYLE_CLASS, "button-config-leave");
        leaveLobbyButton.setOnAction(e -> handleLeaveLobby());

        FlowPane bottomButtonFlowPane = new FlowPane(Orientation.HORIZONTAL, 15, 10);
        bottomButtonFlowPane.getChildren().addAll(leaveLobbyButton, startGameButton);
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
        categoryLoadingStatusLabel = new Label();
        categoryLoadingStatusLabel.getStyleClass().add("category-loading-status");
        categoryLoadingStatusLabel.setTextFill(Color.LIGHTGRAY);
        categoryLoadingStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        roundsSlider = new Slider(1, 18, 5);
        roundsValueLabel = new Label(String.format("%.0f", roundsSlider.getValue()));
        configureSlider(roundsSlider, 1, 0, true);

        answerTimeSlider = new Slider(10, 120, 30);
        answerTimeValueLabel = new Label(String.format("%.0fs", answerTimeSlider.getValue()));
        configureSlider(answerTimeSlider, 10, 1, true);

        Set<String> availableCategoriesRaw = GameService.getAvailableCategories();
        ObservableList<String> categoriesForComboBox = FXCollections.observableArrayList();
        if (availableCategoriesRaw != null && !availableCategoriesRaw.isEmpty()) {
            categoriesForComboBox.add(MIX_CATEGORY_IDENTIFIER);
            categoriesForComboBox.addAll(new TreeSet<>(availableCategoriesRaw));
        } else {
            categoriesForComboBox.add("Brak kategorii");
        }
        categoryComboBox.setItems(categoriesForComboBox);
        if (!categoryComboBox.getItems().isEmpty()) {
            categoryComboBox.getSelectionModel().selectFirst();
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        grid.add(createSettingLabel("Hasło Lobby:"), 0, 0); grid.add(passwordField, 1, 0);
        VBox categoryBox = new VBox(5, categoryComboBox, categoryLoadingStatusLabel);
        grid.add(createSettingLabel("Kategoria Pytań:"), 0, 1); grid.add(categoryBox, 1, 1);
        HBox roundsBox = new HBox(10, roundsSlider, roundsValueLabel);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(roundsSlider, Priority.ALWAYS);
        grid.add(createSettingLabel("Liczba Rund:"), 0, 2); grid.add(roundsBox, 1, 2);
        HBox timeBox = new HBox(10, answerTimeSlider, answerTimeValueLabel);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(answerTimeSlider, Priority.ALWAYS);
        grid.add(createSettingLabel("Czas na Odpowiedź:"), 0, 3); grid.add(timeBox, 1, 3);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140); col0.setPrefWidth(160); col0.setHgrow(Priority.NEVER);
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
        unassignQuizMasterButton = createTileActionButton(null, "ptab-remove");
        SVGPath crossPath = new SVGPath();
        crossPath.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        crossPath.setFill(Color.WHITE);
        unassignQuizMasterButton.setGraphic(crossPath);
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
        teamBlueNameInput.setTextFormatter(createTeamNameTextFormatter());
        applyModernFormFieldStyles(teamBlueNameInput);
        teamBlueNameInput.getStyleClass().add("team-name-input-modern");
        teamBlueTilePane = createPlayerTilePaneForTeams();
        teamBlueScrollPane = wrapTilePaneInScrollPane(teamBlueTilePane);
        VBox teamBluePane = createTeamColumnVBox(null, teamBlueScrollPane, teamBlueNameInput, "Niebiescy");

        waitingPlayersTilePane = createPlayerTilePaneForWaiting();
        waitingPlayersScrollPane = wrapTilePaneInScrollPane(waitingPlayersTilePane);
        VBox waitingPlayersPane = createTeamColumnVBox("Oczekujący", waitingPlayersScrollPane, null, null);

        teamRedNameInput = new TextField("Czerwoni");
        teamRedNameInput.setTextFormatter(createTeamNameTextFormatter());
        applyModernFormFieldStyles(teamRedNameInput);
        teamRedNameInput.getStyleClass().add("team-name-input-modern");
        teamRedTilePane = createPlayerTilePaneForTeams();
        teamRedScrollPane = wrapTilePaneInScrollPane(teamRedTilePane);
        VBox teamRedPane = createTeamColumnVBox(null, teamRedScrollPane, teamRedNameInput, "Czerwoni");

        HBox.setHgrow(teamBluePane, Priority.ALWAYS); teamBluePane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(waitingPlayersPane, Priority.ALWAYS); waitingPlayersPane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teamRedPane, Priority.ALWAYS); teamRedPane.setMaxWidth(Double.MAX_VALUE);
        columnsContainer.getChildren().addAll(teamBluePane, waitingPlayersPane, teamRedPane);

        banPlayerButton = createActionButton("Usuń Gracza z Lobby", "button-remove-participant");
        banPlayerButton.setOnAction(e -> removeSelectedParticipantWithConfirmation());
        FlowPane removeButtonFlowPane = new FlowPane(banPlayerButton);
        removeButtonFlowPane.setAlignment(Pos.CENTER);
        VBox.setMargin(removeButtonFlowPane, new Insets(20,0,0,0));

        mainPanelContainer.getChildren().addAll(usersSettingsTitle, quizMasterDisplayBox, columnsContainer, removeButtonFlowPane);
        VBox.setVgrow(mainPanelContainer, Priority.ALWAYS);
        return mainPanelContainer;
    }

    private TilePane createPlayerTilePaneForTeams() {
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10); tilePane.setVgap(10);
        tilePane.setAlignment(Pos.TOP_LEFT);
        tilePane.setPrefColumns(1);
        tilePane.setTileAlignment(Pos.CENTER_LEFT);
        tilePane.getStyleClass().add("participant-tile-pane");
        return tilePane;
    }

    private TilePane createPlayerTilePaneForWaiting() {
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(8); tilePane.setVgap(8);
        tilePane.setAlignment(Pos.TOP_LEFT);
        tilePane.setTileAlignment(Pos.TOP_LEFT);
        tilePane.getStyleClass().add("participant-tile-pane");
        tilePane.getStyleClass().add("waiting-players-tile-pane");
        return tilePane;
    }

    private SVGPath createArrowPath(String direction) {
        SVGPath arrow = new SVGPath();
        if ("left".equals(direction)) {
            arrow.setContent("M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z");
        } else {
            arrow.setContent("M8.59 16.59L10 18l6-6-6-6-1.41 1.41L13.17 12z");
        }
        arrow.setFill(Color.WHITE);
        return arrow;
    }

    private Node createPlayerTile(PlayerInfo playerInfo, String currentPaneType) {
        HBox tileLayout = new HBox(8);
        tileLayout.getStyleClass().add("player-tile");
        if ("WAITING".equals(currentPaneType)) tileLayout.getStyleClass().add("player-tile-waiting");
        else if ("TEAM_BLUE".equals(currentPaneType)) tileLayout.getStyleClass().add("player-tile-blue");
        else if ("TEAM_RED".equals(currentPaneType)) tileLayout.getStyleClass().add("player-tile-red");
        tileLayout.setUserData(playerInfo);
        tileLayout.setAlignment(Pos.CENTER_LEFT);
        tileLayout.setPadding(new Insets(8, 10, 8, 10));

        Node iconNode;
        if (userIcon != null && !userIcon.isError()) {
            ImageView iconView = new ImageView(userIcon);
            iconView.setFitHeight(28); iconView.setFitWidth(28);
            iconView.getStyleClass().add("player-tile-icon-loaded");
            iconNode = iconView;
        } else {
            SVGPath defaultIconSvg = new SVGPath();
            defaultIconSvg.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
            defaultIconSvg.getStyleClass().add("player-tile-default-svg-icon");
            Pane iconPane = new Pane(defaultIconSvg);
            iconPane.setPrefSize(28, 28); iconPane.setMinSize(28,28); iconPane.setMaxSize(28,28);
            iconPane.getStyleClass().add("player-tile-icon-placeholder");
            iconNode = iconPane;
        }
        tileLayout.getChildren().add(iconNode);

        Label nickLabel = new Label(playerInfo.nickname());
        nickLabel.getStyleClass().add("player-tile-nick");
        nickLabel.setWrapText(true);
        HBox.setHgrow(nickLabel, Priority.ALWAYS);
        nickLabel.setMaxWidth(Double.MAX_VALUE);
        tileLayout.getChildren().add(nickLabel);

        HBox actionButtons = new HBox(5);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button assignToBlueBtn = createTileActionButton(null, "ptab-blue");
        assignToBlueBtn.setGraphic(createArrowPath("left"));
        Tooltip blueTooltip = new Tooltip();
        teamBlueNameInput.textProperty().addListener((obs, oldVal, newVal) -> blueTooltip.setText("Do drużyny " + (newVal.isEmpty() ? "Niebiescy" : newVal) ));
        blueTooltip.setText("Do drużyny " + (teamBlueNameInput.getText().isEmpty() ? "Niebiescy" : teamBlueNameInput.getText()));
        Tooltip.install(assignToBlueBtn, blueTooltip);
        assignToBlueBtn.setOnAction(e -> handleAssignToTeamAction(playerInfo, teamBlueNameInput.getText().trim().isEmpty() ? "Niebiescy" : teamBlueNameInput.getText().trim()));

        Button assignAsQMBtm = createTileActionButton("P", "ptab-qm");
        Tooltip.install(assignAsQMBtm, new Tooltip("Ustaw jako Prowadzącego"));
        assignAsQMBtm.setOnAction(e -> handleAssignAsQuizMasterAction(playerInfo));

        Button assignToRedBtn = createTileActionButton(null, "ptab-red");
        assignToRedBtn.setGraphic(createArrowPath("right"));
        Tooltip redTooltip = new Tooltip();
        teamRedNameInput.textProperty().addListener((obs, oldVal, newVal) -> redTooltip.setText("Do drużyny " + (newVal.isEmpty() ? "Czerwoni" : newVal)));
        redTooltip.setText("Do drużyny " + (teamRedNameInput.getText().isEmpty() ? "Czerwoni" : teamRedNameInput.getText()));
        Tooltip.install(assignToRedBtn, redTooltip);
        assignToRedBtn.setOnAction(e -> handleAssignToTeamAction(playerInfo, teamRedNameInput.getText().trim().isEmpty() ? "Czerwoni" : teamRedNameInput.getText().trim()));

        Button unassignButton = createTileActionButton(null, "ptab-remove-from-team");
        SVGPath crossPathTile = new SVGPath();
        crossPathTile.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        crossPathTile.setFill(Color.WHITE);
        unassignButton.setGraphic(crossPathTile);
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

        if (playerInfo.getRole() != ParticipantRole.QUIZ_MASTER) {
            switch (currentPaneType) {
                case "WAITING":
                    actionButtons.getChildren().addAll(assignToBlueBtn, assignAsQMBtm, assignToRedBtn);
                    break;
                case "TEAM_BLUE": case "TEAM_RED":
                    actionButtons.getChildren().add(unassignButton);
                    break;
            }
        }

        if (!actionButtons.getChildren().isEmpty()) tileLayout.getChildren().add(actionButtons);

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
        slider.setShowTickLabels(true); slider.setShowTickMarks(true);
        slider.setMajorTickUnit(majorTick); slider.setMinorTickCount(minorTick);
        slider.setSnapToTicks(snap); slider.getStyleClass().add("custom-slider-modern");
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
            if (styleClass != null && !styleClass.isEmpty()) button.getStyleClass().add(styleClass);
        }
        return button;
    }

    private VBox createTeamColumnVBox(String staticTitle, ScrollPane playerScrollPane, TextField teamNameInputField, String defaultTeamNameIfApplicable) {
        VBox pane = new VBox(8);
        pane.setAlignment(Pos.TOP_CENTER);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("participant-list-pane-modern");
        pane.setMinWidth(180); pane.setPrefWidth(220); pane.setMaxWidth(Double.MAX_VALUE);
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

    private void setupListenersAndBindings() {
        ChangeListener<Object> settingsChangeListener = (obs, oldVal, newVal) -> {
            if (!uiUpdatingFromServer) {
                debounceTimer.playFromStart();
            }
        };

        roundsSlider.valueProperty().addListener(settingsChangeListener);
        answerTimeSlider.valueProperty().addListener(settingsChangeListener);
        categoryComboBox.valueProperty().addListener(settingsChangeListener);
        passwordField.textProperty().addListener(settingsChangeListener);
        teamBlueNameInput.textProperty().addListener(settingsChangeListener);
        teamRedNameInput.textProperty().addListener(settingsChangeListener);

        categoryComboBox.setOnAction(event -> {
            if (!uiUpdatingFromServer) {
                preloadCategoryData(categoryComboBox.getValue());
                debounceTimer.playFromStart();
            }
        });

        onlineService.currentlyHostedLobbyStateProperty().addListener((obs, oldState, newState) -> {
            if (newState != null && newState.getId().equals(this.lobbyId)) {
                updateFullLobbyState(newState);
            }
        });
    }

    private void updateFieldsFromLobbyState(LobbyStateData lobbyState, boolean isInitialSetup) {
        uiUpdatingFromServer = true;
        try {
            if (lobbyState == null) return;

            GameSettingsData gs = lobbyState.getGameSettings();
            if (gs != null) {
                teamBlueNameInput.setText(gs.teamBlueName() != null ? gs.teamBlueName() : "Niebiescy");
                teamRedNameInput.setText(gs.teamRedName() != null ? gs.teamRedName() : "Czerwoni");

                String categoryToSet = gs.category();
                if (categoryToSet == null && categoryComboBox.getItems().contains(MIX_CATEGORY_IDENTIFIER)) {
                    categoryToSet = MIX_CATEGORY_IDENTIFIER;
                } else if (categoryToSet == null && !categoryComboBox.getItems().isEmpty()) {
                    categoryToSet = categoryComboBox.getItems().get(0);
                }

                if (categoryToSet != null && categoryComboBox.getItems().contains(categoryToSet)) {
                    if (!Objects.equals(categoryComboBox.getValue(), categoryToSet)) {
                        categoryComboBox.setValue(categoryToSet);
                        if (!isInitialSetup) preloadCategoryData(categoryToSet);
                    }
                } else if (!categoryComboBox.getItems().isEmpty()){
                    if(categoryComboBox.getValue() == null) categoryComboBox.getSelectionModel().selectFirst();
                }

                if (!roundsSlider.isValueChanging() && (int) roundsSlider.getValue() != gs.numberOfRounds()) {
                    roundsSlider.setValue(gs.numberOfRounds());
                }
                roundsValueLabel.setText(String.format("%.0f", roundsSlider.getValue()));

                if (!answerTimeSlider.isValueChanging() && (int) answerTimeSlider.getValue() != gs.answerTime()) {
                    answerTimeSlider.setValue(gs.answerTime());
                }
                answerTimeValueLabel.setText(String.format("%.0fs", answerTimeSlider.getValue()));

                lastKnownCategory = categoryToSet;
                lastKnownRounds = gs.numberOfRounds();
                lastKnownAnswerTime = gs.answerTime();
                lastKnownTeamBlueName = teamBlueNameInput.getText();
                lastKnownTeamRedName = teamRedNameInput.getText();
            }

            String currentPassword = lobbyState.getPassword() != null ? lobbyState.getPassword() : "";
            if (!passwordField.getText().equals(currentPassword)) {
                passwordField.setText(currentPassword);
            }
            lastKnownPassword = currentPassword;

        } finally {
            if (!isInitialSetup) {
                Platform.runLater(() -> uiUpdatingFromServer = false);
            } else {
                uiUpdatingFromServer = false;
            }
        }
    }


    private void preloadCategoryData(String categoryName) {
        if (categoryPreloadTask != null && categoryPreloadTask.isRunning()) categoryPreloadTask.cancel(true);
        preloadedGameService = null; isCategoryPreloadingSuccessful = false; updateStartGameButtonState();
        if (MIX_CATEGORY_IDENTIFIER.equals(categoryName) || categoryName == null) {
            categoryLoadingStatusLabel.setText("Kategoria MIX - gotowa."); categoryLoadingStatusLabel.setTextFill(Color.LIGHTGREEN);
            isCategoryPreloadingSuccessful = true; updateStartGameButtonState(); return;
        }
        if ("Brak kategorii".equals(categoryName)){
            categoryLoadingStatusLabel.setText("Wybierz poprawną kategorię."); categoryLoadingStatusLabel.setTextFill(Color.ORANGERED);
            isCategoryPreloadingSuccessful = false; updateStartGameButtonState(); return;
        }
        categoryLoadingStatusLabel.setText("Ładowanie danych dla: " + categoryName + "..."); categoryLoadingStatusLabel.setTextFill(Color.ORANGE);
        categoryPreloadTask = new Task<>() {
            @Override protected GameService call() throws Exception { return new GameService(categoryName); }
        };
        categoryPreloadTask.setOnSucceeded(event -> Platform.runLater(()-> {
            preloadedGameService = categoryPreloadTask.getValue();
            if (preloadedGameService != null && preloadedGameService.getCurrentQuestion() != null && !preloadedGameService.getAllAnswersForCurrentQuestion().isEmpty()) {
                categoryLoadingStatusLabel.setText("Kategoria '" + categoryName + "' załadowana."); categoryLoadingStatusLabel.setTextFill(Color.LIGHTGREEN);
                isCategoryPreloadingSuccessful = true;
            } else {
                categoryLoadingStatusLabel.setText("Błąd ładowania kategorii: '" + categoryName + "'. Brak pytań/odpowiedzi."); categoryLoadingStatusLabel.setTextFill(Color.RED);
                isCategoryPreloadingSuccessful = false; preloadedGameService = null;
            }
            updateStartGameButtonState();
        }));
        categoryPreloadTask.setOnFailed(event -> Platform.runLater(()-> {
            categoryLoadingStatusLabel.setText("Błąd krytyczny ładowania: '" + categoryName + "'."); categoryLoadingStatusLabel.setTextFill(Color.RED);
            isCategoryPreloadingSuccessful = false; preloadedGameService = null; updateStartGameButtonState();
            Throwable ex = categoryPreloadTask.getException();
            if (ex != null) AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd ładowania kategorii", "Nie udało się załadować danych dla kategorii: " + categoryName, ex.getMessage(), Duration.seconds(8));
        }));
        Thread thread = new Thread(categoryPreloadTask); thread.setDaemon(true); thread.start();
    }

    private void updateStartGameButtonState() {
        Platform.runLater(()->{
            boolean teamsAreValid = false;
            LobbyStateData currentLobby = onlineService.currentlyHostedLobbyStateProperty().get();
            if (currentLobby != null && currentLobby.getTeams() != null && currentLobby.getGameSettings() != null) {
                String blueTeamName = currentLobby.getGameSettings().teamBlueName();
                String redTeamName = currentLobby.getGameSettings().teamRedName();
                if (blueTeamName == null || blueTeamName.trim().isEmpty()) blueTeamName = "Niebiescy";
                if (redTeamName == null || redTeamName.trim().isEmpty()) redTeamName = "Czerwoni";

                List<PlayerInfo> teamBlue = currentLobby.getTeams().get(blueTeamName);
                List<PlayerInfo> teamRed = currentLobby.getTeams().get(redTeamName);
                teamsAreValid = (teamBlue != null && !teamBlue.isEmpty()) && (teamRed != null && !teamRed.isEmpty());
            }
            startGameButton.setDisable(!(isCategoryPreloadingSuccessful && teamsAreValid && !waitingForGameStartConfirmation));
        });
    }

    private void actualSaveLobbyConfiguration() {
        if (uiUpdatingFromServer) return;

        String currentBlueNameRaw = teamBlueNameInput.getText().trim();
        String currentRedNameRaw = teamRedNameInput.getText().trim();
        String currentPasswordValue = passwordField.getText();
        String currentCategoryValue = categoryComboBox.getValue();
        int currentRoundsValue = (int) roundsSlider.getValue();
        int currentAnswerTimeValue = (int) answerTimeSlider.getValue();

        String blueTeamFinal = currentBlueNameRaw.isEmpty() ? "Niebiescy" : currentBlueNameRaw;
        String redTeamFinal = currentRedNameRaw.isEmpty() ? "Czerwoni" : currentRedNameRaw;
        String categoryToSend = MIX_CATEGORY_IDENTIFIER.equals(currentCategoryValue) || "Brak kategorii".equals(currentCategoryValue) ? null : currentCategoryValue;

        boolean configChanged = !Objects.equals(currentPasswordValue, lastKnownPassword) ||
                !Objects.equals(blueTeamFinal, lastKnownTeamBlueName) ||
                !Objects.equals(redTeamFinal, lastKnownTeamRedName) ||
                !Objects.equals(categoryToSend, lastKnownCategory) ||
                currentRoundsValue != lastKnownRounds ||
                currentAnswerTimeValue != lastKnownAnswerTime;

        if (!configChanged) {
            return;
        }

        if(blueTeamFinal.equals(redTeamFinal)) {
            String correctedRedName = redTeamFinal + "2";
            correctedRedName = correctedRedName.substring(0, Math.min(correctedRedName.length(), MAX_TEAM_NAME_LENGTH));
            final String finalCorrectedRedName = correctedRedName;
            Platform.runLater(() -> teamRedNameInput.setText(finalCorrectedRedName));
            redTeamFinal = finalCorrectedRedName;
            AutoClosingAlerts.show(stage, Alert.AlertType.WARNING, "Korekta Nazwy Drużyny", null, "Nazwy drużyn nie mogą być takie same. Nazwa drugiej drużyny została automatycznie zmieniona.", Duration.seconds(5));
        }

        lastKnownPassword = currentPasswordValue;
        lastKnownTeamBlueName = blueTeamFinal;
        lastKnownTeamRedName = redTeamFinal;
        lastKnownCategory = categoryToSend;
        lastKnownRounds = currentRoundsValue;
        lastKnownAnswerTime = currentAnswerTimeValue;

        GameSettingsData newSettings = new GameSettingsData(categoryToSend, currentAnswerTimeValue, currentRoundsValue, 6, blueTeamFinal, redTeamFinal);
        Map<String, Object> message = new HashMap<>();
        message.put("action", "configureLobby"); message.put("lobbyId", this.lobbyId);
        message.put("password", currentPasswordValue); message.put("gameSettings", newSettings);
        if (onlineService != null) onlineService.sendJsonMessage(message);
    }


    private void handleAssignAsQuizMasterAction(PlayerInfo player) {
        if (player == null || (currentQuizMaster.get() != null && currentQuizMaster.get().sessionId().equals(player.sessionId()))) return;
        if (onlineService != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "assignRole"); message.put("lobbyId", this.lobbyId);
            message.put("participantSessionId", player.sessionId()); message.put("role", "QUIZ_MASTER");
            onlineService.sendJsonMessage(message);
        }
        clearAllTileSelections();
    }

    private void handleAssignToTeamAction(PlayerInfo player, String teamName) {
        if (player == null) return;
        String targetTeamName = teamName.trim();
        if (targetTeamName.isEmpty()) return;
        if (currentQuizMaster.get() != null && currentQuizMaster.get().sessionId().equals(player.sessionId())) return;
        if (onlineService != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "assignRole"); message.put("lobbyId", this.lobbyId);
            message.put("participantSessionId", player.sessionId()); message.put("role", "PLAYER");
            message.put("targetTeamName", targetTeamName);
            onlineService.sendJsonMessage(message);
        }
        clearAllTileSelections();
    }

    private void updateAllPlayerDisplays(LobbyStateData stateToUse) {
        Platform.runLater(() -> {
            PlayerInfo qm = (stateToUse != null) ? stateToUse.getQuizMaster() : null;
            if (qm != null) {
                quizMasterNickLabel.setText("Prowadzący: " + qm.nickname());
                unassignQuizMasterButton.setVisible(true);
            } else {
                quizMasterNickLabel.setText("Prowadzący: (nieprzypisany)");
                unassignQuizMasterButton.setVisible(false);
            }

            if (stateToUse != null) {
                updatePlayerTiles(waitingPlayersTilePane, stateToUse.getWaitingPlayers(), "WAITING");

                GameSettingsData gs = stateToUse.getGameSettings();
                Map<String, List<PlayerInfo>> teams = stateToUse.getTeams();
                if (gs != null && teams != null) {
                    String blueKey = gs.teamBlueName() == null || gs.teamBlueName().isEmpty() ? "Niebiescy" : gs.teamBlueName();
                    String redKey = gs.teamRedName() == null || gs.teamRedName().isEmpty() ? "Czerwoni" : gs.teamRedName();
                    updatePlayerTiles(teamBlueTilePane, teams.get(blueKey), "TEAM_BLUE");
                    updatePlayerTiles(teamRedTilePane, teams.get(redKey), "TEAM_RED");
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
            updateStartGameButtonState();
        });
    }

    private void updatePlayerTiles(TilePane tilePane, List<PlayerInfo> players, String paneType) {
        tilePane.getChildren().clear();
        if (players != null) {
            for (PlayerInfo player : players) {
                if (player.getRole() == ParticipantRole.QUIZ_MASTER) continue;
                Node playerTile = createPlayerTile(player, paneType);
                tilePane.getChildren().add(playerTile);
            }
        }
    }

    private void removeSelectedParticipantWithConfirmation() {
        PlayerInfo playerToRemove = selectedPlayerForAction;
        if (playerToRemove == null && currentQuizMaster.get() != null && quizMasterDisplayBox.lookup(".selected-quizmaster") != null) playerToRemove = currentQuizMaster.get();
        if (playerToRemove == null) {
            AutoClosingAlerts.show(stage, Alert.AlertType.WARNING, "Błąd", null, "Wybierz uczestnika, którego chcesz usunąć.", Duration.seconds(4)); return;
        }
        final PlayerInfo finalPlayerToRemove = playerToRemove;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Potwierdzenie Usunięcia");
        confirmation.setHeaderText("Czy na pewno chcesz usunąć gracza " + finalPlayerToRemove.nickname() + " z lobby?");
        confirmation.setContentText("Gracz zostanie całkowicie usunięty z tego lobby.");
        Stage alertStage = (Stage) confirmation.getDialogPane().getScene().getWindow();
        if (stage != null && !stage.getIcons().isEmpty()) alertStage.getIcons().addAll(stage.getIcons());
        try {
            DialogPane dialogPane = confirmation.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) dialogPane.getStylesheets().add(cssPath);
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception cssEx) {}
        Optional<ButtonType> result = confirmation.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK){
            if (onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "removeParticipantFromLobby"); message.put("lobbyId", this.lobbyId);
                message.put("participantSessionId", finalPlayerToRemove.sessionId());
                onlineService.sendJsonMessage(message);
                clearAllTileSelections();
            }
        }
    }

    private void handleLeaveLobby() {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Opuścić Lobby?"); confirmationDialog.setHeaderText("Czy na pewno chcesz opuścić to lobby?");
        Stage alertStage = (Stage) confirmationDialog.getDialogPane().getScene().getWindow();
        if (stage != null && !stage.getIcons().isEmpty()) alertStage.getIcons().addAll(stage.getIcons());
        try {
            DialogPane dialogPane = confirmationDialog.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) dialogPane.getStylesheets().add(cssPath);
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception cssEx) {}
        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (onlineService != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", "leaveLobby"); message.put("lobbyId", this.lobbyId);
                onlineService.sendJsonMessage(message);
                onlineService.clearActiveHostConfigFrame();
                onlineService.currentlyHostedLobbyStateProperty().set(null);
            }
            if (onBackToLobbyChoice != null) onBackToLobbyChoice.run();
        }
    }

    public Parent getView() { return rootPane; }

    public void show() {
        if (stage.getScene() == null) {
            Scene scene = new Scene(rootPane, 1280, 800); addStylesToScene(scene); stage.setScene(scene);
        } else {
            stage.getScene().setRoot(rootPane); addStylesToScene(stage.getScene());
        }
        titleLabel.setText("Panel Konfiguracji Lobby: " + this.lobbyId);
        stage.setTitle("Panel Konfiguracji Lobby: " + this.lobbyId);
        stage.setMinWidth(1024); stage.setMinHeight(768); stage.setMaximized(true); stage.show();
        if (categoryComboBox.getValue() != null) {
            preloadCategoryData(categoryComboBox.getValue());
        } else if (!categoryComboBox.getItems().isEmpty() && !categoryComboBox.getItems().get(0).equals("Brak kategorii")) {
            preloadCategoryData(categoryComboBox.getItems().get(0));
        }
    }

    private void addStylesToScene(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !scene.getStylesheets().contains(cssPath)) scene.getStylesheets().add(cssPath);
        } catch (Exception e) {System.err.println("Error loading CSS for OnlineLobbyHostConfigFrame: " + e.getMessage());}
    }

    public void updateFullLobbyState(LobbyStateData clientLobbyState) {
        Platform.runLater(() -> {
            if (clientLobbyState == null || !clientLobbyState.getId().equals(this.lobbyId)) {
                return;
            }
            updateFieldsFromLobbyState(clientLobbyState, false);
            updateAllPlayerDisplays(clientLobbyState);

            if (waitingForGameStartConfirmation && clientLobbyState.getHostSessionId() != null && clientLobbyState.getHostSessionId().equals(clientSessionId)) {
                String lobbyStatus = clientLobbyState.getStatus() != null ? clientLobbyState.getStatus().toUpperCase() : "UNKNOWN";
                if ("BUSY".equals(lobbyStatus) && clientLobbyState.getCurrentQuestionText() != null && !clientLobbyState.getCurrentQuestionText().isEmpty() && !clientLobbyState.getCurrentQuestionText().startsWith("Koniec gry!")) {
                    waitingForGameStartConfirmation = false;
                    OnlineGamePrepFrame prepFrame = new OnlineGamePrepFrame(stage, clientLobbyState, onlineService, clientSessionId);
                    Parent prepFrameRoot = prepFrame.getView();
                    Scene currentScene = stage.getScene();
                    if (onlineService != null) onlineService.setActiveGamePrepFrame(prepFrame);
                    if (currentScene != null && prepFrameRoot != null) {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentScene.getRoot());
                        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(event -> {
                            currentScene.setRoot(prepFrameRoot); prepFrame.show();
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), prepFrameRoot);
                            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
                            if (onlineService != null) onlineService.clearActiveHostConfigFrame();
                        });
                        fadeOut.play();
                    } else if (prepFrameRoot != null) {
                        Scene newScene = new Scene(prepFrameRoot); addStylesToScene(newScene); stage.setScene(newScene);
                        prepFrame.show();
                        if (onlineService != null) onlineService.clearActiveHostConfigFrame();
                    }
                } else if ("AVAILABLE".equals(lobbyStatus)) {
                    waitingForGameStartConfirmation = false;
                }
            }
            if (!waitingForGameStartConfirmation && startGameButton.getText().equals("Uruchamianie...")) {
                startGameButton.setText("Rozpocznij Grę");
            }
            updateStartGameButtonState();
        });
    }
}