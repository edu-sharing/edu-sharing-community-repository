/* tslint:disable */
/* eslint-disable */
import { RatingData } from './rating-data';
export interface RatingDetails {
    affiliation?: {
        [key: string]: RatingData;
    };
    overall?: RatingData;
    user?: number;
}
