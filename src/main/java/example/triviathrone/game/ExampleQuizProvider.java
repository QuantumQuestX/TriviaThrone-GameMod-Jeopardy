package example.triviathrone.game;

import quantumquest.triviathrone.api.game.GameUiDescriptor;
import quantumquest.triviathrone.api.game.IGameCreator;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameProvider;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public final class ExampleQuizProvider implements IGameProvider {
    static final String GAME_DATA_FILE = "game.properties";
    private static final String GAME_FILE_SUFFIX = ".game.throne";
    private static final IGameCreator CREATOR = new ExampleQuizCreator();

    @Override
    public String gameType() {
        return "example-quiz";
    }

    @Override
    public String gameDataFile() {
        return GAME_DATA_FILE;
    }

    @Override
    public String gameDataFormatVersion() {
        return "1.0";
    }

    @Override
    public boolean supportsFile(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(GAME_FILE_SUFFIX);
    }

    @Override
    public Optional<IGameDefinition> load(Path gameDirectory) throws IOException {
        Path gameFile = gameDirectory.resolve(GAME_DATA_FILE);
        if (!Files.isRegularFile(gameFile)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(gameFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        return Optional.of(new ExampleQuizDefinition(
                requiredProperty(properties, "title"),
                requiredProperty(properties, "question"),
                requiredProperty(properties, "answer")
        ));
    }

    @Override
    public Optional<IGameCreator> creator() {
        return Optional.of(CREATOR);
    }

    @Override
    public Optional<GameUiDescriptor> uiDescriptor() {
        return Optional.of(new GameUiDescriptor("editor.html", "game.html"));
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The game data field \"" + name + "\" is required.");
        }
        return value.trim();
    }
}
