package quantumquest.triviathrone.jeopardy.definition;

import quantumquest.triviathrone.jeopardy.dto.MediaType;

import java.util.List;
import java.util.Objects;

public record QuestionDefinition(
        int points,
        String prompt,
        String mediaPath,
        MediaType mediaType,
        List<String> choices,
        Integer correctChoiceIndex
) {
    public QuestionDefinition {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(mediaType, "mediaType");
        choices = choices == null ? List.of() : List.copyOf(choices);
        if (choices.isEmpty() != (correctChoiceIndex == null)
                || correctChoiceIndex != null
                && (correctChoiceIndex < 0 || correctChoiceIndex >= choices.size())) {
            throw new IllegalArgumentException("Correct choice index must refer to an available choice");
        }
    }

    public QuestionDefinition(int points, String prompt, String mediaPath, MediaType mediaType) {
        this(points, prompt, mediaPath, mediaType, List.of(), null);
    }
}
