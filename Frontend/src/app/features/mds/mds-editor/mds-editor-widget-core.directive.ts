import { Directive, Input } from '@angular/core';
import { EditorBulkMode, EditorMode } from '../types/types';
import { TranslateService } from '@ngx-translate/core';
import { MdsEditorInstanceService, Widget } from './mds-editor-instance.service';
import { BehaviorSubject } from 'rxjs';

@Directive()
export abstract class MdsEditorWidgetCore {
    @Input() widget: Widget;
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    readonly editorMode: EditorMode;
    readonly editorBulkMode: EditorBulkMode;

    constructor(
        public mdsEditorInstance: MdsEditorInstanceService,
        protected translate: TranslateService,
    ) {
        this.editorMode = this.mdsEditorInstance.editorMode;
        this.editorBulkMode = this.mdsEditorInstance.editorBulkMode;
    }
}
