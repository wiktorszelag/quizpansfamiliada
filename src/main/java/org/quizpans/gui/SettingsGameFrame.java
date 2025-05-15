package org.quizpans.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
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
import org.quizpans.utils.UsedQuestionsLogger; // Dodany import
import org.quizpans.utils.AutoClosingAlerts; // Dodany import dla alertów
import javafx.util.Duration; // Dodany import

import java.util.Optional;

public class SettingsGameFrame {

    private VBox view;
    private Runnable backAction;
    private Stage ownerStage; // Dodajemy pole na stage dla alertów

    public SettingsGameFrame(Runnable onBack, Stage ownerStage) { // Modyfikacja konstruktora
        this.backAction = onBack;
        this.ownerStage = ownerStage; // Przechowujemy referencję do stage
        createView();
    }

    private LinearGradient createBackgroundGradient() {
        return new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(40));
        view.setAlignment(Pos.CENTER);
        view.setFillWidth(true);
        VBox.setVgrow(view, Priority.ALWAYS);
        view.setBackground(new Background(new BackgroundFill(createBackgroundGradient(), CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("Ustawienia Gry");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setEffect(new DropShadow(15, Color.BLACK));

        Label placeholderLabel = new Label("Tutaj pojawią się opcje ustawień globalnych gry.");
        placeholderLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        placeholderLabel.setTextFill(Color.WHITE);
        placeholderLabel.setWrapText(true);
        placeholderLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(placeholderLabel, new Insets(20, 0, 20, 0));

        Button clearUsedQuestionsButton = new Button("Wyczyść historię użytych pytań");
        clearUsedQuestionsButton.getStyleClass().add("main-menu-button");
        clearUsedQuestionsButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 350px; -fx-background-color: #d32f2f;"); // Czerwony przycisk
        clearUsedQuestionsButton.setOnAction(e -> confirmClearUsedQuestions());
        VBox.setMargin(clearUsedQuestionsButton, new Insets(10,0,20,0));


        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Wróć");
        backButton.getStyleClass().add("main-menu-button");
        backButton.setStyle("-fx-font-size: 18px; -fx-pref-width: 200px;");
        backButton.setOnAction(e -> {
            if (backAction != null) {
                backAction.run();
            }
        });

        VBox settingsOptionsBox = new VBox(15); // Kontener na opcje ustawień
        settingsOptionsBox.setAlignment(Pos.CENTER);
        settingsOptionsBox.getChildren().addAll(placeholderLabel, clearUsedQuestionsButton);

        VBox contentBox = new VBox(30, titleLabel, settingsOptionsBox, spacer, backButton);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(600);

        view.getChildren().add(contentBox);
    }

    private void confirmClearUsedQuestions() {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Potwierdzenie");
        confirmationAlert.setHeaderText("Wyczyścić historię użytych pytań?");
        confirmationAlert.setContentText("Ta operacja usunie zapisane ID wszystkich dotychczas użytych pytań. Pytania będą mogły pojawić się ponownie.\n\nCzy na pewno chcesz kontynuować?");

        if (ownerStage != null) { // Ustaw ownera dla alertu
            confirmationAlert.initOwner(ownerStage);
        }

        try {
            DialogPane dialogPane = confirmationAlert.getDialogPane();
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
            }
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Błąd ładowania CSS dla alertu potwierdzenia: " + e.getMessage());
        }

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            UsedQuestionsLogger.clearUsedQuestionsLog();
            AutoClosingAlerts.show(
                    ownerStage, // Przekaż ownera
                    Alert.AlertType.INFORMATION,
                    "Sukces",
                    null,
                    "Historia użytych pytań została wyczyszczona.",
                    Duration.seconds(3)
            );
        }
    }

    public Parent getView() {
        return view;
    }
}