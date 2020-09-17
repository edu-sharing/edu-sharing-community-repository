import { Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../mds-editor-instance.service';
import { assertUnreachable, InputStatus } from '../types';

export enum ValueType {
    String,
    MultiValue,
    Range,
}

export abstract class MdsEditorWidgetBase {
    @Input() widget: Widget;

    abstract readonly valueType: ValueType;
    readonly isBulk: boolean;

    constructor(
        private mdsEditorInstance: MdsEditorInstanceService,
        private translate: TranslateService,
    ) {
        this.isBulk = this.mdsEditorInstance.isBulk;
    }

    /**
     * Must be called when the widget is available, e.g. in ngOnInit.
     *
     * @returns the initial value.
     */
    initWidget(): readonly string[] {
        if (this.widget.hasCommonInitialValue) {
            return this.widget.initialValue;
        } else {
            switch (this.valueType) {
                case ValueType.String:
                    return [this.translate.instant('MDS.DIFFERENT_VALUES')];
                case ValueType.MultiValue:
                case ValueType.Range:
                    return [];
                default:
                    assertUnreachable(this.valueType);
            }
        }
    }

    setValue(value: string[]): void {
        this.widget.setValue(value);
    }

    setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
    }

    getIsDisabled(): Observable<boolean> {
        if (this.isBulk) {
            return this.widget.observeBulkMode().pipe(map((bulkMode) => bulkMode === 'no-change'));
        } else {
            return of(false);
        }
    }
}
