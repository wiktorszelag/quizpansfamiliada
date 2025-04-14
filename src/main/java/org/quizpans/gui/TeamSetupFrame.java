package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class TeamSetupFrame {
    private final Stage stage;
    private VBox mainPanel;
    private VBox settingsPanel;
    private final ComboBox<Integer> teamSizeComboBox;
    private final ComboBox<String> categoryComboBox;
    private final Spinner<Integer> answerTimeSpinner;
    private final VBox team1MembersPanel;
    private final VBox team2MembersPanel;
    private final TextField team1Field;
    private final TextField team2Field;

    public TeamSetupFrame() {
        stage = new Stage();
        stage.setTitle("Ustawienia drużyn - Familiada");

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception e) {
            System.err.println("Nie można załadować ikony: " + e.getMessage());
        }

        // Initialize components
        teamSizeComboBox = new ComboBox<>();
        categoryComboBox = new ComboBox<>();
        answerTimeSpinner = new Spinner<>(10, 120, 30, 5);

        team1Field = new TextField();
        team2Field = new TextField();
        team1MembersPanel = new VBox(10);
        team2MembersPanel = new VBox(10);

        // Initialize panels
        settingsPanel = new VBox(20);
        initializeMainPanel();
        initializeSettingsPanel();

        Scene scene = new Scene(mainPanel, 900, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }
        stage.setScene(scene);
    }

    private void initializeMainPanel() {
        // Gradient tła
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );

        mainPanel = new VBox(30);
        mainPanel.setPadding(new Insets(30));
        mainPanel.setAlignment(Pos.TOP_CENTER);
        mainPanel.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // Nagłówek
        Label titleLabel = new Label("FAMILIADA");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));

        // Team 1 panel
        VBox team1Panel = createTeamPanel("Drużyna 1", team1Field, team1MembersPanel);

        // Team 2 panel
        VBox team2Panel = createTeamPanel("Drużyna 2", team2Field, team2MembersPanel);

        // Buttons
        HBox buttonPanel = new HBox(20);
        buttonPanel.setAlignment(Pos.CENTER);
        Button startButton = createStyledButton("Rozpocznij grę", "#4CAF50");
        Button settingsButton = createStyledButton("Ustawienia", "#2196F3");
        buttonPanel.getChildren().addAll(startButton, settingsButton);

        mainPanel.getChildren().addAll(
                titleLabel,
                team1Panel,
                team2Panel,
                buttonPanel
        );

        // Event handlers
        startButton.setOnAction(e -> startGame());
        settingsButton.setOnAction(e -> showSettingsPanel());
    }

    private VBox createTeamPanel(String teamName, TextField teamField, VBox membersPanel) {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(20));
        panel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.1),
                new CornerRadii(15),
                Insets.EMPTY
        )));

        Label teamLabel = new Label(teamName);
        teamLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        teamLabel.setTextFill(Color.WHITE);

        teamField.setPromptText("Wprowadź nazwę drużyny");
        teamField.setStyle("-fx-font-size: 18px; -fx-background-radius: 10;");
        teamField.setPrefHeight(40);

        Label membersLabel = new Label("Członkowie drużyny:");
        membersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        membersLabel.setTextFill(Color.WHITE);

        membersPanel.setPadding(new Insets(10));
        membersPanel.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10;");

        panel.getChildren().addAll(teamLabel, teamField, membersLabel, membersPanel);
        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 16px; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 10 20 10 20;");
        button.setEffect(new DropShadow(5, Color.BLACK));

        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        return button;
    }

    private void initializeSettingsPanel() {
        // Gradient tła
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4b6cb7")),
                new Stop(1, Color.web("#182848"))
        );

        settingsPanel.setPadding(new Insets(30));
        settingsPanel.setAlignment(Pos.TOP_CENTER);
        settingsPanel.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // Nagłówek
        Label titleLabel = new Label("USTAWIENIA");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));

        // Initialize combo boxes
        teamSizeComboBox.getItems().addAll(1, 2, 3, 4, 5, 6);
        teamSizeComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getItems().addAll("Testowa1", "Testowa2");
        categoryComboBox.getSelectionModel().selectFirst();

        // Add settings components
        SettingsPanel settingsContent = new SettingsPanel(teamSizeComboBox, categoryComboBox, answerTimeSpinner);

        // Back button
        Button backButton = createStyledButton("Wróć", "#f44336");
        backButton.setOnAction(e -> {
            updateTeamMembers();
            showMainPanel();
        });

        settingsPanel.getChildren().addAll(titleLabel, settingsContent, backButton);
    }

    private void updateTeamMembers() {
        int teamSize = teamSizeComboBox.getValue();
        updateTeamPanel(team1MembersPanel, teamSize);
        updateTeamPanel(team2MembersPanel, teamSize);
    }

    private void updateTeamPanel(VBox panel, int size) {
        panel.getChildren().clear();
        for (int i = 0; i < size; i++) {
            HBox playerRow = new HBox(10);
            playerRow.setAlignment(Pos.CENTER_LEFT);

            Label playerLabel = new Label("Gracz " + (i + 1) + ":");
            playerLabel.setFont(Font.font("Arial", 16));
            playerLabel.setTextFill(Color.WHITE);

            TextField playerField = new TextField();
            playerField.setPromptText("Imię gracza");
            playerField.setStyle("-fx-font-size: 16px; -fx-background-radius: 10;");
            playerField.setPrefHeight(35);

            playerRow.getChildren().addAll(playerLabel, playerField);
            panel.getChildren().add(playerRow);
        }
    }

    private void startGame() {
        String team1Name = team1Field.getText().trim();
        String team2Name = team2Field.getText().trim();

        if (team1Name.isEmpty() || team2Name.isEmpty()) {
            showErrorAlert("Proszę wypełnić nazwy drużyn", "Błąd");
            return;
        }

        List<String> team1Members = getMembers(team1MembersPanel);
        List<String> team2Members = getMembers(team2MembersPanel);

        if (team1Members != null && team2Members != null) {
            int answerTime = answerTimeSpinner.getValue();

            // Płynne przejście i zamknięcie bieżącego okna
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.close(); // ZAMKNIJ obecne okno przed otwarciem nowego
                new TeamSelectionFrame(categoryComboBox.getValue(), answerTime, team1Name, team2Name).show();
            });
            fadeOut.play();
        }
    }

    private List<String> getMembers(VBox panel) {
        List<String> members = new ArrayList<>();
        for (javafx.scene.Node node : panel.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                for (javafx.scene.Node child : row.getChildren()) {
                    if (child instanceof TextField) {
                        String name = ((TextField) child).getText().trim();
                        if (name.isEmpty()) {
                            showErrorAlert("Wprowadź nazwy wszystkich graczy", "Błąd");
                            return null;
                        }
                        members.add(name);
                    }
                }
            }
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
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów alertu: " + e.getMessage());
        }

        alert.showAndWait();
    }

    private void showMainPanel() {
        stage.getScene().setRoot(mainPanel);
        FadeTransition ft = new FadeTransition(Duration.millis(300), mainPanel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showSettingsPanel() {
        stage.getScene().setRoot(settingsPanel);
        FadeTransition ft = new FadeTransition(Duration.millis(300), settingsPanel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public void show() {
        stage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}