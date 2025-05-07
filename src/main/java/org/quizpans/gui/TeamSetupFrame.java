package org.quizpans.gui;

import javafx.animation.FadeTransition;
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
// import java.util.Collections; // Nieużywane, można usunąć jeśli nie planujesz używać
import java.util.List;

public class TeamSetupFrame {
    private Stage stage;
    private VBox mainPanel;
    private VBox settingsPanel;
    private final ComboBox<Integer> teamSizeComboBox;
    private final ComboBox<String> categoryComboBox;
    private final Spinner<Integer> answerTimeSpinner;
    private final VBox team1MembersPanel;
    private final VBox team2MembersPanel;
    private final TextField team1Field;
    private final TextField team2Field;
    private ScrollPane scrollPane;
    private Scene scene;


    public TeamSetupFrame(Stage primaryStage) {
        this.stage = primaryStage;
        this.stage.setTitle("Ustawienia drużyn - Familiada");
        this.stage.setResizable(true);
        this.stage.setMinWidth(850);

        setApplicationIcon();

        teamSizeComboBox = new ComboBox<>();
        categoryComboBox = new ComboBox<>();
        answerTimeSpinner = new Spinner<>(10, 120, 30, 5);
        team1Field = new TextField();
        team2Field = new TextField();
        team1MembersPanel = new VBox(10);
        team2MembersPanel = new VBox(10);

        mainPanel = new VBox(30);
        settingsPanel = new VBox(20);

        initializeMainPanelStructure();
        initializeSettingsPanelStructure();

        scrollPane = new ScrollPane();
        scrollPane.setContent(mainPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        if (this.stage.getScene() == null) {
            scene = new Scene(scrollPane);
            this.stage.setScene(scene);
        } else {
            scene = this.stage.getScene();
            scene.setRoot(scrollPane);
        }
        scene.setFill(gradient);

        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                if (!scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().add(cssPath);
                }
            } else {
                // System.err.println("Nie znaleziono pliku /styles.css");
            }
        } catch (Exception e) {
            // System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        this.stage.setWidth(visualBounds.getWidth());
        this.stage.setHeight(visualBounds.getHeight());
        this.stage.centerOnScreen();

        updateTeamMembers();
    }

    private void setApplicationIcon() {
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream == null) {
                // System.err.println("TeamSetupFrame: Nie można znaleźć pliku ikony: /logo.png");
                return;
            }
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                // System.err.println("TeamSetupFrame: Błąd podczas ładowania obrazu ikony.");
                if (appIcon.getException() != null) {
                    // appIcon.getException().printStackTrace();
                }
                logoStream.close();
                return;
            }
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {
            // System.err.println("TeamSetupFrame: Wyjątek podczas ustawiania ikony aplikacji: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void initializeMainPanelStructure() {
        mainPanel.setPadding(new Insets(30));
        mainPanel.setAlignment(Pos.TOP_CENTER);
        mainPanel.setStyle("-fx-background-color: transparent;");
        mainPanel.setMinHeight(Region.USE_PREF_SIZE);

        Label titleLabel = new Label("FAMILIADA");
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
        Button startButton = createStyledButton("Rozpocznij grę", "#4CAF50");
        Button settingsButton = createStyledButton("Ustawienia", "#2196F3");
        buttonPanel.getChildren().addAll(startButton, settingsButton);
        VBox.setMargin(buttonPanel, new Insets(20, 0, 10, 0));

        mainPanel.getChildren().setAll(
                titleLabel,
                teamDisplayArea,
                buttonPanel
        );

        startButton.setOnAction(e -> startGame());
        settingsButton.setOnAction(e -> showSettingsPanelTransition());
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
        teamNameField.setPrefHeight(45);
        teamNameField.setMaxWidth(Double.MAX_VALUE);
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
        button.setPrefWidth(180);

        button.setOnMouseEntered(e -> button.setScaleX(1.05));
        button.setOnMouseExited(e -> button.setScaleX(1.0));

        return button;
    }

    private void initializeSettingsPanelStructure() {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4b6cb7")),
                new Stop(1, Color.web("#182848"))
        );

        settingsPanel.setPadding(new Insets(30));
        settingsPanel.setAlignment(Pos.TOP_CENTER);
        settingsPanel.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        settingsPanel.setMinHeight(Region.USE_PREF_SIZE);

        Label titleLabel = new Label("USTAWIENIA");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        teamSizeComboBox.getItems().addAll(1, 2, 3, 4, 5, 6);
        teamSizeComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getItems().addAll("Testowa1", "Testowa2"); // Można dynamicznie ładować kategorie
        categoryComboBox.getSelectionModel().selectFirst();

        SettingsPanel settingsContent = new SettingsPanel(teamSizeComboBox, categoryComboBox, answerTimeSpinner);
        VBox.setVgrow(settingsContent, Priority.ALWAYS);

        Button backButton = createStyledButton("Wróć", "#f44336");
        backButton.setOnAction(e -> {
            updateTeamMembers();
            showMainPanelTransition();
        });
        VBox.setMargin(backButton, new Insets(30, 0, 0, 0));

        settingsPanel.getChildren().setAll(titleLabel, settingsContent, backButton);
    }

    private void updateTeamMembers() {
        int teamSize = teamSizeComboBox.getValue();
        updateTeamPanel(team1MembersPanel, teamSize);
        updateTeamPanel(team2MembersPanel, teamSize);
    }

    private void updateTeamPanel(VBox membersPanelContainer, int size) {
        membersPanelContainer.getChildren().clear();
        for (int i = 0; i < size; i++) {
            VBox playerCard = new VBox(8);
            playerCard.setPadding(new Insets(15));
            playerCard.getStyleClass().add("player-card");
            playerCard.setAlignment(Pos.CENTER_LEFT);

            Label playerNumberLabel = new Label("Gracz " + (i + 1));
            playerNumberLabel.getStyleClass().add("player-number-label");

            TextField playerNameField = new TextField();
            playerNameField.setPromptText("Imię gracza " + (i + 1));
            playerNameField.getStyleClass().add("player-name-field");
            VBox.setVgrow(playerNameField, Priority.ALWAYS);

            playerCard.getChildren().addAll(playerNumberLabel, playerNameField);
            membersPanelContainer.getChildren().add(playerCard);
        }
    }

    private void startGame() {
        // System.out.println("TeamSetupFrame: Rozpoczynanie gry...");
        String t1Name = team1Field.getText().trim();
        String t2Name = team2Field.getText().trim();

        if (t1Name.isEmpty()) t1Name = "Drużyna 1";
        if (t2Name.isEmpty()) t2Name = "Drużyna 2";

        final String finalTeam1Name = t1Name;
        final String finalTeam2Name = t2Name;
        final List<String> finalTeam1Members = getMembers(team1MembersPanel, 1);
        final List<String> finalTeam2Members = getMembers(team2MembersPanel, 2);
        final int finalAnswerTime = answerTimeSpinner.getValue();
        final String finalSelectedCategory = categoryComboBox.getValue();

        if (finalTeam1Members.isEmpty() || finalTeam2Members.isEmpty()) {
            showErrorAlert("Obie drużyny muszą mieć co najmniej jednego gracza.", "Błąd");
            return;
        }

        if (finalSelectedCategory == null || finalSelectedCategory.trim().isEmpty()) {
            showErrorAlert("Proszę wybrać kategorię pytań.", "Błąd");
            return;
        }

        // System.out.println("TeamSetupFrame: Kategoria wybrana: " + finalSelectedCategory);
        Parent currentRoot = stage.getScene().getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            // System.out.println("TeamSetupFrame: FadeOut zakończony. Tworzenie i inicjalizacja TeamSelectionFrame.");
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
                // System.err.println("TeamSetupFrame: BŁĄD KRYTYCZNY - teamSelectionRoot jest null po initializeFrameContent!");
                showErrorAlert("Błąd inicjalizacji następnego ekranu (root jest null).", "Błąd krytyczny");
                return;
            }

            stage.getScene().setRoot(teamSelectionRoot);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), teamSelectionRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            // System.out.println("TeamSetupFrame: TeamSelectionFrame pokazany.");
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
        if (members.isEmpty() && teamSizeComboBox.getValue() > 0) {
            // Domyślny gracz jeśli lista jest pusta, a rozmiar drużyny > 0
            // To zachowanie może wymagać przemyślenia - czy na pewno chcemy dodawać domyślnego gracza
            // jeśli użytkownik nie wpisał żadnego, a wybrał rozmiar drużyny?
            // members.add("Gracz 1 (D" + teamNumber + ")");
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
            } else {
                // System.err.println("Nie znaleziono pliku stylów dla alertu: /styles.css");
            }
        } catch (Exception e) {
            // System.err.println("Nie można załadować stylów alertu: " + e.getMessage());
        }
        alert.showAndWait();
    }

    private void showMainPanelTransition() {
        Parent currentRoot = stage.getScene().getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            stage.getScene().setRoot(getRootPane()); // getRootPane() zwróci scrollPane z mainPanel
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), getRootPane());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showSettingsPanelTransition() {
        Parent currentRoot = stage.getScene().getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            if (settingsPanel.getChildren().isEmpty() && settingsPanel.getBackground() == null) {
                initializeSettingsPanelStructure(); // Upewnij się, że jest zainicjalizowany
            }
            stage.getScene().setRoot(settingsPanel);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), settingsPanel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    public void show() {
        this.stage.show();
        if (this.stage.getScene() != null && this.stage.getScene().getRoot() != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), this.stage.getScene().getRoot());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else {
            // System.err.println("TeamSetupFrame: Scena lub root jest null podczas show().");
        }
    }

    public Parent getRootPane() {
        if (this.scrollPane == null) {
            // System.err.println("TeamSetupFrame: KRYTYCZNY BŁĄD - scrollPane jest null w getRootPane(). Inicjalizuję awaryjnie.");
            // Awaryjna inicjalizacja, chociaż to nie powinno się zdarzyć jeśli konstruktor działa poprawnie
            mainPanel = new VBox(30);
            initializeMainPanelStructure();
            scrollPane = new ScrollPane(mainPanel);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            // Dodaj inne potrzebne konfiguracje scrollPane
        }
        if (this.scrollPane.getContent() == null && this.mainPanel != null) {
            this.scrollPane.setContent(this.mainPanel);
        } else if (this.scrollPane.getContent() == null && this.mainPanel == null) {
            // System.err.println("TeamSetupFrame: KRYTYCZNY BŁĄD - mainPanel i scrollPane.content są null w getRootPane(). Zwracam pusty VBox.");
            return new VBox(); // Zabezpieczenie
        }
        return this.scrollPane;
    }
}