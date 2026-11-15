import * as socket from "/js/root/socket.js";

export const Actions = Object.freeze({
    START_GAME: "START_GAME",
    END_GAME: "END_GAME",
    SYNC: "SYNC",
    SELECT_ROUND: "SELECT_ROUND",
    SELECT_QUESTION: "SELECT_QUESTION",
    PLAY_MEDIA: "PLAY_MEDIA",
    OPEN_BUZZ: "OPEN_BUZZ",
    CLOSE_BUZZ: "CLOSE_BUZZ",
    BUZZ: "BUZZ",
    MARK_CORRECT: "MARK_CORRECT",
    MARK_INCORRECT: "MARK_INCORRECT",
    RESET: "RESET",
});

let currentTopic = null;

export async function subscribeToGame(lobbyCode, onStateUpdate) {
    await socket.connectSocket("/ws/lobby");

    const topic = `/topic/game.${lobbyCode}`;

    if (currentTopic) {
        socket.unsubscribe(currentTopic);
    }

    socket.subscribe(topic, (state) => {
        onStateUpdate(state);
    });

    currentTopic = topic;
}

export function sendAction(lobbyCode, action, details = {}) {
    socket.send("/app/game/action", {
        lobbyCode,
        action,
        parameters: details,
    });
}

export function sendBuzz(lobbyCode) {
    sendAction(lobbyCode, Actions.BUZZ);
}

export function sendOpenBuzz(lobbyCode) {
    sendAction(lobbyCode, Actions.OPEN_BUZZ);
}

export function sendCloseBuzz(lobbyCode) {
    sendAction(lobbyCode, Actions.CLOSE_BUZZ);
}

export function disconnect() {
    socket.disconnect();
}
