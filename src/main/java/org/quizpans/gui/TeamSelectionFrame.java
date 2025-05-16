package org.quizpans.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.services.GameService;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class TeamSelectionFrame {
    private Stage stage;
    private final String selectedCategory;
    private final int answerTime;
    private final String team1Name;
    private final String team2Name;
    private final List<String> team1Members;
    private final List<String> team2Members;
    private GameService gameService;
    private boolean team1Starts;
    private volatile boolean loadingComplete = false;
    private volatile boolean animationComplete = false;

    private Button startGameButton;
    private BorderPane mainPane;
    private boolean uiInitialized = false;

    private StackPane customProgressBar;
    private Rectangle progressFillRect;
    private Text progressText;
    private VBox bottomStatusBox;
    private Timeline fakeProgressTimeline;
    private Task<GameService> gameLoadTask;


    private Label startingLabel;
    private Label teamDisplayLabel;
    private StackPane teamDisplayContainer;

    private static final double PROGRESS_BAR_WIDTH = 250; // Szerokość zbliżona do przycisku
    private static final double PROGRESS_BAR_HEIGHT = 50; // Wysokość zbliżona do przycisku


    public TeamSelectionFrame(String selectedCategory, int answerTime,
                              String team1Name, String team2Name,
                              List<String> team1Members, List<String> team2Members,
                              Stage stage, Task<GameService> backgroundTaskIgnored, GameService preloadedServiceIgnored) {
        this.selectedCategory = selectedCategory;
        this.answerTime = answerTime;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = team1Members;
        this.team2Members = team2Members;
        this.stage = stage;
    }

    public void initializeFrameContent() {
        if (!uiInitialized) {
            initUI();
            uiInitialized = true;
        }
        setFrameProperties();
        startSelectionAnimation();
        initializeGameServiceStandardWithTask();
        applyStylesheetsToMainPane();
    }

    private void applyStylesheetsToMainPane() {
        if (mainPane == null) return;
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && mainPane.getStylesheets().isEmpty()) {
                if (stage.getScene() != null && !stage.getScene().getStylesheets().isEmpty()) {
                    mainPane.getStylesheets().addAll(stage.getScene().getStylesheets());
                } else {
                    mainPane.getStylesheets().add(cssPath);
                }
            } else if (cssPath != null && !mainPane.getStylesheets().contains(cssPath)) {
                mainPane.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS in TeamSelectionFrame: " + e.getMessage());
        }
    }

    private void startLoadingVisuals() {
        if (customProgressBar == null || progressFillRect == null || progressText == null) return;

        progressFillRect.setWidth(0);
        progressText.setText("Ładowanie...");
        customProgressBar.setVisible(true);

        final Duration expectedLoadTime = Duration.seconds(5); // Przybliżony oczekiwany czas ładowania

        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }

        fakeProgressTimeline = new Timeline();
        fakeProgressTimeline.getKeyFrames().add(
                new KeyFrame(expectedLoadTime, new KeyValue(progressFillRect.widthProperty(), PROGRESS_BAR_WIDTH * 0.9)) // Do 90%
        );
        fakeProgressTimeline.play();

        if (gameLoadTask != null) {
            gameLoadTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
                if (newMsg != null && !newMsg.isEmpty()) {
                    Platform.runLater(() -> progressText.setText(newMsg));
                }
            });
        }
    }

    private void completeLoadingVisuals() {
        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }
        if (progressFillRect != null) {
            // Animacja do pełnego wypełnienia
            Timeline completeFill = new Timeline(
                    new KeyFrame(Duration.millis(300), new KeyValue(progressFillRect.widthProperty(), PROGRESS_BAR_WIDTH))
            );
            completeFill.setOnFinished(event -> {
                if (customProgressBar != null) customProgressBar.setVisible(false);
                mainPane.setBottom(startGameButton); // Pokaż przycisk start
            });
            completeFill.play();
        }
        if (progressText != null) {
            progressText.setText("Gotowe!");
        }
    }

    private void errorLoadingVisuals(String errorMessage) {
        if (fakeProgressTimeline != null) {
            fakeProgressTimeline.stop();
        }
        if (customProgressBar != null) {
            customProgressBar.setVisible(true); // Upewnij się, że jest widoczny, by pokazać błąd
        }
        if (progressFillRect != null) {
            progressFillRect.setWidth(PROGRESS_BAR_WIDTH); // Pokaż pełny, ale np. na czerwono
            progressFillRect.setFill(Color.INDIANRED);
        }
        if (progressText != null) {
            progressText.setText("Błąd: " + errorMessage.substring(0, Math.min(errorMessage.length(), 25))+"...");
            progressText.setFill(Color.WHITE);
        }
    }


    private void bindTaskToUI() {
        if (customProgressBar != null && bottomStatusBox != null) {
            mainPane.setBottom(bottomStatusBox); // bottomStatusBox zawiera teraz customProgressBar
            bottomStatusBox.setVisible(true);
            customProgressBar.setVisible(true);
            startLoadingVisuals();
        }
    }

    private void unbindTaskFromUIAndPrepareForButton() {
        if (loadingComplete) {
            completeLoadingVisuals();
        } else {
            // Jeśli ładowanie nie zostało ukończone (np. błąd), nie ukrywaj paska od razu
            // errorLoadingVisuals() powinno być wywołane wcześniej
        }
    }

    private synchronized void checkIfReadyToEnableStartButton() {
        if (loadingComplete && animationComplete && startGameButton != null) {
            if (gameService != null && gameService.getCurrentQuestion() != null) {
                startGameButton.setDisable(false);
            } else {
                startGameButton.setDisable(true);
                if (mainPane.getBottom() != bottomStatusBox || (customProgressBar != null && !customProgressBar.isVisible())) {
                    showLoadingError("Dane gry nie zostały poprawnie załadowane, przycisk start nieaktywny.");
                }
            }
        }
    }

    private void updateUIOnLoadComplete() {
        unbindTaskFromUIAndPrepareForButton();

        if (!loadingComplete || gameService == null || gameService.getCurrentQuestion() == null) {
            // showLoadingError już obsłuży wizualizację błędu
            if (! (mainPane.getBottom() == bottomStatusBox && customProgressBar != null && customProgressBar.isVisible()) ) {
                //  showLoadingError("Dane gry nie załadowane poprawnie."); // Unikaj podwójnego komunikatu
            }
            if (startGameButton != null) {
                startGameButton.setDisable(true);
            }
            return;
        }

        if (startGameButton != null) {
            mainPane.setBottom(startGameButton); // To zostanie zrobione w completeLoadingVisuals
            startGameButton.setVisible(true);
            checkIfReadyToEnableStartButton();
        }
    }

    private void showLoadingError(String message) {
        errorLoadingVisuals(message);
        if (bottomStatusBox != null && mainPane.getBottom() != bottomStatusBox) {
            mainPane.setBottom(bottomStatusBox);
            bottomStatusBox.setVisible(true);
        }

        if (startGameButton != null) {
            startGameButton.setVisible(true);
            startGameButton.setDisable(true);
        }

        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd ładowania");
        alert.setHeaderText("Nie można załadować danych gry!");
        alert.setContentText(message + "\nSpróbuj wrócić do menu i zacząć ponownie.");
        try {
            DialogPane dialogPane = alert.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
                dialogPane.getStyleClass().add("custom-alert");
            }
        } catch (Exception ex) {
            System.err.println("Failed to style error alert: " + ex.getMessage());
        }
        // alert.showAndWait(); // Może być zbyt inwazyjne, jeśli błąd jest już pokazany na pasku
    }

    private void initializeGameServiceStandardWithTask() {
        gameLoadTask = new Task<GameService>() {
            @Override
            protected GameService call() throws Exception {
                updateMessage("Inicjalizacja zasobów");
                Platform.runLater(() -> progressText.setText("Inicjalizacja zasobów..."));
                Thread.sleep(500); // Symulacja pracy

                GameService service = null;
                try {
                    service = new GameService(selectedCategory);
                } catch (Exception e) {
                    updateMessage("Błąd inicjalizacji");
                    Platform.runLater(() -> progressText.setText("Błąd inicjalizacji GameService"));
                    throw e;
                }

                updateMessage("Wczytywanie pytania");
                Platform.runLater(() -> progressText.setText("Wczytywanie pytania..."));
                Thread.sleep(1000); // Symulacja pracy

                if (service.getCurrentQuestion() == null) {
                    throw new RuntimeException("Nie udało się załadować pytania dla kategorii: " + selectedCategory);
                }

                updateMessage("Finalizowanie");
                Platform.runLater(() -> progressText.setText("Finalizowanie..."));
                Thread.sleep(500); // Symulacja pracy
                return service;
            }
        };

        bindTaskToUI();

        gameLoadTask.setOnSucceeded(workerStateEvent -> {
            this.gameService = gameLoadTask.getValue();
            this.loadingComplete = (this.gameService != null && this.gameService.getCurrentQuestion() != null);
            Platform.runLater(this::updateUIOnLoadComplete);
        });

        gameLoadTask.setOnFailed(workerStateEvent -> {
            Throwable exception = gameLoadTask.getException();
            if (exception != null) exception.printStackTrace();
            this.loadingComplete = false;
            Platform.runLater(() -> {
                showLoadingError("Błąd ładowania gry" + (exception != null ? "" : ""));
                updateUIOnLoadComplete(); // Aby ukryć/zmienić pasek postępu
            });
        });

        Thread thread = new Thread(gameLoadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setApplicationIcon() {
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream == null) {
                return;
            }
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                if (appIcon.getException() != null) {
                    System.err.println("Error loading app icon: " + appIcon.getException().getMessage());
                }
                logoStream.close();
                return;
            }
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {
            System.err.println("Exception loading app icon: " + e.getMessage());
        }
    }

    private void setFrameProperties() {
        stage.setTitle("QuizPans - Losowanie Drużyny");
        setApplicationIcon();
    }

    public Parent getRootPane() {
        if (mainPane == null) {
            initUI();
        }
        return mainPane;
    }

    private void initUI() {
        if (uiInitialized) {
            return;
        }

        mainPane = new BorderPane();
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(30));

        Label titleUiLabel = new Label("LOSOWANIE DRUŻYNY");
        titleUiLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleUiLabel.setTextFill(Color.WHITE);
        titleUiLabel.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.7)));
        BorderPane.setAlignment(titleUiLabel, Pos.CENTER);
        BorderPane.setMargin(titleUiLabel, new Insets(10, 0, 20, 0));
        mainPane.setTop(titleUiLabel);

        VBox team1InfoPanel = createTeamInfoPanel(team1Name, team1Members);
        mainPane.setLeft(team1InfoPanel);
        BorderPane.setMargin(team1InfoPanel, new Insets(0, 20, 0, 20));

        VBox team2InfoPanel = createTeamInfoPanel(team2Name, team2Members);
        mainPane.setRight(team2InfoPanel);
        BorderPane.setMargin(team2InfoPanel, new Insets(0, 20, 0, 20));

        VBox centerPanel = new VBox(15);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(20));
        centerPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.35); -fx-background-radius: 25;");
        centerPanel.setMinHeight(250);

        startingLabel = new Label();
        startingLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        startingLabel.setTextFill(Color.GOLD);
        startingLabel.setEffect(new DropShadow(5, Color.BLACK));
        startingLabel.setVisible(false);

        teamDisplayLabel = new Label();
        teamDisplayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60));
        teamDisplayLabel.setTextFill(Color.GOLD);
        teamDisplayLabel.setEffect(new DropShadow(15, Color.BLACK));
        teamDisplayLabel.setMinHeight(90);
        teamDisplayLabel.setOpacity(0);
        teamDisplayLabel.setVisible(true);

        teamDisplayContainer = new StackPane(teamDisplayLabel);
        teamDisplayContainer.setAlignment(Pos.CENTER);
        teamDisplayContainer.setMinHeight(100);

        centerPanel.getChildren().addAll(
                startingLabel,
                teamDisplayContainer
        );
        mainPane.setCenter(centerPanel);
        BorderPane.setMargin(centerPanel, new Insets(0,10,0,10));

        Rectangle progressBackgroundRect = new Rectangle(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        progressBackgroundRect.setArcWidth(30);
        progressBackgroundRect.setArcHeight(30);
        progressBackgroundRect.setFill(Color.rgb(0, 0, 0, 0.3));
        progressBackgroundRect.setStroke(Color.rgb(255,255,255,0.2));
        progressBackgroundRect.setStrokeWidth(1.5);

        progressFillRect = new Rectangle(0, PROGRESS_BAR_HEIGHT);
        progressFillRect.setArcWidth(30);
        progressFillRect.setArcHeight(30);
        progressFillRect.setFill(Color.LIGHTSKYBLUE);

        progressText = new Text("Ładowanie...");
        progressText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        progressText.setFill(Color.WHITE);

        customProgressBar = new StackPane(progressBackgroundRect, progressFillRect, progressText);
        customProgressBar.setPrefSize(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        customProgressBar.setMaxSize(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        StackPane.setAlignment(progressFillRect, Pos.CENTER_LEFT); // Kluczowe dla wypełniania od lewej
        customProgressBar.setVisible(false);


        bottomStatusBox = new VBox(5, customProgressBar);
        bottomStatusBox.setAlignment(Pos.CENTER);
        bottomStatusBox.setPadding(new Insets(10,0,20,0));
        bottomStatusBox.setVisible(true);

        startGameButton = new Button("Rozpocznij grę");
        startGameButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        startGameButton.setTextFill(Color.WHITE);
        // Styl przycisku, można go dostosować, aby pasował do PROGRESS_BAR_WIDTH i PROGRESS_BAR_HEIGHT
        startGameButton.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #4CAF50, #388E3C);" +
                        "-fx-background-radius: 30;" + // Pasuje do arcWidth/arcHeight paska
                        "-fx-min-width: " + PROGRESS_BAR_WIDTH + "px;" +
                        "-fx-pref-width: " + PROGRESS_BAR_WIDTH + "px;" +
                        "-fx-min-height: " + PROGRESS_BAR_HEIGHT + "px;" +
                        "-fx-pref-height: " + PROGRESS_BAR_HEIGHT + "px;" +
                        "-fx-padding: 0;" + // Usunięty padding, aby tekst był centralnie jeśli trzeba
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 2);"
        );
        startGameButton.setOnMouseEntered(e -> {
            String currentStyle = startGameButton.getStyle();
            startGameButton.setStyle(currentStyle.replace("linear-gradient(from 0% 0% to 100% 100%, #4CAF50, #388E3C)", "linear-gradient(from 0% 0% to 100% 100%, #66BB6A, #4CAF50)")
                    .replace("dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 2)", "dropshadow(gaussian, rgba(0,0,0,0.6), 15, 0.6, 0, 3)"));
        });
        startGameButton.setOnMouseExited(e -> {
            String currentStyle = startGameButton.getStyle();
            startGameButton.setStyle(currentStyle.replace("linear-gradient(from 0% 0% to 100% 100%, #66BB6A, #4CAF50)", "linear-gradient(from 0% 0% to 100% 100%, #4CAF50, #388E3C)")
                    .replace("dropshadow(gaussian, rgba(0,0,0,0.6), 15, 0.6, 0, 3)", "dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 2)"));
        });

        startGameButton.setVisible(true);
        startGameButton.setDisable(true);
        startGameButton.setOnAction(e -> proceedToGame());

        BorderPane.setAlignment(startGameButton, Pos.CENTER);
        BorderPane.setMargin(startGameButton, new Insets(20, 0, 20, 0));

        mainPane.setBottom(bottomStatusBox);

        uiInitialized = true;
    }

    private VBox createTeamInfoPanel(String teamNameText, List<String> membersList) {
        VBox teamPanel = new VBox(12);
        teamPanel.setPadding(new Insets(20));
        teamPanel.setAlignment(Pos.TOP_CENTER);
        teamPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(218, 165, 32, 0.5);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 0, 1);"
        );
        teamPanel.setPrefWidth(280);
        teamPanel.setMinWidth(240);
        teamPanel.setMaxWidth(350);

        Label nameLabel = new Label(teamNameText);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        nameLabel.setTextFill(Color.GOLD);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setWrapText(true);
        nameLabel.setEffect(new DropShadow(8, Color.rgb(0,0,0, 0.8)));

        Rectangle separator = new Rectangle(180, 2, Color.rgb(218, 165, 32, 0.6));
        separator.setArcWidth(5);
        separator.setArcHeight(5);
        VBox.setMargin(separator, new Insets(10, 0, 10, 0));

        Label membersTitleLabel = new Label("Członkowie:");
        membersTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        membersTitleLabel.setTextFill(Color.WHITE);
        VBox.setMargin(membersTitleLabel, new Insets(6, 0, 6, 0));

        teamPanel.getChildren().addAll(nameLabel, separator, membersTitleLabel);

        VBox membersListVBox = new VBox(7);
        membersListVBox.setAlignment(Pos.CENTER_LEFT);
        membersListVBox.setPadding(new Insets(4, 0, 0, 20));

        if (membersList != null && !membersList.isEmpty()) {
            int playerNum = 1;
            for (String member : membersList) {
                Label memberLabel = new Label(playerNum + ". " + member);
                memberLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 15));
                memberLabel.setTextFill(Color.WHITE);
                memberLabel.setWrapText(true);
                membersListVBox.getChildren().add(memberLabel);
                playerNum++;
            }
        } else {
            Label noMembersLabel = new Label("(Brak graczy)");
            noMembersLabel.setFont(Font.font("Segoe UI", FontWeight.LIGHT, 14));
            noMembersLabel.setTextFill(Color.LIGHTGRAY);
            membersListVBox.getChildren().add(noMembersLabel);
        }
        teamPanel.getChildren().add(membersListVBox);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        teamPanel.getChildren().add(spacer);

        return teamPanel;
    }

    private void startSelectionAnimation() {
        if (startingLabel == null || teamDisplayLabel == null) return;

        animationComplete = false;
        if (startGameButton != null) {
            startGameButton.setDisable(true);
        }

        startingLabel.setVisible(false);
        teamDisplayLabel.setOpacity(0);
        teamDisplayLabel.setVisible(true);

        Random random = new Random();
        this.team1Starts = random.nextBoolean();

        final Duration cycleDuration = Duration.millis(100);
        final Duration pauseDuration = Duration.millis(50);
        final int cycles = 20;
        final Duration finalRevealDelay = Duration.millis(250);

        SequentialTransition sequentialTransition = new SequentialTransition();

        for (int i = 0; i < cycles; i++) {
            final String nameToShow = (i % 2 == 0) ? team1Name : team2Name;
            final boolean isLastCycleBeforeFinal = (i == cycles -1);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(50), teamDisplayLabel);
            fadeOut.setFromValue(teamDisplayLabel.getOpacity());
            fadeOut.setToValue(0.2);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(50), teamDisplayLabel);
            scaleDown.setToX(0.95);
            scaleDown.setToY(0.95);

            ParallelTransition outTransition = new ParallelTransition(fadeOut, scaleDown);

            sequentialTransition.getChildren().add(outTransition);
            sequentialTransition.getChildren().add(new PauseTransition(pauseDuration));

            FadeTransition fadeIn = new FadeTransition(Duration.millis(50), teamDisplayLabel);
            fadeIn.setFromValue(0.2);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(event -> teamDisplayLabel.setText(nameToShow));

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(50), teamDisplayLabel);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);

            ParallelTransition inTransition = new ParallelTransition(fadeIn, scaleUp);
            sequentialTransition.getChildren().add(inTransition);

            if (!isLastCycleBeforeFinal) {
                ScaleTransition scaleNormal = new ScaleTransition(Duration.millis(30), teamDisplayLabel);
                scaleNormal.setToX(1.0);
                scaleNormal.setToY(1.0);
                sequentialTransition.getChildren().add(scaleNormal);
            }
        }

        PauseTransition finalDelay = new PauseTransition(finalRevealDelay);
        sequentialTransition.getChildren().add(finalDelay);

        sequentialTransition.setOnFinished(event -> {
            String selectedTeam = team1Starts ? team1Name : team2Name;
            teamDisplayLabel.setText(selectedTeam);
            teamDisplayLabel.setOpacity(1.0);

            startingLabel.setText("Rozpoczyna drużyna:");
            startingLabel.setOpacity(0);
            startingLabel.setVisible(true);

            FadeTransition fadeInStartingLabel = new FadeTransition(Duration.millis(400), startingLabel);
            fadeInStartingLabel.setToValue(1.0);

            ScaleTransition finalScale = new ScaleTransition(Duration.millis(350), teamDisplayLabel);
            finalScale.setFromX(1.05); finalScale.setFromY(1.05);
            finalScale.setToX(1.15); finalScale.setToY(1.15);
            finalScale.setAutoReverse(true);
            finalScale.setCycleCount(2);

            Glow finalGlow = new Glow(0.0);
            teamDisplayLabel.setEffect(finalGlow);
            Timeline finalGlowTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(finalGlow.levelProperty(), 0.0)),
                    new KeyFrame(Duration.millis(300), new KeyValue(finalGlow.levelProperty(), 0.9)),
                    new KeyFrame(Duration.millis(700), new KeyValue(finalGlow.levelProperty(), 0.0))
            );

            ParallelTransition finalRevealEffects = new ParallelTransition(fadeInStartingLabel, finalScale);
            finalRevealEffects.setOnFinished(e -> {
                finalGlowTimeline.play();
                animationComplete = true;
                Platform.runLater(this::checkIfReadyToEnableStartButton);
            });
            finalRevealEffects.play();
        });

        sequentialTransition.play();
    }

    private void proceedToGame() {
        if (!loadingComplete) {
            // showLoadingError("Gra nie jest jeszcze gotowa."); // Komunikat może być zbędny, bo przycisk jest nieaktywny
            return;
        }

        if (gameService == null || gameService.getCurrentQuestion() == null) {
            showLoadingError("Nie udało się załadować danych gry (brak pytania).");
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            GameFrame gameScreen = new GameFrame(
                    selectedCategory,
                    answerTime,
                    team1Name,
                    team2Name,
                    team1Starts,
                    this.team1Members,
                    this.team2Members,
                    this.stage,
                    this.gameService
            );
            Parent gameRoot = gameScreen.getRootPane();
            if(stage.getScene() == null){
                Scene newScene = new Scene(gameRoot);
                try {
                    String cssPath = getClass().getResource("/styles.css").toExternalForm();
                    if (cssPath != null) newScene.getStylesheets().add(cssPath);
                } catch (Exception cssEx) {
                    System.err.println("Failed to load CSS for GameFrame scene: " + cssEx.getMessage());
                }
                stage.setScene(newScene);
            } else {
                stage.getScene().setRoot(gameRoot);
            }

            gameScreen.initializeGameContent();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), gameRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
}