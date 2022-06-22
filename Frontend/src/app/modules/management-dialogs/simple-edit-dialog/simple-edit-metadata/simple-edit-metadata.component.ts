import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { BehaviorSubject, forkJoin, from, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Node, RestConstants, RestNodeService } from '../../../../core-module/core.module';
import { UIAnimation } from '../../../../core-module/ui/ui-animation';
import { Toast } from '../../../../core-ui-module/toast';
import { MdsEditorWrapperComponent } from '../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { BulkBehavior } from '../../../../features/mds/types/types';

@Component({
    selector: 'es-simple-edit-metadata',
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

    /**
     * validate the form data
     * return true if data is valid, false otherwise
     */
    validate() {
        if (!this.mds.mdsEditorInstance.getIsValid()) {
            this.mds.mdsEditorInstance.showMissingRequiredWidgets();
            return false;
        }
        return true;
    }
    save(): Observable<void> {
        if (!this.isDirty()) {
            // emit null so that next and complete get's called
            return of(null);
        }
        return forkJoin(
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
