import { bootAuthenticatedPage } from "/js/root/page-bootstrap.js";
import { getLobbyDetails } from "/js/root/lobby-storage.js";
import * as socket from "/js/root/socket.js";

const lobbyCode = new URLSearchParams(window.location.search).get("code") || getLobbyDetails().lobbyCode;
const status = document.getElementById("game-status");
const revealButton = document.getElementById("reveal-answer");
const startButton = document.getElementById("start-game");
const endButton = document.getElementById("end-game");

await bootAuthenticatedPage(async () => {
    if (!lobbyCode) {
        throw new Error("No lobby code was provided.");
    }

    await socket.connectSocket("/ws/lobby");
    socket.subscribe(`/topic/game.${lobbyCode}`, renderGameState);
    sendAction("SYNC");

    revealButton.addEventListener("click", () => sendAction("REVEAL_ANSWER"));
    startButton.addEventListener("click", () => sendAction("START_GAME"));
    endButton.addEventListener("click", () => sendAction("END_GAME"));
});

function sendAction(action) {
    socket.send("/app/game/action", {
        lobbyCode,
        action,
        parameters: {},
    });
}

function renderGameState(state) {
    document.getElementById("game-title").textContent = state.title;
    document.getElementById("question-text").textContent = state.question;

    const answerText = document.getElementById("answer-text");
    answerText.hidden = !state.answerRevealed;
    answerText.textContent = state.answerRevealed ? `Answer: ${state.answer}` : "";
    revealButton.disabled = state.answerRevealed || state.ended;
    endButton.disabled = state.ended;
    startButton.disabled = !state.ended;
    status.textContent = state.ended
        ? "The game has ended."
        : state.answerRevealed
          ? "The answer has been revealed."
          : "The game is ready. The host can reveal the answer.";
}
