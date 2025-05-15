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
import javafx.scene.control.ProgressBar;
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

    private Button startGameButton;
    private BorderPane mainPane;
    private boolean uiInitialized = false;

    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private VBox bottomStatusBox;

    private Label startingLabel;
    private Label teamDisplayLabel;
    private StackPane teamDisplayContainer;

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

    private void bindTaskToUI(Task<?> task) {
        if (progressBar != null && progressStatusLabel != null && task != null && bottomStatusBox != null) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            progressStatusLabel.textProperty().bind(task.messageProperty());
            mainPane.setBottom(bottomStatusBox);
            bottomStatusBox.setVisible(true);
            progressBar.setVisible(true);
            progressStatusLabel.setVisible(true);
        }
    }

    private void unbindTaskFromUIAndPrepareForButton() {
        if (progressBar != null && progressStatusLabel != null && bottomStatusBox != null ) {
            progressStatusLabel.textProperty().unbind(); // Odłącz tylko status, progress nie był bindowany
            progressBar.setProgress(0);
            bottomStatusBox.setVisible(false);
            mainPane.setBottom(startGameButton);
        }
    }

    private void updateUIOnLoadComplete() {
        unbindTaskFromUIAndPrepareForButton();

        if (!loadingComplete || gameService == null || gameService.getCurrentQuestion() == null) {
            if (loadingComplete) {
                showLoadingError("Dane gry nie zostały poprawnie załadowane.");
            }
            mainPane.setBottom(null);
            return;
        }

        if (startGameButton != null) {
            mainPane.setBottom(startGameButton);
            startGameButton.setVisible(true);
            startGameButton.setDisable(false);
        }
    }

    private void showLoadingError(String message) {
        unbindTaskFromUIAndPrepareForButton();
        if (progressStatusLabel != null && bottomStatusBox != null) {
            if(progressStatusLabel.textProperty().isBound()){
                progressStatusLabel.textProperty().unbind();
            }
            progressStatusLabel.setText("Błąd: " + message.substring(0, Math.min(message.length(), 60)) + "...");
            progressStatusLabel.setTextFill(Color.RED);
            if(progressBar != null) progressBar.setVisible(false);
            mainPane.setBottom(bottomStatusBox);
            bottomStatusBox.setVisible(true);
            progressStatusLabel.setVisible(true);
        }

        if (startGameButton != null) {
            mainPane.setBottom(startGameButton); // Upewnij się, że jest na dole, nawet jeśli niewidoczny
            startGameButton.setVisible(false);
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
        alert.showAndWait();
    }

    private void initializeGameServiceStandardWithTask() {
        Task<GameService> standardLoadTask = new Task<GameService>() {
            @Override
            protected GameService call() throws Exception {
                updateMessage("Inicjalizacja zasobów..."); // Obejmuje TextNormalizer itp. przy pierwszym użyciu
                GameService service = null;
                try {
                    service = new GameService(selectedCategory);
                } catch (Exception e) {
                    updateMessage("Błąd inicjalizacji GameService: " + e.getMessage().substring(0, Math.min(e.getMessage().length(), 30)) + "...");
                    throw e;
                }

                updateMessage("Wczytywanie pytania...");
                if (service.getCurrentQuestion() == null) {
                    throw new RuntimeException("Nie udało się załadować pytania dla kategorii: " + selectedCategory);
                }

                updateMessage("Finalizowanie...");
                return service;
            }
        };

        bindTaskToUI(standardLoadTask);

        standardLoadTask.setOnSucceeded(workerStateEvent -> {
            unbindTaskFromUIAndPrepareForButton();
            this.gameService = standardLoadTask.getValue();
            this.loadingComplete = (this.gameService != null && this.gameService.getCurrentQuestion() != null);
            if (this.loadingComplete) {
                Platform.runLater(this::updateUIOnLoadComplete);
            } else {
                showLoadingError("Nie udało się wczytać danych gry (standardowe ładowanie).");
            }
        });

        standardLoadTask.setOnFailed(workerStateEvent -> {
            unbindTaskFromUIAndPrepareForButton();
            Throwable exception = standardLoadTask.getException();
            if (exception != null) exception.printStackTrace();
            this.loadingComplete = false;
            Platform.runLater(() -> showLoadingError("Błąd podczas standardowego ładowania gry: " + (exception != null ? exception.getMessage() : "Nieznany błąd")));
        });

        Thread thread = new Thread(standardLoadTask);
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

        progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(350);
        progressBar.setMinHeight(25);
        progressBar.setStyle(
                "-fx-accent: #29b6f6; " +
                        "-fx-control-inner-background: rgba(0,0,0,0.3); " +
                        "-fx-border-radius: 15px; " +
                        "-fx-background-radius: 15px;" +
                        "-fx-padding: 1px;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-width: 1px;"
        );
        progressBar.getStyleClass().add("progress-bar-custom");

        progressStatusLabel = new Label("Ładowanie...");
        progressStatusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        progressStatusLabel.setTextFill(Color.WHITE);
        progressStatusLabel.setPadding(new Insets(5,0,0,0));

        bottomStatusBox = new VBox(5, progressStatusLabel, progressBar);
        bottomStatusBox.setAlignment(Pos.CENTER);
        bottomStatusBox.setPadding(new Insets(10,0,20,0));
        bottomStatusBox.setVisible(true);

        startGameButton = new Button("Rozpocznij grę");
        startGameButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        startGameButton.setTextFill(Color.WHITE);
        startGameButton.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #4CAF50, #388E3C);" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 18 35 18 35;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 2);"
        );
        startGameButton.setOnMouseEntered(e -> startGameButton.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #66BB6A, #4CAF50);" +
                        "-fx-background-radius: 30; -fx-padding: 18 35 18 35; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 15, 0.6, 0, 3);"
        ));
        startGameButton.setOnMouseExited(e -> startGameButton.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #4CAF50, #388E3C);" +
                        "-fx-background-radius: 30; -fx-padding: 18 35 18 35; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 2);"
        ));
        startGameButton.setVisible(false);
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
            finalRevealEffects.setOnFinished(e -> finalGlowTimeline.play());
            finalRevealEffects.play();
        });

        sequentialTransition.play();
    }

    private void proceedToGame() {
        if (!loadingComplete) {
            showLoadingError("Gra nie jest jeszcze gotowa.");
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