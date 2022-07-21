import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { GenericAuthority, Node } from '../../core-module/core.module';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';
import { NodeEntriesDisplayType } from './entries-model';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends NodeEntriesDataType> implements OnInit, OnDestroy {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    private destroyed = new Subject<void>();

    constructor(
        public entriesService: NodeEntriesService<T>,
        public templatesService: NodeEntriesTemplatesService,
        private globalKeyboardShortcuts: KeyboardShortcutsService,
    ) {}

    ngOnInit(): void {
        if (this.entriesService.globalKeyboardShortcuts) {
            this.registerGlobalKeyboardShortcuts();
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerGlobalKeyboardShortcuts() {
        this.globalKeyboardShortcuts.register(
            [
                {
                    modifiers: ['Ctrl/Cmd'],
                    keyCode: 'KeyA',
                    ignoreWhen: (event) =>
                        // SmallGrid doesn't support selection
                        this.entriesService.displayType === NodeEntriesDisplayType.SmallGrid,
                    callback: () => this.entriesService.toggleSelectAll(),
                },
            ],
            { until: this.destroyed },
        );
    }
}

export type NodeEntriesDataType = Node | GenericAuthority;
