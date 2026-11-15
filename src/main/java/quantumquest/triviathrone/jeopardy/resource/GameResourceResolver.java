package quantumquest.triviathrone.jeopardy.resource;

import org.apache.tika.Tika;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import quantumquest.triviathrone.jeopardy.dto.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GameResourceResolver {
    private final Path lobbyDirectory;
    private final Map<String, String> resourcePaths;

    public GameResourceResolver(Path lobbyDirectory, Element gameElement) {
        this.lobbyDirectory = Objects.requireNonNull(lobbyDirectory, "lobbyDirectory");
        this.resourcePaths = readResourcePaths(Objects.requireNonNull(gameElement, "gameElement"));
    }

    public String mediaPathFor(Element question) {
        if (question.hasAttribute("media")) {
            return question.getAttribute("media");
        }

        NodeList mediaNodes = question.getElementsByTagName("media");
        if (mediaNodes.getLength() == 0) {
            return null;
        }

        String resourceId = ((Element) mediaNodes.item(0)).getAttribute("ref");
        String mediaPath = resourcePaths.get(resourceId);
        if (mediaPath == null) {
            throw new IllegalArgumentException("Question references an unknown media resource");
        }
        return mediaPath;
    }

    public MediaType detectMediaType(String mediaPath) throws IOException {
        if (mediaPath == null || mediaPath.isBlank()) {
            return MediaType.UNDEFINED;
        }

        Path root = lobbyDirectory.toAbsolutePath().normalize();
        Path mediaFile = root.resolve(mediaPath).normalize();
        if (!mediaFile.startsWith(root)) {
            return MediaType.UNDEFINED;
        }
        if (!Files.isRegularFile(mediaFile)) {
            return MediaType.UNDEFINED;
        }

        String contentType;
        try (InputStream content = Files.newInputStream(mediaFile)) {
            contentType = new Tika().detect(content);
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.UNDEFINED;
    }

    private static Map<String, String> readResourcePaths(Element gameElement) {
        Map<String, String> paths = new HashMap<>();
        NodeList resourceNodes = gameElement.getElementsByTagName("resource");
        for (int index = 0; index < resourceNodes.getLength(); index++) {
            Element resource = (Element) resourceNodes.item(index);
            String id = resource.getAttribute("id");
            String file = resource.getAttribute("file");
            if (!id.isBlank() && !file.isBlank() && paths.putIfAbsent(id, file) != null) {
                throw new IllegalArgumentException("Duplicate resource id: " + id);
            }
        }
        return Map.copyOf(paths);
    }
}
