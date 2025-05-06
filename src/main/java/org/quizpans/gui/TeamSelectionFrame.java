package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
// Usunięto import ProgressIndicator
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
    private final Stage stage;
    private final String selectedCategory;
    private final int answerTime;
    private final String team1Name;
    private final String team2Name;
    private final List<String> team1Members;
    private final List<String> team2Members;
    private GameService gameService;
    private boolean team1Starts;

    private volatile boolean loadingComplete = false;

    private Label loadingLabel;
    // Usunięto pole progressIndicator
    private Button startGameButton;
    private Timeline loadingTextTimeline;


    public TeamSelectionFrame(String selectedCategory, int answerTime, String team1Name, String team2Name, List<String> team1Members, List<String> team2Members) {
        this.stage = new Stage();
        this.selectedCategory = selectedCategory;
        this.answerTime = answerTime;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Members = team1Members;
        this.team2Members = team2Members;
        initializeFrame();
        initUI();

        new Thread(this::initializeGameService).start();
    }

    private void initializeGameService() {
        try {
            this.gameService = new GameService(selectedCategory);
            loadingComplete = true;
            Platform.runLater(() -> {
                if (loadingTextTimeline != null) {
                    loadingTextTimeline.stop();
                }
                if (loadingLabel != null) loadingLabel.setVisible(false);
                // Usunięto ukrywanie progressIndicator
                if (startGameButton != null) {
                    startGameButton.setVisible(true);
                    startGameButton.setDisable(false);
                    FadeTransition ft = new FadeTransition(Duration.millis(500), startGameButton);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });
        } catch (Exception e) {
            System.err.println("Krytyczny błąd inicjalizacji GameService: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                if (loadingTextTimeline != null) {
                    loadingTextTimeline.stop();
                }
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Błąd krytyczny");
                alert.setHeaderText("Nie można załadować danych gry!");
                alert.setContentText("Wystąpił błąd podczas ładowania pytań lub usługi gry.\nSprawdź połączenie z bazą danych i konfigurację.\n\n" + e.getMessage());
                alert.showAndWait();
                stage.close();
            });
        }
    }


    private void initializeFrame() {
        stage.setTitle("Familiada - Losowanie i Ładowanie Gry");
        stage.setMaximized(true);
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream != null) {
                stage.getIcons().add(new Image(logoStream));
                logoStream.close();
            } else {
                System.err.println("Nie można załadować ikony aplikacji: /logo.png");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować ikony: " + e.getMessage());
        }
    }

    private void initUI() {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        BorderPane mainPane = new BorderPane();
        mainPane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        mainPane.setPadding(new Insets(30));

        Label titleLabel = new Label("LOSOWANIE DRUŻYNY");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.7)));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(10, 0, 40, 0));
        mainPane.setTop(titleLabel);

        VBox team1InfoPanel = createTeamInfoPanel(team1Name, team1Members);
        mainPane.setLeft(team1InfoPanel);
        BorderPane.setMargin(team1InfoPanel, new Insets(0, 30, 0, 30));

        VBox team2InfoPanel = createTeamInfoPanel(team2Name, team2Members);
        mainPane.setRight(team2InfoPanel);
        BorderPane.setMargin(team2InfoPanel, new Insets(0, 30, 0, 30));

        VBox centerPanel = new VBox(20);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(40));
        centerPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.35); -fx-background-radius: 25;");

        loadingLabel = new Label("Ładowanie gry");
        loadingLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        loadingLabel.setTextFill(Color.WHITE);

        loadingTextTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> loadingLabel.setText("Ładowanie gry")),
                new KeyFrame(Duration.millis(500), e -> loadingLabel.setText("Ładowanie gry.")),
                new KeyFrame(Duration.millis(1000), e -> loadingLabel.setText("Ładowanie gry..")),
                new KeyFrame(Duration.millis(1500), e -> loadingLabel.setText("Ładowanie gry...")),
                new KeyFrame(Duration.millis(2000))
        );
        loadingTextTimeline.setCycleCount(Timeline.INDEFINITE);
        loadingTextTimeline.play();


        Label startingLabel = new Label();
        startingLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        startingLabel.setTextFill(Color.GOLD);
        startingLabel.setEffect(new DropShadow(5, Color.BLACK));
        startingLabel.setVisible(false);

        Label teamLabel = new Label();
        teamLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60));
        teamLabel.setTextFill(Color.GOLD);
        teamLabel.setEffect(new DropShadow(15, Color.BLACK));
        teamLabel.setMinHeight(90);
        teamLabel.setOpacity(0);

        // Usunięto tworzenie i stylowanie progressIndicator

        centerPanel.getChildren().addAll(
                startingLabel,
                teamLabel,
                loadingLabel
                // Usunięto progressIndicator z dodawania do panelu
        );
        mainPane.setCenter(centerPanel);

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
        BorderPane.setMargin(startGameButton, new Insets(40, 0, 30, 0));
        mainPane.setBottom(startGameButton);

        startSelectionAnimation(startingLabel, teamLabel);

        Scene scene = new Scene(mainPane);
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            } else {
                System.err.println("Nie znaleziono pliku stylów: /styles.css");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }
        stage.setScene(scene);
    }

    private VBox createTeamInfoPanel(String teamName, List<String> members) {
        VBox teamPanel = new VBox(12);
        teamPanel.setPadding(new Insets(30));
        teamPanel.setAlignment(Pos.TOP_CENTER);
        teamPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(218, 165, 32, 0.5);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 0, 1);"
        );
        teamPanel.setPrefWidth(300);
        teamPanel.setMinWidth(250);
        teamPanel.setMaxWidth(400);

        Label nameLabel = new Label(teamName);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        nameLabel.setTextFill(Color.GOLD);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setWrapText(true);
        nameLabel.setEffect(new DropShadow(8, Color.rgb(0,0,0, 0.8)));

        Rectangle separator = new Rectangle(200, 2, Color.rgb(218, 165, 32, 0.6));
        separator.setArcWidth(5);
        separator.setArcHeight(5);
        VBox.setMargin(separator, new Insets(12, 0, 12, 0));

        Label membersTitleLabel = new Label("Członkowie:");
        membersTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        membersTitleLabel.setTextFill(Color.WHITE);
        VBox.setMargin(membersTitleLabel, new Insets(8, 0, 8, 0));

        teamPanel.getChildren().addAll(nameLabel, separator, membersTitleLabel);

        VBox membersListVBox = new VBox(8);
        membersListVBox.setAlignment(Pos.CENTER_LEFT);
        membersListVBox.setPadding(new Insets(5, 0, 0, 25));

        if (members != null && !members.isEmpty()) {
            int playerNum = 1;
            for (String member : members) {
                Label memberLabel = new Label(playerNum + ". " + member);
                memberLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
                memberLabel.setTextFill(Color.WHITE);
                memberLabel.setWrapText(true);
                membersListVBox.getChildren().add(memberLabel);
                playerNum++;
            }
        } else {
            Label noMembersLabel = new Label("(Brak graczy)");
            noMembersLabel.setFont(Font.font("Segoe UI", FontWeight.LIGHT, 15));
            noMembersLabel.setTextFill(Color.LIGHTGRAY);
            membersListVBox.getChildren().add(noMembersLabel);
        }
        teamPanel.getChildren().add(membersListVBox);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        teamPanel.getChildren().add(spacer);


        return teamPanel;
    }

    private void startSelectionAnimation(Label startingLabel, Label teamLabel) {
        Random random = new Random();
        this.team1Starts = random.nextBoolean();
        final Duration cycleTime = Duration.millis(130);
        final int cycles = 15;
        final Duration finalRevealDelay = Duration.millis(300);

        Timeline animation = new Timeline();

        for (int i = 0; i < cycles; i++) {
            String nameToShow = (i % 2 == 0) ? team1Name : team2Name;
            Duration time = cycleTime.multiply(i);
            animation.getKeyFrames().addAll(
                    new KeyFrame(time,
                            new KeyValue(teamLabel.textProperty(), nameToShow),
                            new KeyValue(teamLabel.opacityProperty(), 1.0),
                            new KeyValue(teamLabel.scaleXProperty(), 1.05),
                            new KeyValue(teamLabel.scaleYProperty(), 1.05)
                    ),
                    new KeyFrame(time.add(cycleTime.multiply(0.8)),
                            new KeyValue(teamLabel.opacityProperty(), 0.5),
                            new KeyValue(teamLabel.scaleXProperty(), 1.0),
                            new KeyValue(teamLabel.scaleYProperty(), 1.0),
                            new KeyValue(teamLabel.rotateProperty(), (i % 2 == 0) ? 2 : -2)
                    ),
                    new KeyFrame(time.add(cycleTime),
                            new KeyValue(teamLabel.rotateProperty(), 0, Interpolator.EASE_BOTH)
                    )
            );
        }

        String selectedTeam = team1Starts ? team1Name : team2Name;
        Duration finalTime = cycleTime.multiply(cycles).add(finalRevealDelay);
        animation.getKeyFrames().add(new KeyFrame(finalTime, e -> {
            teamLabel.setText(selectedTeam);
            teamLabel.setOpacity(1.0);
            teamLabel.setScaleX(1.0);
            teamLabel.setScaleY(1.0);
            teamLabel.setRotate(0);
            startingLabel.setText("Rozpoczyna drużyna:");
            startingLabel.setVisible(true);

            ScaleTransition st = new ScaleTransition(Duration.millis(300), teamLabel);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.15); st.setToY(1.15);
            st.setAutoReverse(true);
            st.setCycleCount(2);

            FadeTransition ft = new FadeTransition(Duration.millis(200), startingLabel);
            ft.setFromValue(0.0); ft.setToValue(1.0);

            Glow glow = new Glow(0.0);
            teamLabel.setEffect(glow);
            Timeline glowTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.0)),
                    new KeyFrame(Duration.millis(300), new KeyValue(glow.levelProperty(), 0.8)),
                    new KeyFrame(Duration.millis(600), new KeyValue(glow.levelProperty(), 0.0))
            );

            st.setOnFinished(evt -> glowTimeline.play());
            ft.play();
            st.play();
        }));

        animation.setOnFinished(event -> {
            System.out.println("Animacja wyboru drużyny zakończona.");
            Timeline resetEffect = new Timeline(new KeyFrame(Duration.millis(100), e -> teamLabel.setEffect(new DropShadow(15, Color.BLACK))));
            resetEffect.play();
        });

        animation.play();
    }

    private void proceedToGame() {
        if (loadingComplete && gameService != null && gameService.getCurrentQuestion() != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.close();
                GameFrame gameFrame = new GameFrame(selectedCategory, answerTime, team1Name, team2Name, team1Starts, this.team1Members, this.team2Members);
                gameFrame.show();
            });
            fadeOut.play();

        } else {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Proszę czekać");
            alert.setHeaderText("Gra nie jest jeszcze gotowa.");
            alert.setContentText("Ładowanie danych gry nie zostało ukończone.");
            alert.showAndWait();
        }
    }

    public void show() {
        stage.show();
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}