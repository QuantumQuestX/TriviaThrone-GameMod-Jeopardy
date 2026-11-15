package quantumquest.triviathrone.jeopardy.session;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.IGameMessage;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.jeopardy.definition.Category;
import quantumquest.triviathrone.jeopardy.definition.JeopardyDefinition;
import quantumquest.triviathrone.jeopardy.definition.QuestionDefinition;
import quantumquest.triviathrone.jeopardy.dto.GameQuestionMessage;
import quantumquest.triviathrone.jeopardy.dto.GameState;
import quantumquest.triviathrone.jeopardy.dto.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class JeopardySession implements IGameSession {
    private final GameQuestionBoard board;
    private final GameScoreboard scoreboard;
    private final String lobbyCode;
    private final JeopardyActionHandler actionHandler;

    private final GameBuzzer buzzer;

    private volatile GameState currentState = GameState.IDLE;
    private volatile Runnable stateChangeListener = () -> {
    };

    private String mediaPlaybackToken;
    private String mediaPauseToken;

    public JeopardySession(String lobbyCode, Set<String> players, JeopardyDefinition definition) {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(players, "players");
        this.board = new GameQuestionBoard(definition);
        this.scoreboard = new GameScoreboard(players);
        this.lobbyCode = lobbyCode;
        this.buzzer = new GameBuzzer(this::onBuzzFinalized);
        this.actionHandler = new JeopardyActionHandler(this);
    }

    public synchronized void selectQuestion(Integer round, Integer category, Integer question) {
        if (currentState != GameState.IDLE) {
            throw new IllegalStateException("Cannot select question now");
        }

        board.selectQuestion(round, category, question);

        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = GameState.QUESTION_SELECTED;
    }

    public synchronized void playMedia() {
        if (currentState != GameState.QUESTION_SELECTED && currentState != GameState.BUZZ_OPEN) {
            return;
        }

        QuestionDefinition question = board.currentQuestion();

        if ((question.mediaType() == MediaType.AUDIO || question.mediaType() == MediaType.VIDEO)
                && question.mediaPath() != null && !question.mediaPath().isBlank()) {
            mediaPlaybackToken = UUID.randomUUID().toString();
            if (currentState == GameState.QUESTION_SELECTED) {
                buzzer.open();
                currentState = GameState.BUZZ_OPEN;
            }
        }
    }

    public synchronized void selectRound(Integer round) {
        if (currentState != GameState.IDLE) {
            throw new IllegalStateException("Cannot change rounds while a question is active");
        }
        board.selectRound(round);
    }

    public synchronized void openBuzz() {
        if (currentState != GameState.QUESTION_SELECTED) {
            return;
        }
        buzzer.open();
        currentState = GameState.BUZZ_OPEN;
    }

    public synchronized void registerBuzz(String username) {
        Objects.requireNonNull(username, "username");
        if (currentState != GameState.BUZZ_OPEN && currentState != GameState.BUZZ_COLLECTING) {
            return;
        }

        GameBuzzer.Registration registration = buzzer.register(username);
        if (registration == GameBuzzer.Registration.FIRST) {
            currentState = GameState.BUZZ_COLLECTING;
            mediaPauseToken = UUID.randomUUID().toString();
        }
    }

    public synchronized void closeBuzz() {
        if (currentState != GameState.BUZZ_OPEN && currentState != GameState.BUZZ_COLLECTING) {
            return;
        }

        currentState = buzzer.closeBuzz() == null ? GameState.QUESTION_SELECTED : GameState.BUZZ_LOCKED;
    }

    public synchronized void markCorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != GameState.BUZZ_LOCKED || buzzWinner == null) {
            return;
        }

        scoreboard.addPoints(buzzWinner, board.currentQuestion().points());
        currentState = GameState.ANSWER_REVEAL;
    }

    public synchronized void markIncorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != GameState.BUZZ_LOCKED || buzzWinner == null) {
            return;
        }

        scoreboard.addPoints(buzzWinner, -(board.currentQuestion().points() / 2));
        buzzer.markIncorrect();
        currentState = GameState.QUESTION_SELECTED;
    }

    public synchronized void resetToIdle() {
        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = GameState.IDLE;
    }

    private void onBuzzFinalized(String winner) {
        synchronized (this) {
            if (currentState != GameState.BUZZ_COLLECTING) {
                return;
            }
            currentState = winner == null ? GameState.QUESTION_SELECTED : GameState.BUZZ_LOCKED;
        }
        stateChangeListener.run();
    }

    public synchronized void setStateChangeListener(Runnable listener) {
        stateChangeListener = listener == null ? () -> {
        } : listener;
    }

    public synchronized GameQuestionMessage getCurrentQuestionMessage() {
        if (currentState == GameState.IDLE) {
            return null;
        }

        return createGameQuestionMessage(board.currentCategory(), board.currentQuestion());
    }

    public synchronized boolean isQuestionUsed(int round, int category, int question) {
        return board.isQuestionUsed(round, category, question);
    }

    public synchronized boolean hasUnusedQuestions() {
        return board.hasUnusedQuestions();
    }

    public synchronized Set<String> getBuzzedPlayers() {
        return buzzer.getBuzzedPlayers();
    }

    public synchronized void close() {
        buzzer.close();
    }

    @Override
    public synchronized GameActionResult handleAction(
            GameActionMessage action,
            String username,
            boolean isHost
    ) {
        return actionHandler.handle(action, username, isHost);
    }

    @Override
    public synchronized IGameMessage stateMessage() {
        return JeopardyStateMessageFactory.stateMessage(this, lobbyCode);
    }

    @Override
    public synchronized IGameMessage endedMessage(String hostUsername) {
        return JeopardyStateMessageFactory.endedMessage(this, lobbyCode, hostUsername);
    }

    boolean canSelectRound(int roundIndex) {
        return board.isValidRoundIndex(roundIndex);
    }

    boolean canSelectQuestion(int roundIndex, int categoryIndex, int questionIndex) {
        return board.isValidQuestion(roundIndex, categoryIndex, questionIndex)
                && !board.isQuestionUsed(roundIndex, categoryIndex, questionIndex);
    }

    private GameQuestionMessage createGameQuestionMessage(Category category, QuestionDefinition question) {
        String mediaUrl = question.mediaPath() == null
                ? null
                : "/api/game/" + lobbyCode + "/media?path="
                + URLEncoder.encode(question.mediaPath(), StandardCharsets.UTF_8);

        return new GameQuestionMessage(
                category.name(),
                question.prompt(),
                question.choices(),
                currentState == GameState.ANSWER_REVEAL ? question.correctChoiceIndex() : null,
                question.mediaType(),
                mediaUrl,
                question.points()
        );
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public String getBuzzWinner() {
        return buzzer.winner();
    }

    public Map<String, Integer> getScores() {
        return scoreboard.snapshot();
    }

    public JeopardyDefinition getDefinition() {
        return board.definition();
    }

    public synchronized int getCurrentRoundIdx() {
        return board.currentRoundIdx();
    }

    public synchronized int getCurrentCategoryIdx() {
        return board.currentCategoryIdx();
    }

    public synchronized int getCurrentQuestionIdx() {
        return board.currentQuestionIdx();
    }

    public synchronized String getMediaPlaybackToken() {
        return mediaPlaybackToken;
    }

    public synchronized String getMediaPauseToken() {
        return mediaPauseToken;
    }
}
