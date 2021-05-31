import {Component, SimpleChanges, OnChanges, OnInit, ViewChild, Input} from '@angular/core';
import {MdsEditorWrapperComponent} from '../../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import {Node, WorkflowEntry} from '../../core-module/rest/data-object';
import {
    CompletionStatusField,
    MdsEditorInstanceService
} from '../../common/ui/mds-editor/mds-editor-instance.service';
import {RequiredMode} from '../../common/ui/mds-editor/types';
import {Toast} from '../../core-ui-module/toast';
import {DialogButton, RestConnectorService, RestNodeService} from '../../core-module/core.module';
import {Observable} from 'rxjs/Rx';
import {SelectionModel} from '@angular/cdk/collections';
import {WorkflowActionCardComponent} from "../workflow-action-card/workflow-action-card.component";

@Component({
    selector: 'app-workflow-panel',
    templateUrl: './workflow-panel.component.html',
    styleUrls: ['./workflow-panel.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class WorkflowPanelComponent implements OnInit,OnChanges {
    @ViewChild(MdsEditorWrapperComponent) mdsEditorWrapper: MdsEditorWrapperComponent;
    @ViewChild(WorkflowActionCardComponent) actionCard: WorkflowActionCardComponent;
    @Input() selection: SelectionModel<Node>;
    WORKFLOW_STATUS = WorkflowStatus;
    requiredWidgets: CompletionStatusField[];
    otherWidgets: CompletionStatusField[];
    card: {title: string, label: string, buttons: DialogButton[]};
    private workflow: WorkflowEntry[];
    constructor(
        private mdsService: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private connector: RestConnectorService,
        private toast: Toast,
    ) { }

    ngOnInit(): void {
        this.mdsEditorWrapper.mdsEditorInstance.values
            .subscribe((values) => {
                console.log('values changed');
                this.toast.showConfigurableDialog({
                    isCancelable: true,
                    title: 'Status ändern?',
                    message: 'Ein manuelles Ändern des Status kann zu Inkonsistenten des Elements und den Dialogen führen.\n\nMöchten Sie die Statusänderung dennoch durchführen?',
                    buttons: [
                        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.toast.closeModalDialog()),
                        new DialogButton('Ändern', DialogButton.TYPE_DANGER, async () => {
                            this.toast.showProgressDialog();
                            await Observable.forkJoin(this.selection.selected.map((node) =>
                                this.nodeService.editNodeMetadataNewVersion(node.ref.id, 'STATE_CHANGED_MANUALLY', values)
                            )).toPromise();
                            this.toast.closeModalDialog();
                        })
                    ]

                })
            })
    }
    ngOnChanges(changes: SimpleChanges) {

        if(changes.selection) {
            this.selection.changed.subscribe((change) => {
                console.log(change);
                const selected = this.selection.selected;
                if(selected?.length) {
                    this.requiredWidgets = null;
                    this.otherWidgets = null;
                    this.mdsService.observeCompletionStatus().subscribe((completion) => {
                        console.log('status change');
                        this.requiredWidgets = completion[RequiredMode.Mandatory].fields.
                        concat(completion[RequiredMode.MandatoryForPublish].fields);
                        this.otherWidgets = completion[RequiredMode.Optional].fields;
                    });
                    this.mdsService.initWithNodes(selected);
                    this.nodeService.getWorkflowHistory(selected[0].ref.id).subscribe((workflow => this.workflow = workflow))
                } else {
                    this.workflow = [];
                }
            });
        }
    }
    findLatestWorkflowByStatus(status: WorkflowStatus) {
        return this.workflow.filter((w) => w.status === status.valueOf())?.[0];
    }

    showMetadataRecordDialog() {
        this.card = {
            title: 'Erfassung beauftragen',
            label: 'Hinweis zum Vorgang angeben',
            buttons: [
                new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.card = null),
                new DialogButton('Ändern', DialogButton.TYPE_DANGER, () =>
                    this.putStatus(WorkflowStatus.METADATA_RECORD_REQUESTED)
                ),
            ]
        };
    }

    private async putStatus(status: WorkflowStatus) {
        this.card = null;
        this.toast.showProgressDialog();
        const workflow: WorkflowEntry = {
            status: status.valueOf(),
            comment: this.actionCard.input,
            receiver: [],
        };
        await Observable.forkJoin(
            this.selection.selected.map((n) => this.nodeService.addWorkflow(n.ref.id, workflow))
        ).toPromise();
        this.toast.closeModalDialog();
    }
}
export enum WorkflowStatus {
    METADATA_RECORD_REQUESTED = '110_METADATA_RECORD_REQUESTED',
}
