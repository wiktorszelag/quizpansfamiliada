package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
// Usunięto Bindings, jeśli nie jest już używany gdzie indziej
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.utils.BackgroundLoader; // Potrzebne tylko jeśli gdzieś indziej sprawdzamy
import org.quizpans.utils.AutoClosingAlerts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TeamSetupFrame {

    private final ComboBox<Integer> teamSizeComboBox;
    private final ComboBox<String> categoryComboBox;
    private final Slider answerTimeSlider;
    private final VBox team1MembersPanel;
    private final VBox team2MembersPanel;
    private final TextField team1Field;
    private final TextField team2Field;
    private Button grajButton;
    private Button dalszeUstawieniaButton;

    private Stage stage;
    private VBox mainPanel;
    private VBox settingsPanelRoot;
    private ScrollPane scrollPaneForMainPanel;
    private Scene scene;
    private Runnable backAction;
    private Set<String> passedCategories;

    private static final Pattern POLISH_CAPITAL_PATTERN = Pattern.compile("[A-ZĄĆĘŁŃÓŚŹŻ]");

    public TeamSetupFrame(Stage primaryStage, Set<String> categories, Runnable onBack) {
        this.stage = primaryStage;
        this.passedCategories = categories;
        this.backAction = onBack;

        teamSizeComboBox = new ComboBox<>();
        categoryComboBox = new ComboBox<>();
        answerTimeSlider = new Slider(10, 120, 30);
        team1Field = new TextField();
        team2Field = new TextField();
        team1MembersPanel = new VBox(10);
        team2MembersPanel = new VBox(10);

        mainPanel = new VBox(30);
        settingsPanelRoot = new VBox(20);
        scrollPaneForMainPanel = new ScrollPane();

        commonSetup();
    }

    public TeamSetupFrame(Stage primaryStage, Runnable onBack) {
        this(primaryStage, null, onBack);
    }


    private void commonSetup() {
        this.stage.setTitle("Ustawienia Gry - Quizpans");
        this.stage.setResizable(true);
        this.stage.setMinWidth(850);

        setApplicationIcon();

        if (!team1Field.getProperties().containsKey("listenerAdded")) {
            addCapitalizationListener(team1Field);
            team1Field.getProperties().put("listenerAdded", true);
        }
        if (!team2Field.getProperties().containsKey("listenerAdded")) {
            addCapitalizationListener(team2Field);
            team2Field.getProperties().put("listenerAdded", true);
        }

        answerTimeSlider.setMajorTickUnit(10);
        answerTimeSlider.setMinorTickCount(1);
        answerTimeSlider.setBlockIncrement(5);
        answerTimeSlider.setShowTickMarks(true);
        answerTimeSlider.setShowTickLabels(true);
        answerTimeSlider.setSnapToTicks(true);

        initializeSettingsPanelStructure();
        initializeMainPanelStructure();

        scrollPaneForMainPanel.setContent(mainPanel);
        scrollPaneForMainPanel.setFitToWidth(true);
        scrollPaneForMainPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPaneForMainPanel.getStyleClass().add("no-scroll-bar");
        scrollPaneForMainPanel.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        if (this.stage != null) {
            this.stage.setWidth(visualBounds.getWidth());
            this.stage.setHeight(visualBounds.getHeight());
            this.stage.centerOnScreen();
        }

        updateTeamMembers();

        if (this.stage != null && this.stage.getScene() == null) {
            this.scene = new Scene(getRootPane());
            loadStylesheets();
            this.stage.setScene(this.scene);
        } else if (this.stage != null) {
            this.scene = this.stage.getScene();
            if (this.scene.getRoot() != settingsPanelRoot && this.scene.getRoot() != scrollPaneForMainPanel) {
                this.scene.setRoot(getRootPane());
            }
            loadStylesheets();
        }

        if (this.scene != null && (this.scene.getFill() == null || !this.scene.getFill().equals(createBackgroundGradient()))) {
            this.scene.setFill(createBackgroundGradient());
        }
    }

    private void loadStylesheets() {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (scene != null && cssPath != null) {
                if (scene.getStylesheets() == null || scene.getStylesheets().isEmpty()) {
                    scene.getStylesheets().add(cssPath);
                } else if (!scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().add(cssPath);
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania arkusza stylów w TeamSetupFrame: " + e.getMessage());
        }
    }

    private void loadStylesheetsToScene(Scene targetScene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (targetScene != null && cssPath != null) {
                if (targetScene.getStylesheets() == null || targetScene.getStylesheets().isEmpty()) {
                    targetScene.getStylesheets().add(cssPath);
                } else if (!targetScene.getStylesheets().contains(cssPath)) {
                    targetScene.getStylesheets().add(cssPath);
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania arkusza stylów do sceny: " + e.getMessage());
        }
    }


    private LinearGradient createBackgroundGradient() {
        return new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
    }

    private void setApplicationIcon() {
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream == null) return;
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                logoStream.close(); return;
            }
            if (stage != null && stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {
            System.err.println("Błąd ładowania ikony aplikacji w TeamSetupFrame: " + e.getMessage());
        }
    }

    private void addCapitalizationListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                String firstChar = newValue.substring(0, 1);
                if (!POLISH_CAPITAL_PATTERN.matcher(firstChar).matches() && Character.isLetter(firstChar.charAt(0))) {
                    Platform.runLater(() -> {
                        int caretPos = textField.getCaretPosition();
                        String corrected = firstChar.toUpperCase() + newValue.substring(1);
                        textField.setText(corrected);
                        if (caretPos <= corrected.length()) textField.positionCaret(caretPos);
                        else textField.positionCaret(corrected.length());
                    });
                }
            }
        });
    }

    private void initializeMainPanelStructure() {
        mainPanel.getChildren().clear();
        mainPanel.setPadding(new Insets(30));
        mainPanel.setAlignment(Pos.TOP_CENTER);
        mainPanel.setMinHeight(Region.USE_PREF_SIZE);

        Label titleLabel = new Label("WPROWADŹ DANE DRUŻYN");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        HBox teamDisplayArea = new HBox(30);
        teamDisplayArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(teamDisplayArea, Priority.ALWAYS);

        VBox team1Panel = createTeamPanel("Drużyna 1", team1Field, team1MembersPanel);
        VBox team2Panel = createTeamPanel("Drużyna 2", team2Field, team2MembersPanel);

        HBox.setHgrow(team1Panel, Priority.ALWAYS);
        HBox.setHgrow(team2Panel, Priority.ALWAYS);
        teamDisplayArea.getChildren().addAll(team1Panel, team2Panel);

        HBox buttonPanel = new HBox(25);
        buttonPanel.setAlignment(Pos.CENTER);

        Button menuButton = createStyledButton("Wróć", "#f44336");
        menuButton.setOnAction(e -> switchToSettingsView());

        grajButton = createStyledButton("Graj", "#4CAF50");
        grajButton.setOnAction(e -> startGame());
        grajButton.setDisable(false); // Przycisk "Graj" jest teraz domyślnie aktywny


        buttonPanel.getChildren().clear();
        buttonPanel.getChildren().addAll(menuButton, grajButton);
        VBox.setMargin(buttonPanel, new Insets(20, 0, 10, 0));
        mainPanel.getChildren().setAll(titleLabel, teamDisplayArea, buttonPanel);
    }

    private VBox createTeamPanel(String teamNameText, TextField teamNameField, VBox membersPanelContainer) {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(25));
        panel.getStyleClass().add("team-setup-panel");

        Label teamLabel = new Label(teamNameText);
        teamLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        teamLabel.setTextFill(Color.WHITE);
        teamLabel.setEffect(new DropShadow(8, Color.rgb(0,0,0,0.6)));
        VBox.setMargin(teamLabel, new Insets(0, 0, 15, 0));

        teamNameField.setPromptText("Wprowadź nazwę drużyny");
        teamNameField.getStyleClass().add("team-name-field");
        VBox.setMargin(teamNameField, new Insets(0, 10, 20, 10));

        Label membersTitleLabel = new Label("Członkowie Drużyny:");
        membersTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        membersTitleLabel.setTextFill(Color.WHITE);
        membersTitleLabel.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.4)));
        VBox.setMargin(membersTitleLabel, new Insets(10, 0, 10, 0));

        membersPanelContainer.setSpacing(15);
        membersPanelContainer.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(membersPanelContainer, Priority.ALWAYS);
        panel.getChildren().addAll(teamLabel, teamNameField, membersTitleLabel, membersPanelContainer);
        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18px; " +
                "-fx-background-radius: 20; " +
                "-fx-padding: 12 25 12 25;");
        button.setEffect(new DropShadow(5, Color.BLACK));
        button.setPrefWidth(220);
        button.setOnMouseEntered(e -> button.setScaleX(1.05));
        button.setOnMouseExited(e -> button.setScaleX(1.0));
        return button;
    }

    private void initializeSettingsPanelStructure() {
        settingsPanelRoot.getChildren().clear();
        settingsPanelRoot.setPadding(new Insets(30));
        settingsPanelRoot.setAlignment(Pos.TOP_CENTER);
        settingsPanelRoot.setFillWidth(true);
        VBox.setVgrow(settingsPanelRoot, Priority.ALWAYS);
        settingsPanelRoot.setMinHeight(Region.USE_PREF_SIZE);
        settingsPanelRoot.setBackground(new Background(new BackgroundFill(createBackgroundGradient(), CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("USTAWIENIA GRY");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        if(teamSizeComboBox.getItems().isEmpty()) {
            teamSizeComboBox.getItems().addAll(1, 2, 3, 4, 5, 6);
        }
        if(teamSizeComboBox.getSelectionModel().isEmpty() && !teamSizeComboBox.getItems().isEmpty()) {
            teamSizeComboBox.getSelectionModel().selectFirst();
        }
        teamSizeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTeamMembers();
            }
        });

        categoryComboBox.getItems().clear();
        if (this.passedCategories != null && !this.passedCategories.isEmpty()) {
            categoryComboBox.setItems(FXCollections.observableArrayList(this.passedCategories));
            if (categoryComboBox.getSelectionModel().isEmpty() && !categoryComboBox.getItems().isEmpty()) {
                categoryComboBox.getSelectionModel().selectFirst();
            }
            categoryComboBox.setDisable(false);
            categoryComboBox.setPromptText("Wybierz kategorię");
        } else {
            categoryComboBox.getItems().add("Brak kategorii (Błąd)");
            categoryComboBox.getSelectionModel().selectFirst();
            categoryComboBox.setDisable(true);
            categoryComboBox.setPromptText("Błąd ładowania kategorii");
            System.err.println("TeamSetupFrame: Kategorie nie zostały poprawnie przekazane lub są puste.");
        }


        SettingsPanel settingsContent = new SettingsPanel(teamSizeComboBox, categoryComboBox, answerTimeSlider);
        VBox.setVgrow(settingsContent, Priority.ALWAYS);

        HBox buttonPanelSettings = new HBox(25);
        buttonPanelSettings.setAlignment(Pos.CENTER);

        Button powrotButton = createStyledButton("Menu Główne", "#f44336");
        powrotButton.setOnAction(e -> {
            if (backAction != null) {
                backAction.run();
            }
        });

        dalszeUstawieniaButton = createStyledButton("Ustaw Drużyny", "#2196F3");
        dalszeUstawieniaButton.setOnAction(e -> switchToMainPanel());
        dalszeUstawieniaButton.setDisable(this.passedCategories == null || this.passedCategories.isEmpty());
        if(dalszeUstawieniaButton.isDisabled()){
            dalszeUstawieniaButton.setTooltip(new Tooltip("Kategorie nie zostały załadowane."));
        }


        buttonPanelSettings.getChildren().addAll(powrotButton, dalszeUstawieniaButton);
        VBox.setMargin(buttonPanelSettings, new Insets(30, 0, 0, 0));

        settingsPanelRoot.getChildren().setAll(titleLabel, settingsContent, buttonPanelSettings);
    }


    private void updateTeamMembers() {
        Integer selectedSize = teamSizeComboBox.getValue();
        if (selectedSize == null) {
            if (!teamSizeComboBox.getItems().isEmpty()) {
                selectedSize = teamSizeComboBox.getItems().get(0);
            } else {
                selectedSize = 1;
            }
        }
        int teamSize = selectedSize;
        updateTeamPanel(team1MembersPanel, teamSize);
        updateTeamPanel(team2MembersPanel, teamSize);
    }

    private void updateTeamPanel(VBox membersPanelContainer, int size) {
        List<String> currentNames = new ArrayList<>();
        for (javafx.scene.Node node : membersPanelContainer.getChildren()) {
            if (node instanceof VBox) {
                if (node.getStyleClass().contains("player-card")) {
                    VBox playerCard = (VBox) node;
                    for (javafx.scene.Node childInCard : playerCard.getChildren()) {
                        if (childInCard instanceof TextField) {
                            currentNames.add(((TextField) childInCard).getText());
                            break;
                        }
                    }
                }
            }
        }

        membersPanelContainer.getChildren().clear();

        for (int i = 0; i < size; i++) {
            VBox playerCard = new VBox(8);
            playerCard.setPadding(new Insets(15));
            playerCard.getStyleClass().add("player-card");
            playerCard.setAlignment(Pos.CENTER_LEFT);

            Label playerNumberLabel = new Label("Gracz " + (i + 1));
            playerNumberLabel.getStyleClass().add("player-number-label");

            TextField playerNameField = new TextField();
            if (i < currentNames.size() && currentNames.get(i) != null) {
                playerNameField.setText(currentNames.get(i));
            }
            playerNameField.setPromptText("Imię gracza " + (i + 1));
            playerNameField.getStyleClass().add("player-name-field");

            addCapitalizationListener(playerNameField);

            VBox.setVgrow(playerNameField, Priority.ALWAYS);

            playerCard.getChildren().addAll(playerNumberLabel, playerNameField);
            membersPanelContainer.getChildren().add(playerCard);
        }
    }

    private void startGame() {
        // Sprawdzenie gotowości modeli nie jest tu potrzebne,
        // bo TeamSelectionFrame obsłuży oczekiwanie na GameService.

        String t1Name = team1Field.getText().trim();
        String t2Name = team2Field.getText().trim();

        if (t1Name.isEmpty()) t1Name = "Drużyna 1";
        if (t2Name.isEmpty()) t2Name = "Drużyna 2";

        final String finalTeam1Name = t1Name;
        final String finalTeam2Name = t2Name;
        final List<String> finalTeam1Members = getMembers(team1MembersPanel, 1);
        final List<String> finalTeam2Members = getMembers(team2MembersPanel, 2);
        final int finalAnswerTime = (int) Math.round(answerTimeSlider.getValue());
        final String finalSelectedCategory = categoryComboBox.getValue();

        if (finalTeam1Members.isEmpty() || finalTeam2Members.isEmpty()) {
            showErrorAlert("Obie drużyny muszą mieć co najmniej jednego gracza.", "Błąd");
            return;
        }

        if (finalSelectedCategory == null || finalSelectedCategory.trim().isEmpty() ||
                finalSelectedCategory.equals("Brak kategorii") ||
                finalSelectedCategory.equals("Brak kategorii (Błąd)") ||
                finalSelectedCategory.equals("Błąd ładowania kategorii") ||
                finalSelectedCategory.equals("Błąd ładowania") ) {
            showErrorAlert("Proszę wybrać poprawną kategorię pytań.", "Błąd wyboru kategorii");
            return;
        }

        proceedToTeamSelection(finalSelectedCategory, finalAnswerTime, finalTeam1Name, finalTeam2Name, finalTeam1Members, finalTeam2Members);
    }

    private void proceedToTeamSelection(String category, int time, String t1Name, String t2Name, List<String> t1Members, List<String> t2Members) {
        Scene currentScene = (stage != null) ? stage.getScene() : null;
        if (currentScene == null) {
            showErrorAlert("Błąd: Brak sceny do przełączenia.", "Błąd krytyczny");
            return;
        }
        Parent currentRoot = currentScene.getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            TeamSelectionFrame teamSelectionScreen = new TeamSelectionFrame(
                    category, time, t1Name, t2Name, t1Members, t2Members, this.stage, null, null
            );

            teamSelectionScreen.initializeFrameContent();
            Parent teamSelectionRoot = teamSelectionScreen.getRootPane();

            if (teamSelectionRoot == null) {
                showErrorAlert("Błąd inicjalizacji następnego ekranu (root jest null).", "Błąd krytyczny");
                currentScene.setRoot(currentRoot);
                FadeTransition fadeInFail = new FadeTransition(Duration.millis(300), currentRoot);
                fadeInFail.setFromValue(0.0);
                fadeInFail.setToValue(1.0);
                fadeInFail.play();
                return;
            }

            currentScene.setRoot(teamSelectionRoot);
            if (currentScene.getFill() == null || !currentScene.getFill().equals(createBackgroundGradient())) {
                currentScene.setFill(createBackgroundGradient());
            }

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), teamSelectionRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private List<String> getMembers(VBox panel, int teamNumber) {
        List<String> members = new ArrayList<>();
        int playerIndex = 1;
        for (javafx.scene.Node node : panel.getChildren()) {
            if (node instanceof VBox && node.getStyleClass().contains("player-card")) {
                VBox playerCard = (VBox) node;
                for (javafx.scene.Node childInCard : playerCard.getChildren()) {
                    if (childInCard instanceof TextField) {
                        String name = ((TextField) childInCard).getText().trim();
                        if (name.isEmpty()) {
                            name = "Gracz " + playerIndex + " (D" + teamNumber + ")";
                        }
                        members.add(name);
                        playerIndex++;
                        break;
                    }
                }
            }
        }
        return members;
    }

    private void showErrorAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            DialogPane dialogPane = alert.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                if(!dialogPane.getStylesheets().contains(cssPath)) {
                    dialogPane.getStylesheets().add(cssPath);
                }
                dialogPane.getStyleClass().add("custom-alert");
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania CSS dla alertu w TeamSetupFrame: " + e.getMessage());
        }
        alert.showAndWait();
    }

    private void switchRootWithFade(Parent newRoot) {
        if (scene == null) {
            System.err.println("Błąd krytyczny: Scena jest null w switchRootWithFade (TeamSetupFrame)!");
            if (stage != null && newRoot != null) {
                Scene tempScene = new Scene(newRoot);
                loadStylesheetsToScene(tempScene);
                stage.setScene(tempScene);
                if (tempScene.getFill() == null || !tempScene.getFill().equals(createBackgroundGradient())) {
                    tempScene.setFill(createBackgroundGradient());
                }
            }
            return;
        }
        Parent currentRoot = scene.getRoot();
        if (currentRoot == null) {
            scene.setRoot(newRoot);
            if (scene.getFill() == null || !scene.getFill().equals(createBackgroundGradient())) {
                scene.setFill(createBackgroundGradient());
            }
            return;
        }

        if (currentRoot == newRoot) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            scene.setRoot(newRoot);
            if (scene.getFill() == null || !scene.getFill().equals(createBackgroundGradient())) {
                scene.setFill(createBackgroundGradient());
            }
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void switchToSettingsView() {
        switchRootWithFade(settingsPanelRoot);
        if (stage != null) stage.setTitle("Ustawienia Gry - Quizpans");
    }

    private void switchToMainPanel() {
        updateTeamMembers();
        switchRootWithFade(scrollPaneForMainPanel);
        if (stage != null) stage.setTitle("Wprowadź Dane Drużyn - Quizpans");
    }

    public Parent getRootPane() {
        if (this.settingsPanelRoot == null) {
            System.err.println("Błąd krytyczny: settingsPanelRoot jest null w getRootPane (TeamSetupFrame)!");
            return new VBox();
        }
        return this.settingsPanelRoot;
    }
}