import { Component, Input, inject, ChangeDetectionStrategy } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfigEntry } from '../../../services/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';

@Component({
    selector: 'es-main-menu-bottom',
    templateUrl: './main-menu-bottom.component.html',
    styleUrls: ['./main-menu-bottom.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class MainMenuBottomComponent {
    @Input() currentScope: string;

    entries$: Observable<ConfigEntry[]>;

    constructor() {
        const mainMenuEntries = inject(MainMenuEntriesService);

        this.entries$ = mainMenuEntries.entries$;
    }
}
