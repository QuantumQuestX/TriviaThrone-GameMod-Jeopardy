package example.triviathrone.game;

import quantumquest.triviathrone.api.game.IGameMessage;

public record ExampleQuizMessage(
        String lobbyCode,
        String gameType,
        String title,
        String question,
        String answer,
        boolean answerRevealed,
        boolean ended
) implements IGameMessage {
}
