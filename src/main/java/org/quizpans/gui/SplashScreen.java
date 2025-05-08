package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SplashScreen {

    private Stage stage;
    private Scene scene;
    private VBox rootPane;
    private Label statusLabel;
    private Button startButton;
    private Timeline dotsAnimation;

    public SplashScreen(Stage primaryStage) {
        this.stage = primaryStage;
        initializeSplashScreen();
    }

    private void initializeSplashScreen() {
        rootPane = new VBox(10);
        rootPane.setAlignment(Pos.CENTER);
        rootPane.setPadding(new Insets(30, 50, 50, 50));
        VBox.setVgrow(rootPane, Priority.ALWAYS);

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        rootPane.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("QuizPans");
        titleLabel.getStyleClass().add("splash-title");
        addPulseAnimation(titleLabel);

        statusLabel = new Label(" ");
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.getStyleClass().add("splash-status-label");

        startButton = new Button("Rozpocznij");
        startButton.getStyleClass().add("main-menu-button");
        startButton.setStyle("-fx-font-size: 18px; -fx-pref-width: 200px; -fx-min-width: 200px; -fx-padding: 10px 20px;");
        startButton.setDisable(true);
        startButton.setOnAction(e -> switchToMainMenu());

        Region spacerTop = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);

        Region spacerMiddle = new Region();
        VBox.setVgrow(spacerMiddle, Priority.ALWAYS);

        rootPane.getChildren().clear();
        rootPane.getChildren().addAll(
                spacerTop,
                titleLabel,
                spacerMiddle,
                startButton,
                statusLabel
        );

        VBox.setMargin(startButton, new Insets(0, 0, 10, 0));
        VBox.setMargin(statusLabel, new Insets(0, 0, 0, 0));

        dotsAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> statusLabel.setText("Łączenie z bazą danych")),
                new KeyFrame(Duration.millis(500), e -> statusLabel.setText("Łączenie z bazą danych.")),
                new KeyFrame(Duration.millis(1000), e -> statusLabel.setText("Łączenie z bazą danych..")),
                new KeyFrame(Duration.millis(1500), e -> statusLabel.setText("Łączenie z bazą danych..."))
        );
        dotsAnimation.setCycleCount(Timeline.INDEFINITE);

        scene = new Scene(rootPane);
        loadStylesheets();

        this.stage.setScene(scene);
        this.stage.setMaximized(true);
        this.stage.setResizable(true);

        checkDatabaseConnection();
    }

    private void addPulseAnimation(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(1200), label);
        st.setByX(0.05);
        st.setByY(0.05);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        st.setAutoReverse(true);
        st.play();
    }

    private void checkDatabaseConnection() {
        dotsAnimation.play();
        statusLabel.setVisible(true);
        statusLabel.setTextFill(Color.LIGHTGRAY);

        Task<Boolean> dbTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = DriverManager.getConnection(
                        DatabaseConfig.getUrl(),
                        DatabaseConfig.getUser(),
                        DatabaseConfig.getPassword())) {
                    Thread.sleep(1500);
                    return conn != null;
                } catch (SQLException | InterruptedException e) {
                    return false;
                }
            }
        };

        dbTask.setOnSucceeded(event -> {
            boolean success = dbTask.getValue();
            Platform.runLater(() -> {
                dotsAnimation.stop();
                updateUIBasedOnConnection(success);
            });
        });

        dbTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                dotsAnimation.stop();
                updateUIBasedOnConnection(false);
            });
        });

        new Thread(dbTask).start();
    }

    private void updateUIBasedOnConnection(boolean success) {
        if (success) {
            statusLabel.setText("Połączono z bazą danych");
            statusLabel.setTextFill(Color.LIMEGREEN);
            startButton.setDisable(false);
        } else {
            statusLabel.setText("Błąd połączenia z bazą!");
            statusLabel.setTextFill(Color.RED);
            startButton.setDisable(true);
            showConnectionErrorAlert();
        }
    }

    private void showConnectionErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd Bazy Danych");
        alert.setHeaderText("Nie można połączyć się z bazą danych.");
        alert.setContentText("Sprawdź połączenie internetowe lub konfigurację.\nAplikacja nie może kontynuować bez połączenia.");
        try {
            DialogPane dialogPane = alert.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
            }
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {}
        alert.showAndWait();
    }

    private void switchToMainMenu() {
        Parent currentRoot = scene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            MainMenuFrame mainMenuFrame = new MainMenuFrame(this.stage);
            Parent mainMenuRoot = mainMenuFrame.getRootPane();

            scene.setRoot(mainMenuRoot);
            mainMenuFrame.show();


            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), mainMenuRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
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

    public void show() {
        this.stage.show();
        if (this.rootPane != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), this.rootPane);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}