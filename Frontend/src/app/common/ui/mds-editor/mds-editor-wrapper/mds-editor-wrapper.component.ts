import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { Node } from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { BulkBehaviour, MdsComponent } from '../../mds/mds.component';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { EditorType, UserPresentableError } from '../types';

/**
 * Wrapper component to select between the legacy `<mds>` component and the Angular-native
 * `<app-mds-editor>`.
 *
 * Input properties have to be stable after initialization.
 *
 * In case <app-mds-editor> is selected, do some data preprocessing.
 */
@Component({
    selector: 'app-mds-editor-wrapper',
    templateUrl: './mds-editor-wrapper.component.html',
    styleUrls: ['./mds-editor-wrapper.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class MdsEditorWrapperComponent implements OnInit, OnChanges {
    // tslint:disable: no-output-on-prefix  // Keep API compatibility.

    // Properties compatible to legacy MdsComponent.
    @ViewChild(MdsComponent) mdsRef: MdsComponent;

    @Input() addWidget = false;
    @Input() allowReplacing = true;
    @Input() bulkBehaviour = BulkBehaviour.Default;
    @Input() create: string;
    @Input() currentValues: any;
    @Input() customTitle: string;
    @Input() embedded = false;
    @Input() extended = false;
    @Input() groupId: string;
    @Input() invalidate: boolean;
    @Input() labelNegative = 'CANCEL';
    @Input() labelPositive = 'SAVE';
    @Input() mode = 'default';
    @Input() nodes: Node[];
    @Input() parentNode: Node;
    @Input() priority = 1;
    @Input() repository: string;
    @Input() setId: string;
    @Input() suggestions: any;

    @Output() extendedChange = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter();
    @Output() onMdsLoaded = new EventEmitter();
    @Output() openContributor = new EventEmitter();
    @Output() openLicense = new EventEmitter();
    @Output() openTemplate = new EventEmitter();

    // Internal state.
    propertiesAreInitialized = false;
    isLoading = true;
    editorType: EditorType;

    constructor(public mdsEditorInstance: MdsEditorInstanceService, private toast: Toast) {}

    ngOnInit(): void {
        this.propertiesAreInitialized = true;
        this.init();
    }

    ngOnChanges(): void {
        if (this.propertiesAreInitialized) {
            throw new Error('Updating input properties after initialization is not supported.');
        }
    }

    handleKeyboardEvent(event: KeyboardEvent): boolean {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.handleKeyboardEvent(event);
            case 'angular':
                // Tell the outer component that we handle all keyboard events. This prevents the
                // dialog to be closed from outside on Escape without confirmation.
                return true;
        }
    }

    async onSave(): Promise<void> {
        this.isLoading = true;
        try {
            const updatedNodes = await this.mdsEditorInstance.save();
            this.toast.toast('WORKSPACE.EDITOR.UPDATED');
            this.onDone.emit(updatedNodes);
        } catch (error) {
            this.handleError(error);
        } finally {
            this.isLoading = false;
        }
    }

    private async init(): Promise<void> {
        this.isLoading = true;
        try {
            if (this.nodes) {
                this.editorType = await this.mdsEditorInstance.initWithNodes(this.nodes);
            } else {
                this.editorType = 'legacy';
            }
        } catch (error) {
            this.handleError(error);
        } finally {
            this.isLoading = false;
        }
    }

    private handleError(error: any): void {
        if (error instanceof UserPresentableError) {
            this.toast.error(null, error.message);
        } else {
            console.error(error);
            this.toast.error(error);
        }
        this.onCancel.emit();
    }
}
