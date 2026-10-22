import { Component, computed, input } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, IconDirective } from 'ngx-edu-sharing-ui';

/**
 * Thumbnail of a collection: its preview image, or the collection icon on the colour of the
 * collection when it has no image of its own.
 */
@Component({
    selector: 'es-collection-thumbnail',
    templateUrl: 'collection-thumbnail.component.html',
    styleUrls: ['collection-thumbnail.component.scss'],
    imports: [AsyncPipe, EduSharingUiCommonModule, IconDirective],
})
export class CollectionThumbnailComponent {
    readonly collection = input.required<Node>();
    /** width of the thumbnail; any CSS length */
    readonly size = input('64px');
    /** width to height ratio, a CSS `aspect-ratio` value; `1` yields a square */
    readonly aspectRatio = input('1');

    protected readonly hasImage = computed(() => {
        const preview = this.collection()?.preview;
        return !!preview?.url && !preview.isIcon;
    });
}
