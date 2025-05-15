package org.quizpans.utils;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;

import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundLoader {

    private static final AtomicBoolean modelLoadingInitiated = new AtomicBoolean(false);
    private static Task<Void> modelLoadingTask = null;

    private static final ReadOnlyBooleanWrapper modelsReady = new ReadOnlyBooleanWrapper(false);
    private static final ReadOnlyBooleanWrapper modelsLoadingComplete = new ReadOnlyBooleanWrapper(false);
    private static final ReadOnlyBooleanWrapper modelsLoadingFailed = new ReadOnlyBooleanWrapper(false);

    public static ReadOnlyBooleanProperty modelsReadyProperty() {
        return modelsReady.getReadOnlyProperty();
    }

    public static ReadOnlyBooleanProperty modelsLoadingCompleteProperty() {
        return modelsLoadingComplete.getReadOnlyProperty();
    }
    public static ReadOnlyBooleanProperty modelsLoadingFailedProperty() {
        return modelsLoadingFailed.getReadOnlyProperty();
    }

    public static synchronized void ensureModelLoadingInitiated() {
        if (!modelLoadingInitiated.compareAndSet(false, true)) {
            return;
        }

        modelLoadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                boolean currentModelsOk = false;
                try {
                    updateMessage("Inicjalizacja modeli językowych (w tle)...");
                    TextNormalizer.normalizeToBaseForm("");
                    boolean spellCheckerOk = SpellCheckerService.isInitialized();
                    boolean synonymsOk = SynonymManager.isLoaded();
                    currentModelsOk = spellCheckerOk && synonymsOk;

                    final boolean finalModelsOk = currentModelsOk;
                    Platform.runLater(() -> modelsReady.set(finalModelsOk));

                    if (!finalModelsOk) {
                        System.err.println("BackgroundLoader: Nie wszystkie modele językowe zainicjalizowane poprawnie.");
                        updateMessage("Problem z inicjalizacją modeli...");
                        Thread.sleep(200);
                    }
                    updateMessage("Modele językowe gotowe (załadowane w tle).");
                    return null;

                } catch (Exception e) {
                    System.err.println("Krytyczny błąd podczas ładowania modeli językowych w tle: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> modelsLoadingFailed.set(true));
                    updateMessage("Błąd ładowania modeli!");
                    throw e;
                } finally {
                    Platform.runLater(() -> modelsLoadingComplete.set(true));
                }
            }
        };

        modelLoadingTask.setOnFailed(workerStateEvent -> {
            System.err.println("BackgroundLoader Task (Models) FAILED.");
            Platform.runLater(() -> {
                modelsLoadingFailed.set(true);
                modelsLoadingComplete.set(true);
            });
        });

        modelLoadingTask.setOnCancelled(workerStateEvent -> {
            System.err.println("BackgroundLoader Task (Models) CANCELLED.");
            Platform.runLater(() -> {
                modelsLoadingFailed.set(true);
                modelsLoadingComplete.set(true);
            });
        });

        modelLoadingTask.setOnSucceeded(workerStateEvent -> {
            System.out.println("BackgroundLoader Task (Models) SUCCEEDED.");
        });

        Thread backgroundThread = new Thread(modelLoadingTask);
        backgroundThread.setDaemon(true);
        backgroundThread.setName("NLP-Model-Background-Loader");
        backgroundThread.start();
    }
}