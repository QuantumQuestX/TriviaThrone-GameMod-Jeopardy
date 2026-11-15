package quantumquest.triviathrone.jeopardy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import quantumquest.triviathrone.api.game.GameCreationAsset;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameFileWriter;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.jeopardy.creator.JeopardyGameCreator;
import quantumquest.triviathrone.jeopardy.definition.JeopardyDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JeopardyGameCreatorTest {
    @TempDir
    Path tempDir;

    @Test
    void createsMultiRoundGameWithBundledMediaThatCanBeLoadedByItsProvider() throws IOException {
        byte[] imageBytes = new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a
        };
        Path uploadedImage = tempDir.resolve("uploaded-cover.png");
        Files.write(uploadedImage, imageBytes);

        ObjectNode data = JsonNodeFactory.instance.objectNode();
        ObjectNode meta = data.putObject("meta");
        meta.put("title", "Test game");
        meta.put("description", "A generated game");
        data.putArray("resources").addObject()
                .put("id", "cover-id")
                .put("name", "cover.png")
                .put("type", "image");

        ArrayNode rounds = data.putArray("rounds");
        ObjectNode firstRound = rounds.addObject();
        firstRound.put("name", "Round 1");
        firstRound.putArray("categories").addObject().put("name", "Science");
        firstRound.withArray("categories").addObject().put("name", "Nature");
        ArrayNode firstQuestions = firstRound.putArray("questions");
        ArrayNode firstRow = firstQuestions.addArray();
        firstRow.add(question(200, "mcq", "What is H2O?", null, true));
        firstRow.add(question(200, "text", "What is photosynthesis?", null, false));
        ArrayNode secondRow = firstQuestions.addArray();
        secondRow.add(question(400, "image", "Name this object.", "cover-id", false));
        secondRow.add(question(400, "text", "What is a mammal?", null, false));

        ObjectNode secondRound = rounds.addObject();
        secondRound.put("name", "Final Round");
        secondRound.putArray("categories").addObject().put("name", "Final");
        secondRound.putArray("questions").addArray()
                .add(question(500, "text", "What is trivia?", null, false));

        GameFileWriter writer = relativePath -> {
            Path file = tempDir.resolve(relativePath);
            Files.createDirectories(file.getParent());
            return Files.newOutputStream(file);
        };
        new JeopardyGameCreator().create(new GameCreationRequest(
                data,
                Map.of("cover-id", new GameCreationAsset("cover.png", uploadedImage))
        ), writer);
        assertArrayEquals(imageBytes, Files.readAllBytes(tempDir.resolve("resources/asset-0.png")));
        new GamePackageManifest(
                GamePackageManifest.CURRENT_MANIFEST_VERSION,
                "jeopardy",
                "1.0.0",
                "2.0.0",
                "jeopardy",
                JeopardyGameProvider.class.getName(),
                "game.xml",
                "1.0",
                "test",
                null,
                List.of(new GamePackageManifest.Asset("resources/asset-0.png", null))
        ).write(tempDir);

        JeopardyDefinition definition = (JeopardyDefinition) new JeopardyGameProvider()
                .load(tempDir)
                .orElseThrow();
        assertEquals("Test game", definition.title());
        assertEquals(2, definition.rounds().size());
        assertEquals("Science", definition.rounds().get(0).categories().get(0).name());
        assertEquals("What is H2O?", definition.rounds().get(0).categories().get(0).questions().get(0).prompt());
        assertEquals(0, definition.rounds().get(0).categories().get(0).questions().get(0).correctChoiceIndex());
        assertEquals("resources/asset-0.png",
                definition.rounds().get(0).categories().get(0).questions().get(1).mediaPath());
    }

    private static ObjectNode question(
            int points,
            String type,
            String prompt,
            String media,
            boolean multipleChoice
    ) {
        ObjectNode question = JsonNodeFactory.instance.objectNode();
        question.put("points", points);
        question.put("type", type);
        question.put("prompt", prompt);
        if (media == null) {
            question.putNull("media");
        } else {
            question.put("media", media);
        }
        if (multipleChoice) {
            ArrayNode options = question.putArray("options");
            options.addObject().put("text", "Water").put("correct", true);
            options.addObject().put("text", "Oxygen").put("correct", false);
        } else {
            question.putNull("options");
        }
        return question;
    }

    @Test
    void packagesEditorAndGamePagesAsModResources() {
        ClassLoader modClassLoader = JeopardyGameProvider.class.getClassLoader();

        org.junit.jupiter.api.Assertions.assertNotNull(modClassLoader.getResource("ui/editor.html"));
        org.junit.jupiter.api.Assertions.assertNotNull(modClassLoader.getResource("ui/game.html"));
        org.junit.jupiter.api.Assertions.assertNotNull(modClassLoader.getResource("ui/css/game.css"));
        org.junit.jupiter.api.Assertions.assertNotNull(modClassLoader.getResource("ui/js/game/index.js"));
    }
}