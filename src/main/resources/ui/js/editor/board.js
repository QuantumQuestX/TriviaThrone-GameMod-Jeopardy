import { Question } from "./question.js";

export class Board {
    constructor(container, onCellClickedCallback) {
        this.container = container;
        this.onCellClickedCallback = onCellClickedCallback;

        this.round = null;

        this.editingQuestion = null;
        this.appliedQuestionIds = new Set();
    }

    loadRound(round) {
        this.round = round;
        this.editingQuestion = null;
        this.render();
    }

    render() {
        if (!this.round) {
            return;
        }

        const catCount = this.round.categoryCount;
        const qCount = this.round.questionCount;

        this.container.innerHTML = "";
        this.container.style.display = "grid";
        this.container.style.gridTemplateColumns = `repeat(${catCount}, 1fr)`;
        this.container.style.gridTemplateRows = `repeat(${qCount}, 1fr)`;

        for (let c = 0; c < catCount; c++) {
            const cat = document.createElement("div");

            cat.className = "j-category";
            cat.textContent = this.round.categories[c]?.name || `Category ${c + 1}`;

            cat.addEventListener("click", () => {
                const name = prompt("Enter category name:", cat.textContent);

                if (name) {
                    this.round.categories[c].name = name;
                    this.render();
                }
            });

            this.container.appendChild(cat);
        }

        for (let r = 0; r < qCount; r++) {
            for (let c = 0; c < catCount; c++) {
                this.renderCell(r, c);
            }
        }
    }

    renderCell(rowIdx, categoryIdx) {
        if (!this.round) {
            return;
        }

        const cell = document.createElement("div");
        const question = this.round.questions[rowIdx][categoryIdx];

        cell.className = "j-cell";
        cell.textContent = question.points;

        if (this.editingQuestion?.id === question.id) {
            cell.classList.add("editing");
        } else if (this.round.appliedQuestionIds.has(question.id)) {
            cell.classList.add("applied");
        } else {
            cell.classList.add("empty");
        }

        cell.addEventListener("click", () => {
            this.editingQuestion = question;
            this.render();
            this.onCellClickedCallback(question);
        });

        this.container.appendChild(cell);
    }

    applyQuestion(question) {
        this.round.appliedQuestionIds.add(question.id);
        this.deselectCell();
        this.render();
    }

    deselectCell() {
        this.editingQuestion = null;
        this.render();
    }
}
