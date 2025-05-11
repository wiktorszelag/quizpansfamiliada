package org.quizpans.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.util.Duration;
import java.util.Optional;

public class AutoClosingAlerts {

    public static Optional<ButtonType> show(Window owner, Alert.AlertType alertType, String title, String headerText, String contentText, Duration autoCloseDuration, ButtonType... buttons) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        if (owner != null) {
            alert.initOwner(owner);
        }

        if (buttons != null && buttons.length > 0) {
            alert.getButtonTypes().setAll(buttons);
        }

        try {
            DialogPane dialogPane = alert.getDialogPane();
            String cssPath = AutoClosingAlerts.class.getResource("/styles.css").toExternalForm();
            if (cssPath != null && !dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
            }
            dialogPane.getStyleClass().add("custom-alert");
        } catch (Exception e) {
            System.err.println("Nie udało się załadować CSS dla AutoClosingAlerts: " + e.getMessage());
        }

        Timeline timeline = null;
        if (autoCloseDuration != null && !autoCloseDuration.isUnknown() && !autoCloseDuration.isIndefinite() && autoCloseDuration.greaterThan(Duration.ZERO)) {
            timeline = new Timeline(new KeyFrame(autoCloseDuration, ae -> {
                if (alert.isShowing()) {
                    alert.close();
                }
            }));
            timeline.setCycleCount(1);

            final Timeline finalTimeline = timeline;
            alert.setOnShown(event -> {
                if (finalTimeline != null) {
                    finalTimeline.playFromStart();
                }
            });
        }

        Optional<ButtonType> result = alert.showAndWait();

        if (timeline != null) {
            timeline.stop();
        }

        return result;
    }
}