package quantumquest.triviathrone.jeopardy.definition;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

final class GameQuestionChoicesParser {
    private GameQuestionChoicesParser() {
    }

    static ParsedChoices parse(Element question) {
        NodeList optionsNodes = question.getElementsByTagName("options");
        if (optionsNodes.getLength() == 0) {
            return new ParsedChoices(List.of(), null);
        }
        if (optionsNodes.getLength() != 1) {
            throw new IllegalArgumentException("Question must have at most one options element");
        }

        NodeList optionNodes = ((Element) optionsNodes.item(0)).getElementsByTagName("option");
        if (optionNodes.getLength() < 2 || optionNodes.getLength() > 4) {
            throw new IllegalArgumentException("Multiple-choice questions must have between two and four choices");
        }

        List<String> choices = new ArrayList<>();
        Integer correctChoiceIndex = null;
        for (int index = 0; index < optionNodes.getLength(); index++) {
            Element option = (Element) optionNodes.item(index);
            String text = option.getTextContent().trim();
            if (text.isBlank()) {
                throw new IllegalArgumentException("Multiple-choice choices cannot be empty");
            }

            String correctAttribute = option.getAttribute("correct");
            if (!correctAttribute.isBlank()
                    && !"true".equals(correctAttribute)
                    && !"false".equals(correctAttribute)) {
                throw new IllegalArgumentException("Multiple-choice correct attribute must be true or false");
            }
            if ("true".equals(correctAttribute)) {
                if (correctChoiceIndex != null) {
                    throw new IllegalArgumentException("Multiple-choice questions must have exactly one correct choice");
                }
                correctChoiceIndex = index;
            }
            choices.add(text);
        }

        if (correctChoiceIndex == null) {
            throw new IllegalArgumentException("Multiple-choice questions must have exactly one correct choice");
        }
        return new ParsedChoices(choices, correctChoiceIndex);
    }

    record ParsedChoices(List<String> choices, Integer correctChoiceIndex) {
    }
}
