import { Round } from "./round.js";
import { ResourceManager } from "./resource-manager.js";
import { ResourceHandler } from "./resource-handler.js";
import { QuestionEditor } from "./question-editor.js";
import { createGameArchive } from "./builder.js";

export class GameController {
    constructor(boardController) {
        this.boardController = boardController;
        this.resourceManager = new ResourceManager();

        this.game = { rounds: [], resources: this.resourceManager };
        this.game.meta = {
            id: crypto.randomUUID(),
            title: "",
            description: "",
            created: new Date().toISOString(),
            version: "1.0",
        };
        this.currentRoundIdx = -1;

        // Elements
        this.roundNameInput = document.getElementById("round-name");
        this.roundSelect = document.getElementById("round-select");
        this.addRoundBtn = document.getElementById("add-round-btn");
        this.removeRoundBtn = document.getElementById("remove-round-btn");

        this.questionInc = document.getElementById("question-count-inc");
        this.questionDec = document.getElementById("question-count-dec");
        this.categoryInc = document.getElementById("category-count-inc");
        this.categoryDec = document.getElementById("category-count-dec");
        this.questionLabel = document.getElementById("question-count-text");
        this.categoryLabel = document.getElementById("category-count-text");

        this.saveGameButton = document.getElementById("save-draft-btn");

        this.gameTitleInp = document.getElementById("game-title");
        this.gameDescriptionInp = document.getElementById("game-description");

        // Constants
        this.minQuestionCount = 1;
        this.maxQuestionCount = 6;

        this.minCategoryCount = 1;
        this.maxCategoryCount = 5;
    }

    init() {
        this.addRoundBtn.addEventListener("click", () => this.addRound());
        this.removeRoundBtn.addEventListener("click", () => this.removeRound());
        this.roundSelect.addEventListener("change", (e) => this.changeRound(e));

        this.questionInc.addEventListener("click", () => this.modifyQuestionCount(1));
        this.questionDec.addEventListener("click", () => this.modifyQuestionCount(-1));
        this.categoryInc.addEventListener("click", () => this.modifyCategoryCount(1));
        this.categoryDec.addEventListener("click", () => this.modifyCategoryCount(-1));

        this.refreshRoundSelect();
        this.updateCountLabels();

        this.resourceHandler = new ResourceHandler(this.resourceManager);
        this.resourceHandler.init();

        const questionEditorPanel = document.getElementById("question-editor-panel");
        const questionEditor = new QuestionEditor(
            questionEditorPanel,
            (q) => this.boardController.board.applyQuestion(q),
            this.resourceManager,
        );

        this.boardController.setQuestionEditor(questionEditor);

        this.saveGameButton.addEventListener("click", () => this.createGameArchive());
    }

    refreshRoundSelect() {
        this.roundSelect.innerHTML = "";
        this.game.rounds.forEach((round, idx) => {
            const option = document.createElement("option");

            option.value = String(idx);
            option.textContent = round.name;
            this.roundSelect.appendChild(option);
        });

        if (this.currentRoundIdx >= 0) {
            this.roundSelect.value = this.currentRoundIdx;
        }
    }

    addRound() {
        const roundNumber = this.game.rounds.length + 1;
        let name = this.roundNameInput.value.trim() || `Round ${roundNumber}`;

        this.roundNameInput.value = "";

        const round = new Round(name);

        round.board = this.boardController.board;
        this.game.rounds.push(round);
        this.currentRoundIdx = this.game.rounds.length - 1;

        this.refreshRoundSelect();
        this.boardController.loadRound(round);
        this.updateCountLabels();
    }

    removeRound() {
        if (this.currentRoundIdx < 0) {
            return;
        }

        this.game.rounds.splice(this.currentRoundIdx, 1);
        this.currentRoundIdx = this.game.rounds.length ? Math.max(0, this.currentRoundIdx - 1) : -1;
        this.refreshRoundSelect();
        this.updateCountLabels();
    }

    changeRound(e) {
        this.currentRoundIdx = parseInt(e.target.value, 10);

        const round = this.game.rounds[this.currentRoundIdx];

        this.boardController.loadRound(round);
        this.updateCountLabels();
    }

    modifyQuestionCount(delta) {
        const round = this.game.rounds[this.currentRoundIdx];

        if (!round) {
            return;
        }

        const newCount = Math.min(this.maxQuestionCount, Math.max(this.minQuestionCount, round.questionCount + delta));

        round.resize(round.categoryCount, newCount);
        this.boardController.loadRound(round);
        this.updateCountLabels();
    }

    modifyCategoryCount(delta) {
        const round = this.game.rounds[this.currentRoundIdx];

        if (!round) {
            return;
        }

        const newCount = Math.min(this.maxCategoryCount, Math.max(this.minCategoryCount, round.categoryCount + delta));

        round.resize(newCount, round.questionCount);
        this.boardController.loadRound(round);
        this.updateCountLabels();
    }

    updateCountLabels() {
        if (this.currentRoundIdx < 0) {
            return;
        }

        const round = this.game.rounds[this.currentRoundIdx];

        if (!round) {
            return;
        }

        this.questionLabel.textContent = round.questionCount;
        this.categoryLabel.textContent = round.categoryCount;
    }

    createGameArchive() {
        this.game.meta.title = this.gameTitleInp.value;
        this.game.meta.description = this.gameDescriptionInp.value;

        createGameArchive(this.game).catch((err) => {
            alert("Failed to create game file:\n" + err);
        });
    }
}
