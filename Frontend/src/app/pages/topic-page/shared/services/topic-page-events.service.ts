import { EventEmitter, Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ColorChangeEvent } from '../types/color-change-event';
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
     * An existing widget node's config has been updated (not added).
     *
     * Emits the page variant node that owns the widget, so listeners can
     * bump the template version when the edit occurs in template mode.
     */
    readonly widgetConfigUpdated: EventEmitter<Node> = new EventEmitter<Node>();
}
