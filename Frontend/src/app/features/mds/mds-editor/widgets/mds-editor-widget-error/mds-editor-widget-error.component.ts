import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'es-mds-editor-widget-error',
    templateUrl: './mds-editor-widget-error.component.html',
    styleUrls: ['./mds-editor-widget-error.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class MdsEditorWidgetErrorComponent {
    @Input() widgetName: string;
    @Input() reason: string;

    constructor() {}
}
