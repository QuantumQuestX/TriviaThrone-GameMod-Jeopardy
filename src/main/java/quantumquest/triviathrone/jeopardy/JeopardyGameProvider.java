package quantumquest.triviathrone.jeopardy;

import quantumquest.triviathrone.api.game.GameUiDescriptor;
import quantumquest.triviathrone.api.game.IGameCreator;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameProvider;
import quantumquest.triviathrone.jeopardy.creator.JeopardyGameCreator;
import quantumquest.triviathrone.jeopardy.definition.GameDefinitionLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class JeopardyGameProvider implements IGameProvider {
    private static final String FILE_SUFFIX = ".game.throne";
    private static final IGameCreator CREATOR = new JeopardyGameCreator();

    @Override
    public String gameType() {
        return "jeopardy";
    }

    @Override
    public String gameDataFile() {
        return "game.xml";
    }

    @Override
    public String gameDataFormatVersion() {
        return "1.0";
    }

    @Override
    public Optional<IGameCreator> creator() {
        return Optional.of(CREATOR);
    }

    @Override
    public Optional<GameUiDescriptor> uiDescriptor() {
        return Optional.of(new GameUiDescriptor("editor.html", "game.html"));
    }

    @Override
    public boolean supportsFile(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX);
    }

    @Override
    public Optional<IGameDefinition> load(Path gameDirectory) {
        if (!Files.isRegularFile(gameDirectory.resolve("manifest.xml"))) {
            return Optional.empty();
        }
        return Optional.of(GameDefinitionLoader.parse(gameDirectory));
    }
}
