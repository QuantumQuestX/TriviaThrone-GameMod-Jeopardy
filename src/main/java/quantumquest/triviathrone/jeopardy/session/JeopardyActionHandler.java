package quantumquest.triviathrone.jeopardy.session;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.jeopardy.action.IJeopardyAction;
import quantumquest.triviathrone.jeopardy.action.JeopardyActionRegistry;
import quantumquest.triviathrone.jeopardy.dto.GameState;

import java.util.Objects;
import java.util.Optional;

final class JeopardyActionHandler {
    private final JeopardySession session;

    JeopardyActionHandler(JeopardySession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    GameActionResult handle(GameActionMessage action, String username, boolean isHost) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(username, "username");

        Optional<IJeopardyAction> decodedAction = JeopardyActionRegistry.decode(action);
        if (decodedAction.isEmpty()) {
            return GameActionResult.rejected();
        }

        IJeopardyAction typedAction = decodedAction.get();
        if (typedAction == IJeopardyAction.Command.BUZZ) {
            return handleBuzz(username, isHost);
        }
        if (!isHost) {
            return GameActionResult.rejected();
        }

        return switch (typedAction) {
            case IJeopardyAction.SelectRound selectRound -> handleSelectRound(selectRound);
            case IJeopardyAction.SelectQuestion selectQuestion -> handleSelectQuestion(selectQuestion);
            case IJeopardyAction.Command command -> handleCommand(command);
        };
    }

    private GameActionResult handleBuzz(String username, boolean isHost) {
        if (isHost) {
            return GameActionResult.rejected();
        }

        session.registerBuzz(username);
        return GameActionResult.allow();
    }

    private GameActionResult handleSelectRound(IJeopardyAction.SelectRound action) {
        if (session.getCurrentState() != GameState.IDLE || !session.canSelectRound(action.roundIndex())) {
            return GameActionResult.rejected();
        }

        session.selectRound(action.roundIndex());
        return GameActionResult.allow();
    }

    private GameActionResult handleSelectQuestion(IJeopardyAction.SelectQuestion action) {
        if (session.getCurrentState() != GameState.IDLE || !session.canSelectQuestion(
                action.roundIndex(),
                action.categoryIndex(),
                action.questionIndex()
        )) {
            return GameActionResult.rejected();
        }

        session.selectQuestion(action.roundIndex(), action.categoryIndex(), action.questionIndex());
        return GameActionResult.allow();
    }

    private GameActionResult handleCommand(IJeopardyAction.Command command) {
        return switch (command) {
            case BUZZ -> GameActionResult.rejected();
            case PLAY_MEDIA -> allow(session::playMedia);
            case OPEN_BUZZ -> allow(session::openBuzz);
            case CLOSE_BUZZ -> allow(session::closeBuzz);
            case MARK_CORRECT -> allow(session::markCorrect);
            case MARK_INCORRECT -> allow(session::markIncorrect);
            case RESET -> handleReset();
        };
    }

    private GameActionResult handleReset() {
        if (!session.hasUnusedQuestions()) {
            return GameActionResult.endGame();
        }

        session.resetToIdle();
        return GameActionResult.allow();
    }

    private static GameActionResult allow(Runnable action) {
        action.run();
        return GameActionResult.allow();
    }
}
