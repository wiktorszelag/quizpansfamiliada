package org.quizpans.online.model;

import java.util.Objects;

public final class PlayerInfo {
    private final String sessionId;
    private final String nickname;
    private final String teamName;
    private ParticipantRole role;

    public PlayerInfo(String sessionId, String nickname, String teamName, ParticipantRole role) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.teamName = teamName;
        this.role = role != null ? role : ParticipantRole.PLAYER;
    }

    public String sessionId() {
        return sessionId;
    }

    public String nickname() {
        return nickname;
    }

    public String teamName() {
        return teamName;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerInfo) obj;
        return Objects.equals(this.sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "PlayerInfo[" +
                "sessionId=" + sessionId + ", " +
                "nickname=" + nickname + ", " +
                "teamName=" + teamName + ", " +
                "role=" + role + ']';
    }
}