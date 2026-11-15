package quantumquest.triviathrone.jeopardy.definition;

import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.jeopardy.session.JeopardySession;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record JeopardyDefinition(String title, String description, List<Round> rounds) implements IGameDefinition {
    public JeopardyDefinition {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        rounds = List.copyOf(Objects.requireNonNull(rounds, "rounds"));
    }

    @Override
    public String gameType() {
        return "jeopardy";
    }

    @Override
    public boolean referencesMedia(String mediaPath) {
        return mediaPath != null && !mediaPath.isBlank()
                && rounds.stream()
                .flatMap(round -> round.categories().stream())
                .flatMap(category -> category.questions().stream())
                .anyMatch(question -> Objects.equals(question.mediaPath(), mediaPath));
    }

    @Override
    public IGameSession createSession(String lobbyCode, Set<String> players) {
        return new JeopardySession(lobbyCode, players, this);
    }
}
