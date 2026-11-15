package quantumquest.triviathrone.jeopardy.action;

import quantumquest.triviathrone.dto.game.GameActionMessage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class JeopardyActionRegistry {
    private static final Map<String, Function<Map<String, Object>, Optional<IJeopardyAction>>> DECODERS = Map.ofEntries(
            Map.entry(IJeopardyAction.Command.BUZZ.wireName(), noParameters(IJeopardyAction.Command.BUZZ)),
            Map.entry(IJeopardyAction.Command.PLAY_MEDIA.wireName(), noParameters(IJeopardyAction.Command.PLAY_MEDIA)),
            Map.entry(IJeopardyAction.Command.OPEN_BUZZ.wireName(), noParameters(IJeopardyAction.Command.OPEN_BUZZ)),
            Map.entry(IJeopardyAction.Command.CLOSE_BUZZ.wireName(), noParameters(IJeopardyAction.Command.CLOSE_BUZZ)),
            Map.entry(IJeopardyAction.Command.MARK_CORRECT.wireName(), noParameters(IJeopardyAction.Command.MARK_CORRECT)),
            Map.entry(IJeopardyAction.Command.MARK_INCORRECT.wireName(), noParameters(IJeopardyAction.Command.MARK_INCORRECT)),
            Map.entry(IJeopardyAction.Command.RESET.wireName(), noParameters(IJeopardyAction.Command.RESET)),
            Map.entry(IJeopardyAction.SelectRound.WIRE_NAME, JeopardyActionRegistry::selectRound),
            Map.entry(IJeopardyAction.SelectQuestion.WIRE_NAME, JeopardyActionRegistry::selectQuestion)
    );

    private JeopardyActionRegistry() {
    }

    public static Optional<IJeopardyAction> decode(GameActionMessage message) {
        Function<Map<String, Object>, Optional<IJeopardyAction>> decoder = DECODERS.get(message.action());
        return decoder == null ? Optional.empty() : decoder.apply(message.parameters());
    }

    private static Function<Map<String, Object>, Optional<IJeopardyAction>> noParameters(
            IJeopardyAction action
    ) {
        return parameters -> parameters.isEmpty() ? Optional.of(action) : Optional.empty();
    }

    private static Optional<IJeopardyAction> selectRound(Map<String, Object> parameters) {
        if (!parameters.keySet().equals(Set.of("roundIdx"))) {
            return Optional.empty();
        }
        Optional<Integer> roundIndex = readIndex(parameters.get("roundIdx"));
        return roundIndex.map(IJeopardyAction.SelectRound::new);
    }

    private static Optional<IJeopardyAction> selectQuestion(Map<String, Object> parameters) {
        if (!parameters.keySet().equals(Set.of("roundIdx", "categoryIdx", "questionIdx"))) {
            return Optional.empty();
        }
        Optional<Integer> roundIndex = readIndex(parameters.get("roundIdx"));
        Optional<Integer> categoryIndex = readIndex(parameters.get("categoryIdx"));
        Optional<Integer> questionIndex = readIndex(parameters.get("questionIdx"));
        if (roundIndex.isEmpty() || categoryIndex.isEmpty() || questionIndex.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IJeopardyAction.SelectQuestion(
                roundIndex.get(), categoryIndex.get(), questionIndex.get()
        ));
    }

    private static Optional<Integer> readIndex(Object value) {
        if (!(value instanceof Number number)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(number.toString()).intValueExact());
        } catch (NumberFormatException | ArithmeticException exception) {
            return Optional.empty();
        }
    }
}
