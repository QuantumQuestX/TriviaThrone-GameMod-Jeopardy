import { apiFetch } from "/js/root/api.js";

export const MediaType = Object.freeze({
    VIDEO: "VIDEO",
    AUDIO: "AUDIO",
    IMAGE: "IMAGE",
    UNDEFINED: "UNDEFINED",
});

export class MediaController {
    constructor() {
        this.videoElement = document.getElementById("q-media-video");
        this.audioElement = document.getElementById("q-media-audio");
        this.imageElement = document.getElementById("q-media-image");
        this.mediaFrame = document.getElementById("media-frame");
        this.placeholderElement = document.getElementById("media-placeholder");
        this.placeholderText = document.getElementById("media-placeholder-text");
        this.type = null;
        this.loadId = 0;
        this.playbackVersion = 0;
        this.objectUrl = null;
        this.mediaLoadPromise = Promise.resolve();
    }

    setMedia(type = MediaType.UNDEFINED, url) {
        if (!type || type === MediaType.UNDEFINED || !url) {
            console.warn("No media to display");

            return Promise.resolve();
        }

        this.clearMedia();
        this.type = type;

        if (type === MediaType.VIDEO) {
            this.mediaFrame.classList.remove("hidden");
            this.showPlaceholder("Video ready. Waiting for the host to start it.");
        } else if (type === MediaType.AUDIO) {
            this.mediaFrame.classList.remove("hidden");
            this.showPlaceholder("Audio ready. Waiting for the host to start it.");
        }

        const loadId = this.loadId;

        this.mediaLoadPromise = this.loadMedia(type, url, loadId);

        return this.mediaLoadPromise;
    }

    clearMedia() {
        this.loadId += 1;
        this.playbackVersion += 1;
        this.mediaLoadPromise = Promise.resolve();

        if (this.objectUrl) {
            URL.revokeObjectURL(this.objectUrl);
            this.objectUrl = null;
        }

        this.videoElement.pause();
        this.videoElement.src = "";
        this.videoElement.classList.add("hidden");

        this.audioElement.pause();
        this.audioElement.src = "";
        this.audioElement.classList.add("hidden");

        this.imageElement.src = "";
        this.imageElement.classList.add("hidden");
        this.mediaFrame.classList.add("hidden");
        this.placeholderElement.classList.add("hidden");
        this.type = null;
    }

    async loadMedia(type, url, loadId) {
        const response = await apiFetch(url);

        if (loadId !== this.loadId) {
            return;
        }

        if (!response.ok) {
            throw new Error(`Could not load question media (${response.status})`);
        }

        const mediaBlob = await response.blob();

        if (loadId !== this.loadId) {
            return;
        }

        const element = this.getElement(type);

        if (!element) {
            throw new Error(`Unsupported question media type: ${type}`);
        }

        this.objectUrl = URL.createObjectURL(mediaBlob);
        element.src = this.objectUrl;

        if (type === MediaType.IMAGE) {
            element.classList.remove("hidden");
        }
    }

    getElement(type) {
        switch (type) {
            case MediaType.VIDEO:
                return this.videoElement;
            case MediaType.AUDIO:
                return this.audioElement;
            case MediaType.IMAGE:
                return this.imageElement;
            default:
                return null;
        }
    }

    play() {
        const playbackVersion = ++this.playbackVersion;

        return this.mediaLoadPromise.then(() => {
            if (playbackVersion !== this.playbackVersion) {
                return;
            }

            if (this.type === MediaType.VIDEO) {
                return this.videoElement.play().then(() => {
                    if (playbackVersion !== this.playbackVersion) {
                        this.videoElement.pause();

                        return;
                    }

                    this.videoElement.classList.remove("hidden");
                    this.placeholderElement.classList.add("hidden");
                });
            } else if (this.type === MediaType.AUDIO) {
                return this.audioElement.play().then(() => {
                    if (playbackVersion === this.playbackVersion) {
                        this.showPlaceholder("Audio is playing. Buzz to pause.");
                    }
                });
            }
        });
    }

    pause() {
        const playbackVersion = ++this.playbackVersion;
        const type = this.type;

        if (type !== MediaType.VIDEO && type !== MediaType.AUDIO) {
            return Promise.resolve();
        }

        const element = this.getElement(type);

        element.pause();
        element.classList.add("hidden");
        this.showPlaceholder(
            type === MediaType.VIDEO ? "Buzz received. Video paused." : "Buzz received. Audio paused.",
        );

        return this.mediaLoadPromise.then(() => {
            if (playbackVersion !== this.playbackVersion) {
                return;
            }

            element.pause();
        });
    }

    showPlaceholder(message) {
        this.placeholderText.textContent = message;
        this.placeholderElement.classList.remove("hidden");
    }
}
