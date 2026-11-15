import * as gameSocket from "./game-socket.js";
import * as socket from "/js/root/socket.js";
import { clearLobbyDetails } from "/js/root/lobby-storage.js";

export function configureGameControls(lobbyCode) {
    document.getElementById("host-play-media").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.PLAY_MEDIA);
    });
    document.getElementById("host-buzzer-enable").addEventListener("click", () => {
        gameSocket.sendOpenBuzz(lobbyCode);
    });
    document.getElementById("host-buzzer-disable").addEventListener("click", () => {
        gameSocket.sendCloseBuzz(lobbyCode);
    });
    document.getElementById("host-show-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.MARK_CORRECT);
    });
    document.getElementById("host-hide-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.MARK_INCORRECT);
    });
    document.getElementById("host-next-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.RESET);
    });
    document.getElementById("host-end-game").addEventListener("click", () => {
        if (window.confirm("End the game and show the final leaderboard to everyone?")) {
            gameSocket.sendAction(lobbyCode, gameSocket.Actions.END_GAME);
        }
    });

    const leaveGame = () => {
        socket.send("/app/leave", { lobbyCode });
        gameSocket.disconnect();
        clearLobbyDetails();
        window.location.href = "/";
    };

    document.getElementById("leave-btn").addEventListener("click", leaveGame);
    document.getElementById("leaderboard-back").addEventListener("click", leaveGame);
    document.getElementById("main-buzzer").addEventListener("click", () => {
        gameSocket.sendBuzz(lobbyCode);
    });
}
