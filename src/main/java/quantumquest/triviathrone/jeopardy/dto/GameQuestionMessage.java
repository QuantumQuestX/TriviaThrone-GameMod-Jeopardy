package quantumquest.triviathrone.jeopardy.dto;

import java.util.List;
import java.util.Objects;

public record GameQuestionMessage(
        String category,
        String prompt,
        List<String> choices,
        Integer correctChoiceIndex,
        MediaType mediaType,
        String mediaUrl,
        int points
) {
    public GameQuestionMessage {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(mediaType, "mediaType");
        choices = choices == null ? List.of() : List.copyOf(choices);
    }
}
