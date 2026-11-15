import { BoardController } from "./board-controller.js";
import { GameController } from "./game-controller.js";

export function initializeEditor() {
    console.log("Editor initialised.");

    const jeopardyCanvas = document.getElementById("jeopardy-canvas");
    const boardController = new BoardController(jeopardyCanvas);
    const gameController = new GameController(boardController);

    gameController.init();
}
