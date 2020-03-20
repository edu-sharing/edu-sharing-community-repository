import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs';
import {
    MainMenuEntriesService,
    Entry,
} from '../../services/main-menu-entries.service';

@Component({
    selector: 'app-main-menu-bottom',
    templateUrl: './main-menu-bottom.component.html',
    styleUrls: ['./main-menu-bottom.component.scss'],
})
export class MainMenuBottomComponent implements OnInit {
    @Input() currentScope: string;

    entries$: Observable<Entry[]>;

    constructor(mainMenuEntries: MainMenuEntriesService) {
        this.entries$ = mainMenuEntries.entries$;
    }

    ngOnInit(): void {}
}
