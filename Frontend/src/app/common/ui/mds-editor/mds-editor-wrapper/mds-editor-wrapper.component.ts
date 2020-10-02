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
import { Node } from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { BulkBehaviour, MdsComponent } from '../../mds/mds.component';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { EditorType, MdsWidget, UserPresentableError } from '../types';

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
    @Input() @mutable currentValues: any[];
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
    @Input() @mutable suggestions: any;

    @Output() extendedChange = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter();
    @Output() onMdsLoaded = new EventEmitter();
    @Output() openContributor = new EventEmitter();
    @Output() openLicense = new EventEmitter();
    @Output() openTemplate = new EventEmitter();

    // Internal state.
    initHasStarted = false;
    isLoading = true;
    editorType: EditorType;

    /** Properties with `@mutable` decorator. */
    mutableProperties: string[];
    private passedThroughProperties: { [key: string]: any } = {};

    constructor(public mdsEditorInstance: MdsEditorInstanceService, private toast: Toast) {
        return new Proxy(this, {
            get: (target: MdsEditorWrapperComponent, p, receiver) => {
                if (p in target) {
                    return (target as any)[p];
                } else if (target.mdsRef && p in target.mdsRef) {
                    const value = (target.mdsRef as any)[p];
                    if (value && this.passedThroughProperties[p as string] !== value) {
                        this.passedThroughProperties[p as string] = value;
                        console.log('passed through', p, value);
                    }
                    if (typeof value === 'function') {
                        return value.bind(target.mdsRef);
                    }
                    return value;
                } else if (!target.mdsRef) {
                    // Probably requested an MDS property, but isn't ready yet
                    return;
                }
                console.log('get undefined property', p);
            },
            // set: (target: MdsEditorWrapperComponent, p, value: any, receiver) => {
            //     if (p in target) {
            //         (target as any)[p] = value;
            //     } else {
            //         (target as any)[p] = value;
            //         console.log('set undefined property', p, value, receiver);
            //     }
            //     return true;
            // }
        });
    }

    ngOnInit(): void {
        // For compatibility reasons, we wait for `loadMds()` to be called before initializing when
        // `nodes` is undefined.
        //
        // TODO: Make sure that input is not modified after `ngOnInit()` and remove calls to
        // `loadMds()`
        if (this.nodes) {
            this.init();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        for (const property in changes) {
            if (this.initHasStarted && !this.mutableProperties?.includes(property)) {
                console.warn(
                    `Updating property '${property}' after initialization is not supported.`,
                    'Changes will not be reflected.',
                    changes[property],
                );
            }
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

    /** @deprecated compatibility to legacy `mds` component */
    getValues(): { [property: string]: string[] } {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.getValues();
            case 'angular':
                // TODO
                return {};
            default:
                console.warn('getValues() was called before init finished');
                return null;
        }
    }

    /** @deprecated compatibility to legacy `mds` component */
    saveValues(): { [property: string]: string[] } {
        switch (this.editorType) {
            case 'legacy':
                return this.mdsRef.saveValues();
            case 'angular':
                // TODO
                return {};
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
                // TODO
                return [];
            default:
                console.warn('get currentWidgets() was called before init finished');
                return null;
        }
    }

    /** @deprecated compatibility to legacy `mds` component */
    loadMds(): void {
        this.init().then(() => {
            switch (this.editorType) {
                case 'legacy':
                    setTimeout(() => {
                        return this.mdsRef.loadMds();
                    });
                    return;
                case 'angular':
                    this.mdsEditorInstance.mdsDefinition$
                        .pipe(first((definition) => definition !== null))
                        .subscribe((definition) => this.onMdsLoaded.emit(definition));
            }
        });
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
        if (this.initHasStarted) {
            console.warn('init() was called more than once');
        }
        this.initHasStarted = true;
        this.isLoading = true;
        this.mdsEditorInstance.isEmbedded = this.embedded;
        try {
            if (this.nodes) {
                this.editorType = await this.mdsEditorInstance.initWithNodes(this.nodes);
            } else {
                this.editorType = await this.mdsEditorInstance.initAlt(
                    this.groupId,
                    this.setId,
                    this.repository,
                    this.currentValues,
                );
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

export function mutable(target: MdsEditorWrapperComponent, property: string): void {
    // `target` is actually the prototype of `MdsEditorWrapperComponent`
    if (target.mutableProperties) {
        target.mutableProperties.push(property);
    } else {
        target.mutableProperties = [property];
    }
}
