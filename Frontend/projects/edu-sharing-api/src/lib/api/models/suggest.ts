/* tslint:disable */
/* eslint-disable */
export interface Suggest {
    /**
     * suggested text with corrected words highlighted
     */
    highlighted?: string;

    /**
     * score of the suggestion
     */
    score: number;

    /**
     * suggested text
     */
    text: string;
}
