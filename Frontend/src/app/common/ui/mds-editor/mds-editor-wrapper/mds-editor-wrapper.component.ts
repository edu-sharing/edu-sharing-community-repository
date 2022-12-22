import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { first } from 'rxjs/operators';
import {Node, RestConstants} from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { BulkBehavior, MdsComponent } from '../../mds/mds.component';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import {
    EditorMode,
    EditorType,
    MdsWidget,
    Suggestions,
    UserPresentableError,
    Values,
} from '../types';

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
    @Input() bulkBehaviour = BulkBehavior.Default;
    @Input() create: string;
    @Input() currentValues: Values;
    @Input() customTitle: string;
    @Input() embedded = false;
    @Input() extended = false;
    @Input() groupId: string;
    @Input() invalidate: boolean;
    @Input() labelNegative = 'CANCEL';
    @Input() labelPositive = 'SAVE';
    @Input() mode: 'search' | 'default' = 'default';
    @Input() nodes: Node[];
    @Input() parentNode: Node;
    @Input() priority = 1;
    @Input() repository = RestConstants.HOME_REPOSITORY;
    @Input() editorMode: EditorMode;
    @Input() setId: string;
    @Input() suggestions: Suggestions;

    @Output() extendedChange = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter<Node[]|Values>();
    @Output() onMdsLoaded = new EventEmitter();
    @Output() openContributor = new EventEmitter();
    /**
     * @DEPRECATED old mds only
     */
    @Output() openLicense = new EventEmitter();
    @Output() openTemplate = new EventEmitter();

    // Internal state.
    isLoading = true;
    editorType: EditorType;

    constructor(public mdsEditorInstance: MdsEditorInstanceService, private toast: Toast) {}

    ngOnInit(): void {
        // For compatibility reasons, we wait for `loadMds()` to be called before initializing when
        // `nodes` is undefined.
        //
        // TODO: Make sure that inputs are ready when this component is initialized and remove calls
        // to `loadMds()`.
        if (this.nodes || this.currentValues) {
            this.init();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if ('suggestions' in changes) {
            this.mdsEditorInstance.suggestions$.next(changes.suggestions.currentValue);
        }
    }

    /** @deprecated compatibility to legacy `mds` component */
    handleKeyboardEvent(event: KeyboardEvent): boolean {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.handleKeyboardEvent(event);
            case 'angular':
                // Tell the outer component that we handle all keyboard events. This prevents the
                // dialog to be closed from outside on Escape without confirmation.
                return true;
            default:
                console.warn('handleKeyboardEvent() was called before init finished');
                return null;
        }
    }

    async getValues(node: Node = null): Promise<{ [property: string]: string[] }> {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.getValues(node?.properties);
            case 'angular':
                return this.mdsEditorInstance.getValues(node);
            default:
                console.warn('getValues() was called before init finished');
                return null;
        }
    }

    /**
     * @deprecated compatibility to legacy `mds` component
     *
     * Use `getValues()`
     */
    async saveValues(): Promise<{ [property: string]: string[] }> {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.saveValues();
            case 'angular':
                const values = await this.mdsEditorInstance.getValues();
                this.onDone.emit(values);
                return values;
            default:
                console.warn('saveValues() was called before init finished');
                return null;
        }
    }

    /** @deprecated compatibility to legacy `mds` component */
    get currentWidgets(): MdsWidget[] {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.currentWidgets;
            case 'angular':
                return this.mdsEditorInstance.widgets.value.map((widget) => widget.definition);
            default:
                console.warn('get currentWidgets() was called before init finished');
                return null;
        }
    }

    /**
     * @deprecated compatibility to legacy `mds` component
     *
     * Use `reInit()` and make sure inputs are prepared before calling.
     */
    loadMds(onlyLegacy = false): void {
        // In case of `SearchComponent`, `currentValues` is not ready when `loadMds` is called. So
        // we wait tick before initializing.
        setTimeout(() => {
            this.init().then(() => {
                switch (this.editorType) {
                    case 'legacy':
                        // Wait for mdsRef
                        setTimeout(() => {
                            return this.mdsRef.loadMds();
                        });
                        return;
                    case 'angular':
                        if(onlyLegacy) {
                            return;
                        }
                        this.mdsEditorInstance.mdsDefinition$
                            .pipe(first((definition) => definition !== null))
                            .subscribe((definition) => this.onMdsLoaded.emit(definition));
                }
            });
        });
    }

    async onSave(): Promise<void> {
        this.isLoading = true;
        try {
            if (!this.mdsEditorInstance.getCanSave()) {
                // no changes, behave like close
                if(this.mdsEditorInstance.getIsValid()){
                    this.onDone.emit(this.nodes);
                    return;
                } else {
                    this.mdsEditorInstance.showMissingRequiredWidgets();
                }
                return;
            }
            const updatedNodes = await this.mdsEditorInstance.save();
            this.toast.toast('WORKSPACE.EDITOR.UPDATED');
            this.onDone.emit(updatedNodes);
        } catch (error) {
            if(error?.error?.error?.endsWith(RestConstants.CONTENT_FILE_EXTENSION_VERIFICATION_EXCEPTION)) {
                this.handleError(new UserPresentableError('MDS.ERROR_FILE_EXTENSION_VERIFICATION_EXCEPTION'));
            } else {
                this.handleError(error);
            }
        } finally {
            this.isLoading = false;
        }
    }

    async reInit(): Promise<void> {
        return this.init();
    }

    private async init(): Promise<void> {
        this.isLoading = true;
        try {
            if (this.nodes) {
                this.editorType = await this.mdsEditorInstance.initWithNodes(this.nodes, {
                    groupId: this.groupId,
                    bulkBehavior: this.bulkBehaviour,
                });
            } else {
                this.editorType = await this.mdsEditorInstance.initWithoutNodes(
                    this.groupId,
                    this.setId,
                    this.repository,
                    this.editorMode ?? 'search',
                    this.currentValues,
                );
            }
            if (!this.editorType) {
                console.warn(
                    'mds ' +
                        this.setId +
                        ' at ' +
                        this.repository +
                        ' did not specify any rendering type',
                );
                this.editorType = 'legacy';
            }
        } catch (error) {
            this.handleError(error);
        } finally {
            this.isLoading = false;
        }
    }

    private handleError(error: any): void {
        console.error(error);
        if (error instanceof UserPresentableError || error.message) {
            this.toast.error(null, error.message);
        } else {
            this.toast.error(error);
        }
        this.onCancel.emit();
    }
}
