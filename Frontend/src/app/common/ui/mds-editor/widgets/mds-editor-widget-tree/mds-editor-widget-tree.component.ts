import { CdkConnectedOverlay, ConnectedPosition, OverlayRef } from '@angular/cdk/overlay';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { fromEvent, ReplaySubject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { DisplayValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree } from './tree';
;

@Component({
    selector: 'app-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
})
export class MdsEditorWidgetTreeComponent
    extends MdsEditorWidgetBase
    implements OnInit, AfterViewInit, OnDestroy {
    readonly valueType: ValueType = ValueType.MultiValue;

    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('input') input: ElementRef<HTMLElement>;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;

    tree: Tree;
    values: DisplayValue[];
    formControl = new FormControl();
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

    ngOnInit(): void {
        const values = this.initWidget();
        this.tree = Tree.generateTree(this.widget.definition.values, values);
        this.values = values.map((value) => this.tree.idToDisplayValue(value));
    }

    ngAfterViewInit(): void {
        fromEvent(this.input.nativeElement, 'focus')
            .pipe(takeUntil(this.destroyed$))
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
        if (this.overlayIsVisible) {
            return;
        }
        this.overlayIsVisible = true;
        setTimeout(() => {
            overlayClickOutside(
                this.overlay.overlayRef,
                this.overlay.origin.elementRef.nativeElement,
            ).subscribe(() => this.closeOverlay());
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

    remove(value: DisplayValue): void {
        this.tree.findById(value.key).checked = false;
        const index = this.values.indexOf(value);
        if (index >= 0) {
            this.values.splice(index, 1);
        }
        this.updateValues();
    }

    updateValues(): void {
        // this.values$.next(this.values);
        this.setValue(this.values.map((value) => value.key));
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
