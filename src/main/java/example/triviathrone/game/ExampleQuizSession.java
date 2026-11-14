package example.triviathrone.game;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.IGameMessage;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.dto.game.GameActionMessage;

import java.util.Objects;

final class ExampleQuizSession implements IGameSession {
    private static final String REVEAL_ANSWER_ACTION = "REVEAL_ANSWER";

    private final String lobbyCode;
    private final ExampleQuizDefinition definition;

    private boolean answerRevealed;
    private boolean closed;

    ExampleQuizSession(String lobbyCode, ExampleQuizDefinition definition) {
        this.lobbyCode = Objects.requireNonNull(lobbyCode, "lobbyCode");
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    @Override
    public synchronized GameActionResult handleAction(GameActionMessage action, String username, boolean isHost) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(username, "username");

        if (closed
                || !isHost
                || !lobbyCode.equals(action.lobbyCode())
                || !REVEAL_ANSWER_ACTION.equals(action.action())
                || !action.parameters().isEmpty()
                || answerRevealed) {
            return GameActionResult.rejected();
        }

        answerRevealed = true;
        return GameActionResult.allow();
    }

    @Override
    public synchronized IGameMessage stateMessage() {
        return message(answerRevealed, false);
    }

    @Override
    public synchronized IGameMessage endedMessage(String hostUsername) {
        return message(true, true);
    }

    @Override
    public void setStateChangeListener(Runnable listener) {
        // This example session has no state changes that occur asynchronously.
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private ExampleQuizMessage message(boolean revealAnswer, boolean gameEnded) {
        return new ExampleQuizMessage(
                lobbyCode,
                definition.gameType(),
                definition.title(),
                definition.question(),
                revealAnswer ? definition.answer() : null,
                revealAnswer,
                gameEnded
        );
    }
}
