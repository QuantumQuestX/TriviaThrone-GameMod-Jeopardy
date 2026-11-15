package quantumquest.triviathrone.jeopardy.session;

import quantumquest.triviathrone.jeopardy.definition.Category;
import quantumquest.triviathrone.jeopardy.definition.JeopardyDefinition;
import quantumquest.triviathrone.jeopardy.definition.QuestionDefinition;
import quantumquest.triviathrone.jeopardy.dto.JeopardyEndedMessage;
import quantumquest.triviathrone.jeopardy.dto.JeopardyStateMessage;
import quantumquest.triviathrone.jeopardy.dto.JeopardyStateMessage.QuestionTileMessage;
import quantumquest.triviathrone.jeopardy.dto.JeopardyStateMessage.RoundBoardMessage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

final class JeopardyStateMessageFactory {
    private JeopardyStateMessageFactory() {
    }

    static JeopardyStateMessage stateMessage(JeopardySession session, String lobbyCode) {
        JeopardyDefinition definition = session.getDefinition();
        List<RoundBoardMessage> rounds = IntStream.range(0, definition.rounds().size())
                .mapToObj(roundIdx -> {
                    var round = definition.rounds().get(roundIdx);
                    List<String> categories = round.categories().stream()
                            .map(Category::name)
                            .toList();
                    int questionCount = round.categories().stream()
                            .mapToInt(category -> category.questions().size())
                            .max()
                            .orElse(0);

                    List<List<QuestionTileMessage>> questions = IntStream.range(0, questionCount)
                            .mapToObj(questionIdx -> IntStream.range(0, round.categories().size())
                                    .mapToObj(categoryIdx -> {
                                        QuestionDefinition question = round.categories()
                                                .get(categoryIdx)
                                                .questions()
                                                .get(questionIdx);
                                        return new QuestionTileMessage(
                                                question.points(),
                                                session.isQuestionUsed(roundIdx, categoryIdx, questionIdx)
                                        );
                                    })
                                    .toList())
                            .toList();

                    return new RoundBoardMessage(round.name(), categories, questions);
                })
                .toList();
        int roundIdx = session.getCurrentRoundIdx();
        String roundName = roundIdx >= 0 && roundIdx < definition.rounds().size()
                ? definition.rounds().get(roundIdx).name()
                : null;

        return new JeopardyStateMessage(
                lobbyCode,
                "jeopardy",
                session.getCurrentState(),
                session.getBuzzWinner(),
                session.getScores(),
                session.getCurrentRoundIdx(),
                session.getCurrentCategoryIdx(),
                session.getCurrentQuestionIdx(),
                definition.title(),
                rounds,
                roundName,
                session.getCurrentQuestionMessage(),
                session.getBuzzedPlayers(),
                session.getMediaPlaybackToken(),
                session.getMediaPauseToken()
        );
    }

    static JeopardyEndedMessage endedMessage(JeopardySession session, String lobbyCode, String hostUsername) {
        List<JeopardyEndedMessage.LeaderboardEntry> leaderboard = session.getScores().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hostUsername))
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new JeopardyEndedMessage.LeaderboardEntry(entry.getKey(), entry.getValue()))
                .toList();

        return new JeopardyEndedMessage(lobbyCode, "jeopardy", true, leaderboard);
    }
}
