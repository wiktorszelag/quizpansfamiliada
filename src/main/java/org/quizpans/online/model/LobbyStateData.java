package org.quizpans.online.model;

import java.util.List;
import java.util.Map;

public class LobbyStateData {
    private String id;
    private String name;
    private String status;
    private GameSettingsData gameSettings;
    private String password; // DODANE POLE
    private String hostSessionId;
    private PlayerInfo quizMaster;
    private List<PlayerInfo> waitingPlayers;
    private Map<String, List<PlayerInfo>> teams;
    private int totalParticipantCount;
    private int maxParticipants;

    public LobbyStateData() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public GameSettingsData getGameSettings() { return gameSettings; }
    public void setGameSettings(GameSettingsData gameSettings) { this.gameSettings = gameSettings; }

    // DODANE METODY DLA HASŁA
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHostSessionId() { return hostSessionId; }
    public void setHostSessionId(String hostSessionId) { this.hostSessionId = hostSessionId; }

    public PlayerInfo getQuizMaster() { return quizMaster; }
    public void setQuizMaster(PlayerInfo quizMaster) { this.quizMaster = quizMaster; }

    public List<PlayerInfo> getWaitingPlayers() { return waitingPlayers; }
    public void setWaitingPlayers(List<PlayerInfo> waitingPlayers) { this.waitingPlayers = waitingPlayers; }

    public Map<String, List<PlayerInfo>> getTeams() { return teams; }
    public void setTeams(Map<String, List<PlayerInfo>> teams) { this.teams = teams; }

    public int getTotalParticipantCount() { return totalParticipantCount; }
    public void setTotalParticipantCount(int totalParticipantCount) { this.totalParticipantCount = totalParticipantCount; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
}