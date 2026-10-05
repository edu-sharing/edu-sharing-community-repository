/**
 * The topic a topic page switched to.
 */
export interface TopicChangedEvent {
    collectionId: string;
    /** The link of the topic, as given by the custom URL function. */
    url: string | null;
}
