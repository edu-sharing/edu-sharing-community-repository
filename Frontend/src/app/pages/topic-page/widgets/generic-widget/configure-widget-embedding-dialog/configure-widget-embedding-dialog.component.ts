import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CardDialogRef } from '../../../../../features/dialogs/card-dialog/card-dialog-ref';
import { SharedModule } from '../../../../../shared/shared.module';

@Component({
    selector: 'es-configure-widget-embedding-dialog',
    imports: [SharedModule],
    templateUrl: 'configure-widget-embedding-dialog.component.html',
    styleUrls: ['configure-widget-embedding-dialog.component.scss'],
})
export class ConfigureWidgetEmbeddingDialogComponent {
    readonly i18nPrefix: string = 'TOPIC_PAGE.CONFIG_WIDGET_EMBEDDING.';
    @Input() dialogRef: CardDialogRef;
    @Input() nodeId: string;
    @Input() propagatedNodeId: string;

    @Output() embedWidget: EventEmitter<'nodeId' | 'configOverwrite'> = new EventEmitter<
        'nodeId' | 'configOverwrite'
    >();

    constructor() {}

    selectOption(option: 'nodeId' | 'configOverwrite') {
        this.dialogRef.close();
        this.embedWidget.emit(option);
    }
}
