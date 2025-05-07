package org.quizpans;

import javafx.application.Application;
import javafx.stage.Stage;
// Zmień import z MainMenuFrame na SplashScreen
import org.quizpans.gui.SplashScreen;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Uruchom SplashScreen jako pierwszy
        SplashScreen splashScreen = new SplashScreen(primaryStage);
        splashScreen.show();
    }

    public static void main(String[] args) {
        // Ustawienie sterownika JDBC przed uruchomieniem JavaFX (dobre praktyki)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Błąd: Nie znaleziono sterownika MySQL JDBC!");
            // Można tu zakończyć aplikację lub pokazać błąd graficzny później
            // Platform.exit();
        }
        launch(args);
    }
}