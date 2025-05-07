package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TeamSetupFrame {

    private final ComboBox<Integer> teamSizeComboBox;
    private final ComboBox<String> categoryComboBox;
    private final Slider answerTimeSlider;
    private final VBox team1MembersPanel;
    private final VBox team2MembersPanel;
    private final TextField team1Field;
    private final TextField team2Field;

    private Stage stage;
    private VBox mainPanel;
    private VBox settingsPanel;
    private ScrollPane scrollPane;
    private Scene scene;
    private Runnable backAction;
    private static final Pattern POLISH_CAPITAL_PATTERN = Pattern.compile("[A-ZĄĆĘŁŃÓŚŹŻ]");

    public TeamSetupFrame(Stage primaryStage) {
        this.stage = primaryStage;
        this.backAction = null;

        teamSizeComboBox = new ComboBox<>();
        categoryComboBox = new ComboBox<>();
        answerTimeSlider = new Slider(10, 120, 30);
        team1Field = new TextField();
        team2Field = new TextField();
        team1MembersPanel = new VBox(10);
        team2MembersPanel = new VBox(10);

        mainPanel = new VBox(30);
        settingsPanel = new VBox(20);
        scrollPane = new ScrollPane();

        commonSetup();
    }

    public TeamSetupFrame(Stage primaryStage, Runnable onBack) {
        this(primaryStage);
        this.backAction = onBack;
        initializeMainPanelStructure();
    }

    private void commonSetup() {
        this.stage.setTitle("Ustawienia drużyn - Quizpans");
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

        initializeMainPanelStructure();
        initializeSettingsPanelStructure();

        scrollPane.setContent(mainPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        this.stage.setWidth(visualBounds.getWidth());
        this.stage.setHeight(visualBounds.getHeight());
        this.stage.centerOnScreen();

        updateTeamMembers();

        if (this.stage.getScene() == null) {
            this.scene = new Scene(getRootPane());
            loadStylesheets();
            this.stage.setScene(this.scene);
        } else {
            this.scene = this.stage.getScene();
            if (this.scene.getRoot() != getRootPane()) {
                this.scene.setRoot(getRootPane());
            }
            loadStylesheets();
        }

        if (this.scene.getFill() == null || !this.scene.getFill().equals(createBackgroundGradient())) {
            this.scene.setFill(createBackgroundGradient());
        }
    }

    private void loadStylesheets() {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (scene != null && cssPath != null) {
                if (scene.getStylesheets() == null) {
                    scene.getStylesheets().add(cssPath);
                } else if (!scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().add(cssPath);
                }
            }
        } catch (Exception e) {}
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
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {}
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
        // --- USUNIĘTO/ZAKOMENTOWANO LINIĘ PONIŻEJ ---
        // mainPanel.setStyle("-fx-background-color: transparent;");
        // --------------------------------------------
        // Można ewentualnie ustawić tło mainPanel na gradient, jeśli scena go nie zapewnia
        // mainPanel.setBackground(new Background(new BackgroundFill(createBackgroundGradient(), CornerRadii.EMPTY, Insets.EMPTY)));
        mainPanel.setMinHeight(Region.USE_PREF_SIZE);


        Label titleLabel = new Label("USTAW DRUŻYNY");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        HBox teamDisplayArea = new HBox(30);
        teamDisplayArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(teamDisplayArea, Priority.ALWAYS);

        // Listenery dla nazw drużyn są w commonSetup

        VBox team1Panel = createTeamPanel("Drużyna 1", team1Field, team1MembersPanel);
        VBox team2Panel = createTeamPanel("Drużyna 2", team2Field, team2MembersPanel);

        HBox.setHgrow(team1Panel, Priority.ALWAYS);
        HBox.setHgrow(team2Panel, Priority.ALWAYS);

        teamDisplayArea.getChildren().addAll(team1Panel, team2Panel);

        HBox buttonPanel = new HBox(25);
        buttonPanel.setAlignment(Pos.CENTER);

        Button startButton = createStyledButton("Rozpocznij grę", "#4CAF50");
        Button settingsButton = createStyledButton("Ustawienia Gry", "#2196F3");

        buttonPanel.getChildren().clear();

        if (this.backAction != null) {
            Button backButton = createStyledButton("Wróć do Menu", "#f44336");
            backButton.setOnAction(e -> {
                if (backAction != null) {
                    backAction.run();
                }
            });
            buttonPanel.getChildren().add(backButton);
        }

        startButton.setOnAction(e -> startGame());
        settingsButton.setOnAction(e -> switchToSettingsView());

        buttonPanel.getChildren().addAll(startButton, settingsButton);

        VBox.setMargin(buttonPanel, new Insets(20, 0, 10, 0));

        mainPanel.getChildren().setAll(
                titleLabel,
                teamDisplayArea,
                buttonPanel
        );
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
        button.setPrefWidth(200);
        button.setOnMouseEntered(e -> button.setScaleX(1.05));
        button.setOnMouseExited(e -> button.setScaleX(1.0));
        return button;
    }

    private void initializeSettingsPanelStructure() {
        settingsPanel.getChildren().clear();
        settingsPanel.setPadding(new Insets(30));
        settingsPanel.setAlignment(Pos.TOP_CENTER);
        settingsPanel.setFillWidth(true);
        VBox.setVgrow(settingsPanel, Priority.ALWAYS);

        settingsPanel.setBackground(new Background(new BackgroundFill(createBackgroundGradient(), CornerRadii.EMPTY, Insets.EMPTY)));
        settingsPanel.setMinHeight(Region.USE_PREF_SIZE);

        Label titleLabel = new Label("USTAWIENIA GRY");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        if(teamSizeComboBox.getItems().isEmpty()) teamSizeComboBox.getItems().addAll(1, 2, 3, 4, 5, 6);
        if(teamSizeComboBox.getSelectionModel().isEmpty()) teamSizeComboBox.getSelectionModel().selectFirst();

        if(categoryComboBox.getItems().isEmpty()) categoryComboBox.getItems().addAll("Testowa1", "Testowa2");
        if(categoryComboBox.getSelectionModel().isEmpty()) categoryComboBox.getSelectionModel().selectFirst();

        SettingsPanel settingsContent = new SettingsPanel(teamSizeComboBox, categoryComboBox, answerTimeSlider);
        VBox.setVgrow(settingsContent, Priority.ALWAYS);

        Button backToTeamSetupButton = createStyledButton("Wróć do Drużyn", "#f44336");
        backToTeamSetupButton.setOnAction(e -> switchToMainPanel());
        VBox.setMargin(backToTeamSetupButton, new Insets(30, 0, 0, 0));

        settingsPanel.getChildren().setAll(titleLabel, settingsContent, backToTeamSetupButton);
    }

    private void updateTeamMembers() {
        Integer selectedSize = teamSizeComboBox.getValue();
        if (selectedSize == null) selectedSize = 1;
        int teamSize = selectedSize;
        updateTeamPanel(team1MembersPanel, teamSize);
        updateTeamPanel(team2MembersPanel, teamSize);
    }

    private void updateTeamPanel(VBox membersPanelContainer, int size) {
        List<String> currentNames = new ArrayList<>();
        for (javafx.scene.Node node : membersPanelContainer.getChildren()) {
            if (node instanceof VBox) {
                for (javafx.scene.Node child : ((VBox) node).getChildren()) {
                    if (child instanceof TextField) {
                        currentNames.add(((TextField) child).getText());
                        break;
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
            if (i < currentNames.size()) {
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

        if (finalSelectedCategory == null || finalSelectedCategory.trim().isEmpty()) {
            showErrorAlert("Proszę wybrać kategorię pytań.", "Błąd");
            return;
        }

        Scene currentScene = stage.getScene();
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
                    finalSelectedCategory,
                    finalAnswerTime,
                    finalTeam1Name,
                    finalTeam2Name,
                    finalTeam1Members,
                    finalTeam2Members,
                    this.stage
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
        } catch (Exception e) {}
        alert.showAndWait();
    }

    private void switchRootWithFade(Parent newRoot) {
        if (scene == null) {
            System.err.println("Błąd krytyczny: Scena jest null w switchRootWithFade!");
            return;
        }
        Parent currentRoot = scene.getRoot();
        if (currentRoot == null) {
            scene.setRoot(newRoot);
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            scene.setRoot(newRoot);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void switchToSettingsView() {
        if (settingsPanel.getChildren().isEmpty()) {
            initializeSettingsPanelStructure();
        }
        switchRootWithFade(settingsPanel);
        stage.setTitle("Ustawienia Gry - Quizpans");
    }

    private void switchToMainPanel() {
        updateTeamMembers();
        switchRootWithFade(scrollPane);
        stage.setTitle("Ustawienia drużyn - Quizpans");
    }

    public Parent getRootPane() {
        if (this.scrollPane == null) {
            System.err.println("Błąd krytyczny: scrollPane jest null w getRootPane!");
            return new VBox();
        }
        if (this.scrollPane.getContent() == null && this.mainPanel != null) {
            this.scrollPane.setContent(this.mainPanel);
        } else if (this.scrollPane.getContent() == null && this.mainPanel == null) {
            System.err.println("Krytyczny błąd: scrollPane i mainPanel są null w getRootPane!");
            return new VBox();
        }
        return this.scrollPane;
    }
}