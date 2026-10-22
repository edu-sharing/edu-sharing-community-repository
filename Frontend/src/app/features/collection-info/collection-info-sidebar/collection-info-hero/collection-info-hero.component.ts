import { Component, computed, input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { CollectionThumbnailComponent } from '../../collection-thumbnail/collection-thumbnail.component';

/** Banner with the thumbnail, the title and the description of a collection. */
@Component({
    selector: 'es-collection-info-hero',
    templateUrl: 'collection-info-hero.component.html',
    styleUrls: ['collection-info-hero.component.scss'],
    imports: [CollectionThumbnailComponent],
})
export class CollectionInfoHeroComponent {
    readonly collection = input.required<Node>();

    protected readonly description = computed(
        () =>
            this.collection()?.collection?.description ??
            this.collection()?.properties?.['cm:description']?.[0],
    );
}
