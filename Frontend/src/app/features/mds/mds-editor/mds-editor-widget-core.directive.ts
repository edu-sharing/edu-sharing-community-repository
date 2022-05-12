import {Directive, Input} from '@angular/core';
import {EditorMode} from '../types/mds-types';
import {EditorBulkMode} from '../types/types';
import {TranslateService} from '@ngx-translate/core';
import {MdsEditorInstanceService, Widget} from './mds-editor-instance.service';

@Directive()
export abstract class MdsEditorWidgetCore {
    @Input() widget: Widget;
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
