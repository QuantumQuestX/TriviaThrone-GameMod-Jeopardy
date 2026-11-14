import { bootAuthenticatedPage } from "/js/root/page-bootstrap.js";
import { apiFetch } from "/js/root/api.js";

const form = document.getElementById("quiz-form");
const status = document.getElementById("creator-status");

await bootAuthenticatedPage(async () => {
    form.addEventListener("submit", (event) => {
        event.preventDefault();
        createGamePackage().catch((error) => {
            console.error("Could not create the example game package.", error);
            status.textContent = "Could not create the game package. Please try again.";
        });
    });
});

async function createGamePackage() {
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());

    status.textContent = "Creating game package…";

    const response = await apiFetch("/api/game/create/example-quiz", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data }),
    });

    if (!response.ok) {
        throw new Error(`Game package creation failed with status ${response.status}.`);
    }

    const objectUrl = URL.createObjectURL(await response.blob());
    const downloadLink = document.createElement("a");
    downloadLink.href = objectUrl;
    downloadLink.download = "example-quiz.game.throne";
    downloadLink.click();
    URL.revokeObjectURL(objectUrl);
    status.textContent = "Game package downloaded. Import it into a lobby to play.";
}
