package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.quizpans.services.GameService;
import org.quizpans.services.GameService.AnswerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NextRoundFrame {

    private BorderPane mainLayout;
    private final Stage stage;
    private final Runnable onNextRoundAction;
    private final GameService gameService;
    private final Set<String> revealedInRoundAnswers;

    private VBox answersDisplayPanel;
    private VBox team1StatsListContainer;
    private VBox team2StatsListContainer;

    private final Map<String, List<Integer>> playerStatsData;
    private final int currentRoundNumberForStats;
    private final String team1NameData;
    private final String team2NameData;
    private final List<String> team1MembersData;
    private final List<String> team2MembersData;
    private final int totalScoreTeam1Data;
    private final int totalScoreTeam2Data;


    private static final String HIDDEN_PLACEHOLDER_TEXT = "■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■";

    public NextRoundFrame(Stage stage, Runnable onNextRoundAction, GameService gameService,
                          Set<String> revealedInRoundAnswers,
                          Map<String, List<Integer>> playerStats,
                          int currentRoundNumForStatsDisplay,
                          String team1Name, String team2Name,
                          List<String> team1Members, List<String> team2Members,
                          int totalScoreTeam1, int totalScoreTeam2) {
        this.stage = stage;
        this.onNextRoundAction = onNextRoundAction;
        this.gameService = gameService;
        this.revealedInRoundAnswers = revealedInRoundAnswers;
        this.playerStatsData = playerStats;
        this.currentRoundNumberForStats = currentRoundNumForStatsDisplay;
        this.team1NameData = team1Name;
        this.team2NameData = team2Name;
        this.team1MembersData = team1Members;
        this.team2MembersData = team2Members;
        this.totalScoreTeam1Data = totalScoreTeam1;
        this.totalScoreTeam2Data = totalScoreTeam2;
        createView();
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    private void createView() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(15));

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        mainLayout.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("Podsumowanie Rundy " + currentRoundNumberForStats);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 10, 0));
        mainLayout.setTop(titleLabel);

        VBox centerContent = new VBox(10);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(5));

        answersDisplayPanel = new VBox(5);
        answersDisplayPanel.setAlignment(Pos.TOP_CENTER);
        answersDisplayPanel.setPadding(new Insets(5, 10, 5, 10));
        answersDisplayPanel.getStyleClass().add("answer-panel");
        populateAnswersPanel();
        answersDisplayPanel.setMaxHeight(280);

        Node totalScoresDisplay = createTotalScoreDisplay();
        VBox.setMargin(totalScoresDisplay, new Insets(10, 0, 5, 0));

        Label statsOverallTitleLabel = new Label("Statystyki Graczy (Poprawne Odp.)");
        statsOverallTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        statsOverallTitleLabel.setTextFill(Color.WHITE);
        statsOverallTitleLabel.setEffect(new DropShadow(5, Color.BLACK));
        VBox.setMargin(statsOverallTitleLabel, new Insets(10,0,5,0));

        HBox statsContainer = new HBox(15);
        statsContainer.setAlignment(Pos.TOP_CENTER);
        statsContainer.setPadding(new Insets(0, 10, 0, 10));

        VBox team1StatsContainer = new VBox(5);
        team1StatsContainer.setAlignment(Pos.TOP_LEFT);
        team1StatsContainer.getStyleClass().add("ranking-list-container");
        Label team1NameLabel = new Label(team1NameData);
        team1NameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        team1NameLabel.setTextFill(Color.GOLD);
        team1NameLabel.setPadding(new Insets(0,0,5,5));
        team1StatsListContainer = new VBox(3);
        populateTeamStatsList(team1StatsListContainer, team1MembersData, playerStatsData);
        HBox.setHgrow(team1StatsContainer, Priority.ALWAYS);
        team1StatsContainer.getChildren().addAll(team1NameLabel, team1StatsListContainer);

        VBox team2StatsContainer = new VBox(5);
        team2StatsContainer.setAlignment(Pos.TOP_LEFT);
        team2StatsContainer.getStyleClass().add("ranking-list-container");
        Label team2NameLabel = new Label(team2NameData);
        team2NameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        team2NameLabel.setTextFill(Color.GOLD);
        team2NameLabel.setPadding(new Insets(0,0,5,5));
        team2StatsListContainer = new VBox(3);
        populateTeamStatsList(team2StatsListContainer, team2MembersData, playerStatsData);
        HBox.setHgrow(team2StatsContainer, Priority.ALWAYS);
        team2StatsContainer.getChildren().addAll(team2NameLabel, team2StatsListContainer);

        statsContainer.getChildren().addAll(team1StatsContainer, team2StatsContainer);

        centerContent.getChildren().addAll(answersDisplayPanel, totalScoresDisplay, statsOverallTitleLabel, statsContainer);

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        mainLayout.setCenter(scrollPane);

        Button nextRoundButton = new Button("Przejdź do kolejnej rundy");
        nextRoundButton.getStyleClass().add("main-menu-button");
        nextRoundButton.setStyle("-fx-font-size: 18px; -fx-min-width: 280px;");
        nextRoundButton.setOnAction(e -> {
            if (onNextRoundAction != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainLayout);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> onNextRoundAction.run());
                fadeOut.play();
            }
        });
        HBox buttonBox = new HBox(nextRoundButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 5, 0));
        mainLayout.setBottom(buttonBox);
    }

    private Node createTotalScoreDisplay() {
        HBox scoreBox = new HBox(40);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setPadding(new Insets(10, 0, 5, 0));

        Label score1Label = new Label(team1NameData + ": ");
        score1Label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        score1Label.setTextFill(Color.WHITE);
        Label score1Value = new Label(String.valueOf(totalScoreTeam1Data));
        score1Value.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        score1Value.setTextFill(Color.GOLD);
        score1Value.setEffect(new DropShadow(5, Color.BLACK));
        HBox team1ScoreDisplay = new HBox(5, score1Label, score1Value);
        team1ScoreDisplay.setAlignment(Pos.CENTER_LEFT);

        Label score2Label = new Label(team2NameData + ": ");
        score2Label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        score2Label.setTextFill(Color.WHITE);
        Label score2Value = new Label(String.valueOf(totalScoreTeam2Data));
        score2Value.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        score2Value.setTextFill(Color.GOLD);
        score2Value.setEffect(new DropShadow(5, Color.BLACK));
        HBox team2ScoreDisplay = new HBox(5, score2Label, score2Value);
        team2ScoreDisplay.setAlignment(Pos.CENTER_LEFT);

        scoreBox.getChildren().addAll(team1ScoreDisplay, team2ScoreDisplay);
        return scoreBox;
    }

    private void populateAnswersPanel() {
        answersDisplayPanel.getChildren().clear();
        if (gameService == null) return;

        List<AnswerData> allAnswers = gameService.getAllAnswersForCurrentQuestion();

        for (AnswerData answerData : allAnswers) {
            BorderPane answerRowPane = new BorderPane();
            answerRowPane.getStyleClass().add("answer-pane");
            answerRowPane.setPrefHeight(50);
            answerRowPane.setMaxWidth(650);

            Label answerTextLabel = new Label();
            answerTextLabel.getStyleClass().add("answer-text-label");
            answerTextLabel.setStyle("-fx-font-size: 20px;");
            answerTextLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            BorderPane.setAlignment(answerTextLabel, Pos.CENTER_LEFT);

            Label pointsTextLabel = new Label();
            pointsTextLabel.getStyleClass().add("points-label");
            pointsTextLabel.setStyle("-fx-font-size: 18px;");
            BorderPane.setAlignment(pointsTextLabel, Pos.CENTER_RIGHT);
            BorderPane.setMargin(pointsTextLabel, new Insets(0, 10, 0, 10));

            Button revealButton = new Button("Odkryj");
            revealButton.getStyleClass().add("main-menu-button");
            revealButton.setStyle("-fx-font-size: 11px; -fx-padding: 3px 6px; -fx-min-width: 60px; -fx-pref-height: 26px;");


            if (revealedInRoundAnswers.contains(answerData.getBaseForm())) {
                answerTextLabel.setText((answerData.getDisplayOrderIndex() + 1) + ". " + capitalizeFirstLetter(answerData.getOriginalText()));
                pointsTextLabel.setText(answerData.getPoints() + " pkt");
                answerTextLabel.getStyleClass().add("revealed");
                pointsTextLabel.getStyleClass().add("revealed");
                answerRowPane.setCenter(answerTextLabel);
                answerRowPane.setRight(pointsTextLabel);
                answerRowPane.getStyleClass().add("answer-revealed");
            } else {
                answerTextLabel.setText((answerData.getDisplayOrderIndex() + 1) + ". " + HIDDEN_PLACEHOLDER_TEXT);
                pointsTextLabel.setText(" ");
                answerTextLabel.getStyleClass().add("hidden");
                pointsTextLabel.getStyleClass().add("hidden");

                HBox rightContainer = new HBox(10, pointsTextLabel, revealButton);
                rightContainer.setAlignment(Pos.CENTER_RIGHT);
                BorderPane.setMargin(rightContainer, new Insets(0,5,0,5));

                answerRowPane.setCenter(answerTextLabel);
                answerRowPane.setRight(rightContainer);

                revealButton.setOnAction(e -> {
                    answerTextLabel.setText((answerData.getDisplayOrderIndex() + 1) + ". " + capitalizeFirstLetter(answerData.getOriginalText()));
                    pointsTextLabel.setText(answerData.getPoints() + " pkt");
                    answerTextLabel.getStyleClass().remove("hidden");
                    answerTextLabel.getStyleClass().add("revealed");
                    pointsTextLabel.getStyleClass().remove("hidden");
                    pointsTextLabel.getStyleClass().add("revealed");
                    answerRowPane.getStyleClass().add("answer-revealed");

                    HBox newRightContainer = new HBox(pointsTextLabel);
                    newRightContainer.setAlignment(Pos.CENTER_RIGHT);
                    BorderPane.setMargin(newRightContainer, new Insets(0,10,0,10));
                    answerRowPane.setRight(newRightContainer);

                    FadeTransition ft = new FadeTransition(Duration.millis(300), answerRowPane);
                    ft.setFromValue(0.5);
                    ft.setToValue(1.0);
                    ft.play();
                });
            }
            answersDisplayPanel.getChildren().add(answerRowPane);
        }
        if (allAnswers.isEmpty() && gameService.getCurrentQuestion() != null ) {
            Label noAnswersLabel = new Label("Brak odpowiedzi do wyświetlenia dla tej rundy.");
            noAnswersLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            noAnswersLabel.setTextFill(Color.LIGHTGRAY);
            answersDisplayPanel.getChildren().add(noAnswersLabel);
        } else if (gameService.getCurrentQuestion() == null) {
            Label errorLabel = new Label("Błąd: Nie załadowano pytania dla tej rundy.");
            errorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            errorLabel.setTextFill(Color.ORANGERED);
            answersDisplayPanel.getChildren().add(errorLabel);
        }
    }

    private void populateTeamStatsList(VBox listContainer, List<String> teamMembers, Map<String, List<Integer>> allPlayerStats) {
        listContainer.getChildren().clear();

        if (teamMembers == null || teamMembers.isEmpty()) {
            Label noPlayersLabel = new Label("Brak graczy w drużynie.");
            noPlayersLabel.setTextFill(Color.LIGHTGRAY);
            noPlayersLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 14));
            listContainer.getChildren().add(noPlayersLabel);
            return;
        }

        int playerIndex = 1;
        for (String playerName : teamMembers) {
            int totalScore = 0;
            if (allPlayerStats != null && allPlayerStats.containsKey(playerName)) {
                List<Integer> scores = allPlayerStats.get(playerName);
                if (scores != null) {
                    for (int score : scores) {
                        totalScore += score;
                    }
                }
            }

            HBox playerRow = new HBox(10);
            playerRow.setAlignment(Pos.CENTER_LEFT);
            playerRow.setPadding(new Insets(3, 5, 3, 5));
            playerRow.getStyleClass().add("ranking-row");

            Label indexLabel = new Label(playerIndex + ".");
            indexLabel.setMinWidth(20);
            indexLabel.setAlignment(Pos.CENTER_RIGHT);
            indexLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
            indexLabel.setTextFill(Color.LIGHTGRAY);

            Label nameLabel = new Label(playerName);
            nameLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            nameLabel.setTextFill(Color.WHITE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label scoreLabel = new Label(String.valueOf(totalScore));
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            scoreLabel.setTextFill(Color.LIGHTGREEN);
            scoreLabel.setMinWidth(30);
            scoreLabel.setAlignment(Pos.CENTER_RIGHT);


            playerRow.getChildren().addAll(indexLabel, nameLabel, scoreLabel);
            listContainer.getChildren().add(playerRow);
            playerIndex++;
        }
    }


    public Parent getView() {
        return mainLayout;
    }

    public void show() {
        if (stage == null || mainLayout == null) {
            System.err.println("NextRoundFrame: Stage or mainLayout is null, cannot show.");
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
            FadeTransition fadeOutCurrent = new FadeTransition(Duration.millis(300), currentRoot);
            fadeOutCurrent.setFromValue(1.0);
            fadeOutCurrent.setToValue(0.0);
            fadeOutCurrent.setOnFinished(event -> {
                currentScene.setRoot(mainLayout);
                applyStyles(currentScene);
                FadeTransition fadeInNew = new FadeTransition(Duration.millis(300), mainLayout);
                fadeInNew.setFromValue(0.0);
                fadeInNew.setToValue(1.0);
                fadeInNew.play();
            });
            fadeOutCurrent.play();
        }
        stage.show();
    }

    private void applyStyles(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            System.err.println("Nie udało się załadować stylów CSS dla NextRoundFrame: " + e.getMessage());
        }
    }


}