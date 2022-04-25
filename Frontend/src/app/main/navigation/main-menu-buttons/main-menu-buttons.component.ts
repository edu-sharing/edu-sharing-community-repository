import { Component, Input, OnInit } from '@angular/core';
import { MainMenuEntriesService } from '../main-menu-entries.service';

@Component({
    selector: 'es-main-menu-buttons',
    templateUrl: './main-menu-buttons.component.html',
    styleUrls: ['./main-menu-buttons.component.scss'],
})
export class MainMenuButtonsComponent implements OnInit {
    @Input() currentScope: string;

    readonly entries$ = this.mainMenuEntries.entries$;

    constructor(private mainMenuEntries: MainMenuEntriesService) {}

    ngOnInit(): void {}
}
