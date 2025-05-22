package org.quizpans.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.quizpans.gui.OnlineLobbyHostConfigFrame;
import org.quizpans.online.model.OnlineLobbyUIData;

import org.quizpans.online.model.PlayerInfo;
import org.quizpans.online.model.GameSettingsData;
import org.quizpans.online.model.LobbyStateData;
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
    private String clientSessionId;

    // Nowa właściwość do obserwowania przez GUI (np. OnlineLobbyChoiceFrame)
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
    }

    public void clearActiveHostConfigFrame() {
        if (this.activeHostConfigFrame != null) {
            System.out.println("OnlineService: Clearing active host config frame for lobby: " + this.activeHostConfigFrame.getLobbyId());
        }
        this.activeHostConfigFrame = null;
        this.currentlyHostedLobbyState.set(null); // Wyczyść też hostowane lobby
    }

    public void removeLobbyUpdateListener() {
        this.activeHostConfigFrame = null;
        System.out.println("OnlineService: Listener (activeHostConfigFrame) usunięty/wyczyszczony.");
    }

    public String getClientSessionId(){
        return clientSessionId;
    }

    public boolean isOpen() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public void connect() {
        if (isOpen()) {
            System.out.println("OnlineService: Already connected. Requesting lobbies.");
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
                    System.out.println("OnlineService: Połączono z serwerem WebSocket: " + serverUri);
                    // Serwer powinien automatycznie wysłać yourSessionId
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("OnlineService: Otrzymano wiadomość: " + message);
                    Platform.runLater(() -> handleServerMessage(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("OnlineService: Rozłączono z serwerem WebSocket. Kod: " + code + ", Powód: " + reason);
                    Platform.runLater(() -> onlineLobbies.clear());
                    clientSessionId = null;
                    clearActiveHostConfigFrame();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("OnlineService: Błąd WebSocket: " + ex.getMessage());

                }
            };
            System.out.println("OnlineService: Próba połączenia z " + serverUri);
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            System.err.println("OnlineService: Niepoprawny URI serwera WebSocket: " + serverUri + " - " + e.getMessage());
        }
    }

    private void handleServerMessage(String message) {
        try {
            Map<String, Object> parsedMessage = gson.fromJson(message, new TypeToken<Map<String, Object>>(){}.getType());
            String messageType = (String) parsedMessage.get("type");

            if ("yourSessionId".equals(messageType) && parsedMessage.containsKey("sessionId")) {
                this.clientSessionId = (String) parsedMessage.get("sessionId");
                System.out.println("OnlineService: Moje ID sesji zostało ustawione na: " + this.clientSessionId);
                // Po otrzymaniu ID sesji, zażądaj listy lobby
                Map<String, Object> requestLobbiesMessage = new HashMap<>();
                requestLobbiesMessage.put("action", "getAllLobbies");
                sendJsonMessage(requestLobbiesMessage);
                return;
            }

            if ("allLobbies".equals(messageType) || "allLobbiesUpdate".equals(messageType)) {
                Object lobbiesData = parsedMessage.get("lobbies");
                if (lobbiesData instanceof List) {
                    List<Map<String, Object>> rawLobbies = (List<Map<String, Object>>) lobbiesData;
                    List<OnlineLobbyUIData> newLobbiesList = rawLobbies.stream()
                            .map(this::parseRawMapToOnlineLobbyUIData)
                            .collect(Collectors.toList());
                    onlineLobbies.setAll(newLobbiesList);
                }
            } else if ("lobbyUpdate".equals(messageType)) {
                Object lobbyData = parsedMessage.get("lobby");
                if (lobbyData instanceof Map) {
                    Map<String, Object> rawLobbyMap = (Map<String, Object>) lobbyData;
                    LobbyStateData clientLobbyState = parseRawMapToClientLobbyStateData(rawLobbyMap);

                    OnlineLobbyUIData uiData = mapLobbyStateDataToUIData(clientLobbyState);
                    if (uiData != null) {
                        int index = -1;
                        for (int i = 0; i < onlineLobbies.size(); i++) {
                            if (onlineLobbies.get(i).getId().equals(uiData.getId())) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            onlineLobbies.set(index, uiData);
                        } else {
                            onlineLobbies.add(uiData);
                        }
                    }

                    if (clientLobbyState != null) {
                        // Jeśli to jest lobby, które aktualnie hostujemy (lub właśnie zaczęliśmy hostować)
                        if (clientSessionId != null && clientSessionId.equals(clientLobbyState.getHostSessionId())) {
                            currentlyHostedLobbyState.set(clientLobbyState); // Ustawia property, na które GUI może nasłuchiwać
                            System.out.println("OnlineService: Ustawiono currentlyHostedLobbyState dla lobby: " + clientLobbyState.getId());
                        }

                        // Jeśli aktywna jest ramka konfiguracji dla tego lobby, zaktualizuj ją
                        if (activeHostConfigFrame != null &&
                                activeHostConfigFrame.getLobbyId().equals(clientLobbyState.getId()) &&
                                activeHostConfigFrame.getView().getScene() != null &&
                                activeHostConfigFrame.getView().getScene().getWindow() != null &&
                                activeHostConfigFrame.getView().getScene().getWindow().isShowing()) {
                            System.out.println("OnlineService: Aktualizowanie aktywnej ramki konfiguracji dla lobby: " + clientLobbyState.getId());
                            activeHostConfigFrame.updateFullLobbyState(clientLobbyState);
                        }
                    }
                }
            }  else if ("error".equals(messageType)) {
                String errorMessage = (String) parsedMessage.get("message");
                System.err.println("OnlineService: Server error: " + errorMessage);
                Platform.runLater(() -> {
                    Stage owner = (Stage) Stage.getWindows().stream().filter(w -> w.isShowing() && w.isFocused()).findFirst().orElse(null);
                    if(owner == null) owner = (Stage) Stage.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                    AutoClosingAlerts.show(owner, Alert.AlertType.ERROR, "Błąd Serwera", null, errorMessage, Duration.seconds(5));
                });
            }
        } catch (JsonSyntaxException e) {
            System.err.println("OnlineService: Błąd parsowania JSON od serwera: " + e.getMessage() + " dla wiadomości: " + message);
        } catch (Exception e) {
            System.err.println("OnlineService: Nieoczekiwany błąd podczas obsługi wiadomości od serwera: " + e.getMessage() + " dla wiadomości: " + message);
            e.printStackTrace();
        }
    }

    private OnlineLobbyUIData parseRawMapToOnlineLobbyUIData(Map<String, Object> rawLobby) {
        String id = (String) rawLobby.get("id");
        String name = (String) rawLobby.get("name");
        String status = "";
        if(rawLobby.get("status") instanceof String){
            status = (String) rawLobby.get("status");
        } else if (rawLobby.get("status") != null) {
            status = rawLobby.get("status").toString();
        }

        OnlineLobbyUIData uiLobby = new OnlineLobbyUIData(id, name, status);

        if (rawLobby.get("totalParticipantCount") instanceof Number) {
            uiLobby.setPlayerCount(((Number) rawLobby.get("totalParticipantCount")).intValue());
        }
        if (rawLobby.get("maxParticipants") instanceof Number) {
            uiLobby.setMaxPlayers(((Number) rawLobby.get("maxParticipants")).intValue());
        }

        if (rawLobby.containsKey("hostSessionId") && rawLobby.get("hostSessionId") instanceof String) {

        }
        return uiLobby;
    }

    private OnlineLobbyUIData mapLobbyStateDataToUIData(LobbyStateData lobbyState) {
        if (lobbyState == null) return null;
        OnlineLobbyUIData uiData = new OnlineLobbyUIData(lobbyState.getId(), lobbyState.getName(), lobbyState.getStatus());
        uiData.setPlayerCount(lobbyState.getTotalParticipantCount());
        uiData.setMaxPlayers(lobbyState.getMaxParticipants());
        return uiData;
    }

    private LobbyStateData parseRawMapToClientLobbyStateData(Map<String, Object> rawLobbyMap) {
        String lobbyJson = gson.toJson(rawLobbyMap);
        Type lobbyStateType = new TypeToken<LobbyStateData>() {}.getType();
        try {
            return gson.fromJson(lobbyJson, lobbyStateType);
        } catch (JsonSyntaxException e) {
            System.err.println("Error deserializing to Client LobbyStateData: " + e.getMessage());
            return null;
        }
    }

    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        } else {
            System.err.println("OnlineService: Nie można wysłać wiadomości - klient WebSocket nie jest połączony.");
            // Można dodać próbę ponownego połączenia lub informację dla użytkownika
            // connect(); // Ostrożnie z automatycznym re-connect w tym miejscu
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
            webSocketClient = null; // null po zamknieciu wazne
        }
        Platform.runLater(() -> onlineLobbies.clear());
        clientSessionId = null;
        clearActiveHostConfigFrame();
    }
}