import { Component, OnInit } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { filter } from 'rxjs/operators';
import { Constraints, NativeWidgetComponent } from '../../../types/types';
import { MdsEditorCommonService } from '../../mds-editor-common.service';
import { Toast } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-mds-editor-widget-version',
    templateUrl: './mds-editor-widget-version.component.html',
    styleUrls: ['./mds-editor-widget-version.component.scss'],
})
export class MdsEditorWidgetVersionComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints: Constraints = {
        requiresNode: true,
        supportsBulk: false,
        supportsInlineEditing: true,
    };
    static readonly graphqlIds = ['version.version', 'version.comment'];
    hasChanges = new BehaviorSubject<boolean>(false);
    loading$ = new BehaviorSubject(false);
    comment: string;
    file: File;
    show: boolean;

    constructor(public mdsEditorValues: MdsEditorInstanceService, private toast: Toast) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$
            .pipe(filter((nodes) => !!nodes))
            .subscribe(
                (nodes) =>
                    (this.show =
                        nodes.some((n) => !n?.properties[RestConstants.CCM_PROP_IO_WWWURL]?.[0]) &&
                        nodes.every((n) => n.type === RestConstants.CCM_TYPE_IO)),
            );
    }

    onChange(): void {
        this.updateState();
    }

    setFile(event: Event) {
        this.file = (event.target as HTMLInputElement).files?.[0];
        this.updateState();
    }

    private updateState() {
        this.hasChanges.next(!!this.comment || !!this.file);
    }

    async forceUpload() {
        this.loading$.next(true);

        try {
            await this.mdsEditorValues.saveContent(this.file, this.comment);
            this.file = null;
            this.toast.toast('DIALOG.SAVED');
        } catch (e) {
            this.toast.error(e);
        }
        this.loading$.next(false);
    }
}
