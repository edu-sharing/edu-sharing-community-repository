import { CdkConnectedOverlay, ConnectedPosition } from '@angular/cdk/overlay';
import {
    AfterViewInit,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, fromEvent, ReplaySubject } from 'rxjs';
import { filter, startWith, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsWidgetType } from '../../../types/types';
import { DisplayValue } from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree } from './tree';
import { FocusOrigin } from '@angular/cdk/a11y';
import { MatChip } from '@angular/material/chips';
import { UIService } from '../../../../../core-module/rest/services/ui.service';

@Component({
    selector: 'es-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
})
export class MdsEditorWidgetTreeComponent
    extends MdsEditorWidgetBase
    implements OnInit, AfterViewInit, OnDestroy
{
    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('chipList', { read: ElementRef }) chipList: ElementRef<HTMLElement>;
    @ViewChild('treeRef') treeRef: MdsEditorWidgetTreeCoreComponent;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;
    @ViewChildren('chip') chips: QueryList<MatChip>;

    valueType: ValueType;
    tree: Tree;
    chipsControl: FormControl;
    indeterminateValues$: BehaviorSubject<string[]>;
    overlayIsVisible = false;
    /**
     * Briefly set to `true` in situations where the input field might get focus as result of a
     * user's action, but we don't want to open the overlay.
     */
    preventOverlayOpen = false;
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'start',
            originY: 'bottom',
            offsetX: 0,
            offsetY: -34,
            overlayX: 'start',
            overlayY: 'top',
        },
        {
            originX: 'start',
            originY: 'top',
            offsetX: 0,
            offsetY: 0,
            overlayX: 'start',
            overlayY: 'bottom',
        },
    ];

    private destroyed$: ReplaySubject<boolean> = new ReplaySubject(1);

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        public uiService: UIService,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnInit(): void {
        if (this.widget.definition.type === MdsWidgetType.SingleValueTree) {
            this.valueType = ValueType.String;
        } else if (this.widget.definition.type === MdsWidgetType.MultiValueTree) {
            this.valueType = ValueType.MultiValue;
        } else {
            throw new Error('Unexpected widget type: ' + this.widget.definition.type);
        }
        this.tree = Tree.generateTree(
            this.widget.definition.values,
            this.widget.getInitialValues()?.jointValues ?? [],
            this.widget.getInitialValues()?.individualValues,
        );
        this.chipsControl = new FormControl(
            [
                ...(this.widget.getInitialValues()?.jointValues ?? []),
                ...(this.widget.getInitialValues()?.individualValues ?? []),
            ].map((value) => this.tree.idToDisplayValue(value)),
            this.getStandardValidators(),
        );
        this.indeterminateValues$ = new BehaviorSubject(
            this.widget.getInitialValues()?.individualValues,
        );
        this.chipsControl.valueChanges.subscribe((values: DisplayValue[]) => {
            this.setValue(values.map((value) => value.key));
        });
        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );
    }

    ngAfterViewInit(): void {
        // We mark all chips as selected for better screen-reader output. However, since selection
        // doesn't do anything, we disable toggling the selection.
        this.chips.changes
            .pipe(startWith(this.chips))
            .subscribe((chips: QueryList<MatChip>) =>
                chips.forEach((chip) => (chip.toggleSelected = () => true)),
            );
    }

    ngOnDestroy() {
        this.destroyed$.next(true);
        this.destroyed$.complete();
    }

    onInputFocusChange(origin: FocusOrigin): void {
        if (!this.preventOverlayOpen && origin && origin !== 'program') {
            this.openOverlay();
        }
    }

    revealInTree(value: DisplayValue): void {
        this.openOverlay();
        setTimeout(() => {
            this.treeCoreComponent.revealInTree(this.tree.findById(value.key));
        });
    }
    focus() {
        this.openOverlay();
    }
    openOverlay(): void {
        if (this.chipsControl.disabled) {
            return;
        }
        if (this.overlayIsVisible) {
            this.treeRef.input.nativeElement.focus();
            return;
        }
        this.overlayIsVisible = true;
    }

    closeOverlay(): void {
        this.overlayIsVisible = false;
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
            this.onBlur.emit();
        });
    }

    toggleOverlay(): void {
        if (this.overlayIsVisible) {
            this.closeOverlay();
        } else {
            this.openOverlay();
        }
    }

    onOverlayKeydown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.closeOverlay();
        } else if (event.key === 'Tab') {
            this.closeOverlay();
        } else {
            const wasHandledByTree = this.treeCoreComponent.handleKeydown(event.code);
            if (wasHandledByTree) {
                event.preventDefault();
            }
        }
    }

    remove(toBeRemoved: DisplayValue): void {
        const treeNode = this.tree.findById(toBeRemoved.key);
        // old values are may not available in tree, so check for null
        if (treeNode) {
            treeNode.isChecked = false;
            treeNode.isIndeterminate = false;
        }
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
        if (this.indeterminateValues$.value?.includes(toBeRemoved.key)) {
            this.indeterminateValues$.next(
                this.indeterminateValues$.value.filter((value) => value !== toBeRemoved.key),
            );
        }
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
        });
    }

    onValuesChange(values: DisplayValue[]): void {
        this.chipsControl.setValue(values);
    }
}
