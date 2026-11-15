package quantumquest.triviathrone.jeopardy.creator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import quantumquest.triviathrone.api.game.GameCreationAsset;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import quantumquest.triviathrone.api.game.GameFileWriter;
import quantumquest.triviathrone.api.game.IGameCreator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JeopardyGameCreator implements IGameCreator {
    private static final Set<String> RESOURCE_TYPES = Set.of("image", "video", "audio", "unknown");
    private static final Set<String> QUESTION_TYPES = Set.of("text", "mcq", "image", "video", "audio");
    private static final GameCreatorDescriptor DESCRIPTOR = new GameCreatorDescriptor(
            "Jeopardy",
            "Build a Jeopardy-style trivia board with custom rounds, categories, questions, and media.",
            editorSchema()
    );

    @Override
    public GameCreatorDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void create(GameCreationRequest request, GameFileWriter files) throws IOException {
        JsonNode data = request.data();
        JsonNode meta = requiredObject(data, "meta");
        String title = requiredText(meta, "title");
        String description = optionalText(meta, "description");
        List<Resource> resources = readResources(data, request.assets(), files);
        JsonNode rounds = requiredArray(data, "rounds");
        if (rounds.isEmpty()) {
            throw new IllegalArgumentException("A Jeopardy game must contain at least one round.");
        }

        try (OutputStream output = files.createFile("game.xml")) {
            try {
                writeGameXml(output, title, description, resources, rounds);
            } catch (XMLStreamException exception) {
                throw new IOException("Could not write Jeopardy game data.", exception);
            }
        }
    }

    private static List<Resource> readResources(
            JsonNode data,
            Map<String, GameCreationAsset> uploadedAssets,
            GameFileWriter files
    ) throws IOException {
        JsonNode resourcesNode = requiredArray(data, "resources");
        List<Resource> resources = new ArrayList<>();
        Set<String> resourceIds = new HashSet<>();

        for (int index = 0; index < resourcesNode.size(); index++) {
            JsonNode resourceNode = resourcesNode.get(index);
            String id = requiredText(resourceNode, "id");
            String name = requiredText(resourceNode, "name");
            String type = requiredText(resourceNode, "type");
            if (!RESOURCE_TYPES.contains(type)) {
                throw new IllegalArgumentException("Unsupported game resource type: " + type);
            }
            if (!resourceIds.add(id)) {
                throw new IllegalArgumentException("Duplicate game resource id: " + id);
            }

            GameCreationAsset asset = uploadedAssets.get(id);
            if (asset == null || !Files.isRegularFile(asset.path())) {
                throw new IllegalArgumentException("A file upload is required for resource: " + id);
            }

            String file = "resources/asset-" + index + safeExtension(asset.filename());
            try (InputStream input = Files.newInputStream(asset.path());
                    OutputStream output = files.createFile(file)) {
                input.transferTo(output);
            }
            resources.add(new Resource(id, file, type));
        }

        if (!resourceIds.equals(uploadedAssets.keySet())) {
            throw new IllegalArgumentException("Uploaded files must match the declared game resources.");
        }
        return List.copyOf(resources);
    }

    private static void writeGameXml(
            OutputStream output,
            String title,
            String description,
            List<Resource> resources,
            JsonNode rounds
    ) throws XMLStreamException {
        XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");
        xml.writeStartDocument("UTF-8", "1.0");
        xml.writeStartElement("game");
        xml.writeAttribute("version", "1.0");
        xml.writeAttribute("id", UUID.randomUUID().toString());
        xml.writeStartElement("meta");
        writeElement(xml, "title", title);
        writeElement(xml, "description", description);
        writeElement(xml, "created", java.time.Instant.now().toString());
        xml.writeEndElement();

        xml.writeStartElement("resources");
        for (Resource resource : resources) {
            xml.writeEmptyElement("resource");
            xml.writeAttribute("id", resource.id());
            xml.writeAttribute("file", resource.file());
            xml.writeAttribute("type", resource.type());
        }
        xml.writeEndElement();

        xml.writeStartElement("rounds");
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            writeRound(xml, rounds.get(roundIndex), roundIndex, resources);
        }
        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.flush();
    }

    private static void writeRound(XMLStreamWriter xml, JsonNode round, int roundIndex, List<Resource> resources)
            throws XMLStreamException {
        String name = requiredText(round, "name");
        JsonNode categories = requiredArray(round, "categories");
        JsonNode questions = requiredArray(round, "questions");
        if (categories.isEmpty() || categories.size() > 5 || questions.isEmpty() || questions.size() > 6) {
            throw new IllegalArgumentException("Each round must have one to five categories and one to six questions.");
        }

        xml.writeStartElement("round");
        xml.writeAttribute("index", Integer.toString(roundIndex));
        xml.writeAttribute("name", name);
        xml.writeAttribute("categoryCount", Integer.toString(categories.size()));
        xml.writeAttribute("questionCount", Integer.toString(questions.size()));

        xml.writeStartElement("categories");
        for (int index = 0; index < categories.size(); index++) {
            xml.writeStartElement("category");
            xml.writeAttribute("row", Integer.toString(index));
            xml.writeCharacters(requiredText(categories.get(index), "name"));
            xml.writeEndElement();
        }
        xml.writeEndElement();

        xml.writeStartElement("questions");
        for (int row = 0; row < questions.size(); row++) {
            JsonNode questionRow = questions.get(row);
            if (!questionRow.isArray() || questionRow.size() != categories.size()) {
                throw new IllegalArgumentException("Every question row must contain one question per category.");
            }
            for (int category = 0; category < questionRow.size(); category++) {
                writeQuestion(xml, questionRow.get(category), row, category, resources);
            }
        }
        xml.writeEndElement();
        xml.writeEndElement();
    }

    private static void writeQuestion(
            XMLStreamWriter xml,
            JsonNode question,
            int row,
            int category,
            List<Resource> resources
    ) throws XMLStreamException {
        String prompt = requiredText(question, "prompt");
        JsonNode pointsNode = question.get("points");
        if (pointsNode == null || !pointsNode.canConvertToInt()) {
            throw new IllegalArgumentException("Question points must be an integer.");
        }
        int points = pointsNode.intValue();
        if (points < 100 || points > 10000 || points % 100 != 0) {
            throw new IllegalArgumentException("Question points must be between 100 and 10000 in increments of 100.");
        }
        String type = requiredText(question, "type");
        if (!QUESTION_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported Jeopardy question type: " + type);
        }

        xml.writeStartElement("question");
        xml.writeAttribute("row", Integer.toString(row));
        xml.writeAttribute("category", Integer.toString(category));
        xml.writeAttribute("points", Integer.toString(points));
        xml.writeAttribute("type", type);
        writeElement(xml, "prompt", prompt);

        if ("mcq".equals(type)) {
            writeOptions(xml, requiredArray(question, "options"));
        } else if (Set.of("image", "video", "audio").contains(type)) {
            String mediaId = requiredText(question, "media");
            Resource resource = resources.stream()
                    .filter(candidate -> candidate.id().equals(mediaId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Question references an unknown media resource."));
            if (!type.equals(resource.type())) {
                throw new IllegalArgumentException("Question media type must match its selected resource.");
            }
            xml.writeEmptyElement("media");
            xml.writeAttribute("ref", mediaId);
        }

        xml.writeEndElement();
    }

    private static void writeOptions(XMLStreamWriter xml, JsonNode options) throws XMLStreamException {
        if (options.size() < 2 || options.size() > 4) {
            throw new IllegalArgumentException("Multiple-choice questions must have between two and four choices.");
        }
        int correctChoices = 0;
        xml.writeStartElement("options");
        for (JsonNode option : options) {
            String text = requiredText(option, "text");
            JsonNode correct = option.get("correct");
            if (correct == null || !correct.isBoolean()) {
                throw new IllegalArgumentException("Multiple-choice answers must indicate whether they are correct.");
            }
            xml.writeStartElement("option");
            if (correct.booleanValue()) {
                xml.writeAttribute("correct", "true");
                correctChoices++;
            }
            xml.writeCharacters(text);
            xml.writeEndElement();
        }
        xml.writeEndElement();
        if (correctChoices != 1) {
            throw new IllegalArgumentException("Multiple-choice questions must have exactly one correct choice.");
        }
    }

    private static void writeElement(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException("The field \"" + field + "\" must be an object.");
        }
        return value;
    }

    private static JsonNode requiredArray(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("The field \"" + field + "\" must be an array.");
        }
        return value;
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("The field \"" + field + "\" is required.");
        }
        return value.textValue().trim();
    }

    private static String optionalText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null) {
            return "";
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("The field \"" + field + "\" must be text.");
        }
        return value.textValue().trim();
    }

    private static String safeExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String extension = filename.substring(dot);
        return extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension : "";
    }

    private static JsonNode editorSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("meta").put("type", "object");
        properties.putObject("rounds").put("type", "array");
        properties.putObject("resources").put("type", "array");
        return schema;
    }

    private record Resource(String id, String file, String type) {
    }
}
