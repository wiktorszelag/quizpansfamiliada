package org.quizpans.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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

public class AboutFrame {

    private VBox view;
    private Runnable backAction;

    public AboutFrame(Runnable onBack) {
        this.backAction = onBack;
        createView();
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(40));
        view.setAlignment(Pos.CENTER);
        view.setFillWidth(true);
        VBox.setVgrow(view, Priority.ALWAYS);

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(1, Color.web("#b21f1f"))
        );
        view.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label aboutLabel = new Label("O Quizpans");
        aboutLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        aboutLabel.setTextFill(Color.WHITE);
        aboutLabel.setEffect(new DropShadow(10, Color.BLACK));

        Label infoPlaceholder = new Label("Wersja 1.0\nCopyright © 2025");
        infoPlaceholder.setFont(Font.font("System", 16));
        infoPlaceholder.setTextFill(Color.LIGHTGRAY);
        infoPlaceholder.setAlignment(Pos.CENTER);
        VBox.setMargin(infoPlaceholder, new Insets(20, 0, 0, 0));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Wróć");
        backButton.getStyleClass().add("main-menu-button");
        backButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-min-width: 150px; -fx-padding: 10px 20px;");
        backButton.setOnAction(e -> {
            if (backAction != null) {
                backAction.run();
            }
        });

        view.getChildren().addAll(aboutLabel, infoPlaceholder, spacer, backButton);
    }

    public Parent getView() {
        return view;
    }
}