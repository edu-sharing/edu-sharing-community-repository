import { Component, Input, OnInit } from '@angular/core';
import { MatRadioChange } from '@angular/material/radio';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { assertUnreachable, BulkMode } from '../../types';
import { ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-container',
    templateUrl: './mds-editor-widget-container.component.html',
    styleUrls: ['./mds-editor-widget-container.component.scss'],
})
export class MdsEditorWidgetContainerComponent implements OnInit {
    @Input() widget: Widget;
    @Input() valueType: ValueType;
    @Input() label: string | boolean;

    readonly ValueType = ValueType;
    readonly isBulk: boolean;
    readonly labelId: string;
    bulkMode: BulkMode;

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {
        this.isBulk = this.mdsEditorInstance.isBulk;
        this.labelId = Math.random().toString(36).substr(2);
    }

    ngOnInit(): void {
        if (this.label === true) {
            this.label = this.widget.definition.caption;
        }
        if (this.widget && this.isBulk) {
            switch (this.valueType) {
                case ValueType.String:
                case ValueType.Range:
                    this.setBulkMode('no-change');
                    break;
                case ValueType.MultiValue:
                    if (this.widget.hasCommonInitialValue) {
                        this.setBulkMode('replace');
                    } else {
                        this.setBulkMode('append');
                    }
                    break;
                default:
                    assertUnreachable(this.valueType);
            }
        }
    }

    onBulkModeReplaceToggleChange(event: MatSlideToggleChange): void {
        this.setBulkMode(event.checked ? 'replace' : 'no-change');
    }

    onBulkModeMultiValueChange(event: MatRadioChange): void {
        this.setBulkMode(event.value);
    }

    private setBulkMode(value: BulkMode): void {
        this.bulkMode = value;
        this.widget.setBulkMode(value);
    }
}
