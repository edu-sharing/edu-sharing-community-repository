import { animate, sequence, style, transition, trigger } from '@angular/animations';
import { SelectionModel } from '@angular/cdk/collections';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    QueryList,
    Renderer2,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    ViewChildren,
    ViewContainerRef,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { NetworkService } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
    ListEventInterface,
    ListOptions,
    ListOptionsConfig,
    NodeEntriesDisplayType,
} from 'src/app/features/node-entries/entries-model';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import {
    ConfigurationService,
    DialogButton,
    EventListener,
    FrameEventsService,
    ListItem,
    NetworkRepositories,
    Node,
    Repository,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestLocatorService,
    RestNetworkService,
    SessionStorageService,
    TemporaryStorageService,
    UIService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { ColorHelper, PreferredColor } from '../../../core-module/ui/color-helper';
import { KeyEvents } from '../../../core-module/ui/key-events';
import { UIAnimation } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';
import { UIConstants } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-constants';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { ActionbarComponent } from '../../../shared/components/actionbar/actionbar.component';
import { NodeUrlComponent } from '../../../../../projects/edu-sharing-ui/src/lib/node-url/node-url.component';
import { NodeTitlePipe } from '../../../../../projects/edu-sharing-ui/src/lib/pipes/node-title.pipe';
import { DistinctClickEvent } from '../../directives/distinct-click.directive';
import {
    DragData,
    DropData,
} from '../../../../../projects/edu-sharing-ui/src/lib/directives/drag-nodes/drag-nodes';
import { NodeHelperService } from '../../node-helper.service';
import { CustomOptions, OptionItem, Scope, Target } from '../../option-item';
import { OptionsHelperService } from '../../options-helper.service';
import { Toast } from '../../toast';

@Component({
    selector: 'es-listTable',
    templateUrl: 'list-table.component.html',
    styleUrls: ['list-table.component.scss'],
    providers: [OptionsHelperService],
    animations: [
        trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger(
            'openOverlayBottom',
            UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST),
        ),
        trigger('orderAnimation', [
            transition(':enter', [
                sequence([
                    animate(UIAnimation.ANIMATION_TIME_SLOW + 'ms ease', style({ opacity: 0 })),
                ]),
            ]),
            transition(':leave', [
                sequence([
                    animate(UIAnimation.ANIMATION_TIME_SLOW + 'ms ease', style({ opacity: 1 })),
                ]),
            ]),
        ]),
    ],
    // Causes action menu not to align properly
    changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * A provider to render multiple Nodes as a list
 */
export class ListTableComponent
    implements OnChanges, OnDestroy, AfterViewInit, EventListener, ListEventInterface<Node>
{
    public static readonly VIEW_TYPE_LIST = 0;
    public static readonly VIEW_TYPE_GRID = 1;
    public static readonly VIEW_TYPE_GRID_SMALL = 2;

    @ViewChild('drag') drag: ElementRef;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;
    @ViewChild('dropdown') dropdownElement: ElementRef;
    @ViewChild('sortDropdownMenuTrigger') sortDropdownMenuTrigger: ElementRef<HTMLButtonElement>;
    @ViewChildren('childList') childList: QueryList<ElementRef>;

    @ContentChild('itemContent') itemContentRef: TemplateRef<any>;

    /** Set the current list of nodes to render */
    @Input() set nodes(nodes: Node[]) {
        // remove all non-virtual nodes which are replaced by the virtual nodes (virtual have higher prio)
        // also validate that this is only enabled for regular nodes
        if (nodes && nodes.length && nodes[0].ref && nodes[0].ref.id) {
            const virtual = nodes.filter((n) => n.virtual);
            nodes = nodes.filter((n) => n.virtual || !virtual.find((v) => v.ref.id === n.ref.id));
        }
        if (this._nodes?.length && nodes?.length > this._nodes?.length) {
            const pos = this._nodes.length;
            setTimeout(() => {
                // handle focus
                (this.childList.toArray()[pos] as any as NodeUrlComponent).focus();
            });
        }
        this._nodes = nodes;
        this.refreshAvailableOptions();
    }

    /**
     * Should the nodes be automatically linked via href.
     *
     * This is important for crawlers
     * Activate it for any kind of nodes list which is supposed to be clickable
     * Please note that when enabled, any click / dblclick callbacks won't have any effect!
     */
    @Input() createLink = false;

    /**
     * Info about the current parent node
     * May be empty if it does not exists
     */
    @Input() parent: Node | any;

    @Input() set columns(columns: ListItem[]) {
        this.columnsOriginal = Helper.deepCopy(columns);
        this.columnsAll = columns;
        this.columnsVisible = [];
        for (const col of columns) {
            if (col.visible) {
                this.columnsVisible.push(col);
            }
        }
        this.changes.detectChanges();
    }

    _customOptions: CustomOptions;
    /**
     * Set additional action options. They will be added as customOptions to the options-service
     */
    @Input() set customOptions(customOptions: CustomOptions) {
        this._customOptions = customOptions;
        this.refreshAvailableOptions();
    }

    /**
     * all options displayed for the current dropdown
     */
    dropdownOptions: OptionItem[];

    _options: OptionItem[];
    set options(options: OptionItem[]) {
        this._options = options;
    }

    /**
     * Shall an icon be shown?
     */
    @Input() hasIcon: boolean;

    /**
     * Total item count, when used, the header of the table will display it.
     */
    @Input() totalCount: number;

    /**
     * Is it possible to load more items? (Otherwise, the icon to load more is hidden.)
     */
    @Input() hasMore: boolean;

    /**
     * Use this material icon. (Only applicable if it's not a node but a group or user.)
     */
    @Input() icon: string;

    @Input() set hasCheckbox(hasCheckbox: boolean) {
        this._hasCheckbox = hasCheckbox;
        if (!hasCheckbox && this.selectedNodes && this.selectedNodes.length > 1) {
            // use a timeout to prevent a ExpressionChangedAfterItHasBeenCheckedError in the parent component
            setTimeout(() => {
                this.selectedNodes = [];
                this.refreshAvailableOptions();
                this.selectionChanged.emit([]);
            });
        }
    }
    get hasCheckbox(): boolean {
        return this._hasCheckbox;
    }

    /**
     * Is a heading in table mode shown (when disabled, no sorting possible)?
     */
    @Input() hasHeading = true;

    /**
     * Can an individual item be clicked?
     */

    @Input() isClickable: boolean;

    /**
     * A custom function to validate if a given node has permissions or should be displayed as "disabled".
     */
    @Input() validatePermissions: (node: Node) => {
        status: boolean;
        message?: string;
        button?: {
            click: Function;
            caption: string;
            icon: string;
        };
    };

    /**
     * Hint that the "apply node" mode is active (when reurl is used).
     */
    @Input() applyMode = false;

    /**
     * Mark / Select the row when clicking on it, isClickable must be set to true in this case.
     */
    @Input() selectOnClick = false;

    /**
     * Hint that the parent is currently fetching nodes.
     *
     * In this case the view will show a loading information.
     */
    @Input() isLoading: boolean;

    /**
     * The View Type, either VIEW_TYPE_LIST, VIEW_TYPE_GRID_SMALL or VIEW_TYPE_GRID.
     *
     * Can be changed on the fly.
     */
    @Input() viewType = ListTableComponent.VIEW_TYPE_LIST;

    /**
     * link to the actionbar component that is in use
     */
    @Input() actionbar: ActionbarComponent;

    /**
     * The current scope this list table is supposed to serve
     */
    @Input() scope: Scope;
    /**
     * Are drag and drop events allowed?
     */
    @Input() dragDrop = false;

    /**
     * Can the content be re-ordered via drag and drop? (Requires dragDrop to be enabled)
     *
     * `onOrderElements` will be emitted containing the new array of items as they are sorted.
     */
    @Input() orderElements = false;

    /**
     * May changes when the user starts ordering elements.
     *
     * Disable it to stop the order animation.
     */
    @Input() orderElementsActive = false;

    /**
     * Is reordering of columns via settings menu allowed?
     */
    @Input() reorderColumns = false;

    /**
     * Info to the component which nodes should currently be selected.
     */
    @Input() selectedNodes: Node[] = [];

    /**
     * Select the property to sort the list by.
     *
     * Must be a name included in your columns.
     */
    @Input() sortBy: string;

    /**
     * Sort ascending or descending.
     */
    @Input() sortAscending = true;

    /**
     * Set the allowed list of possible sort by fields.
     */
    @Input() possibleSortByFields = RestConstants.POSSIBLE_SORT_BY_FIELDS;

    /**
     * Show the sort by dialog when sort is triggered in mobile view.
     */
    @Input() sortByMobile = true;

    /**
     * For Infinite Scroll, when false, also does reload when scrolling inside a div.
     */
    @Input() scrollWindow = true;

    /**
     * For global css styling, the css class name.
     */
    @Input() listClass = 'list';

    /**
     * Should the list table fetch the repo list? (Useful to detect remote repo nodes)
     *
     * Must be set at initial loading!
     */
    @Input() loadRepositories = true;

    /**
     * Prevent key events. (Like when the parent has open windows)
     */
    @Input() preventKeyevents = false;

    /**
     * For collection elements only, tells if the current user can delete the given item.
     */
    @Input() canDelete: (node: Node | any) => boolean;

    /**
     * control the visibility of the reorder dialog (two-way binding)
     */
    @Input() reorderDialog = false;

    /**
     * Can an element be dropped on the element?
     *
     * Called with same parameters as onDrop event.
     */
    @Input() canDrop: (arg0: DropData) => boolean = () => true;

    @Input() optionItems: OptionItem[];

    @Output() reorderDialogChange = new EventEmitter<boolean>();

    @Output() nodesChange = new EventEmitter();

    @Output() orderElementsActiveChange = new EventEmitter();

    /**
     * Called when the user scrolled the list and it should load more data.
     */
    @Output() loadMore = new EventEmitter();

    /**
     * Called when the user changed sort order.
     */
    @Output() sortListener = new EventEmitter<{
        /** Property */
        sortBy: string;
        sortAscending: boolean;
    }>();

    /**
     * Called when the user clicks on a row.
     */
    @Output() clickRow = new EventEmitter<{
        /** Clicked object from list, depends on what you filled in. */
        node: Node | any;
        /** Source click information, may null, e.g. 'preview', 'comments', 'dropdown' */
        source: string;
    }>();
    /**
     * Hint to the caller that he should invalidate / refresh the current content since it was probably modified
     */
    @Output() onRequestRefresh = new EventEmitter<void>();

    /**
     * Called when the user double clicks on a row.
     *
     * Emits an object from the list (usually a node, but depends how you filled it)
     */

    @Output() doubleClickRow = new EventEmitter();

    /**
     * Called when an open node event is received from the action menu
     */
    @Output() openNode = new EventEmitter();
    /**
     * Called when an explicit viewing event is received from the action menu
     */
    @Output() onViewNode = new EventEmitter();

    /**
     * Called when columns list must be updated (e.g. due to config change)
     */
    @Output() onInvalidateColumns = new EventEmitter();

    /**
     * Called when the selection has changed.
     *
     * Emits an array of objects from the list (usually nodes, but depends how you filled it)
     */
    @Output() selectionChanged = new EventEmitter();

    /**
     * Called when the user opens an overflow menu (right side of the node) and the parent
     * component should invalidate the options (may some are not allowed for this item).
     */
    @Output() onUpdateOptions = new EventEmitter();

    /**
     * Called when a drop event happened.
     */
    @Output() onDrop = new EventEmitter<{
        target: Node;
        source: Node[];
        event: any;
        type: 'move' | 'copy';
    }>();

    /**
     * Called when the user clicked the delete for a missing reference object.
     */
    @Output() onDelete = new EventEmitter();

    /**
     * Called when the user performed a custom order of items.
     */
    @Output() onOrderElements = new EventEmitter();

    currentDrag: string;
    currentDragCount = 0;
    dropdownLeft: string;
    dropdownTop: string;
    id: number;
    reorderButtons: DialogButton[];
    dragHover: Node;
    /**
     * Whether the user is currently dragging one or more nodes from this list.
     */
    isNodesDragSource = false;

    private _hasCheckbox: boolean;
    _nodes: any[];
    animateNode: Node;
    columnsAll: ListItem[];
    /** Set the columns to show, see @ListItem */
    private columnsOriginal: ListItem[];
    columnsVisible: ListItem[];
    currentDragColumn: ListItem;
    private repositories: Repository[];
    private destroyed = new Subject<void>();

    constructor(
        private ui: UIService,
        private translate: TranslateService,
        private changeDetectorRef: ChangeDetectorRef,
        private nodeHelper: NodeHelperService,
        private config: ConfigurationService,
        private changes: ChangeDetectorRef,
        private storage: TemporaryStorageService,
        private sessionStorage: SessionStorageService,
        private network: NetworkService,
        private connectors: RestConnectorsService,
        private locator: RestLocatorService,
        private sanitizer: DomSanitizer,
        private route: ActivatedRoute,
        private router: Router,
        private toast: Toast,
        private viewContainerRef: ViewContainerRef,
        private optionsHelper: OptionsHelperService,
        private bridge: BridgeService,
        private frame: FrameEventsService,
        private renderer: Renderer2,
        private mainnavService: MainNavService,
    ) {
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        this.nodeHelper.setViewContainerRef(this.viewContainerRef);
        this.reorderButtons = DialogButton.getSaveCancel(
            () => this.closeReorder(false),
            () => this.closeReorder(true),
        );
        this.reorderButtons.splice(
            0,
            0,
            new DialogButton('RESET', { color: 'standard', position: 'opposite' }, () => {
                this.sessionStorage.delete('workspaceColumns');
                this.onInvalidateColumns.emit();
            }),
        );
        this.id = Math.random();
        frame.addListener(this, this.destroyed);
        // wait for all bindings to finish
        setTimeout(() => {
            this.refreshAvailableOptions();
            this.loadRepos();
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        // Make sure viewType is a number
        if (changes.viewType && typeof changes.viewType.currentValue === 'string') {
            this.viewType = parseInt(changes.viewType.currentValue, 10);
        }
        if (changes.orderElementsActive) {
            this.clearSelection();
        }
    }

    ngAfterViewInit(): void {
        this.optionsHelper.initComponents(this.actionbar, this);
        this.optionsHelper.nodesDeleted
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => this.removeNodes(nodes.error, nodes.objects));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    setViewType(viewType: number) {
        this.viewType = viewType;
        // store in url for remembering layout
        const params: any = {};
        params[UIConstants.QUERY_PARAM_LIST_VIEW_TYPE] = this.viewType;
        this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: params,
            skipLocationChange: true,
        });
        this.changes.detectChanges();
    }

    loadRepos(): void {
        if (!this.loadRepositories) {
            return;
        }
        this.locator.setRoute(this.route).subscribe(() => {
            this.network.getRepositories().subscribe((repositories) => {
                this.repositories = repositories;
                this.changeDetectorRef.detectChanges();
            });
        });
    }

    onEvent(event: string, data: any): void {
        if (event === FrameEventsService.EVENT_PARENT_SCROLL) {
            this.scroll(false);
        }
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {
        if (
            event.code === 'KeyA' &&
            (event.ctrlKey || event.metaKey) &&
            !KeyEvents.eventFromInputField(event) &&
            !this.preventKeyevents
        ) {
            this.toggleAll();
            event.preventDefault();
            event.stopPropagation();
        }
    }

    delete(node: any): void {
        this.onDelete.emit(node);
    }

    isBrightColorCollection(color: string): boolean {
        if (!color) {
            return true;
        }
        return ColorHelper.getPreferredColor(color) === PreferredColor.White;
    }

    toggleAll(): void {
        if (this.selectedNodes?.length === this._nodes.length) {
            this.selectedNodes = [];
            this.refreshAvailableOptions();
            this.selectionChanged.emit(this.selectedNodes);
        } else {
            this.selectAll();
        }
    }

    getTitle(node: Node): string {
        return RestHelper.getTitle(node);
    }

    getCollectionColor(node: any) {
        return node.collection ? node.collection.color : null;
    }

    isHomeNode(node: any): boolean {
        // Repos not loaded or not availale. Assume true so that small images are loaded.
        if (!this.repositories) {
            return true;
        }
        return RestNetworkService.isFromHomeRepo(node, this.repositories);
    }

    getIconUrl(node: any) {
        return this.getReference(node).iconURL;
    }

    getBackgroundColor(node: Node): string | null {
        if (!this.isCollection(node)) {
            return null;
        } else if (
            this.viewType === ListTableComponent.VIEW_TYPE_GRID_SMALL &&
            node.preview &&
            !node.preview.isIcon
        ) {
            return '#000';
        } else {
            return this.getCollectionColor(node);
        }
    }

    getIsDarkColor(node: Node): boolean {
        if (!this.isCollection(node)) {
            return false;
        } else if (
            this.viewType === ListTableComponent.VIEW_TYPE_GRID_SMALL &&
            // node.preview &&
            !node.preview?.isIcon
        ) {
            return false;
        } else {
            return this.isBrightColorCollection(this.getCollectionColor(node));
        }
    }

    isCollection(node: any): boolean {
        return this.nodeHelper.isNodeCollection(node);
    }

    isReference(node: Node): boolean {
        return node.aspects && node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    isDeleted(node: any): boolean {
        return (
            this.isReference(node) &&
            node.aspects.indexOf(RestConstants.CCM_ASPECT_REMOTEREPOSITORY) === -1 &&
            !node.originalId
        );
    }

    askCCPublish(event: any, node: Node): void {
        this.nodeHelper.askCCPublish(node);
        event.stopPropagation();
    }

    getItemCssClass(item: ListItem): string {
        let css =
            item.type.toLowerCase() +
            '_' +
            item.name.toLowerCase().replace(':', '_').replace('.', '_') +
            ' type_' +
            item.type.toLowerCase();
        if (item.config?.showLabel) {
            css += ' item_with_label';
        }
        return css;
    }

    handleKeyboard(event: any): void {
        // Will break ng menu. May add a check whether menu is open
        // if (this.viewType == ListTableComponent.VIEW_TYPE_LIST && (event.key == "ArrowUp" || event.key == "ArrowDown")) {
        //     let next = event.key == "ArrowDown";
        //     let elements: any = document.getElementsByClassName("node-row");
        //     for (let i = 0; i < elements.length; i++) {
        //         let element = elements.item(i);
        //         if (element == event.srcElement) {
        //             if (next && i < elements.length - 1)
        //                 elements.item(i + 1).focus();
        //             if (!next && i > 0) {
        //                 elements.item(i - 1).focus();
        //             }
        //         }
        //     }
        //     event.preventDefault();
        //     event.stopPropagation();
        // }
    }

    /**
     * Called before a drag operation is executed.
     *
     * @param node - The node on which the drag operation was started.
     */
    onNodesDragStart(event: DragEvent, node: Node) {
        this.addToSelectedNodes(node);

        let name = '';
        for (const node of this.selectedNodes) {
            if (name) {
                name += ', ';
            }
            name += RestHelper.getName(node);
        }
        this.currentDrag = name;
        this.currentDragCount = this.selectedNodes.length ? this.selectedNodes.length : 1;
        try {
            event.dataTransfer.setDragImage(this.drag.nativeElement, 100, 20);
        } catch (e) {
            // Do nothing.
        }
        this.isNodesDragSource = true;
    }

    canDropNodes(target: Node, { event, nodes, dropAction }: DragData): boolean {
        if (this.orderElements && this.isNodesDragSource && this.selectedNodes.length === 1) {
            return true;
        }
        return this.canDrop({ event, nodes, dropAction, target });
    }

    onNodesDragEnter(target: Node) {
        if (
            this.orderElements &&
            this.isNodesDragSource &&
            this.selectedNodes.length === 1 &&
            this.selectedNodes[0].ref.id !== target.ref.id
        ) {
            this.orderElementsActive = true;
            this.orderElementsActiveChange.emit(true);
            const targetPos = this._nodes.indexOf(target);
            this.moveNode(this.selectedNodes[0], targetPos);
            // Inform the outer component's variable about the new order
            this.nodesChange.emit(this._nodes);
        }
    }

    onNodesHoveringChange(nodesHovering: boolean, target: Node) {
        if (nodesHovering) {
            this.dragHover = target;
        } else {
            // The enter event of another node might have fired before this leave
            // event and already updated `dragHover`. Only set it to null if that is
            // not the case.
            if (this.dragHover === target) {
                this.dragHover = null;
            }
        }
    }

    onNodesDragEnd() {
        this.isNodesDragSource = false;
    }

    onNodesDrop({ event, nodes, dropAction }: DragData, target: Node) {
        if (this.orderElementsActive) {
            return;
        }
        if (dropAction === 'link') {
            throw new Error('dropAction "link" is not allowed');
        }
        if (this.isNodesDragSource) {
            this.onOrderElements.emit(this._nodes);
        }
        this.onDrop.emit({ target, source: nodes, event, type: dropAction });
    }

    onDistinctClick(event: DistinctClickEvent, node: Node, region?: string) {
        // in link mode, we will not emit any events
        if (this.createLink) {
            return;
        }
        if (this.orderElementsActive) {
            return;
        }
        if (!this.isClickable) {
            // Do nothing
            return;
        }
        if (event.pointerType === 'touch' || event.pointerType === 'pen') {
            this.doubleClickRow.emit(node);
        } else if (!this.selectOnClick) {
            // Propagate event
            this.clickRowSender(node, region);
            this.refreshAvailableOptions(node);
        } else if (this.hasCheckbox && event.event.ctrlKey) {
            this.toggleSelection(node);
        } else if (this.hasCheckbox && event.event.shiftKey && this.selectedNodes.length > 0) {
            // Select from-to range via shift key.
            this.expandNodeSelection(node);
        } else {
            // Single value select
            const clickedNodeWasSelected = this.getSelectedPos(node) !== -1;
            if (this.selectedNodes.length === 1 && clickedNodeWasSelected) {
                this.clearSelection();
            } else {
                this.setSelectionToSingleNode(node);
            }
            if (this.mainnavService.getDialogs().nodeSidebar) {
                this.mainnavService.getDialogs().nodeSidebar = node;
            }
        }
        event.event.stopPropagation();
    }

    onCheckboxClick(node: Node) {
        if (this.ui.shiftKeyPressed && this.selectedNodes.length > 0 && !this.isSelected(node)) {
            this.expandNodeSelection(node);
        } else {
            this.toggleSelection(node);
        }
    }

    private addToSelectedNodes(node: Node) {
        if (this.getSelectedPos(node) >= 0) {
            // The node under the cursor is already part of the selection.
            return;
        }
        if (this.hasCheckbox) {
            // Nodes are selectable, but the current node is not yet part of the selection.
            this.selectedNodes.push(node);
        } else {
            // Multi-node selection is not supported by this list.
            this.selectedNodes = [node];
        }
        this.selectionChanged.emit(this.selectedNodes);
    }

    private filterCallbacks(options: OptionItem[], node: Node): OptionItem[] {
        return options.filter((option) => !option.showCallback || option.showCallback(node));
    }

    private selectAll(): void {
        this.selectedNodes = [];
        for (const node of this._nodes) {
            this.selectedNodes.push(node);
        }
        this.refreshAvailableOptions();
        this.selectionChanged.emit(this.selectedNodes);
    }

    private moveNode(node: Node, targetPos: number): void {
        const sourcePos = this._nodes.indexOf(node);
        if (sourcePos < 0) {
            throw new Error('Cannot move node: node not in nodes list');
        }
        this._nodes.splice(sourcePos, 1);
        this._nodes.splice(targetPos, 0, node);
    }

    noPermissions(node: Node): boolean {
        return this.validatePermissions != null && this.validatePermissions(node).status === false;
    }

    closeReorder(save: boolean): void {
        this.reorderDialog = false;
        this.reorderDialogChange.emit(false);
        if (save) {
            this.columns = this.columnsAll;
            this.sessionStorage.set('workspaceColumns', this.columnsAll);
        } else {
            this.columns = this.columnsOriginal;
        }
    }

    allowDragColumn(event: any, index: number, target: ListItem): void {
        if (!this.reorderColumns || index === 0 || !this.currentDragColumn) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        if (this.currentDragColumn === target) {
            return;
        }
        const posOld = this.columnsAll.indexOf(this.currentDragColumn);
        const posNew = this.columnsAll.indexOf(target);
        const old = this.columnsAll[posOld];
        this.columnsAll[posOld] = this.columnsAll[posNew];
        this.columnsAll[posNew] = old;
    }

    dropColumn(event: any, index: number, target: ListItem): void {
        if (!this.reorderColumns || index === 0) {
            return;
        }
        this.currentDragColumn = null;
        event.preventDefault();
        event.stopPropagation();
    }

    allowDeleteColumn(event: any): void {
        if (!this.reorderColumns || !this.currentDragColumn) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
    }

    deleteColumn(event: any): void {
        if (!this.currentDragColumn) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        this.columnsAll[this.columnsAll.indexOf(this.currentDragColumn)].visible = false;
        this.columns = this.columnsAll;
        this.currentDragColumn = null;
    }

    animateIcon(node: Node, animate: boolean): void {
        if (animate) {
            if (this.nodeHelper.hasAnimatedPreview(node)) {
                this.animateNode = node;
            }
        } else {
            this.animateNode = null;
        }
    }

    dragStartColumn(event: any, index: number, column: ListItem): void {
        if (!this.allowDragColumn || index === 0) {
            return;
        }
        event.dataTransfer.setData('text', index);
        event.dataTransfer.effectAllowed = 'all';
        this.currentDragColumn = column;
    }

    private clickRowSender(node: Node, source: string): void {
        this.clickRow.emit({ node, source });
    }

    canBeSorted(sortBy: any): boolean {
        return (
            this.possibleSortByFields &&
            this.possibleSortByFields.filter((p) => p.name === sortBy.name).length > 0
        );
    }

    getSortableColumns(): ListItem[] {
        const result: ListItem[] = [];
        if (!this.columnsAll) {
            return result;
        }
        for (const col of this.columnsAll) {
            if (this.canBeSorted(col)) {
                result.push(col);
            }
        }
        return result;
    }

    setSortingIntern(sortBy: ListItem, isPrimaryElement: boolean, target: HTMLElement): void {
        if (
            isPrimaryElement &&
            window.innerWidth < UIConstants.MOBILE_WIDTH + UIConstants.MOBILE_STAGE * 4
        ) {
            if (this.sortByMobile) {
                this.triggerSortDropdownMenu(target);
            }
            return;
        }
        let ascending = this.sortAscending;
        if (this.sortBy === sortBy.name) {
            ascending = !ascending;
        }
        (sortBy as any).ascending = ascending;
        this.setSorting(sortBy);
    }

    private triggerSortDropdownMenu(target: HTMLElement): void {
        const targetRect = target.getClientRects()[0];
        const menuTriggerStyle = {
            position: 'fixed',
            top: targetRect.top + 'px',
            left: targetRect.left + 'px',
            width: targetRect.width + 'px',
            height: targetRect.height + 'px',
        };
        for (const [key, value] of Object.entries(menuTriggerStyle)) {
            this.renderer.setStyle(this.sortDropdownMenuTrigger.nativeElement, key, value);
        }
        this.sortDropdownMenuTrigger.nativeElement.click();
    }

    setSorting(sortBy: any): void {
        if (!this.canBeSorted(sortBy)) {
            return;
        }
        this.sortListener.emit({
            sortBy: sortBy.name,
            sortAscending: sortBy.ascending,
        });
    }

    callOption(option: OptionItem, node: Node): void {
        if (!this.optionIsValid(option, node)) {
            if (option.disabledCallback) {
                option.disabledCallback(node);
            }
            return;
        }
        option.callback(node);
    }

    public scroll(fromUser: boolean): void {
        if (!fromUser) {
            // check if there is a footer
            const elements = document.getElementsByTagName('footer');
            if (elements.length && elements.item(0).innerHTML.trim()) {
                return;
            }
        }
        this.loadMore.emit();
    }

    contextMenu(event: any, node: Node): void {
        event.preventDefault();
        event.stopPropagation();

        if (!this._options || this._options.length < 2) {
            return;
        }
        this.dropdownLeft = event.clientX + 'px';
        this.dropdownTop = event.clientY + 'px';
        let nodes = [node];
        if (this.getSelectedPos(node) !== -1) {
            nodes = this.selectedNodes;
        } else {
            this.setSelectionToSingleNode(node);
        }
        this.showDropdown(true);
    }

    getReference(node: any) {
        return node.reference ? node.reference : node;
    }

    showDropdown(openMenu = false, event: any = null): void {
        if (openMenu) {
            if (event) {
                if (event.clientX + event.clientY) {
                    this.dropdownLeft = event.clientX + 'px';
                    this.dropdownTop = event.clientY + 'px';
                } else {
                    const rect = event.srcElement.getBoundingClientRect();
                    this.dropdownLeft = rect.left + rect.width / 2 + 'px';
                    this.dropdownTop = rect.top + rect.height / 2 + 'px';
                }
            }
        }
        // Short delay to let onUpdateOptions handler run and angular menu get the correct data from start.
        this.changeDetectorRef.detectChanges();
        this.menuTrigger.openMenu();
    }

    doubleClick(node: Node): void {
        // click events are not supported if createLink = true
        if (this.createLink) {
            return;
        }
        this.doubleClickRow.emit(node);
    }

    private isSelected(node: Node) {
        return this.getSelectedPos(node) !== -1;
    }

    private toggleSelection(node: Node) {
        if (this.isSelected(node)) {
            this.unselectNode(node);
        } else {
            this.addNodeToSelection(node);
        }
    }

    private addNodeToSelection(node: Node) {
        this.selectedNodes.push(node);
        this.onSelectionChanged(node);
    }

    private unselectNode(node: Node) {
        const pos = this.getSelectedPos(node);
        this.selectedNodes.splice(pos, 1);
        this.onSelectionChanged(node);
    }

    setSelectionToSingleNode(node: Node) {
        this.selectedNodes = [node];
        this.onSelectionChanged(node);
    }

    private clearSelection() {
        this.selectedNodes = [];
        this.onSelectionChanged();
    }

    private expandNodeSelection(to: Node) {
        const from = this.selectedNodes[this.selectedNodes.length - 1];
        const pos1 = RestHelper.getRestObjectPositionInArray(from, this._nodes);
        const pos2 = RestHelper.getRestObjectPositionInArray(to, this._nodes);
        const start = pos1 < pos2 ? pos1 : pos2;
        const end = pos1 < pos2 ? pos2 : pos1;
        for (let i = start; i <= end; i++) {
            if (this.getSelectedPos(this._nodes[i]) === -1) {
                this.selectedNodes.push(this._nodes[i]);
            }
        }
        this.onSelectionChanged();
    }

    private onSelectionChanged(activeNode?: Node) {
        this.refreshAvailableOptions(activeNode);
        this.selectionChanged.emit(this.selectedNodes);
        // This was done under some conditions before. Comment back in when
        // facing problems.
        //
        // this.changes.detectChanges();
    }

    getLRMIAttribute(data: any, item: ListItem): string {
        return this.nodeHelper.getLRMIAttribute(data, item);
    }

    getLRMIProperty(data: any, item: ListItem): string {
        return this.nodeHelper.getLRMIProperty(data, item);
    }

    getSelectedPos(selected: Node): number {
        if (!this.selectedNodes || !this.selectedNodes.length) {
            return -1;
        }
        return RestHelper.getRestObjectPositionInArray(selected, this.selectedNodes);
    }

    optionIsValid(optionItem: OptionItem, node: Node): boolean {
        if (optionItem.enabledCallback) {
            return optionItem.enabledCallback(node);
        }
        return optionItem.isEnabled;
    }

    optionIsShown(optionItem: OptionItem, node: Node): boolean {
        if (optionItem.showCallback) {
            return optionItem.showCallback(node);
        }
        return true;
    }

    public refreshAvailableOptions(node: Node = null) {
        this.optionsHelper.setData({
            scope: this.scope,
            activeObjects: this.selectedNodes,
            selectedObjects: this.orderElementsActive ? [] : this.selectedNodes,
            allObjects: this._nodes,
            parent: this.parent,
            customOptions: this._customOptions,
        });
        // only refresh global if no node was given
        this.optionsHelper.refreshComponents();
    }

    removeNodes(error: boolean, objects: Node[] | any[]) {
        if (error) {
            return;
        }
        for (const object of objects) {
            const p = RestHelper.getRestObjectPositionInArray(object, this._nodes);
            if (p !== -1) {
                this._nodes.splice(p, 1);
            }
        }
        this.selectedNodes = [];
        this.selectionChanged.emit([]);
        this.nodesChange.emit(this._nodes);
        this.refreshAvailableOptions();
    }
    replaceNodes(newObjects: Node[], localArray: Node[]) {
        newObjects?.forEach((o: any) => {
            const index = localArray.findIndex((n) => n.ref.id === o.ref.id);
            if (index === -1) {
                console.warn(
                    'tried to update node which not exist inside the list -> falling back to full reload',
                    o,
                );
                this.onRequestRefresh.emit();
                return;
            }
            localArray.splice(index, 1, o);
        });
    }
    updateNodes(objects: Node[] | any) {
        this.replaceNodes(objects, this._nodes);
        this.replaceNodes(objects, this.selectedNodes ?? []);
        this.nodesChange.emit(this._nodes);
    }
    addVirtualNodes(objects: Node[]) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        // remove the elements which will get added so they will replace the current state
        this._nodes = this._nodes.filter(
            (n) => objects.find((o: Node) => o.ref.id === n.ref.id) == null,
        );
        this._nodes = objects.concat(this._nodes);
        this.nodesChange.emit(this._nodes);
        this.selectedNodes = objects;
        this.selectionChanged.emit(objects);
        this.refreshAvailableOptions();
        console.log(objects);
    }

    showReorder() {
        this.reorderDialog = true;
        this.changes.detectChanges();
    }

    isSavedSearch(node: Node) {
        return node?.type === RestConstants.CCM_TYPE_SAVED_SEARCH;
    }

    getPreviewSrc(node: any) {
        if (!this.isCollection(node) || !node.preview.isIcon) {
            if (node.preview.data) {
                return this.sanitizer.bypassSecurityTrustResourceUrl(
                    'data:' + node.preview.mimetype + ';base64,' + node.preview.data,
                );
            } else {
                return (
                    node.preview.url +
                    ((this.isHomeNode(node) ||
                        RestNetworkService.getRepository(node)?.repositoryType ===
                            RestConstants.REPOSITORY_TYPE_ALFRESCO) &&
                    this.animateNode !== node
                        ? '&crop=true&maxWidth=300&maxHeight=300'
                        : '')
                );
            }
        }
        return null;
    }

    getOptionsAlways() {
        return this._options?.filter((o) => o.showAlways);
    }

    getPrimaryTitle(node: Node) {
        if (
            [RestConstants.CM_PROP_TITLE, RestConstants.LOM_PROP_TITLE].indexOf(
                this.columnsVisible[0]?.name,
            ) !== -1
        ) {
            return new NodeTitlePipe(this.translate).transform(node);
        }
        return node.name;
    }

    getRowId(node: Node | any, rowIndex: number): string {
        return `list-table-node-${node.ref?.id || node.authorityName}-row-${rowIndex + 1}`;
    }

    getDescribedBy(node: Node): string {
        return this.columnsVisible
            .map((_, index) => this.getRowId(node, index))
            .filter((_, index) => index > 0)
            .join(' ');
    }

    /*
        from list event interface
     */
    getDisplayType(): NodeEntriesDisplayType {
        return this.viewType;
    }

    setDisplayType(displayType: NodeEntriesDisplayType): void {
        this.viewType = displayType;
    }

    getSelection(): SelectionModel<Node> {
        return null;
    }

    setOptions(options: ListOptions): void {
        this.options = options?.[Target.List];
        this.dropdownOptions = options?.[Target.ListDropdown];
    }

    showReorderColumnsDialog(): void {
        this.reorderDialog = true;
    }

    /**
     * @deprecated
     * config paramter will be ignored
     * Switch to new @NodeEntriesComponent
     */
    async initOptionsGenerator(config: ListOptionsConfig) {
        await this.optionsHelper.initComponents(this.actionbar, this);
        this.optionsHelper.refreshComponents();
    }

    getShownOptions(node: Node) {
        return this._options?.filter((o) => this.optionIsShown(o, node));
    }

    deleteNodes(objects: Node[]): void {
        this._nodes = this._nodes.filter((n) => !objects.includes(n));
        this.nodesChange.emit(this._nodes);
        this.selectedNodes = [];
        this.selectionChanged.emit([]);
        this.refreshAvailableOptions();
    }
}
