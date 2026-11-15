import * as gameSocket from "./game-socket.js";

export class GameBoardView {
    constructor(lobbyCode) {
        this.lobbyCode = lobbyCode;
        this.isHost = false;
        this.selectedRoundIdx = 0;
    }

    setHost(isHost) {
        this.isHost = isHost;
    }

    render(state, preserveSelection = false) {
        const board = document.getElementById("jeopardy-board");
        const questionDisplay = document.getElementById("question-display");
        const hasQuestion = Boolean(state.question);

        board.classList.toggle("hidden", hasQuestion);
        questionDisplay.classList.toggle("hidden", !hasQuestion);

        if (hasQuestion) {
            return;
        }

        if (!preserveSelection && Number.isInteger(state.curRoundIdx)) {
            this.selectedRoundIdx = state.curRoundIdx;
        }

        const roundIndex = this.selectedRoundIdx;
        const round = state.rounds?.[roundIndex];

        if (!round) {
            return;
        }

        document.getElementById("jeopardy-round-title").textContent = round.name;
        this.renderRoundSelector(state, roundIndex);
        this.renderQuestionGrid(state, round, roundIndex);
    }

    renderRoundSelector(state, roundIndex) {
        const roundSelector = document.getElementById("round-selector");

        roundSelector.replaceChildren();
        state.rounds.forEach((round, index) => {
            const tab = document.createElement("button");

            tab.className = "secondary-btn tt-button tt-button--secondary";
            tab.textContent = round.name;
            tab.disabled = !this.isHost || state.state !== "IDLE";
            tab.setAttribute("role", "tab");
            tab.setAttribute("aria-selected", String(index === roundIndex));
            tab.addEventListener("click", () => {
                this.selectedRoundIdx = index;
                this.render(state, true);
                gameSocket.sendAction(this.lobbyCode, gameSocket.Actions.SELECT_ROUND, { roundIdx: index });
            });
            roundSelector.appendChild(tab);
        });
    }

    renderQuestionGrid(state, round, roundIndex) {
        const canvas = document.getElementById("jeopardy-canvas");

        canvas.replaceChildren();
        canvas.style.gridTemplateColumns = `repeat(${Math.max(round.categories.length, 1)}, minmax(0, 1fr))`;
        canvas.style.gridTemplateRows = `repeat(${round.questions.length + 1}, minmax(0, 1fr))`;

        round.categories.forEach((category) => {
            const heading = document.createElement("div");

            heading.className = "j-category";
            heading.textContent = String(category ?? "");
            canvas.appendChild(heading);
        });

        round.questions.forEach((row, questionIndex) => {
            row.forEach((tile, categoryIndex) => {
                canvas.appendChild(
                    this.createQuestionCell(state, round, tile, {
                        roundIndex,
                        questionIndex,
                        categoryIndex,
                    }),
                );
            });
        });
    }

    createQuestionCell(state, round, tile, position) {
        const { roundIndex, questionIndex, categoryIndex } = position;
        const cell = document.createElement("button");

        cell.className = "j-cell";
        cell.textContent = tile.used ? "✓" : tile.points;
        cell.disabled = !this.isHost || state.state !== "IDLE" || tile.used;

        if (tile.used) {
            cell.classList.add("used");
        }

        if (
            state.curRoundIdx === roundIndex &&
            state.curCatIdx === categoryIndex &&
            state.curQuestionIdx === questionIndex &&
            state.state !== "IDLE"
        ) {
            cell.classList.add("selected");
        }

        cell.setAttribute(
            "aria-label",
            `${round.categories[categoryIndex]}, ${tile.points} points${tile.used ? ", used" : ""}`,
        );
        cell.addEventListener("click", () =>
            gameSocket.sendAction(this.lobbyCode, gameSocket.Actions.SELECT_QUESTION, {
                roundIdx: roundIndex,
                categoryIdx: categoryIndex,
                questionIdx: questionIndex,
            }),
        );

        return cell;
    }
}
