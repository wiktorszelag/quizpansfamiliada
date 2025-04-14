package org.quizpans;

import javafx.application.Application;
import javafx.stage.Stage;
import org.quizpans.gui.TeamSetupFrame;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        TeamSetupFrame teamSetupFrame = new TeamSetupFrame();
        teamSetupFrame.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}