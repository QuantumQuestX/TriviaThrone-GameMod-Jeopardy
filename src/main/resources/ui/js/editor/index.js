import { bootAuthenticatedPage } from "/js/root/page-bootstrap.js";

await bootAuthenticatedPage(async () => {
    const editor = await import("./editor.js");

    editor.initializeEditor();
});
