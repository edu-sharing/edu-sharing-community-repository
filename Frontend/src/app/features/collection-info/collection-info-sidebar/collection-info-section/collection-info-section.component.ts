import { Component, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

/**
 * A titled block of a detail panel. The heading is a translation key; the content is projected.
 * Consecutive sections are separated by a divider line.
 */
@Component({
    selector: 'es-collection-info-section',
    templateUrl: 'collection-info-section.component.html',
    styleUrls: ['collection-info-section.component.scss'],
    imports: [TranslateModule],
})
export class CollectionInfoSectionComponent {
    readonly heading = input<string>();
}
