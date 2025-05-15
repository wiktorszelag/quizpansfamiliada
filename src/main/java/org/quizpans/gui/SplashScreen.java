package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
import org.quizpans.services.GameService;
import org.quizpans.utils.BackgroundLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

public class SplashScreen {

    private Stage stage;
    private Scene scene;
    private VBox rootPane;
    private Label statusLabel;
    private Button startButton;
    private Task<Boolean> dbCheckTask;
    private Task<Set<String>> categoryLoadTask;
    private ProgressBar initialLoadProgressBar;
    private StackPane bottomContainer;
    private Set<String> loadedCategories = null;

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
        VBox.setMargin(statusLabel, new Insets(5, 0, 0, 0));

        startButton = new Button("Rozpocznij");
        startButton.getStyleClass().add("main-menu-button");
        startButton.setStyle("-fx-font-size: 18px; -fx-pref-width: 200px; -fx-min-width: 200px; -fx-padding: 10px 20px;");
        startButton.setDisable(true);
        startButton.setOnAction(e -> handleStartButtonClick());

        initialLoadProgressBar = new ProgressBar();
        initialLoadProgressBar.setPrefWidth(250);
        initialLoadProgressBar.setVisible(false);

        bottomContainer = new StackPane(startButton, initialLoadProgressBar);
        StackPane.setAlignment(startButton, Pos.CENTER);
        StackPane.setAlignment(initialLoadProgressBar, Pos.CENTER);
        VBox.setMargin(bottomContainer, new Insets(0, 0, 10, 0));

        Region spacerTop = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);
        Region spacerMiddle = new Region();
        VBox.setVgrow(spacerMiddle, Priority.ALWAYS);

        rootPane.getChildren().clear();
        rootPane.getChildren().addAll(
                spacerTop,
                titleLabel,
                spacerMiddle,
                bottomContainer,
                statusLabel
        );

        scene = new Scene(rootPane);
        loadStylesheets();

        this.stage.setScene(scene);
        this.stage.setMaximized(true);
        this.stage.setResizable(true);

        checkDatabaseConnectionAndLoadCategories();
    }

    private void addPulseAnimation(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(1200), label);
        st.setByX(0.05); st.setByY(0.05);
        st.setCycleCount(ScaleTransition.INDEFINITE); st.setAutoReverse(true);
        st.play();
    }

    private void checkDatabaseConnectionAndLoadCategories() {
        statusLabel.setVisible(true);
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setText("Sprawdzanie połączenia z bazą danych...");
        initialLoadProgressBar.setVisible(false);
        startButton.setVisible(true);
        startButton.setDisable(true);


        dbCheckTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("Sprawdzanie połączenia z bazą danych...");
                Thread.sleep(200);
                try (Connection conn = DriverManager.getConnection(
                        DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword())) {
                    return conn != null && conn.isValid(2);
                } catch (SQLException e) {
                    System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
                    updateMessage("Błąd połączenia z bazą danych!");
                    return false;
                }
            }
        };

        // Celowo nie bindowane do statusLabel, aby nie nadpisywać statusu ładowania kategorii
        // statusLabel.textProperty().bind(dbCheckTask.messageProperty());

        dbCheckTask.setOnSucceeded(event -> Platform.runLater(() -> {
            boolean dbOk = Boolean.TRUE.equals(dbCheckTask.getValue());
            // if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind(); // Niepotrzebne jeśli nie bindowane

            if (dbOk) {
                statusLabel.setText("Połączono z bazą. Ładowanie kategorii...");
                statusLabel.setTextFill(Color.LIGHTYELLOW);
                startButton.setVisible(false);
                initialLoadProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                initialLoadProgressBar.setVisible(true);
                loadCategoriesInBackground();
            } else {
                updateUIBasedOnDBConnection(false);
                startButton.setDisable(true);
                if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind(); // Na wszelki wypadek
                statusLabel.setText("Błąd połączenia z bazą danych!");
            }
        }));

        dbCheckTask.setOnFailed(event -> Platform.runLater(() -> {
            if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();
            updateUIBasedOnDBConnection(false);
            startButton.setDisable(true);
            statusLabel.setText("Błąd połączenia z bazą danych!");
        }));

        new Thread(dbCheckTask).start();
    }

    private void loadCategoriesInBackground() {
        categoryLoadTask = new Task<Set<String>>() {
            @Override
            protected Set<String> call() throws Exception {
                updateMessage("Pobieranie kategorii pytań...");
                Thread.sleep(200);
                try {
                    return GameService.getAvailableCategories();
                } catch (Exception e) {
                    System.err.println("Błąd podczas pobierania kategorii: " + e.getMessage());
                    updateMessage("Błąd ładowania kategorii!");
                    throw e;
                }
            }
        };

        statusLabel.textProperty().bind(categoryLoadTask.messageProperty());

        categoryLoadTask.setOnSucceeded(event -> Platform.runLater(() -> {
            this.loadedCategories = categoryLoadTask.getValue();
            initialLoadProgressBar.setVisible(false);
            startButton.setVisible(true);
            if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();

            if (this.loadedCategories != null && !this.loadedCategories.isEmpty()) {
                statusLabel.setText("Kategorie załadowane. Gotowy do startu!");
                statusLabel.setTextFill(Color.LIMEGREEN);
                startButton.setDisable(false);
            } else {
                statusLabel.setText("Nie udało się załadować kategorii lub są puste.");
                statusLabel.setTextFill(Color.ORANGERED);
                startButton.setDisable(true);
                showResourceLoadingErrorAlert(new RuntimeException("Brak kategorii lub błąd ładowania."));
            }
        }));

        categoryLoadTask.setOnFailed(event -> Platform.runLater(() -> {
            initialLoadProgressBar.setVisible(false);
            startButton.setVisible(true);
            startButton.setDisable(true);
            if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();
            statusLabel.setText("Krytyczny błąd ładowania kategorii!");
            statusLabel.setTextFill(Color.RED);
            showResourceLoadingErrorAlert(categoryLoadTask.getException());
        }));

        new Thread(categoryLoadTask).start();
    }


    private void handleStartButtonClick() {
        if (loadedCategories == null || loadedCategories.isEmpty()) {
            showResourceLoadingErrorAlert(new RuntimeException("Kategorie nie zostały poprawnie załadowane."));
            return;
        }
        BackgroundLoader.ensureModelLoadingInitiated();
        switchToMainMenu(this.loadedCategories);
    }


    private void updateUIBasedOnDBConnection(boolean success) {
        statusLabel.setTextFill(success ? Color.LIMEGREEN : Color.RED);
        if (!success) {
            showConnectionErrorAlert();
        }
    }

    private void showConnectionErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd Bazy Danych");
        alert.setHeaderText("Nie można połączyć się z bazą danych.");
        alert.setContentText("Sprawdź połączenie internetowe lub konfigurację.\nAplikacja nie może kontynuować bez połączenia.");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        alert.showAndWait();
    }

    private void showResourceLoadingErrorAlert(Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd Ładowania Zasobów");
        alert.setHeaderText("Wystąpił problem podczas ładowania zasobów aplikacji.");
        alert.setContentText("Aplikacja może nie działać poprawnie.\nSpróbuj ponownie uruchomić aplikację.\nSzczegóły: " + (exception != null ? exception.getMessage() : "Nieznany błąd"));
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        alert.showAndWait();
    }


    private void switchToMainMenu(Set<String> categoriesToPass) {
        Parent currentRoot = scene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            MainMenuFrame mainMenuFrame = new MainMenuFrame(this.stage, categoriesToPass);
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
                if (scene.getStylesheets() == null || scene.getStylesheets().isEmpty() ) {
                    scene.getStylesheets().add(cssPath);
                } else if (!scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().add(cssPath);
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania CSS w SplashScreen: " + e.getMessage());
        }
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