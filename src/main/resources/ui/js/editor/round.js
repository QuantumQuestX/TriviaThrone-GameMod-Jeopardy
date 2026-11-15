import { Question } from "./question.js";

export class Round {
    constructor(name, categoryCount = 1, questionCount = 1) {
        this.name = name;
        this.categories = Array.from({ length: categoryCount }, (_, i) => ({ name: `Category ${i + 1}` }));
        this.questions = Array.from({ length: questionCount }, (_, r) =>
            Array.from({ length: categoryCount }, (_, c) => new Question(c, r, (r + 1) * 100)),
        );
        this.appliedQuestionIds = new Set();
    }

    get categoryCount() {
        return this.categories.length;
    }
    get questionCount() {
        return this.questions.length;
    }

    resize(newCategoryCount, newQuestionCount) {
        while (this.categories.length < newCategoryCount) {
            this.categories.push({ name: `Category ${this.categories.length + 1}` });
        }

        this.categories.length = newCategoryCount;

        this.questions = Array.from({ length: newQuestionCount }, (_, r) =>
            Array.from(
                { length: newCategoryCount },
                (_, c) => this.questions[r]?.[c] || new Question(c, r, (r + 1) * 100),
            ),
        );

        this.appliedQuestionIds = new Set(
            [...this.appliedQuestionIds].filter((id) => this.questions.some((row) => row.some((q) => q.id === id))),
        );
    }
}
