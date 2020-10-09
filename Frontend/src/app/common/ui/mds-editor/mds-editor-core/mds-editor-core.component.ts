import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { MdsView } from '../types';

@Component({
    selector: 'app-mds-editor-core',
    templateUrl: './mds-editor-core.component.html',
    styleUrls: ['./mds-editor-core.component.scss'],
})
export class MdsEditorCoreComponent {
    views: MdsView[];
    hasExtendedWidgets: boolean;
    readonly shouldShowExtendedWidgets$: BehaviorSubject<boolean>;

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {
        this.shouldShowExtendedWidgets$ = this.mdsEditorInstance.shouldShowExtendedWidgets$;
        this.mdsEditorInstance.mdsInitDone.subscribe(() => this.init());
    }

    private async init(): Promise<void> {
        if (this.views) {
            // Make sure existing views are destroyed and reinitialized.
            this.views = [];
            await tick();
        }
        this.views = this.mdsEditorInstance.views;
        this.hasExtendedWidgets = this.mdsEditorInstance.widgets.some(
            (widget) => widget.definition.isExtended,
        );
        // Wait for `MdsEditorViewComponent`s to be injected.
        await tick();
        // Wait for `MdsEditorViewComponent`s to inject their widgets.
        await tick();
        this.mdsEditorInstance.mdsInflated.next();
    }
}

function tick(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve));
}
