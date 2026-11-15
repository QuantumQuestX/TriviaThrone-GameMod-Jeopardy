package quantumquest.triviathrone.jeopardy.action;

public sealed interface IJeopardyAction permits IJeopardyAction.Command,
        IJeopardyAction.SelectQuestion,
        IJeopardyAction.SelectRound {

    enum Command implements IJeopardyAction {
        BUZZ("BUZZ"),
        PLAY_MEDIA("PLAY_MEDIA"),
        OPEN_BUZZ("OPEN_BUZZ"),
        CLOSE_BUZZ("CLOSE_BUZZ"),
        MARK_CORRECT("MARK_CORRECT"),
        MARK_INCORRECT("MARK_INCORRECT"),
        RESET("RESET");

        private final String wireName;

        Command(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }
    }

    record SelectRound(int roundIndex) implements IJeopardyAction {
        public static final String WIRE_NAME = "SELECT_ROUND";
    }

    record SelectQuestion(int roundIndex, int categoryIndex, int questionIndex) implements IJeopardyAction {
        public static final String WIRE_NAME = "SELECT_QUESTION";
    }
}
