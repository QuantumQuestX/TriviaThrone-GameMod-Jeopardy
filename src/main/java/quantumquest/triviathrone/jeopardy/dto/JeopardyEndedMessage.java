package quantumquest.triviathrone.jeopardy.dto;

import quantumquest.triviathrone.api.game.IGameMessage;

import java.util.List;
import java.util.Objects;

public record JeopardyEndedMessage(
        String lobbyCode,
        String gameType,
        boolean gameEnded,
        List<LeaderboardEntry> leaderboard
) implements IGameMessage {
    public JeopardyEndedMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(gameType, "gameType");
        leaderboard = List.copyOf(Objects.requireNonNull(leaderboard, "leaderboard"));
    }

    public record LeaderboardEntry(String username, int score) {
        public LeaderboardEntry {
            Objects.requireNonNull(username, "username");
        }
    }
}
