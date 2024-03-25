import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DropdownComponent } from '../../../shared/components/dropdown/dropdown.component';
import { OptionItem } from '../../../core-ui-module/option-item';
import { ConfigEntry } from '../../../core-ui-module/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';
import { MenuCloseReason } from '@angular/material/menu/menu';

@Component({
    selector: 'es-main-menu-dropdown',
    templateUrl: './main-menu-dropdown.component.html',
    styleUrls: ['./main-menu-dropdown.component.scss'],
})
export class MainMenuDropdownComponent implements OnChanges, AfterViewInit {
    @ViewChild('dropdown', { static: true }) dropdown: DropdownComponent;

    @Input() currentScope: string;

    optionItems$: Observable<OptionItem[]>;
    @Output() onClose: EventEmitter<MenuCloseReason>;

    constructor(private mainMenuEntries: MainMenuEntriesService) {}

    ngAfterViewInit(): void {
        this.onClose = this.dropdown.menu.closed;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.currentScope) {
            this.setOptionItems();
        }
    }

    private setOptionItems() {
        this.optionItems$ = this.mainMenuEntries.entries$.pipe(
            map((entries) => this.toOptionItems(entries)),
        );
    }

    private toOptionItems(entries: ConfigEntry[]): OptionItem[] {
        return entries.map((entry) => {
            const optionItem = new OptionItem(entry.name, entry.icon, entry.open);
            optionItem.isSeparate = entry.isSeparate;
            optionItem.isEnabled = !entry.isDisabled;
            optionItem.isSelected = this.currentScope === entry.scope;
            return optionItem;
        });
    }
}
