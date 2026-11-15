package quantumquest.triviathrone.jeopardy.dto;

import quantumquest.triviathrone.api.game.IGameMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record JeopardyStateMessage(String lobbyCode, String gameType, GameState state, String buzzWinner,
                                   Map<String, Integer> scores,
                                   Integer curRoundIdx, Integer curCatIdx, Integer curQuestionIdx,
                                   String gameTitle, List<RoundBoardMessage> rounds, String roundName,
                                   GameQuestionMessage question, Set<String> buzzedPlayers,
                                   String mediaPlaybackToken, String mediaPauseToken) implements IGameMessage {
    public JeopardyStateMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(state, "state");
        scores = Map.copyOf(Objects.requireNonNull(scores, "scores"));
        Objects.requireNonNull(gameTitle, "gameTitle");
        rounds = List.copyOf(Objects.requireNonNull(rounds, "rounds"));
        buzzedPlayers = Set.copyOf(Objects.requireNonNull(buzzedPlayers, "buzzedPlayers"));
    }

    public record RoundBoardMessage(String name, List<String> categories, List<List<QuestionTileMessage>> questions) {
        public RoundBoardMessage {
            Objects.requireNonNull(name, "name");
            categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
            questions = Objects.requireNonNull(questions, "questions").stream()
                    .map(questionRow -> List.copyOf(Objects.requireNonNull(questionRow, "question row")))
                    .toList();
        }
    }

    public record QuestionTileMessage(int points, boolean used) {
    }
}
