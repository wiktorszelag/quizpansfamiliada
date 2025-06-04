package org.quizpans.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.quizpans.gui.MainMenuFrame;
import org.quizpans.gui.onlinegame.OnlineLobbyChoiceFrame;
import org.quizpans.gui.onlinegame.OnlineLobbyHostConfigFrame;
import org.quizpans.gui.onlinegame.OnlineGamePrepFrame;
import org.quizpans.online.model.OnlineLobbyUIData;
import org.quizpans.online.model.LobbyStateData;
import org.quizpans.online.model.GameSettingsData;
import org.quizpans.online.model.PlayerInfo;
import org.quizpans.utils.AutoClosingAlerts;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OnlineService {

    private WebSocketClient webSocketClient;
    private final String serverUri;
    private final Gson gson = new Gson();

    private final ObservableList<OnlineLobbyUIData> onlineLobbies = FXCollections.observableArrayList();
    private OnlineLobbyHostConfigFrame activeHostConfigFrame;
    private OnlineGamePrepFrame activeGamePrepFrame;
    private String clientSessionId;

    private final ObjectProperty<LobbyStateData> currentlyHostedLobbyState = new SimpleObjectProperty<>(null);

    public OnlineService(String serverUri) {
        this.serverUri = serverUri;
    }

    public ObservableList<OnlineLobbyUIData> getOnlineLobbies() {
        return onlineLobbies;
    }

    public ObjectProperty<LobbyStateData> currentlyHostedLobbyStateProperty() {
        return currentlyHostedLobbyState;
    }

    public void setActiveHostConfigFrame(OnlineLobbyHostConfigFrame frame) {
        this.activeHostConfigFrame = frame;
        if (frame != null) {
            this.activeGamePrepFrame = null;
        }
    }

    public void setActiveGamePrepFrame(OnlineGamePrepFrame frame) {
        this.activeGamePrepFrame = frame;
        if (frame != null) {
            this.activeHostConfigFrame = null;
        }
    }

    public OnlineGamePrepFrame getActiveGamePrepFrame() {
        return activeGamePrepFrame;
    }

    public void clearActiveHostConfigFrame() {
        this.activeHostConfigFrame = null;
    }

    public void clearActiveGamePrepFrame() {
        this.activeGamePrepFrame = null;
    }

    public String getClientSessionId(){
        return clientSessionId;
    }

    public boolean isOpen() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public void connect() {
        if (isOpen()) {
            System.out.println("CLIENT OnlineService: Already connected. Requesting lobbies.");
            Platform.runLater(()-> {
                Map<String, Object> requestLobbiesMessage = new HashMap<>();
                requestLobbiesMessage.put("action", "getAllLobbies");
                sendJsonMessage(requestLobbiesMessage);
            });
            return;
        }
        try {
            webSocketClient = new WebSocketClient(new URI(serverUri)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("CLIENT OnlineService: Połączono z serwerem WebSocket: " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("CLIENT OnlineService RAW MESSAGE RECEIVED: " + message);
                    Platform.runLater(() -> handleServerMessageInternal(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("CLIENT OnlineService: Rozłączono z serwerem WebSocket. Kod: " + code + ", Powód: " + reason);
                    Platform.runLater(() -> {
                        onlineLobbies.clear();
                        currentlyHostedLobbyState.set(null);
                        if (activeHostConfigFrame != null || activeGamePrepFrame != null) {
                            showStatusMessage("Rozłączono z serwerem. Lobby zostało zamknięte.", "error", 8000, true);
                        }
                    });
                    clientSessionId = null;
                    clearActiveHostConfigFrame();
                    clearActiveGamePrepFrame();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("CLIENT OnlineService: Błąd WebSocket: " + ex.getMessage());
                    Platform.runLater(() -> {
                        if (activeHostConfigFrame != null || activeGamePrepFrame != null) {
                            showStatusMessage("Błąd połączenia z serwerem.", "error", 8000, false);
                        }
                    });
                }
            };
            System.out.println("CLIENT OnlineService: Próba połączenia z " + serverUri);
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            System.err.println("CLIENT OnlineService: Niepoprawny URI serwera WebSocket: " + serverUri + " - " + e.getMessage());
        }
    }

    private void handleServerMessageInternal(String message) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> parsedMessage = gson.fromJson(message, mapType);
            String messageType = (String) parsedMessage.get("type");
            System.out.println("CLIENT OnlineService: Parsed message type: " + messageType);


            if ("yourSessionId".equals(messageType) && parsedMessage.containsKey("sessionId")) {
                this.clientSessionId = (String) parsedMessage.get("sessionId");
                System.out.println("CLIENT OnlineService: Moje ID sesji: " + this.clientSessionId);
                Map<String, Object> requestLobbiesMessage = new HashMap<>();
                requestLobbiesMessage.put("action", "getAllLobbies");
                sendJsonMessage(requestLobbiesMessage);
                return;
            }

            if ("allLobbies".equals(messageType) || "allLobbiesUpdate".equals(messageType)) {
                Object lobbiesData = parsedMessage.get("lobbies");
                System.out.println("CLIENT OnlineService: Received " + messageType + " with data: " + lobbiesData);
                if (lobbiesData instanceof List) {
                    List<Map<String, Object>> rawLobbies = (List<Map<String, Object>>) lobbiesData;
                    List<OnlineLobbyUIData> newLobbiesList = rawLobbies.stream()
                            .map(this::parseRawMapToOnlineLobbyUIData)
                            .collect(Collectors.toList());
                    onlineLobbies.setAll(newLobbiesList);
                }
            } else if ("lobbyUpdate".equals(messageType)) {
                Object lobbyDataRaw = parsedMessage.get("lobby");
                System.out.println("CLIENT OnlineService: Received lobbyUpdate. Raw lobby data: " + lobbyDataRaw);
                if (lobbyDataRaw instanceof Map) {
                    Map<String, Object> rawLobbyMap = (Map<String, Object>) lobbyDataRaw;
                    LobbyStateData receivedLobbyState = parseRawMapToClientLobbyStateData(rawLobbyMap);

                    if (receivedLobbyState != null) {
                        System.out.println("CLIENT OnlineService: Parsed LobbyStateData ID: " + receivedLobbyState.getId() + " Waiting: " + receivedLobbyState.getWaitingPlayers() + " Teams: " + receivedLobbyState.getTeams());
                        String receivedLobbyId = receivedLobbyState.getId();

                        onlineLobbies.stream()
                                .filter(l -> l.getId().equals(receivedLobbyId))
                                .findFirst()
                                .ifPresent(uiData -> {
                                    uiData.setStatus(receivedLobbyState.getStatus());
                                    uiData.setPlayerCount(receivedLobbyState.getTotalParticipantCount());
                                    uiData.setMaxPlayers(receivedLobbyState.getMaxParticipants());
                                    int index = onlineLobbies.indexOf(uiData);
                                    if (index != -1) onlineLobbies.set(index, uiData);
                                });

                        if (clientSessionId != null && clientSessionId.equals(receivedLobbyState.getHostSessionId())) {
                            System.out.println("CLIENT OnlineService: This client is host, updating currentlyHostedLobbyState for lobby: " + receivedLobbyId);
                            currentlyHostedLobbyState.set(receivedLobbyState);
                        }

                        if (activeHostConfigFrame != null && activeHostConfigFrame.getLobbyId().equals(receivedLobbyId)) {
                            System.out.println("CLIENT OnlineService: activeHostConfigFrame found for lobby: " + receivedLobbyId + ". Calling updateFullLobbyState.");
                            activeHostConfigFrame.updateFullLobbyState(receivedLobbyState);
                        }

                        if (activeGamePrepFrame != null && activeGamePrepFrame.getLobbyIdInternal() != null && activeGamePrepFrame.getLobbyIdInternal().equals(receivedLobbyId)) {
                            String status = receivedLobbyState.getStatus() != null ? receivedLobbyState.getStatus().toUpperCase() : "UNKNOWN";
                            boolean isHost = clientSessionId != null && clientSessionId.equals(receivedLobbyState.getHostSessionId());

                            if ("BUSY".equals(status) && receivedLobbyState.getCurrentQuestionText() != null && !receivedLobbyState.getCurrentQuestionText().isEmpty() && !receivedLobbyState.getCurrentQuestionText().startsWith("Koniec gry!")) {
                                if ((isHost && activeGamePrepFrame.isWaitingForServerAfterStartSignal()) || (!isHost && activeGamePrepFrame.isPlayerWaitingForGameStart())) {
                                    activeGamePrepFrame.proceedToOnlineGame();
                                }
                            } else if ("AVAILABLE".equals(status) || (receivedLobbyState.getCurrentQuestionText() != null && receivedLobbyState.getCurrentQuestionText().startsWith("Koniec gry!"))) {
                                showStatusMessage("Lobby '" + receivedLobbyState.getName() + "' zakończyło grę lub host opuścił.", "info", 5000, true);
                            }
                        }
                    } else {
                        System.err.println("CLIENT OnlineService: receivedLobbyState is null after parsing.");
                    }
                }
            }  else if ("error".equals(messageType)) {
                String errorMessage = (String) parsedMessage.get("message");
                System.err.println("CLIENT OnlineService: Server error: " + errorMessage);
                showStatusMessage("Serwer: " + errorMessage, "error", 5000, false);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("CLIENT OnlineService: Błąd parsowania JSON od serwera: " + message + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("CLIENT OnlineService: Nieoczekiwany błąd podczas obsługi wiadomości od serwera: " + message + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private OnlineLobbyUIData parseRawMapToOnlineLobbyUIData(Map<String, Object> rawLobby) {
        String id = (String) rawLobby.get("id");
        String name = (String) rawLobby.get("name");
        String status = (rawLobby.get("status") instanceof String) ? (String) rawLobby.get("status") : String.valueOf(rawLobby.get("status"));
        OnlineLobbyUIData uiLobby = new OnlineLobbyUIData(id, name, status);
        if (rawLobby.get("totalParticipantCount") instanceof Number) {
            uiLobby.setPlayerCount(((Number) rawLobby.get("totalParticipantCount")).intValue());
        }
        if (rawLobby.get("maxParticipants") instanceof Number) {
            uiLobby.setMaxPlayers(((Number) rawLobby.get("maxParticipants")).intValue());
        }
        return uiLobby;
    }

    private LobbyStateData parseRawMapToClientLobbyStateData(Map<String, Object> rawLobbyMap) {
        String lobbyJson = gson.toJson(rawLobbyMap);
        Type lobbyStateType = new TypeToken<LobbyStateData>() {}.getType();
        try {
            return gson.fromJson(lobbyJson, lobbyStateType);
        } catch (JsonSyntaxException e) {
            System.err.println("CLIENT OnlineService: Error deserializing to LobbyStateData: " + lobbyJson + " - " + e.getMessage());
            return null;
        }
    }

    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            System.out.println("CLIENT OnlineService: Sending message: " + message);
            webSocketClient.send(message);
        } else {
            System.err.println("CLIENT OnlineService: Nie można wysłać wiadomości - klient WebSocket nie jest połączony.");
        }
    }

    public void sendJsonMessage(Map<String, Object> messageData) {
        String jsonMessage = gson.toJson(messageData);
        sendMessage(jsonMessage);
    }

    public void disconnect() {
        if (webSocketClient != null) {
            if (webSocketClient.isOpen()) {
                webSocketClient.close();
            }
            webSocketClient = null;
        }
        Platform.runLater(() -> {
            onlineLobbies.clear();
            currentlyHostedLobbyState.set(null);
        });
        clientSessionId = null;
        clearActiveHostConfigFrame();
        clearActiveGamePrepFrame();
    }

    private void showStatusMessage(String message, String type, int durationSeconds, boolean switchToLobbyChoiceOnError) {
        Platform.runLater(() -> {
            Stage currentOwner = null;
            List<Window> allWindows = Stage.getWindows().stream().filter(Window::isShowing).collect(Collectors.toList());

            if (activeGamePrepFrame != null && activeGamePrepFrame.getView().getScene() != null && activeGamePrepFrame.getView().getScene().getWindow() != null && activeGamePrepFrame.getView().getScene().getWindow().isShowing()) {
                currentOwner = (Stage) activeGamePrepFrame.getView().getScene().getWindow();
            } else if (activeHostConfigFrame != null && activeHostConfigFrame.getView().getScene() != null && activeHostConfigFrame.getView().getScene().getWindow() != null && activeHostConfigFrame.getView().getScene().getWindow().isShowing()) {
                currentOwner = (Stage) activeHostConfigFrame.getView().getScene().getWindow();
            } else if (!allWindows.isEmpty()) {
                currentOwner = (Stage) allWindows.get(allWindows.size() - 1);
            }

            final Stage finalOwner = currentOwner;

            AutoClosingAlerts.show(finalOwner, "error".equals(type) ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION,
                    "error".equals(type) ? "Błąd" : "Informacja", null, message, Duration.seconds(durationSeconds));

            if (("error".equals(type) || "info".equals(type)) && switchToLobbyChoiceOnError && finalOwner != null) {
                if (activeHostConfigFrame != null || activeGamePrepFrame != null) {
                    OnlineLobbyChoiceFrame lobbyChoiceFrame = new OnlineLobbyChoiceFrame(finalOwner, this, () -> {
                        MainMenuFrame mainMenu = new MainMenuFrame(finalOwner);
                        Parent mainMenuRoot = mainMenu.getRootPane();
                        if (mainMenuRoot != null) {
                            if(finalOwner.getScene() != null) finalOwner.getScene().setRoot(mainMenuRoot);
                            else finalOwner.setScene(new Scene(mainMenuRoot));
                            mainMenu.show();
                        }
                    });
                    clearActiveHostConfigFrame();
                    clearActiveGamePrepFrame();
                    lobbyChoiceFrame.show();
                }
            }
        });
    }
}