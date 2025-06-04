package org.quizpans.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert; // Import dla Alert
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.quizpans.gui.onlinegame.OnlineLobbyChoiceFrame;
import org.quizpans.services.OnlineService; // Import dla OnlineService
import org.quizpans.utils.AutoClosingAlerts; // Import dla AutoClosingAlerts

import java.io.InputStream;
import java.util.Set;

public class MainMenuFrame {

    private Stage stage;
    private VBox rootPane;
    private Set<String> availableCategories; // Kategorie przekazywane ze SplashScreen
    private OnlineService onlineService;     // Serwis do obsługi logiki online

    public MainMenuFrame(Stage primaryStage, Set<String> availableCategories) {
        this.stage = primaryStage;
        this.availableCategories = availableCategories; // Przechowujemy kategorie dla gry lokalnej
        this.stage.setTitle("QuizPans - Menu Główne");
        setApplicationIcon();
        initializeMainMenu();
    }

    // Konstruktor używany np. przy powrocie z innego ekranu, gdzie kategorie już są (lub nie są potrzebne)
    public MainMenuFrame(Stage primaryStage) {
        this(primaryStage, null); // Jeśli kategorie nie są od razu dostępne, można je ustawić później lub obsłużyć inaczej
    }

    private void setApplicationIcon() {
        try {
            InputStream logoStream = getClass().getResourceAsStream("/logo.png");
            if (logoStream == null) {
                System.err.println("MainMenuFrame: Nie znaleziono ikony logo.png");
                return;
            }
            Image appIcon = new Image(logoStream);
            if (appIcon.isError()) {
                System.err.println("MainMenuFrame: Błąd podczas ładowania ikony aplikacji.");
                if (appIcon.getException() != null) {
                    appIcon.getException().printStackTrace();
                }
                logoStream.close();
                return;
            }
            if (stage.getIcons().isEmpty()) {
                stage.getIcons().add(appIcon);
            }
            logoStream.close();
        } catch (Exception e) {
            System.err.println("MainMenuFrame: Wyjątek podczas ładowania ikony aplikacji: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeMainMenu() {
        rootPane = new VBox(30); // Zwiększony odstęp dla lepszego wyglądu
        rootPane.setAlignment(Pos.CENTER);
        rootPane.setPadding(new Insets(50)); // Zwiększony padding
        rootPane.setFillWidth(true);
        VBox.setVgrow(rootPane, Priority.ALWAYS);

        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")), // Ciemnoniebieski
                new Stop(1, Color.web("#b21f1f"))  // Ciemnoczerwony
        );
        rootPane.setBackground(new Background(new BackgroundFill(backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label titleLabel = new Label("QuizPans");
        titleLabel.getStyleClass().add("main-menu-title"); // Styl z styles.css

        Button localGameButton = createMenuButton("Rozgrywka Lokalna");
        localGameButton.setOnAction(e -> {
            // Upewniamy się, że kategorie są dostępne przed przejściem
            if (this.availableCategories == null || this.availableCategories.isEmpty()) {
                // Próba ponownego pobrania lub pokazanie błędu
                // W tym miejscu zakładamy, że SplashScreen przekazał kategorie.
                // Jeśli nie, trzeba by dodać logikę ich ponownego załadowania lub obsługi błędu.
                System.err.println("MainMenuFrame: Brak dostępnych kategorii do rozpoczęcia gry lokalnej!");
                AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd Gry Lokalnej",
                        "Nie można rozpocząć gry.",
                        "Kategorie pytań nie zostały załadowane. Spróbuj ponownie uruchomić aplikację.",
                        Duration.seconds(5));
                return;
            }
            switchToTeamSetupView(this.availableCategories);
        });

        Button onlineGameButton = createMenuButton("Starcie Online");
        onlineGameButton.setOnAction(e -> {
            System.out.println("MainMenuFrame: Kliknięto Starcie Online");

            if (this.onlineService == null) {
                String serverWebSocketUrl = "ws://localhost:8080/lobby"; // Upewnij się, że to poprawny adres
                this.onlineService = new OnlineService(serverWebSocketUrl);
            }

            if (!this.onlineService.isOpen()) {
                this.onlineService.connect(); // Próba połączenia jest asynchroniczna
            }

            OnlineLobbyChoiceFrame lobbyChoiceScreen = new OnlineLobbyChoiceFrame(
                    this.stage,
                    this.onlineService,
                    this::switchToMainMenuView // Akcja powrotu do tego menu
            );
            // Przełączamy widok. OnlineLobbyChoiceFrame sam obsłuży wyświetlanie
            // stanu ładowania lub błędów połączenia.
            switchToView(lobbyChoiceScreen.getView());
        });

        Button gameSettingsButton = createMenuButton("Ustawienia");
        gameSettingsButton.setOnAction(e -> switchToGameSettingsView());

        Button aboutButton = createMenuButton("O Quizpans");
        aboutButton.setOnAction(e -> switchToAboutView());

        Button exitButton = createMenuButton("Zakończ Grę");
        exitButton.setOnAction(e -> {
            if (onlineService != null && onlineService.isOpen()) {
                onlineService.disconnect(); // Ważne: rozłącz WebSocket przed zamknięciem
            }
            Platform.exit();
        });

        VBox buttonBox = new VBox(20, localGameButton, onlineGameButton, gameSettingsButton, aboutButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(400); // Ograniczenie szerokości kontenera przycisków

        rootPane.getChildren().addAll(titleLabel, buttonBox);

        // Upewniamy się, że scena jest zmaksymalizowana, jeśli to pożądane
        this.stage.setMaximized(true);
        this.stage.setResizable(true); // Pozwól na zmianę rozmiaru, jeśli użytkownik wyjdzie z trybu pełnoekranowego
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("main-menu-button"); // Styl z styles.css
        // Można dodać dodatkowe efekty hover/pressed bezpośrednio tutaj, jeśli style CSS nie wystarczą
        // button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #...;"));
        // button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #...;"));
        return button;
    }

    private void switchToView(Parent newRoot) {
        if (stage == null || stage.getScene() == null) {
            System.err.println("MainMenuFrame: Stage lub Scene jest null w switchToView. Nie można przełączyć widoku.");
            // Można rozważyć utworzenie nowej sceny, jeśli to konieczne, ale zazwyczaj stage.getScene() powinno istnieć
            if (stage != null && newRoot != null) {
                Scene tempScene = new Scene(newRoot, stage.getWidth(), stage.getHeight()); // Użyj wymiarów stage
                applyStylesToScene(tempScene);
                stage.setScene(tempScene);
                fadeInNewRoot(newRoot);
            }
            return;
        }

        Scene currentScene = this.stage.getScene();
        Parent currentRootNode = currentScene.getRoot();

        if (currentRootNode == newRoot) {
            System.out.println("MainMenuFrame: Próba przełączenia na ten sam widok.");
            return; // Już na tym widoku
        }

        if (currentRootNode == null) { // Jeśli scena nie ma roota, po prostu ustaw nowy
            currentScene.setRoot(newRoot);
            applyStylesToScene(currentScene); // Upewnij się, że style są załadowane
            fadeInNewRoot(newRoot);
            return;
        }

        // Animacja przejścia
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRootNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            currentScene.setRoot(newRoot);
            applyStylesToScene(currentScene); // Zastosuj style po ustawieniu nowego roota
            fadeInNewRoot(newRoot);
        });
        fadeOut.play();
    }

    private void fadeInNewRoot(Parent newRoot) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void applyStylesToScene(Scene scene) {
        if (scene == null) return;
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            if (cssPath != null) {
                if (scene.getStylesheets().isEmpty() || !scene.getStylesheets().contains(cssPath)) {
                    scene.getStylesheets().clear(); // Wyczyść stare style, jeśli są, aby uniknąć konfliktów
                    scene.getStylesheets().add(cssPath);
                }
            } else {
                System.err.println("MainMenuFrame: Nie znaleziono pliku styles.css");
            }
        } catch (Exception e) {
            System.err.println("MainMenuFrame: Błąd ładowania CSS dla sceny: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void switchToTeamSetupView(Set<String> categories) { // Metoda teraz przyjmuje kategorie
        if (categories == null || categories.isEmpty()) {
            System.err.println("MainMenuFrame: Próba przejścia do TeamSetupView, ale 'availableCategories' jest null lub puste!");
            AutoClosingAlerts.show(stage, Alert.AlertType.ERROR, "Błąd Ładowania Kategori",
                    "Nie można skonfigurować gry lokalnej.",
                    "Lista kategorii pytań nie została załadowana. Spróbuj ponownie uruchomić aplikację.",
                    Duration.seconds(5));
            return;
        }
        TeamSetupFrame teamSetupFrame = new TeamSetupFrame(this.stage, categories, this::switchToMainMenuView);
        Parent teamSetupRoot = teamSetupFrame.getRootPane();
        switchToView(teamSetupRoot);
    }

    private void switchToGameSettingsView() {
        SettingsGameFrame settingsGameFrame = new SettingsGameFrame(this::switchToMainMenuView, this.stage);
        Parent settingsGameRoot = settingsGameFrame.getView();
        switchToView(settingsGameRoot);
    }

    private void switchToAboutView() {
        AboutFrame aboutFrame = new AboutFrame(this::switchToMainMenuView);
        Parent aboutPane = aboutFrame.getView();
        switchToView(aboutPane);
    }

    // Metoda publiczna do przełączania z powrotem na ten ekran (MainMenu)
    public void switchToMainMenuView() {
        if (this.rootPane == null) {
            // Jeśli rootPane nie został jeszcze zainicjalizowany (co nie powinno się zdarzyć, jeśli menu było już pokazane)
            initializeMainMenu();
        }
        switchToView(this.rootPane);
    }

    public void show() {
        if (this.stage.getScene() == null) {
            Scene scene = new Scene(this.rootPane); // Tworzymy nową scenę z rootPane
            applyStylesToScene(scene); // Stosujemy style do nowej sceny
            this.stage.setScene(scene);
        } else {
            this.stage.getScene().setRoot(this.rootPane); // Upewniamy się, że root jest ustawiony
            applyStylesToScene(this.stage.getScene());    // Stosujemy/aktualizujemy style istniejącej sceny
        }
        this.stage.show();
        this.stage.setMaximized(true); // Upewniamy się, że jest zmaksymalizowane
    }

    public VBox getRootPane() {
        if (this.rootPane == null) {
            // Leniwa inicjalizacja, jeśli getRootPane jest wywoływane przed show() lub konstruktorem
            initializeMainMenu();
        }
        return this.rootPane;
    }
}