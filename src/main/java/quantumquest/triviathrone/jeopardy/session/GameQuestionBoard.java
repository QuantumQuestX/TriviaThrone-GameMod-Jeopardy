package quantumquest.triviathrone.jeopardy.session;

import quantumquest.triviathrone.jeopardy.definition.Category;
import quantumquest.triviathrone.jeopardy.definition.JeopardyDefinition;
import quantumquest.triviathrone.jeopardy.definition.QuestionDefinition;
import quantumquest.triviathrone.jeopardy.definition.Round;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GameQuestionBoard {
    private final JeopardyDefinition definition;
    private final Set<String> usedQuestions = ConcurrentHashMap.newKeySet();
    private int currentRoundIdx;
    private int currentCategoryIdx;
    private int currentQuestionIdx;

    GameQuestionBoard(JeopardyDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    void selectQuestion(Integer round, Integer category, Integer question) {
        Objects.requireNonNull(round, "round");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(question, "question");

        if (!isValidRoundIndex(round)) {
            throw new IllegalArgumentException("Round index is out of range");
        }
        if (!isValidCategoryIndex(round, category)) {
            throw new IllegalArgumentException("Category index is out of range");
        }
        if (!isValidQuestionIndex(round, category, question)) {
            throw new IllegalArgumentException("Question index is out of range");
        }

        String key = questionKey(round, category, question);
        if (usedQuestions.contains(key)) {
            throw new IllegalStateException("Question already used");
        }

        currentRoundIdx = round;
        currentCategoryIdx = category;
        currentQuestionIdx = question;
        usedQuestions.add(key);
    }

    void selectRound(Integer round) {
        Objects.requireNonNull(round, "round");
        if (!isValidRoundIndex(round)) {
            throw new IllegalArgumentException("Round index is out of range");
        }
        currentRoundIdx = round;
    }

    boolean isValidRoundIndex(int round) {
        return round >= 0 && round < definition.rounds().size();
    }

    boolean isValidQuestion(int round, int category, int question) {
        return isValidQuestionIndex(round, category, question);
    }

    boolean isQuestionUsed(int round, int category, int question) {
        return usedQuestions.contains(questionKey(round, category, question));
    }

    boolean hasUnusedQuestions() {
        int totalQuestions = definition.rounds().stream()
                .flatMap(round -> round.categories().stream())
                .mapToInt(category -> category.questions().size())
                .sum();
        return usedQuestions.size() < totalQuestions;
    }

    JeopardyDefinition definition() {
        return definition;
    }

    Round currentRound() {
        return definition.rounds().get(currentRoundIdx);
    }

    Category currentCategory() {
        return currentRound().categories().get(currentCategoryIdx);
    }

    QuestionDefinition currentQuestion() {
        return currentCategory().questions().get(currentQuestionIdx);
    }

    int currentRoundIdx() {
        return currentRoundIdx;
    }

    int currentCategoryIdx() {
        return currentCategoryIdx;
    }

    int currentQuestionIdx() {
        return currentQuestionIdx;
    }

    private static String questionKey(int round, int category, int question) {
        return round + ":" + category + ":" + question;
    }

    private boolean isValidCategoryIndex(int round, int category) {
        return isValidRoundIndex(round) && category >= 0
                && category < definition.rounds().get(round).categories().size();
    }

    private boolean isValidQuestionIndex(int round, int category, int question) {
        return isValidCategoryIndex(round, category) && question >= 0
                && question < definition.rounds().get(round).categories().get(category).questions().size();
    }
}
