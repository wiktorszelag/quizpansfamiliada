package org.quizpans.online.model;

public record GameSettingsData(
        String category,
        int answerTime,
        int numberOfRounds,
        int maxPlayersPerTeam,
        String teamBlueName,
        String teamRedName
) {
    // Konstruktor domyślny dla Gson i inicjalizacji
    public GameSettingsData() {
        this(null, 30, 5, 3, "Niebiescy", "Czerwoni");
    }
    public GameSettingsData(String category, int answerTime, int numberOfRounds, int maxPlayersPerTeam, String teamBlueName, String teamRedName){
        this.category = category;
        this.answerTime = answerTime;
        this.numberOfRounds = numberOfRounds;
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        this.teamBlueName = teamBlueName;
        this.teamRedName = teamRedName;
    }
}