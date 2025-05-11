package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.FontPosture; // Dodany import
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class EndGameFrame {

    private BorderPane mainLayout;
    private final Stage stage;
    private final String team1Name;
    private final String team2Name;
    private final int finalTeam1Score;
    private final int finalTeam2Score;
    private final List<String> team1Members;
    private final List<String> team2Members;
    private final Map<String, List<Integer>> cumulativePlayerStats;
    private final Runnable onPlayAgainAction;
    private final Runnable onExitAction;

    public EndGameFrame(Stage stage,
                        String team1Name, String team2Name,
                        int finalTeam1Score, int finalTeam2Score,
                        List<String> team1Members, List<String> team2Members,
                        Map<String, List<Integer>> cumulativePlayerStats,
                        Runnable onPlayAgainAction, Runnable onExitAction) {
        this.stage = stage;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.finalTeam1Score = finalTeam1Score;
        this.finalTeam2Score = finalTeam2Score;
        this.team1Members = (team1Members != null) ? new ArrayList<>(team1Members) : Collections.emptyList();
        this.team2Members = (team2Members != null) ? new ArrayList<>(team2Members) : Collections.emptyList();
        this.cumulativePlayerStats = cumulativePlayerStats;
        this.onPlayAgainAction = onPlayAgainAction;
        this.onExitAction = onExitAction;
        createView();
    }

    private void createView() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(0.5, Color.web("#5c2c56")),
                new Stop(1, Color.web("#b21f1f"))
        );
        mainLayout.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("Koniec Gry!");
        titleLabel.setFont(Font.font("Arial Black", FontWeight.BOLD, 64));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(20, Color.BLACK));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(10, 0, 30, 0));
        mainLayout.setTop(titleLabel);

        VBox centerContent = new VBox(25);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(20));

        String winnerText;
        Color winnerColor = Color.LIGHTGREEN;
        boolean isDraw = false;

        if (finalTeam1Score > finalTeam2Score) {
            winnerText = "Wygrywa drużyna " + team1Name + "!";
        } else if (finalTeam2Score > finalTeam1Score) {
            winnerText = "Wygrywa drużyna " + team2Name + "!";
        } else {
            winnerText = "Remis!";
            winnerColor = Color.LIGHTSKYBLUE;
            isDraw = true;
        }

        Label winnerLabel = new Label(winnerText);
        winnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        winnerLabel.setTextFill(winnerColor);
        winnerLabel.setTextAlignment(TextAlignment.CENTER);
        winnerLabel.setWrapText(true);
        winnerLabel.setEffect(new DropShadow(15, Color.BLACK));
        if (!isDraw) {
            winnerLabel.setEffect(new Glow(0.8));
        }

        ScaleTransition st = new ScaleTransition(Duration.millis(700), winnerLabel);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1.0); st.setToY(1.0);
        st.setCycleCount(1);
        st.play();


        HBox scoresPane = new HBox(50);
        scoresPane.setAlignment(Pos.CENTER);
        scoresPane.setPadding(new Insets(20, 0, 20, 0));

        VBox team1ScoreDisplay = createScoreBox(team1Name, finalTeam1Score, finalTeam1Score > finalTeam2Score && !isDraw);
        VBox team2ScoreDisplay = createScoreBox(team2Name, finalTeam2Score, finalTeam2Score > finalTeam1Score && !isDraw);

        scoresPane.getChildren().addAll(team1ScoreDisplay, team2ScoreDisplay);

        Label statsTitleLabel = new Label("Końcowe Statystyki Graczy");
        statsTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        statsTitleLabel.setTextFill(Color.WHITE);
        statsTitleLabel.setEffect(new DropShadow(5, Color.BLACK));
        VBox.setMargin(statsTitleLabel, new Insets(20,0,10,0));

        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.TOP_CENTER);
        statsContainer.setPadding(new Insets(0, 15, 0, 15));

        VBox team1StatsListContainer = createTeamStatsPanel(team1Name, team1Members, cumulativePlayerStats);
        VBox team2StatsListContainer = createTeamStatsPanel(team2Name, team2Members, cumulativePlayerStats);

        HBox.setHgrow(team1StatsListContainer, Priority.ALWAYS);
        HBox.setHgrow(team2StatsListContainer, Priority.ALWAYS);
        statsContainer.getChildren().addAll(team1StatsListContainer, team2StatsListContainer);

        centerContent.getChildren().addAll(winnerLabel, scoresPane, statsTitleLabel, statsContainer);

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        mainLayout.setCenter(scrollPane);

        Button playAgainButton = new Button("Zagraj Ponownie");
        playAgainButton.getStyleClass().add("main-menu-button");
        playAgainButton.setStyle("-fx-font-size: 18px; -fx-min-width: 220px;");
        playAgainButton.setOnAction(e -> {
            if (onPlayAgainAction != null) {
                onPlayAgainAction.run();
            }
        });

        Button exitButton = new Button("Zakończ Grę");
        exitButton.getStyleClass().add("main-menu-button");
        exitButton.setStyle("-fx-font-size: 18px; -fx-min-width: 220px; -fx-background-color: #F44336;");
        exitButton.setOnAction(e -> {
            if (onExitAction != null) {
                onExitAction.run();
            } else {
                Platform.exit();
            }
        });

        HBox buttonBox = new HBox(30, playAgainButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(25, 0, 15, 0));
        mainLayout.setBottom(buttonBox);
    }

    private VBox createScoreBox(String teamNameText, int score, boolean isWinner) {
        VBox scoreBox = new VBox(5);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setPadding(new Insets(15));
        scoreBox.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 10px;");
        if (isWinner) {
            scoreBox.setStyle("-fx-background-color: rgba(255,215,0,0.3); -fx-background-radius: 10px; -fx-border-color: gold; -fx-border-width: 2px; -fx-border-radius: 10px;");
        }

        Label nameLabel = new Label(teamNameText);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        nameLabel.setTextFill(isWinner ? Color.GOLD : Color.WHITE);

        Label scoreLabel = new Label(String.valueOf(score) + " pkt");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        scoreLabel.setTextFill(isWinner ? Color.WHITE : Color.GOLD);
        if (isWinner) {
            nameLabel.setEffect(new DropShadow(8,Color.BLACK));
            scoreLabel.setEffect(new DropShadow(8,Color.BLACK));
        }

        scoreBox.getChildren().addAll(nameLabel, scoreLabel);
        return scoreBox;
    }

    private VBox createTeamStatsPanel(String teamNameText, List<String> teamMembers, Map<String, List<Integer>> allPlayerStats) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.getStyleClass().add("ranking-list-container");
        panel.setMaxWidth(400);
        panel.setPrefWidth(350);


        Label teamNameLabel = new Label(teamNameText);
        teamNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        teamNameLabel.setTextFill(Color.GOLD);
        teamNameLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(teamNameLabel, new Insets(0,0,10,0));

        VBox playerListVBox = new VBox(5);

        if (teamMembers.isEmpty()) {
            Label noPlayersLabel = new Label("Brak zdefiniowanych graczy");
            noPlayersLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 14)); // Poprawiony Font.ITALIC
            noPlayersLabel.setTextFill(Color.LIGHTGRAY);
            playerListVBox.getChildren().add(noPlayersLabel);
        } else {
            int playerIdx = 1;
            for (String playerName : teamMembers) {
                int totalCorrectAnswers = 0;
                if (allPlayerStats != null && allPlayerStats.containsKey(playerName)) {
                    List<Integer> scoresPerRound = allPlayerStats.get(playerName);
                    if (scoresPerRound != null) {
                        for (int scoreInRound : scoresPerRound) { // Zmieniona nazwa zmiennej, żeby uniknąć konfliktu
                            totalCorrectAnswers += scoreInRound;
                        }
                    }
                }

                HBox playerRow = new HBox(10);
                playerRow.setAlignment(Pos.CENTER_LEFT);
                playerRow.getStyleClass().add("ranking-row");

                Label indexLabel = new Label(playerIdx + ".");
                indexLabel.setMinWidth(25);
                indexLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
                indexLabel.setTextFill(Color.LIGHTGRAY);

                Label nameLabel = new Label(playerName);
                nameLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
                nameLabel.setTextFill(Color.WHITE);
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Label scoreValLabel = new Label(String.valueOf(totalCorrectAnswers));
                scoreValLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                scoreValLabel.setTextFill(Color.LIGHTGREEN);
                scoreValLabel.setMinWidth(30);
                scoreValLabel.setAlignment(Pos.CENTER_RIGHT);

                playerRow.getChildren().addAll(indexLabel, nameLabel, scoreValLabel);
                playerListVBox.getChildren().add(playerRow);
                playerIdx++;
            }
        }
        panel.getChildren().addAll(teamNameLabel, playerListVBox);
        return panel;
    }

    public Parent getView() {
        return mainLayout;
    }

    public void show() {
        if (stage == null || mainLayout == null) {
            System.err.println("EndGameFrame: Stage or mainLayout is null, cannot show.");
            return;
        }
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            Scene newScene = new Scene(mainLayout, stage.getWidth(), stage.getHeight());
            applyStyles(newScene);
            stage.setScene(newScene);
        } else {
            Parent currentRoot = currentScene.getRoot();
            if (currentRoot == mainLayout) {
                stage.show();
                return;
            }
            FadeTransition fadeOutCurrent = new FadeTransition(Duration.millis(400), currentRoot);
            fadeOutCurrent.setFromValue(1.0);
            fadeOutCurrent.setToValue(0.0);
            fadeOutCurrent.setOnFinished(event -> {
                currentScene.setRoot(mainLayout);
                applyStyles(currentScene);
                FadeTransition fadeInNew = new FadeTransition(Duration.millis(600), mainLayout);
                fadeInNew.setFromValue(0.0);
                fadeInNew.setToValue(1.0);
                fadeInNew.play();
            });
            fadeOutCurrent.play();
        }
        stage.setMaximized(true);
        stage.show();
    }

    private void applyStyles(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            System.err.println("Nie udało się załadować stylów CSS dla EndGameFrame: " + e.getMessage());
        }
    }
}