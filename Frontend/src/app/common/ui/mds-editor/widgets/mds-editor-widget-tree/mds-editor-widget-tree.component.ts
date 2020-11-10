import { CdkConnectedOverlay, ConnectedPosition, OverlayRef } from '@angular/cdk/overlay';
import {AfterViewInit, ApplicationRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import { FormControl } from '@angular/forms';
import {BehaviorSubject, fromEvent, merge, Observable, ReplaySubject} from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { MdsWidgetType } from '../../types';
import { DisplayValue } from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree } from './tree';
import {TranslateService} from '@ngx-translate/core';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
@Component({
    selector: 'app-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
})
export class MdsEditorWidgetTreeComponent
    extends MdsEditorWidgetBase
    implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('input') input: ElementRef<HTMLElement>;
    @ViewChild('treeRef') treeRef: MdsEditorWidgetTreeCoreComponent;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;

    valueType: ValueType;
    tree: Tree;
    chipsControl: FormControl;
    indeterminateValues$: BehaviorSubject<string[]>;
    overlayIsVisible = false;
    readonly overlayPositions: readonly ConnectedPosition[] = [
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

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private applicationRef: ApplicationRef
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

    ngAfterViewInit(): void {
        merge(
            fromEvent(this.input.nativeElement, 'focus'),
            fromEvent(this.input.nativeElement, 'keyup'),
            fromEvent(this.input.nativeElement, 'mouseup')
        ).pipe(takeUntil(this.destroyed$))
            .subscribe(() => {
                this.openOverlay();
            });

    }

    ngOnDestroy() {
        this.destroyed$.next(true);
        this.destroyed$.complete();
    }

    revealInTree(value: DisplayValue): void {
        this.openOverlay();
        setTimeout(() => {
            this.treeCoreComponent.revealInTree(this.tree.findById(value.key));
        });
    }

    openOverlay(): void {
        // Don't interfere with change detection
        setTimeout(async () => {
            this.overlayIsVisible = true;
            setTimeout(() => {
                this.treeRef.input.nativeElement.focus()
            });
            // Wait for overlay
            setTimeout(() => {
                overlayClickOutside(
                    this.overlay.overlayRef,
                    this.overlay.origin.elementRef.nativeElement,
                ).subscribe(() => this.closeOverlay());
            });
        });
    }

    closeOverlay(): void {
        this.overlayIsVisible = false;
    }

    onOverlayKeydown(event: KeyboardEvent) {
        const targetInsideHost = this.overlay.origin.elementRef.nativeElement.contains(
            event.target,
        );
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.closeOverlay();
        } else if (targetInsideHost && event.key === 'Tab') {
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
        treeNode.isChecked = false;
        treeNode.isIndeterminate = false;
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
        if (this.indeterminateValues$.value?.includes(toBeRemoved.key)) {
            this.indeterminateValues$.next(
                this.indeterminateValues$.value.filter((value) => value !== toBeRemoved.key),
            );
        }
    }

    onValuesChange(values: DisplayValue[]): void {
        this.chipsControl.setValue(values);
    }
}

// Adapted from
// https://netbasal.com/advanced-angular-implementing-a-reusable-autocomplete-component-9908c2f04f5
// TODO: replace with built-in event after upgrade to Angular 10.
function overlayClickOutside(overlayRef: OverlayRef, origin: HTMLElement) {
    return fromEvent<MouseEvent>(document, 'click').pipe(
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
