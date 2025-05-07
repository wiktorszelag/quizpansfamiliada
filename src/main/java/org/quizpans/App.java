package org.quizpans;

import javafx.application.Application;
import javafx.stage.Stage;
import org.quizpans.gui.TeamSetupFrame;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Przekazujemy primaryStage do TeamSetupFrame
        TeamSetupFrame teamSetupFrame = new TeamSetupFrame(primaryStage);
        teamSetupFrame.show(); // show() w TeamSetupFrame powinno teraz obsługiwać wyświetlanie
    }

    public static void main(String[] args) {
        launch(args);
    }
}