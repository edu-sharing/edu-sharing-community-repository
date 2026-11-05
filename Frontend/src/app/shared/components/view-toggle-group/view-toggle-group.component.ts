import { Component, input, model } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { IconDirective } from 'ngx-edu-sharing-ui';

/** One selectable entry of an {@link ViewToggleGroupComponent}. */
export interface ViewToggleItem<T = string> {
    /** value the group reports when this entry is selected */
    id: T;
    /** icon id as understood by the `esIcon` directive */
    icon: string;
    /** translation key, used as tooltip and accessible name */
    label: string;
    disabled?: boolean;
}

/**
 * A group of mutually exclusive icon toggles, e.g. to switch between a grid and a list view.
 * The group only knows its items and the selected id; what a selection means is up to the host.
 */
@Component({
    selector: 'es-view-toggle-group',
    templateUrl: 'view-toggle-group.component.html',
    styleUrls: ['view-toggle-group.component.scss'],
    imports: [MatButtonToggleModule, MatTooltipModule, TranslateModule, IconDirective],
})
export class ViewToggleGroupComponent<T = string> {
    readonly items = input.required<ViewToggleItem<T>[]>();
    /** id of the selected item */
    readonly value = model.required<T>();
    /** translation key naming the whole group for assistive technology */
    readonly groupLabel = input<string>();
    readonly disabled = input(false);
}
