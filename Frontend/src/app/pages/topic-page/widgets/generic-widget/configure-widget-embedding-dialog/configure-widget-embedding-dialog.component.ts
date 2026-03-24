import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CardDialogRef } from '../../../../../features/dialogs/card-dialog/card-dialog-ref';
import { SharedModule } from '../../../../../shared/shared.module';

export interface WidgetEmbeddingOption {
    type: 'default' | 'advanced';
    mode: 'nodeId' | 'configOverwrite';
}

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
    @Output() embedWidget: EventEmitter<WidgetEmbeddingOption> =
        new EventEmitter<WidgetEmbeddingOption>();

    activeTab: 'default' | 'advanced' = 'default';

    onTabChanged(index: number) {
        this.activeTab = index === 0 ? 'default' : 'advanced';
    }

    selectMode(mode: 'nodeId' | 'configOverwrite') {
        this.dialogRef.close();
        this.embedWidget.emit({
            type: this.activeTab,
            mode: mode,
        });
    }
}
