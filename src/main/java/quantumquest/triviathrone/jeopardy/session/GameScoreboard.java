package quantumquest.triviathrone.jeopardy.session;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GameScoreboard {
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();

    GameScoreboard(Set<String> players) {
        Objects.requireNonNull(players, "players").forEach(username ->
                scores.put(Objects.requireNonNull(username, "player username"), 0));
    }

    void addPoints(String username, int points) {
        scores.computeIfPresent(Objects.requireNonNull(username, "username"), (_, score) -> score + points);
    }

    Map<String, Integer> snapshot() {
        return Map.copyOf(scores);
    }
}
