package quantumquest.triviathrone.jeopardy.definition;

import java.util.List;
import java.util.Objects;

public record Round(String name, List<Category> categories) {
    public Round {
        Objects.requireNonNull(name, "name");
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    }
}
