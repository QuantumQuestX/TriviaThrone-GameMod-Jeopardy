import { createId } from "./id.js";

export class ResourceManager {
    constructor() {
        this.resources = new Map();
    }

    addFile(file) {
        for (const res of this.resources.values()) {
            if (res.name === file.name && res.size === file.size) {
                return res;
            }
        }

        const id = createId();

        const resourceType = this._detectType(file);

        const resource = {
            id,
            file,
            name: file.name,
            mimeType: file.type,
            type: resourceType,
            size: file.size,
        };

        this.resources.set(id, resource);

        return resource;
    }

    remove(id) {
        this.resources.delete(id);
    }

    get(id) {
        return this.resources.get(id);
    }

    getAll() {
        return Array.from(this.resources.values());
    }

    _detectType(file) {
        if (file.type.startsWith("image/")) {
            return "image";
        }

        if (file.type.startsWith("video/")) {
            return "video";
        }

        if (file.type.startsWith("audio/")) {
            return "audio";
        }

        return "unknown";
    }
}
