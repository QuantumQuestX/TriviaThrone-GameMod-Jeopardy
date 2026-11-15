package quantumquest.triviathrone.jeopardy.session;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.jeopardy.action.IJeopardyAction;
import quantumquest.triviathrone.jeopardy.definition.Category;
import quantumquest.triviathrone.jeopardy.definition.JeopardyDefinition;
import quantumquest.triviathrone.jeopardy.dto.MediaType;
import quantumquest.triviathrone.jeopardy.definition.QuestionDefinition;
import quantumquest.triviathrone.jeopardy.definition.Round;
import quantumquest.triviathrone.jeopardy.dto.GameState;
import quantumquest.triviathrone.jeopardy.dto.JeopardyStateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {
    private JeopardySession session;

    @AfterEach
    void closeSession() {
        if (session != null) session.close();
    }

    @Test
    void startingVideoOpensBuzzerAndPublishesMediaUrl() {
        session = createSession(MediaType.VIDEO, "resources/clip one.mp4");
        session.selectQuestion(0, 0, 0);

        session.playMedia();

        assertEquals(GameState.BUZZ_OPEN, session.getCurrentState());
        assertNotNull(session.getMediaPlaybackToken());
        assertEquals(
                "/api/game/ROOM01/media?path=resources%2Fclip+one.mp4",
                session.getCurrentQuestionMessage().mediaUrl()
        );
        assertNull(session.getMediaPauseToken());
    }

    @Test
    void firstBuzzEmitsPauseTokenAndResetClearsPlaybackState() {
        session = createSession(MediaType.AUDIO, "resources/clue.mp3");
        session.selectQuestion(0, 0, 0);
        session.playMedia();
        String playbackToken = session.getMediaPlaybackToken();

        session.registerBuzz("player");
        session.registerBuzz("another-player");
        session.closeBuzz();

        assertEquals(GameState.BUZZ_LOCKED, session.getCurrentState());
        assertNotNull(session.getMediaPauseToken());
        assertNotNull(playbackToken);

        session.resetToIdle();

        assertEquals(GameState.IDLE, session.getCurrentState());
        assertNull(session.getMediaPlaybackToken());
        assertNull(session.getMediaPauseToken());
    }

    @Test
    void nonAudioVideoMediaDoesNotOpenBuzzer() {
        session = createSession(MediaType.IMAGE, "resources/clue.png");
        session.selectQuestion(0, 0, 0);

        session.playMedia();

        assertEquals(GameState.QUESTION_SELECTED, session.getCurrentState());
        assertNull(session.getMediaPlaybackToken());
    }

    @Test
    void mediaCanStartAfterHostManuallyOpensBuzzer() {
        session = createSession(MediaType.VIDEO, "resources/clue.mp4");
        session.selectQuestion(0, 0, 0);
        session.openBuzz();

        session.playMedia();

        assertEquals(GameState.BUZZ_OPEN, session.getCurrentState());
        assertNotNull(session.getMediaPlaybackToken());
    }

    @Test
    void reportsWhetherQuestionsRemainUnused() {
        session = createSession(MediaType.VIDEO, "resources/clue.mp4");

        assertTrue(session.hasUnusedQuestions());
        session.selectQuestion(0, 0, 0);
        assertTrue(session.hasUnusedQuestions());

        session.resetToIdle();
        session.selectQuestion(0, 0, 1);

        assertFalse(session.hasUnusedQuestions());
    }

    @Test
    void multipleChoiceAnswersAreRevealedOnlyAfterHostMarksCorrect() {
        QuestionDefinition question = new QuestionDefinition(
                200,
                "Which ocean is largest?",
                null,
                MediaType.UNDEFINED,
                List.of("Atlantic Ocean", "Pacific Ocean"),
                1
        );
        session = new JeopardySession(
                "ROOM01",
                java.util.Set.of("host", "player"),
                new JeopardyDefinition(
                        "Test game",
                        "Description",
                        List.of(new Round("Round 1", List.of(new Category("Geography", List.of(question)))))
                )
        );

        session.selectQuestion(0, 0, 0);

        assertEquals(List.of("Atlantic Ocean", "Pacific Ocean"),
                session.getCurrentQuestionMessage().choices());
        assertNull(session.getCurrentQuestionMessage().correctChoiceIndex());

        session.openBuzz();
        session.registerBuzz("player");
        session.closeBuzz();
        session.markCorrect();

        assertEquals(1, session.getCurrentQuestionMessage().correctChoiceIndex());
    }

    @Test
    void gameDefinitionExposesActionsAndStateThroughTheSharedGameApi() {
        JeopardyDefinition definition = new JeopardyDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(
                                new QuestionDefinition(100, "Prompt", null, MediaType.UNDEFINED)
                        ))
                )))
        );
        IGameSession gameSession = definition.createSession("ROOM01", java.util.Set.of("host", "player"));
        session = (JeopardySession) gameSession;
        GameActionMessage action = new GameActionMessage(
                "ROOM01",
                IJeopardyAction.SelectQuestion.WIRE_NAME,
                Map.of("roundIdx", 0, "categoryIdx", 0, "questionIdx", 0)
        );

        GameActionResult result = gameSession.handleAction(action, "host", true);
        JeopardyStateMessage state = (JeopardyStateMessage) gameSession.stateMessage();

        assertTrue(result.accepted());
        assertEquals(GameState.QUESTION_SELECTED, state.state());
        assertEquals("jeopardy", state.gameType());
    }

    @Test
    void rejectsUnknownMalformedAndOutOfRangeActionsWithoutChangingState() {
        session = createSession(MediaType.UNDEFINED, null);

        assertFalse(session.handleAction(
                new GameActionMessage("ROOM01", "UNKNOWN", Map.of()), "host", true
        ).accepted());
        assertFalse(session.handleAction(
                new GameActionMessage(
                        "ROOM01",
                        IJeopardyAction.Command.PLAY_MEDIA.wireName(),
                        Map.of("unexpected", true)
                ),
                "host",
                true
        ).accepted());
        assertFalse(session.handleAction(
                new GameActionMessage(
                        "ROOM01",
                        IJeopardyAction.SelectQuestion.WIRE_NAME,
                        Map.of("roundIdx", 0, "categoryIdx", 0.5, "questionIdx", 0)
                ),
                "host",
                true
        ).accepted());
        assertFalse(session.handleAction(
                new GameActionMessage(
                        "ROOM01",
                        IJeopardyAction.SelectQuestion.WIRE_NAME,
                        Map.of("roundIdx", 5, "categoryIdx", 0, "questionIdx", 0)
                ),
                "host",
                true
        ).accepted());

        assertEquals(GameState.IDLE, session.getCurrentState());
    }

    @Test
    void rejectsQuestionAndRoundSelectionWhileAQuestionIsActive() {
        session = createSession(MediaType.UNDEFINED, null);
        GameActionMessage selectQuestion = new GameActionMessage(
                "ROOM01",
                IJeopardyAction.SelectQuestion.WIRE_NAME,
                Map.of("roundIdx", 0, "categoryIdx", 0, "questionIdx", 0)
        );

        assertTrue(session.handleAction(selectQuestion, "host", true).accepted());
        assertFalse(session.handleAction(
                new GameActionMessage(
                        "ROOM01",
                        IJeopardyAction.SelectRound.WIRE_NAME,
                        Map.of("roundIdx", 0)
                ),
                "host",
                true
        ).accepted());
        assertFalse(session.handleAction(selectQuestion, "host", true).accepted());
        assertEquals(GameState.QUESTION_SELECTED, session.getCurrentState());
    }

    private static JeopardySession createSession(MediaType mediaType, String mediaPath) {
        QuestionDefinition question = new QuestionDefinition(100, "Prompt", mediaPath, mediaType);
        JeopardyDefinition definition = new JeopardyDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(
                                question,
                                new QuestionDefinition(200, "Second prompt", null, MediaType.UNDEFINED)
                        ))
                )))
        );
        return new JeopardySession("ROOM01", java.util.Set.of("host", "player"), definition);
    }
}