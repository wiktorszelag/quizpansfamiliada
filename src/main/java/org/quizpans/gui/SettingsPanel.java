package org.quizpans.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SettingsPanel extends VBox {

    public SettingsPanel(ComboBox<Integer> teamSizeComboBox,
                         ComboBox<String> categoryComboBox,
                         Slider answerTimeSlider) {

        setPadding(new Insets(40));
        setSpacing(25);
        setAlignment(Pos.CENTER);
        setMaxWidth(700);

        initializeComponents(teamSizeComboBox, categoryComboBox, answerTimeSlider);
    }

    private void initializeComponents(ComboBox<Integer> teamSizeComboBox,
                                      ComboBox<String> categoryComboBox,
                                      Slider answerTimeSlider) {

        getChildren().addAll(
                createSettingRow("Liczba osób w drużynie:", teamSizeComboBox),
                createSettingRow("Wybierz kategorię:", categoryComboBox),
                createAnswerTimeSettingRow("Czas na odpowiedź:", answerTimeSlider)
        );
    }

    private HBox createSettingRow(String labelText, Node controlNode) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setFont(Font.font("System", FontWeight.NORMAL, 18));
        label.setTextFill(Color.WHITE);

        StackPane labelWrapper = new StackPane(label);
        labelWrapper.setPrefWidth(280);
        StackPane.setAlignment(label, Pos.CENTER_LEFT);

        if (controlNode instanceof Control) {
            Control control = (Control) controlNode;
            control.setPrefWidth(300);
        }

        row.getChildren().addAll(labelWrapper, controlNode);
        return row;
    }

    private HBox createAnswerTimeSettingRow(String labelText, Slider slider) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);

        Label descriptionLabel = new Label(labelText);
        descriptionLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        descriptionLabel.setTextFill(Color.WHITE);

        StackPane labelWrapper = new StackPane(descriptionLabel);
        labelWrapper.setPrefWidth(280);
        StackPane.setAlignment(descriptionLabel, Pos.CENTER_LEFT);

        Label valueLabel = new Label(String.format("%.0f s", slider.getValue()));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setMinWidth(55);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);


        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format("%.0f s", newVal.doubleValue()));
        });

        HBox sliderWithValueBox = new HBox(10);
        sliderWithValueBox.setAlignment(Pos.CENTER_LEFT);
        sliderWithValueBox.setPrefWidth(300);

        slider.prefWidthProperty().bind(sliderWithValueBox.widthProperty().subtract(valueLabel.widthProperty()).subtract(sliderWithValueBox.getSpacing()));


        sliderWithValueBox.getChildren().addAll(slider, valueLabel);

        row.getChildren().addAll(labelWrapper, sliderWithValueBox);
        return row;
    }
}