import { Component, ElementRef, input, output, viewChild } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { MdsModule } from '../../mds/mds.module';
import { User } from '../../../core-module/core.module';

/** the image change the preview widget reports; the host persists it */
export type CollectionPreviewChange = { file: File | null; delete: boolean };

/**
 * Form for the look of a collection: a preview card with the image, title, description, author
 * and colour. It edits the given node in place, so the host reads the values back from it.
 */
@Component({
    selector: 'es-collection-design-form',
    templateUrl: 'collection-design-form.component.html',
    styleUrls: ['collection-design-form.component.scss'],
    imports: [SharedModule, MdsModule],
})
export class CollectionDesignFormComponent {
    /** the collection that is edited; `collection.color` has to be set */
    readonly collection = input.required<Node>();
    /** the colours to choose from */
    readonly colors = input.required<string[]>();
    /** the user that owns the collection, shown as its author */
    readonly user = input<User>();
    /** whether the author may be replaced by a free text */
    readonly authorFreetextAllowed = input(false);
    /** whether the author is already a free text that must stay editable */
    readonly authorFreetextFixedAllowed = input(false);
    /** the text an author free text starts with */
    readonly defaultAuthorName = input('');
    readonly showAuthor = input(true);
    /** icon of the button that changes the image, see the preview widget */
    readonly menuIcon = input<string>(null);
    /** whether removing the image is an entry of the image menu */
    readonly deleteAsOption = input(true);

    readonly previewChange = output<CollectionPreviewChange>();

    private readonly authorFreetextInput =
        viewChild<ElementRef<HTMLInputElement>>('authorFreetextInput');

    protected get authorFreetext(): boolean {
        return this.collection().collection.authorFreetext != null;
    }

    protected setTitle(title: string): void {
        this.collection().title = title;
    }

    protected setDescription(description: string): void {
        this.collection().collection.description = description;
    }

    protected setAuthorFreetext(author: string): void {
        this.collection().collection.authorFreetext = author;
    }

    protected setColor(color: string): void {
        this.collection().collection.color = color;
    }

    protected switchToAuthorFreetext(): void {
        this.collection().collection.authorFreetext = this.defaultAuthorName();
        setTimeout(() => this.authorFreetextInput()?.nativeElement.focus());
    }

    protected cancelAuthorFreetext(): void {
        this.collection().collection.authorFreetext = null;
    }

    /** moves the selected colour through the grid; the colours are laid out in rows of six */
    protected setColorByDirection(event: Event): void {
        const rowLength = 6;
        const colors = this.colors();
        let index = colors.indexOf(this.collection().collection.color);
        switch ((event as KeyboardEvent).key) {
            case 'ArrowUp':
                index -= rowLength;
                break;
            case 'ArrowDown':
                index += rowLength;
                break;
            case 'ArrowLeft':
                index -= 1;
                break;
            case 'ArrowRight':
                index += 1;
                break;
        }
        if (index >= 0 && index < colors.length) {
            this.setColor(colors[index]);
            event.preventDefault();
        }
    }
}
