package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint; // Import dla Paint
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private ScrollPane scrollPane;

    public TeamSetupFrame() {
        stage = new Stage();
        stage.setTitle("Ustawienia drużyn - Familiada");

        stage.setResizable(true);
        stage.setMinWidth(850);
        // --- ZMIANA: Usunięcie setMinHeight ---
        // stage.setMinHeight(700);
        // --- KONIEC ZMIANY ---


        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream != null) {
                stage.getIcons().add(new Image(logoStream));
                logoStream.close();
            } else {
                System.err.println("Nie można załadować ikony: /logo.png");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować ikony: " + e.getMessage());
        }

        teamSizeComboBox = new ComboBox<>();
        categoryComboBox = new ComboBox<>();
        answerTimeSpinner = new Spinner<>(10, 120, 30, 5);

        team1Field = new TextField();
        team2Field = new TextField();
        team1MembersPanel = new VBox(10);
        team2MembersPanel = new VBox(10);

        settingsPanel = new VBox(20);
        initializeMainPanel(); // Tworzy mainPanel (VBox)
        initializeSettingsPanel();

        scrollPane = new ScrollPane();
        scrollPane.setContent(mainPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("no-scroll-bar");
        // --- ZMIANA: Ustawienie przezroczystego tła dla ScrollPane ---
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        // --- KONIEC ZMIANY ---

        // ScrollPane jest rootem sceny, scena nie ma rozmiaru
        Scene scene = new Scene(scrollPane);

        // --- ZMIANA: Definicja gradientu i ustawienie go jako tło Sceny ---
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        scene.setFill(gradient); // Ustawienie wypełnienia sceny
        // --- KONIEC ZMIANY ---


        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            } else {
                System.err.println("Nie znaleziono pliku /styles.css");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować arkusza stylów: " + e.getMessage());
        }

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());
        stage.centerOnScreen();

        stage.setScene(scene);
        updateTeamMembers();
    }

    private void initializeMainPanel() {
        mainPanel = new VBox(30);
        mainPanel.setPadding(new Insets(30));
        mainPanel.setAlignment(Pos.TOP_CENTER);
        // --- ZMIANA: Ustawienie przezroczystego tła dla VBox ---
        mainPanel.setStyle("-fx-background-color: transparent;");
        // --- KONIEC ZMIANY ---
        mainPanel.setMinHeight(Region.USE_PREF_SIZE); // Pozwól rosnąć/kurczyć się


        Label titleLabel = new Label("FAMILIADA");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        HBox teamDisplayArea = new HBox(30);
        teamDisplayArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(teamDisplayArea, Priority.ALWAYS); // Pozwól HBox rosnąć pionowo


        VBox team1Panel = createTeamPanel("Drużyna 1", team1Field, team1MembersPanel);
        VBox team2Panel = createTeamPanel("Drużyna 2", team2Field, team2MembersPanel);

        HBox.setHgrow(team1Panel, Priority.ALWAYS);
        HBox.setHgrow(team2Panel, Priority.ALWAYS);

        teamDisplayArea.getChildren().addAll(team1Panel, team2Panel);

        HBox buttonPanel = new HBox(25);
        buttonPanel.setAlignment(Pos.CENTER);
        Button startButton = createStyledButton("Rozpocznij grę", "#4CAF50");
        Button settingsButton = createStyledButton("Ustawienia", "#2196F3");
        buttonPanel.getChildren().addAll(startButton, settingsButton);
        VBox.setMargin(buttonPanel, new Insets(20, 0, 10, 0));

        mainPanel.getChildren().addAll(
                titleLabel,
                teamDisplayArea,
                buttonPanel
        );

        startButton.setOnAction(e -> startGame());
        settingsButton.setOnAction(e -> showSettingsPanel());
    }

    private VBox createTeamPanel(String teamName, TextField teamField, VBox membersPanel) {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));
        panel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.15),
                new CornerRadii(15),
                Insets.EMPTY
        )));
        panel.setStyle("-fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-border-width: 1;");
        panel.setMinWidth(300);

        Label teamLabel = new Label(teamName);
        teamLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        teamLabel.setTextFill(Color.WHITE);
        VBox.setMargin(teamLabel, new Insets(0, 0, 10, 0));

        teamField.setPromptText("Wprowadź nazwę drużyny");
        teamField.setPrefHeight(40);
        teamField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(teamField, new Insets(0, 5, 15, 5));

        Label membersLabel = new Label("Członkowie drużyny:");
        membersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        membersLabel.setTextFill(Color.WHITE);
        VBox.setMargin(membersLabel, new Insets(10, 0, 5, 0));

        membersPanel.setPadding(new Insets(10));
        membersPanel.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-background-radius: 10;");
        VBox.setVgrow(membersPanel, Priority.ALWAYS); // Pozwól panelowi graczy rosnąć


        panel.getChildren().addAll(teamLabel, teamField, membersLabel, membersPanel);
        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18px; " +
                "-fx-background-radius: 20; " +
                "-fx-padding: 12 25 12 25;");
        button.setEffect(new DropShadow(5, Color.BLACK));
        button.setPrefWidth(180);

        button.setOnMouseEntered(e -> button.setScaleX(1.05));
        button.setOnMouseExited(e -> button.setScaleX(1.0));

        return button;
    }

    private void initializeSettingsPanel() {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4b6cb7")),
                new Stop(1, Color.web("#182848"))
        );

        settingsPanel.setPadding(new Insets(30));
        settingsPanel.setAlignment(Pos.TOP_CENTER);
        settingsPanel.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("USTAWIENIA");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        VBox.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        teamSizeComboBox.getItems().addAll(1, 2, 3, 4, 5, 6);
        teamSizeComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getItems().addAll("Testowa1", "Testowa2");
        categoryComboBox.getSelectionModel().selectFirst();

        SettingsPanel settingsContent = new SettingsPanel(teamSizeComboBox, categoryComboBox, answerTimeSpinner);
        VBox.setVgrow(settingsContent, Priority.ALWAYS);

        Button backButton = createStyledButton("Wróć", "#f44336");
        backButton.setOnAction(e -> {
            updateTeamMembers();
            showMainPanel();
        });
        VBox.setMargin(backButton, new Insets(30, 0, 0, 0));

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
            playerLabel.setMinWidth(Control.USE_PREF_SIZE);

            TextField playerField = new TextField();
            playerField.setPromptText("Imię gracza");
            playerField.setPrefHeight(35);
            HBox.setHgrow(playerField, Priority.ALWAYS);

            playerRow.getChildren().addAll(playerLabel, playerField);
            panel.getChildren().add(playerRow);
        }
    }

    private void startGame() {
        String team1Name = team1Field.getText().trim();
        String team2Name = team2Field.getText().trim();

        if (team1Name.isEmpty()) team1Name = "Drużyna 1";
        if (team2Name.isEmpty()) team2Name = "Drużyna 2";

        List<String> team1Members = getMembers(team1MembersPanel, 1);
        List<String> team2Members = getMembers(team2MembersPanel, 2);

        if (team1Members.isEmpty() || team2Members.isEmpty()) {
            showErrorAlert("Obie drużyny muszą mieć co najmniej jednego gracza.", "Błąd");
            return;
        }

        int answerTime = answerTimeSpinner.getValue();
        String selectedCategory = categoryComboBox.getValue();

        if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
            showErrorAlert("Proszę wybrać kategorię pytań.", "Błąd");
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        String finalTeam1Name = team1Name;
        String finalTeam2Name = team2Name;
        fadeOut.setOnFinished(e -> {
            stage.close();
            new TeamSelectionFrame(selectedCategory, answerTime, finalTeam1Name, finalTeam2Name, team1Members, team2Members).show();
        });
        fadeOut.play();
    }

    private List<String> getMembers(VBox panel, int teamNumber) {
        List<String> members = new ArrayList<>();
        int playerIndex = 1;
        for (javafx.scene.Node node : panel.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                for (javafx.scene.Node child : row.getChildren()) {
                    if (child instanceof TextField) {
                        String name = ((TextField) child).getText().trim();
                        if (name.isEmpty()) {
                            name = "Gracz " + playerIndex + " (D" + teamNumber + ")";
                        }
                        members.add(name);
                        playerIndex++;
                        break;
                    }
                }
            }
        }
        if (members.isEmpty() && !panel.getChildren().isEmpty()) {
            if(playerIndex == 1) members.add("Gracz 1 (D" + teamNumber + ")");
        } else if (members.isEmpty()) {
            return Collections.emptyList();
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
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
                dialogPane.getStyleClass().add("custom-alert");
            } else {
                System.err.println("Nie znaleziono pliku stylów dla alertu: /styles.css");
            }
        } catch (Exception e) {
            System.err.println("Nie można załadować stylów alertu: " + e.getMessage());
        }

        alert.showAndWait();
    }

    private void showMainPanel() {
        stage.getScene().setRoot(scrollPane);
        FadeTransition ft = new FadeTransition(Duration.millis(300), scrollPane);
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
        if (stage.getScene() != null && scrollPane != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), scrollPane);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }
}