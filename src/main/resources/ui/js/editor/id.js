export function createId() {
    const cryptoApi = globalThis.crypto;

    if (typeof cryptoApi?.randomUUID === "function") {
        return cryptoApi.randomUUID();
    }

    if (typeof cryptoApi?.getRandomValues === "function") {
        const bytes = new Uint8Array(16);

        cryptoApi.getRandomValues(bytes);
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;

        return [...bytes]
            .map((byte, index) => `${byte.toString(16).padStart(2, "0")}${[3, 5, 7, 9].includes(index) ? "-" : ""}`)
            .join("");
    }

    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (character) => {
        const value = Math.random() * 16 | 0;
        const replacement = character === "x" ? value : (value & 0x3) | 0x8;

        return replacement.toString(16);
    });
}
