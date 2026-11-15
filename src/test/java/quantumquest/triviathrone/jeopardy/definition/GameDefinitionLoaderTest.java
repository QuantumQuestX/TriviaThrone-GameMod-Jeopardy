package quantumquest.triviathrone.jeopardy.definition;

import quantumquest.triviathrone.api.game.GamePackageManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import quantumquest.triviathrone.jeopardy.JeopardyGameProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameDefinitionLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsMultipleChoiceOptionsAndCorrectChoice() throws IOException {
        writeManifest("""
                <options>
                    <option>Atlantic Ocean</option>
                    <option correct="true">Pacific Ocean</option>
                    <option>Indian Ocean</option>
                </options>
                """);

        QuestionDefinition question = GameDefinitionLoader.parse(tempDir)
                .rounds().get(0).categories().get(0).questions().get(0);

        assertEquals(List.of("Atlantic Ocean", "Pacific Ocean", "Indian Ocean"), question.choices());
        assertEquals(1, question.correctChoiceIndex());
    }

    @Test
    void rejectsMultipleChoiceQuestionsWithoutExactlyOneCorrectChoice() throws IOException {
        writeManifest("""
                <options>
                    <option>Atlantic Ocean</option>
                    <option>Pacific Ocean</option>
                </options>
                """);

        assertThrows(IllegalArgumentException.class, () -> GameDefinitionLoader.parse(tempDir));
    }

    private void writeManifest(String options) throws IOException {
        Files.writeString(tempDir.resolve("game.xml"), """
                <game version="1.0">
                    <meta><title>Test</title><description>Test game</description></meta>
                    <rounds>
                        <round name="Round 1" categoryCount="1" questionCount="1">
                            <categories><category row="0">Geography</category></categories>
                            <questions>
                                <question row="0" category="0" points="200" type="mcq">
                                    <prompt>Which ocean is largest?</prompt>
                                    %s
                                </question>
                            </questions>
                        </round>
                    </rounds>
                </game>
                """.formatted(options));
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
                List.of()
        ).write(tempDir);
    }
}