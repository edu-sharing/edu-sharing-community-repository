import { Component } from '@angular/core';
import { SharedModule } from '../../../../../shared/shared.module';

/**
 * Entry of the topic page's sidebar menu for passing pages on to sub-collections. Only announces
 * the feature so far.
 */
@Component({
    selector: 'es-topic-page-inheritance-sidebar',
    imports: [SharedModule],
    template: `
        <es-info-message class="message" mode="info">
            {{ 'TOPIC_PAGE.SIDEBAR.INHERITANCE.PLACEHOLDER' | translate }}
        </es-info-message>
    `,
    styles: `
        .message {
            display: block;
            padding: 20px;
        }
    `,
})
export class TopicPageInheritanceSidebarComponent {}
