package org.quizpans.online.model;

public class OnlineLobbyUIData {
    private final String id;
    private final String name;
    private String status;
    private int playerCount;
    private int maxPlayers;

    public OnlineLobbyUIData(String id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.playerCount = 0;
        this.maxPlayers = 13;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    @Override
    public String toString() {
        return name + " (Status: " + status + ", Graczy: " + playerCount + "/" + maxPlayers + ")";
    }
}