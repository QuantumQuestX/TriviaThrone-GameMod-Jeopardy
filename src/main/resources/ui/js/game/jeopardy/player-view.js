export class PlayerView {
    constructor(username, { isSelf = false } = {}) {
        this.username = username;
        this.score = 0;
        this.isSelf = isSelf;

        this.element = this.createElement();
        this.buzzerElement = this.isSelf ? this.element : this.element.querySelector(".player-buzzer");
        this.scoreElement = this.isSelf
            ? document.getElementById("main-score")
            : this.element.querySelector(".player-score");
    }

    createElement() {
        if (this.isSelf) {
            return document.getElementById("main-buzzer");
        }

        const wrapper = document.createElement("div");

        wrapper.classList.add("player");

        const playerName = document.createElement("div");

        playerName.classList.add("player-name");
        playerName.textContent = this.username;

        const playerBuzzer = document.createElement("button");

        playerBuzzer.classList.add("player-buzzer", "secondary-btn");
        playerBuzzer.textContent = "Buzz";

        const playerScore = document.createElement("div");

        playerScore.classList.add("player-score");
        playerScore.textContent = "0";

        wrapper.appendChild(playerName);
        wrapper.appendChild(playerBuzzer);
        wrapper.appendChild(playerScore);

        return wrapper;
    }

    mount(parent) {
        if (!this.isSelf) {
            parent.appendChild(this.element);
        }
    }

    updateScore(newScore) {
        this.score = newScore;
        this.scoreElement.textContent = newScore.toString();
    }

    setBuzzWinner(isWinner) {
        this.buzzerElement.classList.toggle("buzzed", isWinner);
    }

    setBuzzEnabled(enabled) {
        if (this.isSelf) {
            this.buzzerElement.disabled = !enabled;
        }
    }
}
