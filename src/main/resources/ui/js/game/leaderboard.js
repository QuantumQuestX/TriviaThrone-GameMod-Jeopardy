export function renderLeaderboard(list, entries, createElement = (tagName) => document.createElement(tagName)) {
    list.replaceChildren();

    if (!entries.length) {
        const emptyMessage = createElement("li");

        emptyMessage.textContent = "No players to rank.";
        list.appendChild(emptyMessage);

        return;
    }

    entries.forEach((entry, index) => {
        const row = createElement("li");

        row.className = "leaderboard-entry";

        const rowDelay = index * 420;

        row.style.animationDelay = `${rowDelay}ms`;

        const rank = createElement("span");

        rank.className = "leaderboard-rank";
        rank.textContent = `#${index + 1}`;
        rank.style.animationDelay = `${rowDelay}ms`;

        const username = createElement("span");

        username.className = "leaderboard-player";
        username.textContent = entry.username;
        username.style.animationDelay = `${rowDelay + 100}ms`;

        const score = createElement("span");

        score.className = "leaderboard-score";
        score.textContent = `${entry.score} points`;
        score.style.animationDelay = `${rowDelay + 200}ms`;

        row.append(rank, username, score);
        list.appendChild(row);
    });
}
