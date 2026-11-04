import {
    Component,
    computed,
    contentChild,
    input,
    output,
    signal,
    TemplateRef,
} from '@angular/core';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule, ListItem } from 'ngx-edu-sharing-ui';

/** context of the row template: the property id and its position */
export type ScopePropertyRowContext = { $implicit: string; index: number };

/**
 * Picks properties (filterable checkbox list) and sorts the picked ones (drag and drop). Used for the
 * field list and the card columns of a scope. Extras per row (e.g. the visibility of a field) are
 * projected as `<ng-template #row let-id let-index="index">`.
 */
@Component({
    selector: 'es-scope-property-list',
    templateUrl: './scope-property-list.component.html',
    styleUrls: ['./scope-property-list.component.scss'],
    imports: [
        DragDropModule,
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatInputModule,
        MatTooltipModule,
        NgTemplateOutlet,
        TranslateModule,
    ],
})
export class ScopePropertyListComponent {
    /** selectable properties (label set), in the order to offer them */
    readonly properties = input<ListItem[]>([]);
    /** picked properties, in their order */
    readonly selected = input<string[]>([]);
    /** picked properties that can't be removed */
    readonly locked = input<string[]>([]);
    /** translation of the locked hint */
    readonly lockedHint = input<string>('');
    /** translation shown while nothing is picked */
    readonly emptyText = input<string>('');
    /** data-test prefix */
    readonly testId = input<string>('scope-property-list');
    readonly selectedChange = output<string[]>();

    protected readonly rowTemplate = contentChild<TemplateRef<ScopePropertyRowContext>>('row');
    protected readonly filter = signal('');
    private readonly labels = computed(
        () => new Map(this.properties().map((item) => [item.name, item.label || item.name])),
    );
    protected readonly selectedSet = computed(() => new Set(this.selected()));
    /** picked properties are listed even if they are not offered (anymore), so they can be removed */
    protected readonly visibleProperties = computed(() => {
        const offered = new Set(this.properties().map((item) => item.name));
        const missing = this.selected()
            .filter((id) => !offered.has(id))
            .map((id) => {
                const item = new ListItem('NODE', id);
                item.label = id;
                return item;
            });
        const filter = this.filter().trim().toLowerCase();
        return [...this.properties(), ...missing].filter(
            (item) =>
                !filter ||
                item.name.toLowerCase().includes(filter) ||
                (item.label ?? '').toLowerCase().includes(filter),
        );
    });

    getLabel(id: string): string {
        return this.labels().get(id) ?? id;
    }

    isLocked(id: string): boolean {
        return this.locked().includes(id);
    }

    toggle(id: string, checked: boolean) {
        const selected = this.selected();
        if (checked && !selected.includes(id)) {
            this.selectedChange.emit([...selected, id]);
        } else if (!checked && !this.isLocked(id)) {
            this.selectedChange.emit(selected.filter((s) => s !== id));
        }
    }

    remove(index: number) {
        const selected = [...this.selected()];
        if (!this.isLocked(selected[index])) {
            selected.splice(index, 1);
            this.selectedChange.emit(selected);
        }
    }

    drop(event: CdkDragDrop<string[]>) {
        const selected = [...this.selected()];
        moveItemInArray(selected, event.previousIndex, event.currentIndex);
        this.selectedChange.emit(selected);
    }
}
