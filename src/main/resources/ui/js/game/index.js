import { bootAuthenticatedPage } from "/js/root/page-bootstrap.js";

await bootAuthenticatedPage(async () => {
    await import("./game.js");
});
