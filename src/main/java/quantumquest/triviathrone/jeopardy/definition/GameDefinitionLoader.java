package quantumquest.triviathrone.jeopardy.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.jeopardy.JeopardyGameProvider;
import quantumquest.triviathrone.jeopardy.dto.MediaType;
import quantumquest.triviathrone.jeopardy.resource.GameResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameDefinitionLoader {
    private GameDefinitionLoader() {
    }

    public static JeopardyDefinition parse(Path lobbyDir) {
        Objects.requireNonNull(lobbyDir, "lobbyDir");
        try {
            GamePackageManifest manifest = GamePackageManifest.read(lobbyDir);
            if (!"jeopardy".equals(manifest.modId())
                    || !"jeopardy".equals(manifest.gameType())
                    || !JeopardyGameProvider.class.getName().equals(manifest.providerClass())) {
                throw new IllegalArgumentException("The package manifest does not identify the Jeopardy provider.");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Path gameDataFile = lobbyDir.resolve(manifest.gameDataFile());
            Document doc = factory.newDocumentBuilder().parse(gameDataFile.toFile());

            doc.getDocumentElement().normalize();

            Element gameElement = doc.getDocumentElement();
            if (!"game".equals(gameElement.getTagName())
                    || !manifest.gameDataFormatVersion().equals(gameElement.getAttribute("version"))) {
                throw new IllegalArgumentException("The Jeopardy game-data format version does not match the manifest.");
            }
            GameResourceResolver resources = new GameResourceResolver(lobbyDir, gameElement);

            Element metaElement = getSingleChild(gameElement, "meta");

            String title = getTextContent(metaElement, "title");
            String description = getTextContent(metaElement, "description");

            Element roundsElement = getSingleChild(gameElement, "rounds");
            NodeList roundNodes = roundsElement.getElementsByTagName("round");

            List<Round> rounds = new ArrayList<>();

            for (int i = 0; i < roundNodes.getLength(); i++) {
                Element roundElement = (Element) roundNodes.item(i);

                String roundName = roundElement.getAttribute("name");

                int categoryCount = Integer.parseInt(roundElement.getAttribute("categoryCount"));
                int questionCount = Integer.parseInt(roundElement.getAttribute("questionCount"));
                if (categoryCount < 1 || questionCount < 1) {
                    throw new IllegalArgumentException("Each round must have categories and questions");
                }

                QuestionDefinition[][] matrix = new QuestionDefinition[categoryCount][questionCount];

                Element questionsElement = getSingleChild(roundElement, "questions");
                NodeList questionNodes = questionsElement.getElementsByTagName("question");

                for (int q = 0; q < questionNodes.getLength(); q++) {
                    Element questionElement = (Element) questionNodes.item(q);

                    int row = Integer.parseInt(questionElement.getAttribute("row"));
                    int category = Integer.parseInt(questionElement.getAttribute("category"));
                    int points = Integer.parseInt(questionElement.getAttribute("points"));
                    if (category < 0 || category >= categoryCount || row < 0 || row >= questionCount) {
                        throw new IllegalArgumentException("Question position is outside the configured board");
                    }
                    if (matrix[category][row] != null) {
                        throw new IllegalArgumentException("Duplicate question position");
                    }

                    String prompt = getTextContent(questionElement, "prompt");
                    GameQuestionChoicesParser.ParsedChoices parsedChoices =
                            GameQuestionChoicesParser.parse(questionElement);
                    if ("mcq".equals(questionElement.getAttribute("type")) && parsedChoices.choices().isEmpty()) {
                        throw new IllegalArgumentException("Multiple-choice questions must have answer choices");
                    }

                    String mediaPath = resources.mediaPathFor(questionElement);
                    MediaType mediaType = resources.detectMediaType(mediaPath);

                    matrix[category][row] = new QuestionDefinition(
                            points,
                            prompt,
                            mediaPath,
                            mediaType,
                            parsedChoices.choices(),
                            parsedChoices.correctChoiceIndex()
                    );
                }

                List<Category> categories = new ArrayList<>();
                NodeList categoryNodes = getSingleChild(roundElement, "categories").getElementsByTagName("category");

                for (int c = 0; c < categoryCount; c++) {
                    List<QuestionDefinition> questions = new ArrayList<>();

                    for (int r = 0; r < questionCount; r++) {
                        QuestionDefinition qDef = matrix[c][r];
                        if (qDef == null) {
                            throw new IllegalStateException("missing question at category " + c + ", row " + r);
                        }
                        questions.add(qDef);
                    }

                    String categoryName = c < categoryNodes.getLength()
                            ? categoryNodes.item(c).getTextContent().trim()
                            : "";
                    categories.add(new Category(
                            categoryName.isBlank() ? "Category " + (c + 1) : categoryName,
                            questions
                    ));
                }

                rounds.add(new Round(roundName, categories));
            }

            return new JeopardyDefinition(title, description, rounds);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Could not parse Jeopardy game data", e);
        }
    }

    private static Element getSingleChild(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            throw new IllegalStateException("Missing element: " + tag);
        }
        return (Element) list.item(0);
    }

    private static String getTextContent(Element root, String tag) {
        Element child = getSingleChild(root, tag);
        return child.getTextContent().trim();
    }

}
