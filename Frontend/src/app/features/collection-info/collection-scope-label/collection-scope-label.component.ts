import { Component, computed, inject, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { IconDirective } from 'ngx-edu-sharing-ui';
import { NodeHelperService } from '../../../services/node-helper.service';

/** Visibility of a collection (private, shared, public, …) as an icon with its name. */
@Component({
    selector: 'es-collection-scope-label',
    templateUrl: 'collection-scope-label.component.html',
    styleUrls: ['collection-scope-label.component.scss'],
    imports: [TranslateModule, IconDirective],
})
export class CollectionScopeLabelComponent {
    private readonly nodeHelper = inject(NodeHelperService);

    readonly collection = input.required<Node>();
    /** whether the icon of the visibility is shown in front of its name */
    readonly showIcon = input(true);

    protected readonly scope = computed(() => {
        const collection = this.collection();
        return collection ? this.nodeHelper.getCollectionScopeInfo(collection) : null;
    });
}
