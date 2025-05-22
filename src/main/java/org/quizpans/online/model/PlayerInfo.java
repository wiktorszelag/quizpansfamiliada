package org.quizpans.online.model;

public record PlayerInfo(
        String sessionId,
        String nickname,
        String teamName
) {}