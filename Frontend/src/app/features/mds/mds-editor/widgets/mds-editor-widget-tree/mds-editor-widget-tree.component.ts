import {
    CdkConnectedOverlay,
    ConnectedPosition,
    ConnectedOverlayPositionChange,
} from '@angular/cdk/overlay';
import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    QueryList,
    signal,
    ViewChild,
    ViewChildren,
    WritableSignal,
} from '@angular/core';
import { FormControl, UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, firstValueFrom, Observable, ReplaySubject } from 'rxjs';
import { debounceTime, filter, map, startWith, takeUntil } from 'rxjs/operators';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsWidget, MdsWidgetValue } from '../../../types/types';
import { MdsWidgetType, ValueType } from 'ngx-edu-sharing-ui';
import { DisplayValue } from '../DisplayValues';
import { MdsEditorWidgetBase, MdsEditorWidgetChipsSuggestionBase } from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree, TreeNode } from './tree';
import { MatChipOption, MatChipRow } from '@angular/material/chips';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { MatButton } from '@angular/material/button';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { Toast } from '../../../../../services/toast';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

interface UserProposalGroup {
    value: string;
    displayValue: DisplayValue;
    count: number;
    suggestions: SuggestionResponseDto[];
}

@Component({
    selector: 'es-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetTreeComponent
    extends MdsEditorWidgetChipsSuggestionBase
    implements OnInit, AfterViewInit, OnDestroy
{
    inputControl = new FormControl('');
    private hasFocus = true;
    isTree: boolean;
    showDropdownArrow: boolean;
    private ignoreNextFocusEvent = false;
    valueSelection: string = 'valueSelection';
    add(value: DisplayValue): void {
        const treeNode = this.tree.findById(value.key);
        // old values are may not available in tree, so check for null
        if (treeNode) {
            treeNode.isChecked = true;
            treeNode.isIndeterminate = false;
        }
        const values: DisplayValue[] = this.chipsControl.value;
        this.chipsControl.setValue([...values, value]);
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
        });
    }
    getChipsValue(value: DisplayValue) {
        if (value.key === value.label || !value.label) {
            const ds = this.widget
                .getInitialDisplayValues()
                .value?.values?.find((v) => v.key === value.key)?.displayString;
            return {
                key: value.key,
                label: ds || value.key,
            };
        }
        return value;
    }
    toDisplayValue(value: MdsWidgetValue | string): DisplayValue {
        if (typeof value === 'string') {
            return this.tree.toDisplayValue(value);
        }
        return {
            key: value.id,
            label: value.caption,
        };
    }
    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('container') container: MdsEditorWidgetContainerComponent;
    @ViewChild('chipList', { read: ElementRef }) chipList: ElementRef<HTMLElement>;
    @ViewChild('treeRef') treeRef: MdsEditorWidgetTreeCoreComponent;
    @ViewChild('openButton') openButtonRef: MatButton;
    @ViewChild('inputElement') inputElement: ElementRef<HTMLInputElement>;
    @ViewChild('box') boxRef: ElementRef<HTMLElement>;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;
    @ViewChildren('chip') chips: QueryList<MatChipRow>;

    userProposalGroups$: Observable<UserProposalGroup[]>;
    valueType: ValueType;
    tree: Tree;
    indeterminateValues$: BehaviorSubject<string[]>;
    overlayIsVisible = false;
    /**
     * Briefly set to `true` in situations where the input field might get focus as result of a
     * user's action, but we don't want to open the overlay.
     */
    preventOverlayOpen = false;
    overlayPositions: ConnectedPosition[] = [
        {
            originX: 'start',
            originY: 'bottom',
            offsetX: 0,
            offsetY: this.editorMode !== this.valueSelection ? -34 : 0,
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
    positionY: WritableSignal<string> = signal(null);
    private destroyed$: ReplaySubject<boolean> = new ReplaySubject(1);

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        toast: Toast,
        private changeDetectorRef: ChangeDetectorRef,
        public uiService: UIService,
    ) {
        super(toast, mdsEditorInstance, translate);
    }

    async ngOnInit() {
        if (this.mdsEditorInstance.editorMode === 'inline') {
            this.onBlur.subscribe(() => (this.hasFocus = false));
            // an host listener is not triggering
            document.addEventListener(
                'blur',
                (e) => {
                    if (!this.hasFocus) {
                        return;
                    }
                    this.onBlurInput(e);
                },
                true,
            );
        }
        this.chipsControl = new UntypedFormControl(null, this.getStandardValidators());
        if (this.widget.definition.type === MdsWidgetType.SingleValueTree) {
            this.valueType = ValueType.String;
        } else if (
            [
                MdsWidgetType.MultiValueTree,
                MdsWidgetType.MultiValueFixedBadges,
                MdsWidgetType.MultiValueSuggestBadges,
            ].includes(this.widget.definition.type as MdsWidgetType)
        ) {
            this.valueType = ValueType.MultiValue;
        } else {
            throw new Error('Unexpected widget type: ' + this.widget.definition.type);
        }
        this.isTree = [MdsWidgetType.MultiValueTree, MdsWidgetType.SingleValueTree].includes(
            this.widget.definition.type as MdsWidgetType,
        );
        this.showDropdownArrow =
            this.isTree ||
            (this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges &&
                !!this.widget.definition.values);

        this.inputControl.valueChanges
            .pipe(
                takeUntil(this.destroyed$),
                debounceTime(100),
                filter((v) => v?.length >= 2 && !this.overlayIsVisible),
            )
            .subscribe(() => this.openOverlay());
        this.tree = Tree.generateTree(
            this.widget.definition.values,
            (await this.widget.getInitalValuesAsync()).jointValues ?? [],
            (await this.widget.getInitalValuesAsync()).individualValues,
        );
        super.initSuggestions();
        this.chipsControl = new UntypedFormControl(
            [
                ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
                ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
            ].map((value) => this.tree.toDisplayValue(value)),
            this.getStandardValidators(),
        );
        this.indeterminateValues$ = new BehaviorSubject(
            (await this.widget.getInitalValuesAsync()).individualValues,
        );
        this.chipsControl.valueChanges.subscribe((values: DisplayValue[]) => {
            // temporary hack if you want to apply all
            // this.setValue(values.map((value) => value.key).concat(MdsService.unfoldTreeChilds(values.map((value) => value.key), this.widget.definition)));
            this.setValue(values.map((value) => value.key));
        });
        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );
        this.userProposalGroups$ = this.widget.getSuggestions().pipe(
            map((suggestions) => {
                if (!suggestions) return [];
                const currentKeys = new Set<string>(
                    (this.chipsControl?.value ?? [])
                        .filter((v: DisplayValue) => v?.key)
                        .map((v: DisplayValue) => v.key),
                );
                const grouped = new Map<string, UserProposalGroup>();
                for (const s of suggestions.filter(
                    (s) =>
                        s.type === 'USER_PROPOSAL' &&
                        s.status === 'PENDING' &&
                        !currentKeys.has(s.value as string),
                )) {
                    const key = s.value as string;
                    if (grouped.has(key)) {
                        const group = grouped.get(key);
                        group.count++;
                        group.suggestions.push(s);
                    } else {
                        grouped.set(key, {
                            value: key,
                            displayValue: this.toDisplayValue(key),
                            count: 1,
                            suggestions: [s],
                        });
                    }
                }
                return Array.from(grouped.values()).sort((a, b) => b.count - a.count);
            }),
        );

        this.widget.getShowAiSuggestions().subscribe(([show, suggestions]) => {
            if (show) {
                suggestions
                    ?.filter(
                        (s) =>
                            s.type === 'AI' &&
                            s.status === 'PENDING' &&
                            !this.widget.getValue().includes(s.value as string),
                    )
                    .filter(
                        // filter for non duplicate values
                        (obj, index, self) =>
                            index === self.findIndex((o) => o.value === obj.value),
                    )
                    .filter(
                        // validate valuespace
                        (obj) => {
                            if (this.widget.definition.values) {
                                if (
                                    !this.widget.definition.values.some((v) => v.id === obj.value)
                                ) {
                                    console.warn(
                                        'Invalid suggestion "' +
                                            obj.value +
                                            '" received for ' +
                                            this.widget.definition.id +
                                            ', not in valuespace',
                                        obj,
                                        this.widget.definition.values,
                                    );
                                    return false;
                                }
                            }
                            return true;
                        },
                    )
                    .forEach((s) => this.addSuggestion(new BehaviorSubject(s)));
            } else {
                const values: DisplayValue[] = this.chipsControl.value;
                suggestions
                    ?.filter((s) => s.type === 'AI' && s.status === 'ACCEPTED')
                    .forEach((s) => {
                        void this.remove(
                            values.find((v) => v.key === s.value),
                            false,
                        );
                        void this.updateSuggestionState(new BehaviorSubject(s), 'PENDING');
                    });
            }
        });

        this.registerValueChanges(this.chipsControl);
    }

    ngAfterViewInit(): void {
        // We mark all chips as selected for better screen-reader output. However, since selection
        // doesn't do anything, we disable toggling the selection.
        this.chips.changes
            .pipe(startWith(this.chips))
            .subscribe((chips: QueryList<MatChipOption>) =>
                chips.forEach((chip) => (chip.toggleSelected = () => true)),
            );
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
    focus() {
        this.openOverlay();
    }
    openOverlay(event?: FocusEvent): void {
        if (this.ignoreNextFocusEvent) {
            this.ignoreNextFocusEvent = false;
            return;
        }
        if (this.chipsControl.disabled) {
            return;
        }
        if (!event) {
            if (this.overlayIsVisible) {
                this.overlayIsVisible = false;
                this.changeDetectorRef.detectChanges();
                setTimeout(() => (document.activeElement as HTMLElement)?.blur());
                return;
            }
        }
        if (this.isTree && this.overlayIsVisible) {
            this.treeRef?.input?.nativeElement.focus();
            return;
        }
        this.overlayIsVisible = true;
        this.changeDetectorRef.detectChanges();
        if (this.isTree) {
            setTimeout(() => this.treeRef?.input?.nativeElement.focus());
        }
    }
    onPositionChange(event: ConnectedOverlayPositionChange) {
        this.positionY.set(event.connectionPair?.originY || null);
    }
    closeOverlay(event?: FocusEvent): void {
        // prevent directly closing because cdk outside click might trigger
        if (
            UIHelper.isParentElementOfElement(
                event?.target as HTMLElement,
                this.boxRef.nativeElement,
            )
        ) {
            return;
        }
        this.overlayIsVisible = false;
        this.positionY.set(null);
        if (!this.isTree) {
            //this.inputControl.setValue('');
            if (this.editorMode !== this.valueSelection) {
                this.ignoreNextFocusEvent = true;
            }
            this.inputControl.setValue('');
            this.inputElement?.nativeElement.focus();
        } else {
            this.openButtonRef.focus();
        }
        this.onBlur.emit();
    }

    onOverlayKeydown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.closeOverlay();
        } else {
            const wasHandledByTree = this.treeCoreComponent.handleKeydown(event.code);
            if (wasHandledByTree) {
                event.preventDefault();
            }
        }
    }

    async remove(toBeRemoved: DisplayValue, removeSuggestion = true): Promise<void> {
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
        if (removeSuggestion) {
            const suggestion = await firstValueFrom(this.isSuggestion(toBeRemoved));
            if (suggestion) {
                this.removeSuggestion(new BehaviorSubject(suggestion));
            }
        }
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
        });
    }

    onValuesChange(values: DisplayValue[]): void {
        this.chipsControl.setValue([...values]);
        this.changeDetectorRef.detectChanges();
    }

    onBlurInput(event: FocusEvent) {
        if (event.relatedTarget === this.treeRef?.input?.nativeElement) {
            return;
        }
        if (event.target === this.inputElement?.nativeElement) {
            return;
        }
        if (
            !UIHelper.isParentElementOfElement(
                event.target as HTMLElement,
                this.container.nativeElement.nativeElement,
            )
        ) {
            return;
        }
        this.onBlur.emit();
    }
    public static mapGraphqlId(definition: MdsWidget) {
        // attach the "RangedValue" graphql Attributes
        return MdsEditorWidgetBase.attachGraphqlSelection(definition, ['id', 'value']);
    }

    protected readonly ValueType = ValueType;

    addSelectedTreeNode() {
        if (this.treeRef?.selectedNode) {
            this.treeRef.toggleNode(this.treeRef.selectedNode, true, true, true);
            this.inputElement?.nativeElement.focus();
        } else {
            const selected = this.treeRef?.findNodeByKeyOrCaption(this.inputControl.value);
            if (selected) {
                this.treeRef?.toggleNode(selected, true, true, true);
            } else if (
                [MdsWidgetType.MultiValueSuggestBadges, MdsWidgetType.MultiValueBadges].includes(
                    this.widget.definition.type as MdsWidgetType,
                )
            ) {
                this.add({
                    key: this.inputControl.value,
                    label: this.inputControl.value,
                });
                this.inputControl.reset();
            }
        }
    }

    isSuggestion(value: DisplayValue | TreeNode) {
        return this.widget
            .getSuggestions()
            .pipe(
                map((suggestions) =>
                    suggestions?.find(
                        (s) =>
                            s.value === ((value as DisplayValue).key || (value as TreeNode).id) &&
                            s.status === 'ACCEPTED',
                    ),
                ),
            );
    }

    reorderChip(event: CdkDragDrop<any>) {
        const currentValue = [...this.chipsControl.value];
        moveItemInArray(currentValue, event.previousIndex, event.currentIndex);
        this.chipsControl.setValue(currentValue);
    }

    async acceptUserProposalGroup(group: UserProposalGroup): Promise<void> {
        const values: DisplayValue[] = this.chipsControl.value;
        if (!values.some((v) => v.key === group.displayValue.key)) {
            this.add(group.displayValue);
        }
        await this.applyUserProposalGroupStatus(group, 'ACCEPTED');
    }

    async declineUserProposalGroup(group: UserProposalGroup): Promise<void> {
        await this.applyUserProposalGroupStatus(group, 'DECLINED');
    }

    private async applyUserProposalGroupStatus(
        group: UserProposalGroup,
        status: 'ACCEPTED' | 'DECLINED',
    ): Promise<void> {
        const allSuggestions = await firstValueFrom(this.widget.getSuggestions());
        for (const s of group.suggestions) {
            this.widget.setSuggestionState(new BehaviorSubject(s), status);
        }
        this.mdsEditorInstance.updateHasChanges();
        this.widget.setSuggestions(allSuggestions);
        setTimeout(() => console.log(this.chipsControl.value), 100);
    }
}
