/**
 * A rule on a page template swimlane that stands for a number of swimlanes: one per item the
 * source resolves to. Creating a variant from the template replaces the rule by its result.
 */
export interface SwimlaneRepeat {
    /** name of a source registered in `SwimlaneRepeatService` */
    source: string;
    /** property the items are ordered by; defaults to the source's own order */
    orderBy?: string;
    /** reverses the order; without `orderBy` it has no effect */
    orderDescending?: boolean;
    /** upper bound on the number of swimlanes the rule produces */
    maxItems?: number;
}

/**
 * One resolved item of a repeat source, addressable as `${item.<field>}` in the swimlane.
 */
export interface RepeatItem {
    nodeId: string;
    ref: string;
    title?: string;
    description?: string;
}
