import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
    inject,
} from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { ConfigEntry } from '../../../services/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';
import { DropdownComponent, OptionItem } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-main-menu-dropdown',
    templateUrl: './main-menu-dropdown.component.html',
    styleUrls: ['./main-menu-dropdown.component.scss'],
    standalone: false,
})
export class MainMenuDropdownComponent implements OnChanges, AfterViewInit, OnDestroy {
    private mainMenuEntries = inject(MainMenuEntriesService);
    private route = inject(ActivatedRoute);

    @ViewChild('dropdown', { static: true }) dropdown: DropdownComponent;

    @Input() currentScope: string;

    private readonly destroyed$ = new Subject<void>();
    optionItems$: Observable<OptionItem[]>;
    @Output() closeDropdown = new EventEmitter<void>();

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    ngAfterViewInit(): void {
        this.dropdown.menu.closed
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.closeDropdown.emit());
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.currentScope) {
            this.setOptionItems();
        }
    }

    private setOptionItems() {
        this.optionItems$ = combineLatest([
            this.mainMenuEntries.entries$,
            this.route.queryParams,
        ]).pipe(map(([entries, currentParams]) => this.toOptionItems(entries, currentParams)));
    }

    private toOptionItems(entries: ConfigEntry[], currentParams: Params): OptionItem[] {
        const preservedParams: Params = {};
        for (const key of UIHelper.COPY_URL_PARAMS) {
            if (currentParams.hasOwnProperty(key)) {
                preservedParams[key] = currentParams[key];
            }
        }
        return entries.map((entry) => {
            // The router navigates in-app links itself, so the callback only reports the switch.
            const optionItem = new OptionItem(
                entry.name,
                entry.icon,
                entry.routerLink ? () => entry.notifyViewSwitched?.() : entry.open,
            );
            if (entry.routerLink) {
                optionItem.link = {
                    routerLink: entry.routerLink,
                    queryParams: { ...preservedParams, ...entry.queryParams },
                };
            } else if (entry.url) {
                optionItem.link = { href: entry.url, openInNew: entry.openInNew };
            }
            optionItem.isSeparate = entry.isSeparate;
            optionItem.isEnabled = !entry.isDisabled;
            optionItem.isSelected = this.currentScope === entry.scope;
            return optionItem;
        });
    }
}
