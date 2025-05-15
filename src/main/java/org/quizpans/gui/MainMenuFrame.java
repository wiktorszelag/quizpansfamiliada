package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;
// Usunięto BackgroundLoader
import java.io.InputStream;
import java.util.Set;

public class MainMenuFrame {

    private Stage stage;
    private VBox rootPane;
    private Set<String> availableCategories;

    public MainMenuFrame(Stage primaryStage, Set<String> availableCategories) {
        this.stage = primaryStage;
        this.availableCategories = availableCategories;
        this.stage.setTitle("Quizpans - Menu Główne");
        setApplicationIcon();
        initializeMainMenu();
    }

    public MainMenuFrame(Stage primaryStage) {
        this(primaryStage, null);
    }


    private void setApplicationIcon() {
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream == null) return;
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                logoStream.close();
                return;
            }
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {
            System.err.println("Błąd ładowania ikony: " + e.getMessage());
        }
    }

    private void initializeMainMenu() {
        rootPane = new VBox(30);
        rootPane.setAlignment(Pos.CENTER);
        rootPane.setPadding(new Insets(50));
        rootPane.setFillWidth(true);
        VBox.setVgrow(rootPane, Priority.ALWAYS);

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        rootPane.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("Quizpans");
        titleLabel.getStyleClass().add("main-menu-title");

        Button localGameButton = createMenuButton("Rozgrywka Lokalna");
        localGameButton.setOnAction(e -> switchToTeamSetupView());

        Button onlineGameButton = createMenuButton("Starcie Online");
        onlineGameButton.setOnAction(e -> showFeatureNotAvailableAlert("Tryb Online"));
        onlineGameButton.setDisable(true);

        Button gameSettingsButton = createMenuButton("Ustawienia");
        gameSettingsButton.setOnAction(e -> switchToGameSettingsView());

        Button aboutButton = createMenuButton("O Quizpans");
        aboutButton.setOnAction(e -> switchToAboutView());

        Button exitButton = createMenuButton("Zakończ Grę");
        exitButton.setOnAction(e -> Platform.exit());

        VBox buttonBox = new VBox(20, localGameButton, onlineGameButton, gameSettingsButton, aboutButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(400);

        rootPane.getChildren().addAll(titleLabel, buttonBox);

        this.stage.setMaximized(true);
        this.stage.setResizable(true);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("main-menu-button");
        return button;
    }

    private void switchToView(Parent newRoot) {
        Scene currentScene = this.stage.getScene();
        if (currentScene == null) {
            if (stage != null && newRoot != null) {
                Scene tempScene = new Scene(newRoot);
                try {
                    String cssPath = getClass().getResource("/styles.css").toExternalForm();
                    if (cssPath != null) tempScene.getStylesheets().add(cssPath);
                } catch (Exception e) {
                    System.err.println("Błąd ładowania CSS dla nowej sceny: " + e.getMessage());
                }
                stage.setScene(tempScene);
                if (newRoot.getOpacity() == 0) {
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                }
            }
            return;
        }

        Parent currentRootNode = currentScene.getRoot();
        if (currentRootNode == null || currentRootNode == newRoot) {
            currentScene.setRoot(newRoot);
            if (currentRootNode == null && newRoot != null) {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRootNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            currentScene.setRoot(newRoot);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }


    private void switchToTeamSetupView() {
        TeamSetupFrame teamSetupFrame = new TeamSetupFrame(this.stage, this.availableCategories, this::switchToMainMenuView);
        Parent teamSetupRoot = teamSetupFrame.getRootPane();
        switchToView(teamSetupRoot);
    }

    private void switchToGameSettingsView() {
        SettingsGameFrame settingsGameFrame = new SettingsGameFrame(this::switchToMainMenuView, this.stage);
        Parent settingsGameRoot = settingsGameFrame.getView();
        switchToView(settingsGameRoot);
        this.stage.setMaximized(true);
        this.stage.setResizable(true);
    }


    private void switchToAboutView() {
        AboutFrame aboutFrame = new AboutFrame(this::switchToMainMenuView);
        Parent aboutPane = aboutFrame.getView();
        switchToView(aboutPane);
        this.stage.setMaximized(true);
        this.stage.setResizable(false);
    }

    private void switchToMainMenuView() {
        switchToView(this.rootPane);
        this.stage.setMaximized(true);
        this.stage.setResizable(true);
    }

    private void showFeatureNotAvailableAlert(String featureName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Funkcja Niedostępna");
        alert.setHeaderText(featureName + " - Już wkrótce!");
        alert.setContentText("Pracujemy nad dodaniem tej funkcji. Sprawdzaj aktualizacje!");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        alert.showAndWait();
    }

    public void show() {
        this.stage.show();
    }

    public VBox getRootPane() {
        return this.rootPane;
    }
}