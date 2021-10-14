/* tslint:disable */
/* eslint-disable */
export interface Collection {
    authorFreetext?: string;
    childCollectionsCount?: number;
    childReferencesCount?: number;
    color?: string;
    description?: string;

    /**
     * false
     */
    fromUser: boolean;

    /**
     * false
     */
    level0: boolean;
    orderMode?: string;
    pinned?: boolean;
    scope?: string;
    title: string;
    type: string;
    viewtype: string;
    x?: number;
    y?: number;
    z?: number;
}
