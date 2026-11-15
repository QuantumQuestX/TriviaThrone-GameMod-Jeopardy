import { apiFetch } from "/js/root/api.js";

export async function createGameArchive(game) {
    const resources = game.resources.getAll();
    const data = {
        meta: {
            title: game.meta.title,
            description: game.meta.description,
        },
        rounds: game.rounds.map((round) => ({
            name: round.name,
            categories: round.categories.map((category) => ({ name: category.name })),
            questions: round.questions.map((row) =>
                row.map((question) => ({
                    points: question.points,
                    type: question.type,
                    prompt: question.prompt,
                    media: question.media,
                    options: question.options,
                })),
            ),
        })),
        resources: resources.map((resource) => ({
            id: resource.id,
            name: resource.name,
            type: resource.type,
        })),
    };

    const request = new FormData();

    request.append("data", JSON.stringify(data));

    for (const resource of resources) {
        request.append(`asset_${resource.id}`, resource.file, resource.name);
    }

    const response = await apiFetch("/api/game/create/jeopardy", {
        method: "POST",
        body: request,
    });

    if (!response.ok) {
        const message = await response.text();

        throw new Error(message || `Game creation failed (${response.status}).`);
    }

    const archive = await response.blob();
    const objectUrl = URL.createObjectURL(archive);
    const link = document.createElement("a");

    link.href = objectUrl;
    link.download = `${sanitizeFileName(game.meta.title || "game")}.game.throne`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

function sanitizeFileName(name) {
    return name.replace(/[^a-z0-9_\-]/gi, "_");
}
