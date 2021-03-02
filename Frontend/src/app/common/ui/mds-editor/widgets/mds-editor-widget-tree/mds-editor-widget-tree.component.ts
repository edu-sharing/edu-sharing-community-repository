import { CdkConnectedOverlay, ConnectedPosition, OverlayRef } from '@angular/cdk/overlay';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, fromEvent, ReplaySubject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsWidgetType } from '../../types';
import { DisplayValue } from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree } from './tree';
import { FocusOrigin } from '@angular/cdk/a11y';
@Component({
    selector: 'app-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
})
export class MdsEditorWidgetTreeComponent extends MdsEditorWidgetBase implements OnInit, OnDestroy {
    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('chipList', { read: ElementRef }) chipList: ElementRef<HTMLElement>;
    @ViewChild('treeRef') treeRef: MdsEditorWidgetTreeCoreComponent;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;

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
            originX: 'center',
            originY: 'bottom',
            offsetX: 0,
            offsetY: -22,
            overlayX: 'center',
            overlayY: 'top',
        },
        {
            originX: 'center',
            originY: 'top',
            offsetX: 0,
            offsetY: 0,
            overlayX: 'center',
            overlayY: 'bottom',
        },
    ];

    private destroyed$: ReplaySubject<boolean> = new ReplaySubject(1);

    constructor(mdsEditorInstance: MdsEditorInstanceService, translate: TranslateService) {
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
            this.widget.getInitialValues().jointValues,
            this.widget.getInitialValues().individualValues,
        );
        this.chipsControl = new FormControl(
            [
                ...this.widget.getInitialValues().jointValues,
                ...(this.widget.getInitialValues().individualValues ?? []),
            ].map((value) => this.tree.idToDisplayValue(value)),
            this.getStandardValidators(),
        );
        this.indeterminateValues$ = new BehaviorSubject(
            this.widget.getInitialValues().individualValues,
        );
        this.chipsControl.valueChanges.subscribe((values: DisplayValue[]) => {
            this.setValue(values.map((value) => value.key));
        });
        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
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

    openOverlay(): void {
        if (this.overlayIsVisible) {
            this.treeRef.input.nativeElement.focus();
            return;
        }
        this.overlayIsVisible = true;
        // Wait for overlay
        setTimeout(() => {
            overlayClickOutside(
                this.overlay.overlayRef,
                this.overlay.origin.elementRef.nativeElement,
            ).subscribe(() => this.closeOverlay());
        });
    }

    closeOverlay(): void {
        this.overlayIsVisible = false;
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
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
        const targetInsideHost = this.overlay.origin.elementRef.nativeElement.contains(
            event.target,
        );
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

// Adapted from
// https://netbasal.com/advanced-angular-implementing-a-reusable-autocomplete-component-9908c2f04f5
// TODO: replace with built-in event after upgrade to Angular 10.
function overlayClickOutside(overlayRef: OverlayRef, origin: HTMLElement) {
    return fromEvent<MouseEvent>(document, 'mousedown').pipe(
        filter((event) => {
            const clickTarget = event.target as HTMLElement;
            const notOrigin = !origin.contains(clickTarget);
            const notOverlay =
                !!overlayRef && overlayRef.overlayElement.contains(clickTarget) === false;
            return notOrigin && notOverlay;
        }),
        takeUntil(overlayRef.detachments()),
    );
}
