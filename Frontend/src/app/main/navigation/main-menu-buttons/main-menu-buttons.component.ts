import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MainMenuEntriesService } from '../main-menu-entries.service';

@Component({
    selector: 'es-main-menu-buttons',
    templateUrl: './main-menu-buttons.component.html',
    styleUrls: ['./main-menu-buttons.component.scss'],
    standalone: false,
})
export class MainMenuButtonsComponent {
    @Input() currentScope: string;
    @Output() entryClicked = new EventEmitter<void>();

    readonly entries$ = this.mainMenuEntries.entries$;

    constructor(private mainMenuEntries: MainMenuEntriesService) {}
}
