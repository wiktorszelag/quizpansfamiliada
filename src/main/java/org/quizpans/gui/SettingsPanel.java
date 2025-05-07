package org.quizpans.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SettingsPanel extends VBox {
    public SettingsPanel(ComboBox<Integer> teamSizeComboBox,
                         ComboBox<String> categoryComboBox,
                         Spinner<Integer> answerTimeSpinner) {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        // Gradient tła
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4b6cb7")),
                new Stop(1, Color.web("#182848"))
        );
        setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        initializeComponents(teamSizeComboBox, categoryComboBox, answerTimeSpinner);
    }

    private void initializeComponents(ComboBox<Integer> teamSizeComboBox,
                                      ComboBox<String> categoryComboBox,
                                      Spinner<Integer> answerTimeSpinner) {
        teamSizeComboBox.setStyle("-fx-font-size: 16px; -fx-background-radius: 10;");
        categoryComboBox.setStyle("-fx-font-size: 16px; -fx-background-radius: 10;");
        answerTimeSpinner.setStyle("-fx-font-size: 16px; -fx-background-radius: 10;");

        getChildren().addAll(
                createSettingRow("Liczba osób w drużynie:", teamSizeComboBox),
                createSettingRow("Wybierz kategorię:", categoryComboBox),
                createSettingRow("Czas na odpowiedź (sekundy):", answerTimeSpinner)
        );
    }

    private HBox createSettingRow(String labelText, Control control) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.WHITE);
        label.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));

        control.setPrefSize(200, 35);

        row.getChildren().addAll(label, control);
        return row;
    }
}