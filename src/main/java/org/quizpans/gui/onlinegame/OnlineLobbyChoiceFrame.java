package org.quizpans.gui.onlinegame;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.online.model.LobbyStateData;
import org.quizpans.online.model.OnlineLobbyUIData;
import org.quizpans.services.OnlineService;
import org.quizpans.utils.AutoClosingAlerts;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineLobbyChoiceFrame {

    private static final Logger LOGGER = Logger.getLogger(OnlineLobbyChoiceFrame.class.getName());

    private Stage stage;
    private BorderPane rootPane;
    private OnlineService onlineService;
    private Runnable onBackAction;
    private ListView<OnlineLobbyUIData> lobbyListView;
    private Button actionButton;
    private Button refreshButton;
    private Button backButton;

    private ChangeListener<LobbyStateData> hostedLobbyListener;


    private static final Color COLOR_ACCENT_MAIN_BLUE = Color.rgb(0, 160, 255);
    private static final Color COLOR_ACCENT_GOLD = Color.rgb(255, 200, 0);
    private static final Color COLOR_TEXT_BRIGHT_WHITE = Color.rgb(240, 245, 255);
    private static final Color COLOR_TEXT_SUBTLE_GREY = Color.rgb(170, 180, 205);

    private static final Color CONTAINER_LIST_BG = Color.rgb(25, 28, 50, 0.85);
    private static final Color CONTAINER_LIST_BORDER = Color.rgb(60, 70, 100, 0.7);
    private static final Color CONTAINER_LIST_BORDER_GLOW = COLOR_ACCENT_MAIN_BLUE.deriveColor(0, 1.0, 1.0, 0.35);

    private static final Color CELL_BG_NORMAL = Color.rgb(42, 48, 75, 0.88);
    private static final Color CELL_BG_HOVER = Color.rgb(58, 64, 95, 0.92);
    private static final Color CELL_BG_SELECTED = COLOR_ACCENT_MAIN_BLUE.deriveColor(0, 1, 1, 0.35);
    private static final Color CELL_BORDER_NORMAL = Color.rgb(85, 95, 135, 0.65);
    private static final Color CELL_BORDER_SELECTED = COLOR_ACCENT_GOLD;

    private static final String FONT_DEFAULT_UI = "Segoe UI";
    private static final String FONT_TITLE_ACCENT = "Arial Black";

    private static final String ACTION_REQUEST_HOST_LOBBY = "requestHostLobby";
    private static final String ACTION_GET_ALL_LOBBIES = "getAllLobbies";


    public OnlineLobbyChoiceFrame(Stage stage, OnlineService onlineService, Runnable onBackAction) {
        this.stage = stage;
        this.onlineService = onlineService;
        this.onBackAction = onBackAction;
        initializeView();
        setupOnlineServiceListeners();
    }

    private LinearGradient getPageStandardBackground() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1e2b58")),
                new Stop(1, Color.web("#a01c1c")));
    }

    private void initializeView() {
        rootPane = new BorderPane();
        rootPane.setPadding(new Insets(25));
        rootPane.setBackground(new Background(new BackgroundFill(getPageStandardBackground(), CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("Wybierz Lobby Online");
        titleLabel.setFont(Font.font(FONT_TITLE_ACCENT, FontWeight.BOLD, 38));
        titleLabel.setTextFill(COLOR_ACCENT_GOLD);
        titleLabel.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.6), 18, 0.1, 0, 3));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(10, 0, 30, 0));
        rootPane.setTop(titleLabel);

        lobbyListView = new ListView<>();
        if (onlineService != null && onlineService.getOnlineLobbies() != null) {
            lobbyListView.setItems(onlineService.getOnlineLobbies());
        }
        lobbyListView.setPlaceholder(new Label("Oczekiwanie na dane z serwera..."));
        lobbyListView.setBackground(Background.EMPTY);
        lobbyListView.getStyleClass().add("no-scroll-bar");
        lobbyListView.setFocusTraversable(false);
        lobbyListView.setPrefHeight(420);

        lobbyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (actionButton != null) {
                actionButton.setDisable(newSelection == null);
                if (newSelection != null) {
                    String status = newSelection.getStatus() != null ? newSelection.getStatus().toUpperCase() : "UNKNOWN";
                    if ("AVAILABLE".equals(status)) {
                        actionButton.setText("Hostuj / Konfiguruj");
                    } else {
                        actionButton.setText("Akcja Niedostępna");
                        actionButton.setDisable(true);
                    }
                } else {
                    actionButton.setText("Wybierz Lobby");
                }
            }
        });

        lobbyListView.setCellFactory(param -> new LobbyListCell());

        VBox listFrameContainer = new VBox(lobbyListView);
        listFrameContainer.setAlignment(Pos.TOP_CENTER);
        listFrameContainer.setMaxWidth(720);
        listFrameContainer.setPadding(new Insets(20));
        VBox.setVgrow(lobbyListView, Priority.ALWAYS);

        CornerRadii frameRadii = new CornerRadii(18);
        listFrameContainer.setBackground(new Background(new BackgroundFill(CONTAINER_LIST_BG, frameRadii, Insets.EMPTY)));

        BorderStroke outerFrameBorder = new BorderStroke(CONTAINER_LIST_BORDER, BorderStrokeStyle.SOLID, frameRadii, new BorderWidths(1.8));
        BorderStroke innerFrameGlow = new BorderStroke(CONTAINER_LIST_BORDER_GLOW, BorderStrokeStyle.SOLID, frameRadii, new BorderWidths(2.6), new Insets(-1.3));
        listFrameContainer.setBorder(new Border(outerFrameBorder, innerFrameGlow));

        DropShadow frameOuterShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.4), 28, 0.0, 0, 8);
        listFrameContainer.setEffect(frameOuterShadow);

        rootPane.setCenter(listFrameContainer);
        BorderPane.setAlignment(listFrameContainer, Pos.TOP_CENTER);

        double commonButtonHeightPadding = 12;
        Font commonButtonFont = Font.font(FONT_DEFAULT_UI, FontWeight.BOLD, 15);

        actionButton = new Button("Wybierz Lobby");
        actionButton.getStyleClass().add("main-menu-button");
        actionButton.setMinWidth(220);
        actionButton.setPrefWidth(260);
        actionButton.setFont(Font.font(FONT_DEFAULT_UI, FontWeight.BOLD, 16));
        actionButton.setPadding(new Insets(commonButtonHeightPadding, 20, commonButtonHeightPadding, 20));
        actionButton.setDisable(true);
        actionButton.setOnAction(e -> handleLobbyAction());

        refreshButton = new Button("Odśwież Listę");
        refreshButton.getStyleClass().add("main-menu-button");
        refreshButton.setMinWidth(180);
        refreshButton.setPrefWidth(180);
        refreshButton.setFont(commonButtonFont);
        refreshButton.setPadding(new Insets(commonButtonHeightPadding, 15, commonButtonHeightPadding, 15));
        refreshButton.setOnAction(e -> handleRefreshAction());

        backButton = new Button("Wróć do Menu");
        String baseExitColorStyle = "-fx-background-color: #901818; -fx-text-fill: " + toWebColor(COLOR_TEXT_BRIGHT_WHITE) + ";";
        String hoverExitColorStyle = "-fx-background-color: #a82020; -fx-text-fill: " + toWebColor(COLOR_TEXT_BRIGHT_WHITE) + ";";
        backButton.getStyleClass().add("main-menu-button");
        backButton.setStyle(baseExitColorStyle);
        backButton.setMinWidth(180);
        backButton.setPrefWidth(180);
        backButton.setFont(commonButtonFont);
        backButton.setPadding(new Insets(commonButtonHeightPadding, 15, commonButtonHeightPadding, 15));
        backButton.setOnMouseEntered(e -> backButton.setStyle(hoverExitColorStyle));
        backButton.setOnMouseExited(e -> backButton.setStyle(baseExitColorStyle));
        backButton.setOnAction(e -> {
            if (hostedLobbyListener != null && onlineService != null) {
                onlineService.currentlyHostedLobbyStateProperty().removeListener(hostedLobbyListener);
            }
            if (onBackAction != null) {
                onBackAction.run();
            }
        });

        HBox allButtonsContainer = new HBox(20);
        allButtonsContainer.getChildren().addAll(actionButton, refreshButton, backButton);
        allButtonsContainer.setAlignment(Pos.CENTER);
        BorderPane.setMargin(allButtonsContainer, new Insets(30, 0, 20, 0));
        rootPane.setBottom(allButtonsContainer);
    }

    private void setupOnlineServiceListeners() {
        if (onlineService == null) return;

        if (onlineService.getOnlineLobbies() != null) {
            onlineService.getOnlineLobbies().addListener((ListChangeListener<OnlineLobbyUIData>) c -> {
                Platform.runLater(() -> {
                    if (lobbyListView.getItems().isEmpty()) {
                        lobbyListView.setPlaceholder(new Label(onlineService.isOpen() ? "Brak dostępnych lobby." : "Połącz się z serwerem klikając 'Odśwież'."));
                    } else {
                        lobbyListView.setPlaceholder(null);
                    }
                    lobbyListView.refresh();
                });
            });
        }

        hostedLobbyListener = (obs, oldHostedLobby, newHostedLobby) -> {
            if (newHostedLobby != null && onlineService.getClientSessionId() != null &&
                    onlineService.getClientSessionId().equals(newHostedLobby.getHostSessionId())) {
                System.out.println("OnlineLobbyChoiceFrame: Klient został hostem lobby " + newHostedLobby.getId() + ". Przechodzenie do konfiguracji.");
                if (stage.getScene() != null && stage.getScene().getRoot() == this.rootPane) {
                    switchToHostConfigFrame(newHostedLobby.getId());
                }
            }
        };
        onlineService.currentlyHostedLobbyStateProperty().removeListener(hostedLobbyListener);
        onlineService.currentlyHostedLobbyStateProperty().addListener(hostedLobbyListener);


        if (onlineService.getOnlineLobbies().isEmpty() && onlineService.isOpen()){
            handleRefreshAction();
        } else if (!onlineService.isOpen()){
            lobbyListView.setPlaceholder(new Label("Naciśnij 'Odśwież', aby połączyć i pobrać listę lobby."));
        }
    }

    private String toWebColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    private void handleLobbyAction() {
        if (onlineService == null) {
            AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd", null, "Usługa online nie jest dostępna.", Duration.seconds(4));
            return;
        }
        OnlineLobbyUIData selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();
        if (selectedLobby != null) {
            String status = selectedLobby.getStatus() != null ? selectedLobby.getStatus().toUpperCase() : "UNKNOWN";
            if ("AVAILABLE".equals(status)) {
                Map<String, Object> message = new HashMap<>();
                message.put("action", ACTION_REQUEST_HOST_LOBBY);
                message.put("lobbyId", selectedLobby.getId());
                onlineService.sendJsonMessage(message);
                System.out.println("Wysłano żądanie hostowania lobby: " + selectedLobby.getId() + ". Oczekiwanie na lobbyUpdate.");
            } else {
                AutoClosingAlerts.show(stage, Alert.AlertType.INFORMATION, "Lobby", null, "To lobby nie jest obecnie dostępne do hostowania.", Duration.seconds(3));
            }
        }
    }

    public void switchToHostConfigFrame(String lobbyIdToConfig) {
        if (hostedLobbyListener != null && onlineService != null) {
            onlineService.currentlyHostedLobbyStateProperty().removeListener(hostedLobbyListener);
        }
        Platform.runLater(() -> {
            OnlineLobbyHostConfigFrame hostConfigFrame = new OnlineLobbyHostConfigFrame(this.stage, this.onlineService, lobbyIdToConfig, this::show);
            if (onlineService != null) {
                onlineService.setActiveHostConfigFrame(hostConfigFrame);
                LobbyStateData currentHostedState = onlineService.currentlyHostedLobbyStateProperty().get();
                if (currentHostedState != null && currentHostedState.getId().equals(lobbyIdToConfig)) {
                    hostConfigFrame.updateFullLobbyState(currentHostedState);
                }
            }
            hostConfigFrame.show();
        });
    }


    private void handleRefreshAction() {
        if (onlineService == null) {
            AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd", null, "Usługa online nie jest dostępna.", Duration.seconds(4));
            lobbyListView.setPlaceholder(new Label("Usługa online niedostępna."));
            return;
        }
        lobbyListView.setPlaceholder(new Label("Pobieranie listy lobby..."));
        if (!onlineService.isOpen()) {
            onlineService.connect();
        } else {
            Map<String, Object> requestLobbiesMessage = new HashMap<>();
            requestLobbiesMessage.put("action", ACTION_GET_ALL_LOBBIES);
            onlineService.sendJsonMessage(requestLobbiesMessage);
        }
    }


    public Parent getView() {
        return rootPane;
    }

    public void show() {
        if (stage == null ) {
            LOGGER.warning("Stage is null, cannot show OnlineLobbyChoiceFrame.");
            return;
        }
        if (onlineService != null) {
            onlineService.clearActiveHostConfigFrame();
            if (hostedLobbyListener == null) {
                hostedLobbyListener = (obs, oldHostedLobby, newHostedLobby) -> {
                    if (newHostedLobby != null && onlineService.getClientSessionId() != null &&
                            onlineService.getClientSessionId().equals(newHostedLobby.getHostSessionId())) {
                        if (stage.getScene() != null && stage.getScene().getRoot() == this.rootPane) {
                            switchToHostConfigFrame(newHostedLobby.getId());
                        }
                    }
                };
            }
            onlineService.currentlyHostedLobbyStateProperty().removeListener(hostedLobbyListener);
            onlineService.currentlyHostedLobbyStateProperty().addListener(hostedLobbyListener);
        }

        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            Scene newScene = new Scene(rootPane);
            stage.setScene(newScene);
            applyCssToScene(newScene);
        } else {
            currentScene.setRoot(rootPane);
            applyCssToScene(currentScene);
        }

        performFadeIn(rootPane);
        stage.setTitle("QuizPans - Wybierz Lobby Online");
        if(!stage.isShowing()){
            stage.setMaximized(true);
            stage.show();
        }
        if (onlineService != null && onlineService.isOpen()) {
            handleRefreshAction();
        }
    }

    private void applyCssToScene(Scene scene){
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && (scene.getStylesheets().isEmpty() || !scene.getStylesheets().contains(cssPath))) {
                if(!scene.getStylesheets().contains(cssPath)) scene.getStylesheets().add(cssPath);
            }
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Nie można załadować pliku CSS: /styles.css.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Wystąpił nieoczekiwany błąd podczas ładowania CSS.", e);
        }
    }

    private void performFadeIn(Parent newRoot) {
        newRoot.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), newRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private static class LobbyListCell extends ListCell<OnlineLobbyUIData> {
        private final HBox cellContentHolder = new HBox();
        private final Circle statusIndicator = new Circle(10);
        private final VBox lobbyInfoVBox = new VBox(3);
        private final Label nameDisplayLabel = new Label();
        private final Label gameModeDisplayLabel = new Label();
        private final HBox detailsBox = new HBox(8);
        private final Label statusDisplayLabel = new Label();
        private final Label playersDisplayLabel = new Label();
        private final Separator detailsSeparator = new Separator(Orientation.VERTICAL);

        private final CornerRadii itemCardRadius = new CornerRadii(12);
        private final DropShadow itemCardOuterShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.28), 12, 0.0, 2, 3.5);

        public LobbyListCell() {
            super();
            cellContentHolder.setAlignment(Pos.CENTER_LEFT);
            cellContentHolder.setPadding(new Insets(18, 22, 18, 22));
            HBox.setHgrow(lobbyInfoVBox, Priority.ALWAYS);
            HBox.setMargin(lobbyInfoVBox, new Insets(0, 0, 0, 18));

            nameDisplayLabel.setFont(Font.font(FONT_DEFAULT_UI, FontWeight.BOLD, 21));
            nameDisplayLabel.setTextFill(COLOR_TEXT_BRIGHT_WHITE.deriveColor(0,1,0.98,1));

            gameModeDisplayLabel.setFont(Font.font(FONT_DEFAULT_UI, FontWeight.NORMAL, FontPosture.ITALIC, 13));
            gameModeDisplayLabel.setTextFill(COLOR_TEXT_SUBTLE_GREY.brighter());
            VBox.setMargin(gameModeDisplayLabel, new Insets(2,0,5,0));
            gameModeDisplayLabel.setVisible(false);
            gameModeDisplayLabel.setManaged(false);

            statusDisplayLabel.setFont(Font.font(FONT_DEFAULT_UI, FontWeight.SEMI_BOLD, 14));
            playersDisplayLabel.setFont(Font.font(FONT_DEFAULT_UI, FontWeight.NORMAL, 14));
            playersDisplayLabel.setTextFill(COLOR_TEXT_SUBTLE_GREY.brighter());

            detailsSeparator.setPrefHeight(16);
            detailsSeparator.setValignment(VPos.CENTER);

            detailsBox.setAlignment(Pos.CENTER_LEFT);
            detailsBox.getChildren().addAll(statusDisplayLabel, detailsSeparator, playersDisplayLabel);

            lobbyInfoVBox.getChildren().addAll(nameDisplayLabel, gameModeDisplayLabel, detailsBox);
            cellContentHolder.getChildren().addAll(statusIndicator, lobbyInfoVBox);

            this.setBackground(Background.EMPTY);
            this.setPadding(new Insets(6,3,6,3));
            cellContentHolder.setEffect(itemCardOuterShadow);

            selectedProperty().addListener((obs, o, isSelected) -> styleCard(isSelected, isHover()));
            hoverProperty().addListener((obs, o, isHovered) -> styleCard(isSelected(), isHovered));

            styleCard(false, false);
        }

        private void styleCard(boolean selected, boolean hovered) {
            BackgroundFill bgFill;
            Color borderColor;
            BorderWidths borderWidth = new BorderWidths(1.6);

            if (selected) {
                bgFill = new BackgroundFill(CELL_BG_SELECTED, itemCardRadius, Insets.EMPTY);
                borderColor = CELL_BORDER_SELECTED;
                borderWidth = new BorderWidths(2.2);
            } else if (hovered) {
                bgFill = new BackgroundFill(CELL_BG_HOVER, itemCardRadius, Insets.EMPTY);
                borderColor = CELL_BORDER_NORMAL.brighter().deriveColor(0,1,1.2,1);
            } else {
                bgFill = new BackgroundFill(CELL_BG_NORMAL, itemCardRadius, Insets.EMPTY);
                borderColor = CELL_BORDER_NORMAL;
            }
            cellContentHolder.setBackground(new Background(bgFill));
            cellContentHolder.setBorder(new Border(new BorderStroke(borderColor, BorderStrokeStyle.SOLID, itemCardRadius, borderWidth)));
        }

        @Override
        protected void updateItem(OnlineLobbyUIData item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                cellContentHolder.setVisible(false);
            } else {
                cellContentHolder.setVisible(true);
                nameDisplayLabel.setText(item.getName() != null ? item.getName() : "Lobby bez nazwy");

                String statusText = item.getStatus() != null ? item.getStatus() : "Nieznany";
                statusDisplayLabel.setText(statusText);
                playersDisplayLabel.setText(item.getPlayerCount() + "/" + item.getMaxPlayers() + " uczestników");

                Color indicatorDotColor; Color statusHighlightColor;
                String currentStatus = item.getStatus() != null ? item.getStatus().toUpperCase() : "UNKNOWN";
                switch (currentStatus) {
                    case "AVAILABLE": indicatorDotColor = Color.LIMEGREEN; statusHighlightColor = Color.rgb(180,255,190); break;
                    case "BUSY": indicatorDotColor = Color.ORANGERED; statusHighlightColor = Color.rgb(255,180,160); break;
                    default: indicatorDotColor = Color.SLATEGRAY; statusHighlightColor = COLOR_TEXT_SUBTLE_GREY; break;
                }
                statusIndicator.setFill(indicatorDotColor);
                statusIndicator.setStroke(indicatorDotColor.darker().deriveColor(0,1,1,0.5));
                statusIndicator.setStrokeWidth(1.2);

                DropShadow statusDotShadow = new DropShadow(BlurType.GAUSSIAN, indicatorDotColor.deriveColor(0,0.6,1,0.35), 6,0.25,0,0.5);
                InnerShadow statusDotInnerLight = new InnerShadow(BlurType.GAUSSIAN, Color.rgb(255,255,255,0.1), 2,0,0,0);
                statusIndicator.setEffect(new javafx.scene.effect.Blend(javafx.scene.effect.BlendMode.SCREEN, statusDotShadow, statusDotInnerLight));

                statusDisplayLabel.setTextFill(statusHighlightColor);

                styleCard(isSelected(), isHover());
                setGraphic(cellContentHolder);
            }
        }
    }
}