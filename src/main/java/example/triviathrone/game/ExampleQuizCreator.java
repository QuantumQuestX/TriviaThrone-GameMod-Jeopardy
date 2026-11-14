package example.triviathrone.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import quantumquest.triviathrone.api.game.GameFileWriter;
import quantumquest.triviathrone.api.game.IGameCreator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class ExampleQuizCreator implements IGameCreator {
    private static final GameCreatorDescriptor DESCRIPTOR = createDescriptor();

    @Override
    public GameCreatorDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void create(GameCreationRequest request, GameFileWriter files) throws IOException {
        if (!request.assets().isEmpty()) {
            throw new IllegalArgumentException("The example quiz does not accept uploaded assets.");
        }

        JsonNode data = request.data();
        Properties properties = new Properties();
        properties.setProperty("title", requiredText(data, "title"));
        properties.setProperty("question", requiredText(data, "question"));
        properties.setProperty("answer", requiredText(data, "answer"));

        try (Writer writer = new OutputStreamWriter(
                files.createFile(ExampleQuizProvider.GAME_DATA_FILE),
                StandardCharsets.UTF_8
        )) {
            properties.store(writer, "Example quiz game data");
        }
    }

    private static GameCreatorDescriptor createDescriptor() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        addTextField(properties, "title", "Game title", "Example quiz");
        addTextField(properties, "question", "Question", "What is the answer to life, the universe, and everything?");
        addTextField(properties, "answer", "Answer", "42");

        schema.putArray("required")
                .add("title")
                .add("question")
                .add("answer");

        return new GameCreatorDescriptor(
                "Example quiz",
                "Create a single-question quiz game to demonstrate the game mod API.",
                schema
        );
    }

    private static void addTextField(ObjectNode properties, String name, String title, String defaultValue) {
        ObjectNode field = properties.putObject(name);
        field.put("type", "string");
        field.put("title", title);
        field.put("default", defaultValue);
    }

    private static String requiredText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("The field \"" + field + "\" is required.");
        }
        return value.textValue().trim();
    }
}
