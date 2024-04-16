import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfigEntry } from '../../../services/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';

@Component({
    selector: 'es-main-menu-bottom',
    templateUrl: './main-menu-bottom.component.html',
    styleUrls: ['./main-menu-bottom.component.scss'],
})
export class MainMenuBottomComponent implements OnInit {
    @Input() currentScope: string;

    entries$: Observable<ConfigEntry[]>;

    constructor(mainMenuEntries: MainMenuEntriesService) {
        this.entries$ = mainMenuEntries.entries$;
    }

    ngOnInit(): void {}
}
