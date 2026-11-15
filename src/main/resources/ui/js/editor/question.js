export class Question {
    constructor(categoryIdx, rowIdx, points = 100, type = "text", prompt = "", media = null, options = null) {
        this.id = crypto.randomUUID();
        this.categoryIdx = categoryIdx;
        this.rowIdx = rowIdx;
        this.points = points;
        this.type = type;
        this.prompt = prompt;
        this.media = media;
        this.options = options;
    }

    setPrompt(prompt) {
        this.prompt = prompt;
    }

    setType(type) {
        this.type = type;
    }

    setMedia(media) {
        this.media = media;
    }

    setOptions(options) {
        this.options = options;
    }

    setPoints(points) {
        this.points = points;
    }
}
