import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs';
import {
    MainMenuEntriesService,
} from '../../services/main-menu-entries.service';
import {ConfigEntry} from '../../../core-ui-module/node-helper.service';

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
