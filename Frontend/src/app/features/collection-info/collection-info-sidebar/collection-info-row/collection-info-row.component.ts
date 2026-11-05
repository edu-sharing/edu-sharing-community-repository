import { Component, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { IconDirective } from 'ngx-edu-sharing-ui';

/** A labelled value with a leading icon. The value is the projected content. */
@Component({
    selector: 'es-collection-info-row',
    templateUrl: 'collection-info-row.component.html',
    styleUrls: ['collection-info-row.component.scss'],
    imports: [TranslateModule, IconDirective],
})
export class CollectionInfoRowComponent {
    /** icon id as understood by the `esIcon` directive */
    readonly icon = input.required<string>();
    /** translation key of the label */
    readonly label = input.required<string>();
}
