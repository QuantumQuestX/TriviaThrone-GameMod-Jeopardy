import { Board } from "./board.js";
import { Question } from "./question.js";
import { QuestionEditor } from "./question-editor.js";

export class BoardController {
    constructor(container) {
        this.container = container;
        this.board = new Board(container, this.cellClicked.bind(this));

        this.QUESTION_EDITOR_SIDE_PANEL = "right-panel";
        this.initUI();
    }

    setQuestionEditor(questionEditor) {
        this.questionEditor = questionEditor;
    }

    loadRound(round) {
        this.board.loadRound(round);
    }

    initUI() {
        document
            .getElementById("left-panel-fold-btn")
            .addEventListener("click", () => this.toggleSidePanel("left-panel"));

        document.getElementById("right-panel-fold-btn").addEventListener("click", () => {
            if (this.toggleSidePanel(this.QUESTION_EDITOR_SIDE_PANEL)) {
                this.questionEditor.close();
                this.board.deselectCell();
            }
        });
    }

    toggleSidePanel(panelId, toState = null) {
        const panel = document.getElementById(panelId);

        if (!panel) {
            return;
        }

        if (toState !== null) {
            panel.classList.toggle("collapsed", !toState);
        } else {
            panel.classList.toggle("collapsed");
        }

        return panel.classList.contains("collapsed");
    }

    cellClicked(question) {
        this.toggleSidePanel(this.QUESTION_EDITOR_SIDE_PANEL, true);
        this.questionEditor.open(question);
    }
}
