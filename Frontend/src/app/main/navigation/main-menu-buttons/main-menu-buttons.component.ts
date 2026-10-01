import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { Helper } from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { ConfigEntry } from '../../../services/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';

interface MenuButton {
    entry: ConfigEntry;
    queryParams?: Params;
}

@Component({
    selector: 'es-main-menu-buttons',
    templateUrl: './main-menu-buttons.component.html',
    styleUrls: ['./main-menu-buttons.component.scss'],
    standalone: false,
})
export class MainMenuButtonsComponent {
    private mainMenuEntries = inject(MainMenuEntriesService);
    private route = inject(ActivatedRoute);

    @Input() currentScope: string;
    @Output() entryClicked = new EventEmitter<void>();

    readonly buttons$ = combineLatest([this.mainMenuEntries.entries$, this.route.queryParams]).pipe(
        map(([entries, currentParams]) => {
            const preservedParams: Params = {};
            for (const key of UIHelper.COPY_URL_PARAMS) {
                if (currentParams.hasOwnProperty(key)) {
                    preservedParams[key] = currentParams[key];
                }
            }
            return entries.map(
                (entry): MenuButton => ({
                    entry,
                    queryParams: entry.routerLink
                        ? { ...preservedParams, ...entry.queryParams }
                        : undefined,
                }),
            );
        }),
    );

    /**
     * Whether the entry is rendered as a real link, so the browser offers "open in new tab" etc.
     */
    isLink(entry: ConfigEntry): boolean {
        return !entry.isDisabled && !!(entry.routerLink || entry.url);
    }

    onClick(event: MouseEvent, entry: ConfigEntry): void {
        if (entry.isDisabled) {
            return;
        }
        if (entry.routerLink && this.isLink(entry) && Helper.isModifiedClick(event)) {
            // The browser opens the link in a new tab; this tab keeps its current view.
            return;
        }
        if (entry.routerLink && this.isLink(entry)) {
            // The router link directive navigates on its own.
            entry.notifyViewSwitched?.();
        } else if (entry.url && this.isLink(entry) && !Helper.isModifiedClick(event)) {
            // Plain clicks go through the entry so platform specific URL handling is kept.
            event.preventDefault();
            entry.open();
        } else if (!this.isLink(entry)) {
            entry.open();
        }
        this.entryClicked.emit();
    }
}
