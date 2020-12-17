import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { BehaviorSubject, from, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { MdsEditorWrapperComponent } from '../../../../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { BulkBehavior } from '../../../../common/ui/mds/mds.component';
import { Node, RestConstants, RestNodeService } from '../../../../core-module/core.module';
import { UIAnimation } from '../../../../core-module/ui/ui-animation';
import { Toast } from '../../../../core-ui-module/toast';

@Component({
    selector: 'app-simple-edit-metadata',
    templateUrl: 'simple-edit-metadata.component.html',
    styleUrls: ['simple-edit-metadata.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class SimpleEditMetadataComponent {
    readonly BulkBehaviour = BulkBehavior;

    @ViewChild('mds') mds: MdsEditorWrapperComponent;

    @Input() nodes: Node[];
    @Input() fromUpload: boolean;
    @Output() onError = new EventEmitter<void>();

    isInited = new BehaviorSubject(false);

    constructor(private nodeApi: RestNodeService, private toast: Toast) {}

    isDirty() {
        if (this.mds.mdsRef) {
            return this.mds.mdsRef.isDirty();
        }
        return this.mds.mdsEditorInstance.getHasUserChanges();
    }

    save(): Observable<void> {
        if (!this.isDirty()) {
            return of();
        }
        return Observable.forkJoin(
            this.nodes.map((n) =>
                from(this.mds.getValues(n)).pipe(
                    switchMap((props) => {
                        delete props[RestConstants.CM_NAME];
                        return this.nodeApi.editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_METADATA_UPDATE,
                            props,
                        );
                    }),
                ),
            ),
        ).pipe(map(() => {}));
    }
}
