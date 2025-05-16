package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.config.DatabaseConfig;
import org.quizpans.services.GameService;
import org.quizpans.utils.BackgroundLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

public class SplashScreen {

    private Stage stage;
    private Scene scene;
    private VBox rootPane;
    private Label statusLabel;
    private Button startButton;
    private Task<Boolean> dbCheckTask;
    private Task<Set<String>> categoryLoadTask;
    private Set<String> loadedCategories = null;

    private StackPane customProgressBar;
    private Rectangle progressFillRect;
    private Text progressText;
    private StackPane bottomContainer;
    private Timeline fakeProgressTimeline;

    private static final double PROGRESS_BAR_WIDTH = 250;
    private static final double PROGRESS_BAR_HEIGHT = 50;
    private static final Color LOADING_GREEN_COLOR = Color.rgb(76, 175, 80);
    private static final Color ERROR_RED_COLOR = Color.INDIANRED;

    private boolean dbCheckCompleted = false;
    private boolean categoriesLoadCompleted = false;
    private boolean modelsLoadCompleted = false;
    private boolean dbCheckFailed = false;
    private boolean categoriesLoadFailed = false;
    private boolean modelsLoadFailed = false;


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
        VBox.setMargin(statusLabel, new Insets(10, 0, 0, 0));

        startButton = new Button("Rozpocznij");
        startButton.getStyleClass().add("main-menu-button");
        startButton.setStyle("-fx-font-size: 18px; -fx-pref-width: " + PROGRESS_BAR_WIDTH + "px; -fx-min-width: " + PROGRESS_BAR_WIDTH + "px; -fx-pref-height:" + PROGRESS_BAR_HEIGHT + "px; -fx-min-height:" + PROGRESS_BAR_HEIGHT + "px; -fx-padding: 10px 20px;");
        startButton.setDisable(true);
        startButton.setOnAction(e -> handleStartButtonClick());
        startButton.setVisible(false);

        createCustomProgressBar();

        bottomContainer = new StackPane(customProgressBar, startButton);
        StackPane.setAlignment(startButton, Pos.CENTER);
        StackPane.setAlignment(customProgressBar, Pos.CENTER);
        VBox.setMargin(bottomContainer, new Insets(0, 0, 0, 0));

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

        startInitialLoad();

        BackgroundLoader.modelsLoadingCompleteProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                modelsLoadCompleted = true;
                modelsLoadFailed = false;
                Platform.runLater(this::updateCombinedProgress);
            }
        });
        BackgroundLoader.modelsLoadingFailedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                modelsLoadFailed = true;
                modelsLoadCompleted = false;
                Platform.runLater(() -> {
                    progressText.setText("Błąd ładowania modeli!");
                    checkAndFinalizeLoading();
                });
            }
        });
    }

    private void createCustomProgressBar() {
        Rectangle progressBackgroundRect = new Rectangle(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        progressBackgroundRect.setArcWidth(30);
        progressBackgroundRect.setArcHeight(30);
        progressBackgroundRect.setFill(Color.rgb(0, 0, 0, 0.3));
        progressBackgroundRect.setStroke(Color.rgb(255,255,255,0.2));
        progressBackgroundRect.setStrokeWidth(1.5);

        progressFillRect = new Rectangle(0, PROGRESS_BAR_HEIGHT);
        progressFillRect.setArcWidth(30);
        progressFillRect.setArcHeight(30);
        progressFillRect.setFill(LOADING_GREEN_COLOR);

        progressText = new Text("Ładowanie...");
        progressText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        progressText.setFill(Color.WHITE);

        customProgressBar = new StackPane(progressBackgroundRect, progressFillRect, progressText);
        customProgressBar.setPrefSize(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        customProgressBar.setMaxSize(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        StackPane.setAlignment(progressFillRect, Pos.CENTER_LEFT);
        customProgressBar.setVisible(false);
    }


    private void startInitialLoad() {
        customProgressBar.setVisible(true);
        startButton.setVisible(false);
        progressFillRect.setWidth(0);
        progressFillRect.setFill(LOADING_GREEN_COLOR);
        progressText.setText("Startowanie...");
        statusLabel.setText(" ");
        statusLabel.setTextFill(Color.LIGHTGRAY);


        checkDatabaseConnection();
    }

    private void setBottomStatus(String message, Color color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
    }


    private void updateCombinedProgress() {
        double totalTasks = 3.0;
        double completedTasks = 0;

        if (dbCheckCompleted && !dbCheckFailed) completedTasks++;
        if (categoriesLoadCompleted && !categoriesLoadFailed) completedTasks++;
        if (modelsLoadCompleted && !modelsLoadFailed) completedTasks++;

        double currentProgress = completedTasks / totalTasks;
        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }

        fakeProgressTimeline = new Timeline();
        fakeProgressTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(500), new KeyValue(progressFillRect.widthProperty(), PROGRESS_BAR_WIDTH * currentProgress))
        );
        fakeProgressTimeline.play();

        String currentProgressMessage = "";
        boolean anErrorOccurred = dbCheckFailed || categoriesLoadFailed || modelsLoadFailed;

        statusLabel.setText(" ");

        if (anErrorOccurred) {
            progressFillRect.setFill(ERROR_RED_COLOR);
            if (progressFillRect.getWidth() < PROGRESS_BAR_WIDTH *0.33){
                progressFillRect.setWidth(PROGRESS_BAR_WIDTH *0.33);
            }
            if (dbCheckFailed && (progressText.getText() == null || !progressText.getText().contains("Baza"))) currentProgressMessage = "Błąd Bazy Danych!";
            else if (categoriesLoadFailed && (progressText.getText() == null || !progressText.getText().contains("Kategorie"))) currentProgressMessage = "Błąd Kategorii!";
            else if (modelsLoadFailed && (progressText.getText() == null || !progressText.getText().contains("Modele"))) currentProgressMessage = "Błąd Modeli!";
            else if (progressText.getText() == null || progressText.getText().isEmpty() || progressText.getText().equals("Startowanie...") || progressText.getText().equals("Ładowanie kategorii...") || progressText.getText().equals("Ładowanie modeli...") || progressText.getText().equals("Baza danych OK.") || progressText.getText().equals("Kategorie załadowane.")) {
                currentProgressMessage = "Błąd ładowania!";
            }


            if(!currentProgressMessage.isEmpty()) progressText.setText(currentProgressMessage);

        } else if (currentProgress < 1.0) {
            progressFillRect.setFill(LOADING_GREEN_COLOR);
            if (dbCheckCompleted && !categoriesLoadCompleted && !categoriesLoadFailed) {
                currentProgressMessage = "Ładowanie kategorii...";
            } else if (dbCheckCompleted && categoriesLoadCompleted && !modelsLoadCompleted && !modelsLoadFailed) {
                currentProgressMessage = "Ładowanie modeli...";
            } else if (!dbCheckCompleted){
                currentProgressMessage = "Sprawdzanie bazy...";
            }
            else {
                currentProgressMessage = "Finalizowanie...";
            }
            progressText.setText(currentProgressMessage);
        } else {
            progressFillRect.setFill(LOADING_GREEN_COLOR);
            progressText.setText("Kończenie...");
        }
        checkAndFinalizeLoading();
    }


    private void checkAndFinalizeLoading() {
        boolean allDone = dbCheckCompleted && categoriesLoadCompleted && modelsLoadCompleted;
        boolean anyFailed = dbCheckFailed || categoriesLoadFailed || modelsLoadFailed;

        if (allDone && !anyFailed) {
            completeLoadingVisuals("Gotowy do startu!");
            startButton.setDisable(false);
        } else if (anyFailed) {
            String finalErrorMsgInProgressBar = progressText.getText();
            if (finalErrorMsgInProgressBar == null || finalErrorMsgInProgressBar.isEmpty() ||
                    !(finalErrorMsgInProgressBar.toLowerCase().contains("błąd") || finalErrorMsgInProgressBar.toLowerCase().contains("error"))) {
                if(dbCheckFailed) finalErrorMsgInProgressBar = "Błąd Bazy Danych!";
                else if(categoriesLoadFailed) finalErrorMsgInProgressBar = "Błąd Kategorii!";
                else if(modelsLoadFailed) finalErrorMsgInProgressBar = "Błąd Modeli AI!";
                else finalErrorMsgInProgressBar = "Nieznany błąd ładowania!";
            }
            errorLoadingVisuals(finalErrorMsgInProgressBar);
            startButton.setDisable(true);
        }
    }


    private void completeLoadingVisuals(String messageInBar) {
        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }
        Timeline completeFill = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(progressFillRect.widthProperty(), PROGRESS_BAR_WIDTH))
        );
        completeFill.setOnFinished(event -> {
            customProgressBar.setVisible(false);
            startButton.setVisible(true);
        });
        completeFill.play();
        progressFillRect.setFill(LOADING_GREEN_COLOR);
        progressText.setText(messageInBar);
        setBottomStatus(" ", Color.LIGHTGRAY);
    }

    private void errorLoadingVisuals(String errorMessageInBar) {
        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }
        customProgressBar.setVisible(true);
        startButton.setVisible(false);

        if (progressFillRect.getWidth() < PROGRESS_BAR_WIDTH *0.1) {
            progressFillRect.setWidth(PROGRESS_BAR_WIDTH *0.33);
        }
        progressFillRect.setFill(ERROR_RED_COLOR);
        progressText.setText(errorMessageInBar);
        setBottomStatus(" ", Color.LIGHTGRAY);
    }


    private void addPulseAnimation(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(1200), label);
        st.setByX(0.05); st.setByY(0.05);
        st.setCycleCount(ScaleTransition.INDEFINITE); st.setAutoReverse(true);
        st.play();
    }

    private void checkDatabaseConnection() {
        progressText.setText("Sprawdzanie bazy...");

        dbCheckTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(200);
                try (Connection conn = DriverManager.getConnection(
                        DatabaseConfig.getUrl(), DatabaseConfig.getUser(), DatabaseConfig.getPassword())) {
                    return conn != null && conn.isValid(2);
                } catch (SQLException e) {
                    return false;
                }
            }
        };

        dbCheckTask.setOnSucceeded(event -> Platform.runLater(() -> {
            dbCheckCompleted = true;
            boolean dbOk = Boolean.TRUE.equals(dbCheckTask.getValue());
            if (dbOk) {
                dbCheckFailed = false;
                progressText.setText("Baza danych OK.");
                loadCategoriesInBackground();
            } else {
                dbCheckFailed = true;
                progressText.setText("Błąd bazy danych!");
                showConnectionErrorAlert();
            }
            updateCombinedProgress();
        }));

        dbCheckTask.setOnFailed(event -> Platform.runLater(() -> {
            dbCheckCompleted = true;
            dbCheckFailed = true;
            progressText.setText("Błąd krytyczny bazy!");
            showConnectionErrorAlert();
            updateCombinedProgress();
        }));

        new Thread(dbCheckTask).start();
    }

    private void loadCategoriesInBackground() {
        progressText.setText("Ładowanie kategorii...");

        categoryLoadTask = new Task<Set<String>>() {
            @Override
            protected Set<String> call() throws Exception {
                Thread.sleep(200);
                try {
                    return GameService.getAvailableCategories();
                } catch (Exception e) {
                    throw e;
                }
            }
        };

        categoryLoadTask.setOnSucceeded(event -> Platform.runLater(() -> {
            categoriesLoadCompleted = true;
            this.loadedCategories = categoryLoadTask.getValue();
            if (this.loadedCategories != null && !this.loadedCategories.isEmpty()) {
                categoriesLoadFailed = false;
                progressText.setText("Kategorie załadowane.");
            } else {
                categoriesLoadFailed = true;
                progressText.setText("Błąd kategorii!");
                showResourceLoadingErrorAlert(new RuntimeException("Nie udało się załadować kategorii lub są puste."));
            }
            updateCombinedProgress();
        }));

        categoryLoadTask.setOnFailed(event -> Platform.runLater(() -> {
            categoriesLoadCompleted = true;
            categoriesLoadFailed = true;
            progressText.setText("Błąd ładowania kategorii!");
            showResourceLoadingErrorAlert(categoryLoadTask.getException());
            updateCombinedProgress();
        }));

        new Thread(categoryLoadTask).start();
    }


    private void handleStartButtonClick() {
        if (dbCheckFailed || categoriesLoadFailed || modelsLoadFailed) {
            showResourceLoadingErrorAlert(new RuntimeException("Nie można uruchomić gry z powodu błędów ładowania."));
            return;
        }
        if (loadedCategories == null || loadedCategories.isEmpty()) {
            showResourceLoadingErrorAlert(new RuntimeException("Kategorie nie zostały poprawnie załadowane."));
            return;
        }
        if (!BackgroundLoader.modelsReadyProperty().get() || BackgroundLoader.modelsLoadingFailedProperty().get()) {
            showResourceLoadingErrorAlert(new RuntimeException("Modele językowe nie są gotowe lub wystąpił błąd ich ładowania."));
            return;
        }
        switchToMainMenu(this.loadedCategories);
    }

    private void showConnectionErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd Bazy Danych");
        alert.setHeaderText("Nie można połączyć się z bazą danych.");
        alert.setContentText("Sprawdź połączenie internetowe lub konfigurację.\nAplikacja nie może kontynuować bez połączenia.");
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if(cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
                dialogPane.getStyleClass().add("custom-alert");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować styles.css dla alertu: " + e.getMessage());
        }
        alert.showAndWait();
    }

    private void showResourceLoadingErrorAlert(Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd Ładowania Zasobów");
        alert.setHeaderText("Wystąpił problem podczas ładowania zasobów aplikacji.");
        alert.setContentText("Aplikacja może nie działać poprawnie.\nSpróbuj ponownie uruchomić aplikację.\nSzczegóły: " + (exception != null ? exception.getMessage() : "Nieznany błąd"));
        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if(cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
                dialogPane.getStyleClass().add("custom-alert");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować styles.css dla alertu: " + e.getMessage());
        }
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