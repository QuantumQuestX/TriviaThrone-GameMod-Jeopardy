import { PlayerView } from "./jeopardy/player-view.js";
import { QuestionManager } from "./jeopardy/question-manager.js";
import { renderLeaderboard } from "./leaderboard.js";
import { GameBoardView } from "./game-board-view.js";

const stateLabels = {
    IDLE: "Choose a question to begin.",
    QUESTION_SELECTED: "Question selected. The host can open the buzzer.",
    QUESTION_OPEN: "Question is open.",
    BUZZ_OPEN: "Buzz in now!",
    BUZZ_COLLECTING: "Buzz received. Locking in the fastest player…",
    BUZZ_LOCKED: "Buzzer locked. The host is judging the answer.",
    ANSWER_REVEAL: "Answer marked correct.",
};

export class GameView {
    constructor(lobbyCode, currentUser) {
        this.lobbyCode = lobbyCode;
        this.currentUser = currentUser;
        this.playerViews = new Map();
        this.questionManager = new QuestionManager();
        this.boardView = new GameBoardView(lobbyCode);
        this.isHost = false;
        this.hostUsername = null;
        this.lastQuestionKey = null;
        this.lastMediaPlaybackToken = null;
        this.lastMediaPauseToken = null;
    }

    initializeLobby(lobby) {
        this.hostUsername = lobby.lobbyHost;
        this.isHost = this.hostUsername === this.currentUser;
        this.boardView.setHost(this.isHost);
        document.getElementById("lobby-code").textContent = this.lobbyCode;
        document.getElementById("username-display").textContent = this.currentUser || "Unknown";
        document.getElementById("host-panel").style.display = this.isHost ? "" : "none";
        document.getElementById("main-buzzer-container").classList.toggle("hidden", this.isHost);
        this.layoutPlayers([...lobby.usernames].sort());
    }

    handleMessage(state) {
        if (state?.gameEnded) {
            this.showLeaderboard(state["leaderboard"]);

            return;
        }

        this.applyGameState(state);
    }

    applyGameState(state) {
        if (!state || state.lobbyCode !== this.lobbyCode) {
            return;
        }

        const players = Object.keys(state.scores || {}).sort();

        if (players.some((username) => username !== this.hostUsername && !this.playerViews.has(username))) {
            this.layoutPlayers(players);
        }

        for (const [username, score] of Object.entries(state.scores || {})) {
            this.playerViews.get(username)?.updateScore(score);
        }

        this.boardView.render(state);
        this.renderQuestion(state);
        this.renderBuzzer(state);
        this.renderHostControls(state);

        document.getElementById("game-title").textContent = state.gameTitle || "Trivia Throne";
        document.title = `${state.gameTitle || "Trivia Throne"} - TriviaThrone`;
        document.getElementById("game-status").textContent = state.buzzWinner
            ? `${state.buzzWinner} buzzed first.`
            : stateLabels[state.state] || "Waiting for the host…";
        this.renderMediaPlayback(state);
        this.renderMediaPause(state);
    }

    layoutPlayers(players) {
        const left = document.getElementById("players-left");
        const right = document.getElementById("players-right");
        const others = players.filter((username) => username !== this.currentUser && username !== this.hostUsername);
        const midpoint = Math.ceil(others.length / 2);

        left.replaceChildren();
        right.replaceChildren();
        this.playerViews.clear();

        others.slice(0, midpoint).forEach((username) => {
            const view = new PlayerView(username);

            view.mount(left);
            this.playerViews.set(username, view);
        });

        others.slice(midpoint).forEach((username) => {
            const view = new PlayerView(username);

            view.mount(right);
            this.playerViews.set(username, view);
        });

        if (!this.isHost) {
            this.playerViews.set(this.currentUser, new PlayerView(this.currentUser, { isSelf: true }));
        }
    }

    showLeaderboard(entries) {
        document.getElementById("question-panel").classList.add("hidden");
        document.getElementById("game-status").classList.add("hidden");
        document.getElementById("host-panel").classList.add("hidden");
        document.getElementById("player-buzzer-panel").classList.add("hidden");
        document.getElementById("leaderboard-panel").classList.remove("hidden");
        renderLeaderboard(document.getElementById("leaderboard-list"), entries || []);
    }

    renderQuestion(state) {
        const question = state.question;

        if (!question) {
            if (this.lastQuestionKey !== null) {
                this.questionManager.reset();
            }

            this.lastQuestionKey = null;

            return;
        }

        const key = `${state.curRoundIdx}:${state.curCatIdx}:${state.curQuestionIdx}`;

        if (key !== this.lastQuestionKey) {
            this.questionManager.showQuestion(question).catch((error) => {
                console.error("Could not load question media:", error);
                document.getElementById("game-status").textContent = "Could not load question media.";
            });
            this.lastQuestionKey = key;
        }

        if (state.state === "ANSWER_REVEAL" && Number.isInteger(question.correctChoiceIndex)) {
            this.questionManager.revealCorrectAnswer(question.correctChoiceIndex);
        }
    }

    renderBuzzer(state) {
        const buzzerOpen = state.state === "BUZZ_OPEN" || state.state === "BUZZ_COLLECTING";
        const alreadyBuzzed = (state.buzzedPlayers || []).includes(this.currentUser);
        const selfView = this.playerViews.get(this.currentUser);

        selfView?.setBuzzEnabled(!this.isHost && buzzerOpen && !alreadyBuzzed);

        for (const [username, view] of this.playerViews) {
            view.setBuzzWinner(username === state.buzzWinner);
        }

        const buzzer = document.getElementById("main-buzzer");

        buzzer.textContent = this.isHost ? "HOST" : alreadyBuzzed && buzzerOpen ? "LOCKED" : "BUZZ";
    }

    renderHostControls(state) {
        const playMediaButton = document.getElementById("host-play-media");
        const hasUnusedQuestions =
            state.rounds?.some((round) => round.questions.some((row) => row.some((tile) => !tile.used))) ?? false;
        const nextQuestionButton = document.getElementById("host-next-question");

        nextQuestionButton.textContent = hasUnusedQuestions ? "Next Question" : "Finish & Show Leaderboard";

        const mediaType = state.question?.mediaType;
        const canStartMedia = state.state === "QUESTION_SELECTED" || state.state === "BUZZ_OPEN";
        const hasPlayableMedia =
            canStartMedia && Boolean(state.question?.mediaUrl) && (mediaType === "VIDEO" || mediaType === "AUDIO");

        playMediaButton.disabled = !this.isHost || !hasPlayableMedia;
        playMediaButton.textContent =
            mediaType === "VIDEO"
                ? "Play Video + Open Buzzer"
                : mediaType === "AUDIO"
                  ? "Play Audio + Open Buzzer"
                  : "Play Media + Open Buzzer";

        document.getElementById("host-buzzer-enable").disabled = state.state !== "QUESTION_SELECTED";
        document.getElementById("host-buzzer-disable").disabled =
            state.state !== "BUZZ_OPEN" && state.state !== "BUZZ_COLLECTING";
        document.getElementById("host-show-question").disabled = state.state !== "BUZZ_LOCKED";
        document.getElementById("host-hide-question").disabled = state.state !== "BUZZ_LOCKED";
        document.getElementById("host-next-question").disabled = state.state === "IDLE";
    }

    renderMediaPlayback(state) {
        const token = state.mediaPlaybackToken;

        if (!token) {
            this.lastMediaPlaybackToken = null;

            return;
        }

        if (token === this.lastMediaPlaybackToken) {
            return;
        }

        this.lastMediaPlaybackToken = token;
        this.questionManager.playMedia().catch((error) => {
            if (error.name === "AbortError") {
                return;
            }

            console.warn("Could not start synchronized media playback:", error);
            document.getElementById("game-status").textContent = "Your browser blocked synchronized media playback.";
        });
    }

    renderMediaPause(state) {
        const token = state.mediaPauseToken;

        if (!token) {
            this.lastMediaPauseToken = null;

            return;
        }

        if (token === this.lastMediaPauseToken) {
            return;
        }

        this.lastMediaPauseToken = token;
        this.questionManager.pauseMedia().catch((error) => {
            console.warn("Could not pause media after a buzz:", error);
        });
    }
}
