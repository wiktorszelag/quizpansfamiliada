package org.quizpans;

import javafx.application.Application;
import javafx.stage.Stage;
import org.quizpans.gui.SplashScreen;
import org.quizpans.utils.BackgroundLoader;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        BackgroundLoader.ensureModelLoadingInitiated();
        SplashScreen splashScreen = new SplashScreen(primaryStage);
        splashScreen.show();
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Błąd: Nie znaleziono sterownika MySQL JDBC!");
        }
        launch(args);
    }
}