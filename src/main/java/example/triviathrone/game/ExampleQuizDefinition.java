package example.triviathrone.game;

import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameSession;

import java.util.Objects;
import java.util.Set;

final class ExampleQuizDefinition implements IGameDefinition {
    private final String title;
    private final String question;
    private final String answer;

    ExampleQuizDefinition(String title, String question, String answer) {
        this.title = Objects.requireNonNull(title, "title");
        this.question = Objects.requireNonNull(question, "question");
        this.answer = Objects.requireNonNull(answer, "answer");
    }

    @Override
    public String gameType() {
        return "example-quiz";
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public boolean referencesMedia(String mediaPath) {
        return false;
    }

    @Override
    public IGameSession createSession(String lobbyCode, Set<String> players) {
        Objects.requireNonNull(players, "players");
        return new ExampleQuizSession(lobbyCode, this);
    }

    String question() {
        return question;
    }

    String answer() {
        return answer;
    }
}
