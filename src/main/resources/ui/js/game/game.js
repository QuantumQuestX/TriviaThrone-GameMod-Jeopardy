import * as gameSocket from "./game-socket.js";
import { apiFetch } from "/js/root/api.js";
import { getCurrentUser } from "/js/root/auth.js";
import { getLobbyDetails, getLobbyPassword } from "/js/root/lobby-storage.js";
import { configureGameControls } from "./game-controls.js";
import { GameView } from "./game-view.js";

const currentUser = getCurrentUser();
const lobbyCode = new URLSearchParams(window.location.search).get("code") || getLobbyDetails().lobbyCode;

if (!lobbyCode) {
    window.location.href = "/";
} else {
    initializeGame().catch((error) => {
        console.error("Could not start the game page:", error);
        document.getElementById("game-status").textContent =
            "Could not connect to this game. Return to the lobby and try again.";
    });
}

async function initializeGame() {
    await gameSocket.connectToLobby(lobbyCode, currentUser, getLobbyPassword() || null);
    const response = await apiFetch(`/api/lobbies/info?code=${encodeURIComponent(lobbyCode)}`);

    if (!response.ok) {
        throw new Error("Could not load lobby information");
    }

    const responseBody = await response.text();
    if (!responseBody.trim()) {
        throw new Error(`Lobby information response was empty (HTTP ${response.status})`);
    }

    const lobby = JSON.parse(responseBody);

    if (!lobby) {
        localStorage.removeItem("lobbyDetails");
        window.location.href = "/";

        return;
    }

    const view = new GameView(lobbyCode, currentUser);

    view.initializeLobby(lobby);
    await gameSocket.subscribeToGame(lobbyCode, (state) => view.handleMessage(state));
    gameSocket.sendAction(lobbyCode, gameSocket.Actions.SYNC);
    configureGameControls(lobbyCode);
}
