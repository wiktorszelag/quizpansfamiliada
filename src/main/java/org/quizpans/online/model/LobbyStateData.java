package org.quizpans.online.model;

import java.util.List;
import java.util.Map;

public class LobbyStateData {
    private String id;
    private String name;
    private String status;
    private GameSettingsData gameSettings;
    private String password;
    private String hostSessionId;
    private PlayerInfo quizMaster;
    private List<PlayerInfo> waitingPlayers;
    private Map<String, List<PlayerInfo>> teams;
    private int totalParticipantCount;
    private int maxParticipants;

    private int currentRoundNumber;
    private String currentQuestionText;
    private int totalRounds;
    private String currentPlayerSessionId;
    private boolean isTeam1Turn;
    private int team1Score;
    private int team2Score;
    private int team1Errors;
    private int team2Errors;
    private List<Map<String, Object>> revealedAnswersData;
    private int currentRoundPoints;
    private int currentAnswerTimeRemaining;

    public LobbyStateData() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public GameSettingsData getGameSettings() { return gameSettings; }
    public void setGameSettings(GameSettingsData gameSettings) { this.gameSettings = gameSettings; }

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

    public int getCurrentRoundNumber() { return currentRoundNumber; }
    public void setCurrentRoundNumber(int currentRoundNumber) { this.currentRoundNumber = currentRoundNumber; }

    public String getCurrentQuestionText() { return currentQuestionText; }
    public void setCurrentQuestionText(String currentQuestionText) { this.currentQuestionText = currentQuestionText; }

    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }

    public String getCurrentPlayerSessionId() { return currentPlayerSessionId; }
    public void setCurrentPlayerSessionId(String currentPlayerSessionId) { this.currentPlayerSessionId = currentPlayerSessionId; }

    public boolean isTeam1Turn() { return isTeam1Turn; }
    public void setTeam1Turn(boolean team1Turn) { isTeam1Turn = team1Turn; }

    public int getTeam1Score() { return team1Score; }
    public void setTeam1Score(int team1Score) { this.team1Score = team1Score; }

    public int getTeam2Score() { return team2Score; }
    public void setTeam2Score(int team2Score) { this.team2Score = team2Score; }

    public int getTeam1Errors() { return team1Errors; }
    public void setTeam1Errors(int team1Errors) { this.team1Errors = team1Errors; }

    public int getTeam2Errors() { return team2Errors; }
    public void setTeam2Errors(int team2Errors) { this.team2Errors = team2Errors; }

    public List<Map<String, Object>> getRevealedAnswersData() { return revealedAnswersData; }
    public void setRevealedAnswersData(List<Map<String, Object>> revealedAnswersData) { this.revealedAnswersData = revealedAnswersData; }

    public int getCurrentRoundPoints() { return currentRoundPoints; }
    public void setCurrentRoundPoints(int currentRoundPoints) { this.currentRoundPoints = currentRoundPoints; }

    public int getCurrentAnswerTimeRemaining() { return currentAnswerTimeRemaining; }
    public void setCurrentAnswerTimeRemaining(int currentAnswerTimeRemaining) { this.currentAnswerTimeRemaining = currentAnswerTimeRemaining; }
}