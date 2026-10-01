import { EventEmitter, Injectable } from '@angular/core';
import { ColorChangeEvent } from '../types/color-change-event';
import { WidgetConfigUpdatedEvent } from '../types/widget-config-updated-event';
import { WidgetNodeAddedEvent } from '../types/widget-node-added-event';

/**
 * An application-wide event broker for topic-page events.
 */
@Injectable({
    providedIn: 'root',
})
export class TopicPageEventsService {
    /**
     * A swimlane color has been changed via a widget.
     *
     * The emitted value must be persisted in the structure of the page variant.
     */
    readonly swimlaneColorChanged: EventEmitter<ColorChangeEvent> =
        new EventEmitter<ColorChangeEvent>();

    /**
     * A new widget node has been added to the topic page.
     *
     * The emitted value must be persisted in the structure of the page variant.
     */
    readonly widgetNodeAdded: EventEmitter<WidgetNodeAddedEvent> =
        new EventEmitter<WidgetNodeAddedEvent>();

    /**
     * An existing widget node's config has been updated (not added), with the values it replaced.
     */
    readonly widgetConfigUpdated: EventEmitter<WidgetConfigUpdatedEvent> =
        new EventEmitter<WidgetConfigUpdatedEvent>();

    /**
     * Properties of the node with the emitted ID were restored by undo or redo, so whatever
     * displays the node has to read it again.
     */
    readonly widgetNodeRestored: EventEmitter<string> = new EventEmitter<string>();
}
