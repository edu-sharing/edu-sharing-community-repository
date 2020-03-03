import {
    animate,
    sequence,
    style,
    transition,
    trigger,
} from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChild,
    ElementRef,
    EventEmitter, HostListener,
    Input,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    ConfigurationService,
    DialogButton,
    EventListener,
    FrameEventsService,
    ListItem,
    NetworkRepositories,
    Node,
    Repository, RestConnectorsService,
    RestConstants,
    RestHelper,
    RestLocatorService,
    RestNetworkService,
    TemporaryStorageService,
    UIService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { ColorHelper } from '../../../core-module/ui/color-helper';
import { KeyEvents } from '../../../core-module/ui/key-events';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { AddElement } from '../../add-element';
import { NodeHelper } from '../../node-helper';
import {OptionItem, Scope} from '../../option-item';
import { Toast } from '../../toast';
import { UIHelper } from '../../ui-helper';
import {WorkspaceManagementDialogsComponent} from '../../../modules/management-dialogs/management-dialogs.component';
import {ActionbarComponent} from '../../../common/ui/actionbar/actionbar.component';
import {OptionsHelperService} from '../../../common/options-helper';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {MainNavComponent} from '../../../common/ui/main-nav/main-nav.component';
import {DragData, DropData} from '../../directives/drag-nodes/drag-nodes';

@Component({
    selector: 'listTable',
    templateUrl: 'list-table.component.html',
    styleUrls: ['list-table.component.scss'],
    providers: [ OptionsHelperService ],
    animations: [
        trigger(
            'openOverlay',
            UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST),
        ),
        trigger(
            'openOverlayBottom',
            UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST),
        ),
        trigger('orderAnimation', [
            transition(':enter', [
                sequence([
                    animate(
                        UIAnimation.ANIMATION_TIME_SLOW + 'ms ease',
                        style({ opacity: 0 }),
                    ),
                ]),
            ]),
            transition(':leave', [
                sequence([
                    animate(
                        UIAnimation.ANIMATION_TIME_SLOW + 'ms ease',
                        style({ opacity: 1 }),
                    ),
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
export class ListTableComponent implements EventListener {
    public static VIEW_TYPE_LIST = 0;
    public static VIEW_TYPE_GRID = 1;
    public static VIEW_TYPE_GRID_SMALL = 2;

    @ViewChild('addElementRef') addElementRef: ElementRef;
    @ViewChild('drag') drag: ElementRef;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;
    @ViewChild('dropdown') dropdownElement: ElementRef;
    @ViewChild('dropdownContainer')
    dropdownContainerElement: ElementRef;

    @ContentChild('itemContent') itemContentRef: TemplateRef<any>;

    /** Set the current list of nodes to render */
    @Input() set nodes(nodes: Node[]) {
        // remove all non-virtual nodes which are replaced by the virtual nodes (virtual have higher prio)
        // also validate that this is only enabled for regular nodes
        if (nodes && nodes.length && nodes[0].ref && nodes[0].ref.id) {
            const virtual = nodes.filter(n => n.virtual);
            nodes = nodes.filter(
                n => n.virtual || !virtual.find(v => v.ref.id === n.ref.id),
            );
        }
        this._nodes = nodes;
        this.refreshAvailableOptions();
    }

    /**
     * Should the nodes be automatically linked via href.
     *
     * This is important for crawlers
     * Activate it for any kind of nodes list which is supposed to be clickable
     */
    @Input() createLink = false;

    /**
     * Info about the current parent node
     * May be empty if it does not exists
     */
    @Input() parent: Node|any;

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

    _customOptions: OptionItem[];
    /**
     * Set additional action options. They will be added as customOptions to the options-service
     */
    @Input() set customOptions (customOptions: OptionItem[]){
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
        console.log(options);
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

    /**
     * Are checkboxes visible? (If disabled, only single select.)
     *
     * If not null, shows a "Add Element" option as a first element (used for collections)
     * The AddElement defines label, icon and other details
     * The event onAddElement will be called when the user selects this element
     */
    @Input() addElement: AddElement;

    @Input() set hasCheckbox(hasCheckbox: boolean) {
        this._hasCheckbox = hasCheckbox;
        if (!hasCheckbox && this.selectedNodes.length > 1) {
            // use a timeout to prevent a ExpressionChangedAfterItHasBeenCheckedError in the parent component
            setTimeout(() => {
                this.selectedNodes = [];
                this.refreshAvailableOptions();
                this.onSelectionChanged.emit([]);
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
    @Input() validatePermissions: (
        node: Node,
    ) => {
        status: boolean;
        message: string;
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
     * Link to the MainNavComponent
     * Required to refresh particular events when triggered, e.g. a node was bookmarked
     */
    @Input() mainNav: MainNavComponent;
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
    @Input() canDelete: (node: Node|any) => boolean;

    /**
     * control the visibility of the reorder dialog (two-way binding)
     */
    @Input() reorderDialog = false;
    @Output() reorderDialogChange = new EventEmitter<boolean>();
    /**
     * Can an element be dropped on the element?
     *
     * Called with same parameters as onDrop event.
     */
    @Input() canDrop: (arg: {
        source: Node[];
        target: Node;
        event: DragEvent;
    }) => boolean = () => true;

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
     * Called when the user double clicks on a row.
     *
     * Emits an object from the list (usually a node, but depends how you filled it)
     */

    @Output() doubleClickRow = new EventEmitter();

    /**
     * Called when an explicit viewing event is received from the action menu
     */
    @Output() openNode = new EventEmitter();

    /**
     * Called when the selection has changed.
     *
     * Emits an array of objects from the list (usually nodes, but depends how you filled it)
     */
    @Output() onSelectionChanged = new EventEmitter();

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
     * Called when the user changed the order of the columns.
     */
    @Output() columnsChanged = new EventEmitter<ListItem[]>();

    /**
     * Called when the user clicked the "addElement" item.
     */
    @Output() onAddElement = new EventEmitter();

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
    private _nodes: any[];
    private animateNode: Node;
    private columnsAll: ListItem[];
    /** Set the columns to show, see @ListItem */
    private columnsOriginal: ListItem[];
    private columnsVisible: ListItem[];
    private currentDragColumn: ListItem;
    private optionsAlways: OptionItem[] = [];
    private repositories: Repository[];
    private sortMenu = false;

    constructor(
        private ui: UIService,
        private translate: TranslateService,
        private cd: ChangeDetectorRef,
        private config: ConfigurationService,
        private changes: ChangeDetectorRef,
        private storage: TemporaryStorageService,
        private network: RestNetworkService,
        private connectors: RestConnectorsService,
        private locator: RestLocatorService,
        private route: ActivatedRoute,
        private router: Router,
        private toast: Toast,
        private optionsHelper: OptionsHelperService,
        private bridge: BridgeService,
        private frame: FrameEventsService,
        private sanitizer: DomSanitizer,
    ) {
        this.reorderButtons = DialogButton.getSaveCancel(
            () => this.closeReorder(false),
            () => this.closeReorder(true),
        );
        this.id = Math.random();
        frame.addListener(this);
        // wait for all bindings to finish
        setTimeout(() => {
            this.refreshAvailableOptions();
            this.loadRepos();
        });
    }
    setViewType(viewType: number){
        this.viewType = viewType;
        this.changes.detectChanges();
    }

    loadRepos(): void {
        if (!this.loadRepositories) {
            return;
        }
        this.locator.setRoute(this.route).subscribe(() => {
            this.locator.locateApi().subscribe(() => {
                this.network
                    .getRepositories()
                    .subscribe((data: NetworkRepositories) => {
                        this.repositories = data.repositories;
                        this.cd.detectChanges();
                    });
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
            (event.ctrlKey || this.ui.isAppleCmd()) &&
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
        return (
            ColorHelper.getColorBrightness(color) >
            ColorHelper.BRIGHTNESS_THRESHOLD_COLLECTIONS
        );
    }

    toggleAll(): void {
        if (this.selectedNodes.length === this._nodes.length) {
            this.selectedNodes = [];
            this.refreshAvailableOptions();
            this.onSelectionChanged.emit(this.selectedNodes);
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

    isCollection(node: any): boolean {
        return NodeHelper.isNodeCollection(node);
    }

    isReference(node: Node): boolean {
        return  node.aspects && node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !==-1;
    }

    isDeleted(node: any): boolean {
        return this.isReference(node) && !node.originalId;
    }

    addElementClicked(): void {
        this.onAddElement.emit();
    }

    askCCPublish(event: any, node: Node): void {
        NodeHelper.askCCPublish(this.translate, node);
        event.stopPropagation();
    }

    getItemCssClass(item: ListItem): string {
        return (
            item.type.toLowerCase() +
            '_' +
            item.name
                .toLowerCase()
                .replace(':', '_')
                .replace('.', '_')
        );
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

    generateRoute(item: Node | any): string | null {
        if (this.createLink) {
            return UIHelper.createUrlToNode(this.router, item);
        }
        return null;
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
        this.currentDragCount = this.selectedNodes.length
            ? this.selectedNodes.length
            : 1;
        try {
            event.dataTransfer.setDragImage(this.drag.nativeElement, 100, 20);
        } catch (e) {
            // Do nothing.
        }
        this.isNodesDragSource = true;
    }

    canDropNodes(target: Node, { event, nodes }: DragData) {
        if (
            this.orderElements &&
            this.isNodesDragSource &&
            this.selectedNodes.length === 1
        ) {
            return true;
        }
        return this.canDrop({ source: nodes, target, event });
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

    onNodesDrop({ event, nodes, dropAction }: DropData, target: Node) {
        if (dropAction === 'link') {
            throw new Error('dropAction "link" is not allowed');
        }
        if (this.isNodesDragSource) {
            this.onOrderElements.emit(this._nodes);
        }
        this.onDrop.emit({ target, source: nodes, event, type: dropAction });
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
        this.onSelectionChanged.emit(this.selectedNodes);
    }

    private filterCallbacks(options: OptionItem[], node: Node): OptionItem[] {
        return options.filter(
            option => !option.showCallback || option.showCallback(node),
        );
    }

    private selectAll(): void {
        this.selectedNodes = [];
        for (const node of this._nodes) {
            this.selectedNodes.push(node);
        }
        this.refreshAvailableOptions();
        this.onSelectionChanged.emit(this.selectedNodes);
    }

    private moveNode(node: Node, targetPos: number): void {
        const sourcePos = this._nodes.indexOf(node);
        if (sourcePos < 0) {
            throw new Error('Cannot move node: node not in nodes list');
        }
        this._nodes.splice(sourcePos, 1);
        this._nodes.splice(targetPos, 0, node);
    }

    private noPermissions(node: Node): boolean {
        return (
            this.validatePermissions != null &&
            this.validatePermissions(node).status === false
        );
    }

    private closeReorder(save: boolean): void {
        this.reorderDialog = false;
        this.reorderDialogChange.emit(false);
        if (save) {
            this.columns = this.columnsAll;
            this.columnsChanged.emit(this.columnsAll);
        } else {
            this.columns = this.columnsOriginal;
        }
    }

    private allowDragColumn(event: any, index: number, target: ListItem): void {
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

    private dropColumn(event: any, index: number, target: ListItem): void {
        if (!this.reorderColumns || index === 0) {
            return;
        }
        this.currentDragColumn = null;
        event.preventDefault();
        event.stopPropagation();
    }

    private allowDeleteColumn(event: any): void {
        if (!this.reorderColumns || !this.currentDragColumn) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
    }

    private deleteColumn(event: any): void {
        if (!this.currentDragColumn) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        this.columnsAll[
            this.columnsAll.indexOf(this.currentDragColumn)
        ].visible = false;
        this.columns = this.columnsAll;
        this.currentDragColumn = null;
    }

    private animateIcon(node: Node, animate: boolean): void {
        if (animate) {
            if (NodeHelper.hasAnimatedPreview(node)) {
                this.animateNode = node;
            }
        } else {
            this.animateNode = null;
        }
    }

    private dragStartColumn(event: any, index: number, column: ListItem): void {
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

    private canBeSorted(sortBy: any): boolean {
        return (
            this.possibleSortByFields &&
            this.possibleSortByFields.filter(p => p.name === sortBy.name)
                .length > 0
        );
    }

    private getSortableColumns(): ListItem[] {
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

    private setSortingIntern(
        sortBy: ListItem,
        isPrimaryElement: boolean,
    ): void {
        if (
            isPrimaryElement &&
            window.innerWidth <
                UIConstants.MOBILE_WIDTH + UIConstants.MOBILE_STAGE * 4
        ) {
            if (this.sortByMobile) {
                this.sortMenu = true;
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

    private setSorting(sortBy: any): void {
        if (!this.canBeSorted(sortBy)) {
            return;
        }
        this.sortListener.emit({
            sortBy: sortBy.name,
            sortAscending: sortBy.ascending,
        });
    }

    private callOption(option: OptionItem, node: Node): void {
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
            console.log(elements);
            if (elements.length && elements.item(0).innerHTML.trim()) {
                return;
            }
        }
        this.loadMore.emit();
    }

    private contextMenu(event: any, node: Node): void {
        event.preventDefault();
        event.stopPropagation();

        if (!this._options || this._options.length < 2) {
            return;
        }
        this.dropdownLeft = event.clientX + 'px';
        this.dropdownTop = event.clientY + 'px';
        this.showDropdown(node, true);
    }

    private getReference(node: any) {
        return node.reference ? node.reference : node;
    }

    private showDropdown(
        node: Node,
        openMenu = false,
        event: any = null,
    ): void {
        this.select(node, 'dropdown', false, false);
        if (openMenu) {
            if (event) {
                console.log(event);
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
        setTimeout(() => this.menuTrigger.openMenu());
    }

    private doubleClick(node: Node): void {
        this.doubleClickRow.emit(node);
    }

    private select(
        node: Node,
        from: string = null,
        fireEvent = true,
        unselect = true,
    ): boolean {
        if (from !== 'checkbox' && !this.isClickable) {
            return false;
        }
        if (from !== 'checkbox' && !this.selectOnClick && fireEvent) {
            this.clickRowSender(node, from);
            this.refreshAvailableOptions(node);
            return false;
        }

        if (!this.hasCheckbox || from !== 'checkbox') {
            // Single value select
            if (
                this.selectedNodes.length &&
                this.selectedNodes[0] === node &&
                unselect
            ) {
                this.selectedNodes = [];
            } else {
                this.selectedNodes = [node];
            }
            if (this.mainNav && this.mainNav.management.nodeSidebar) {
                this.mainNav.management.nodeSidebar = node;
            }
            this.onSelectionChanged.emit(this.selectedNodes);
            this.refreshAvailableOptions(node);
            return false;
        }
        const pos = this.getSelectedPos(node);
        // Select from-to range via shift key.
        if (
            from === 'checkbox' &&
            pos === -1 &&
            this.ui.isShiftCmd() &&
            this.selectedNodes.length === 1
        ) {
            const pos1 = RestHelper.getRestObjectPositionInArray(
                node,
                this._nodes,
            );
            const pos2 = RestHelper.getRestObjectPositionInArray(
                this.selectedNodes[0],
                this._nodes,
            );
            const start = pos1 < pos2 ? pos1 : pos2;
            const end = pos1 < pos2 ? pos2 : pos1;
            for (let i = start; i <= end; i++) {
                if (this.getSelectedPos(this._nodes[i]) === -1) {
                    this.selectedNodes.push(this._nodes[i]);
                }
            }
        } else {
            if (pos !== -1 && unselect) {
                this.selectedNodes.splice(pos, 1);
            }
            if (pos === -1) {
                this.selectedNodes.push(node);
            }
        }
        this.refreshAvailableOptions(node);
        this.onSelectionChanged.emit(this.selectedNodes);
        this.changes.detectChanges();
        return false;
    }

    private getAttribute(data: any, item: ListItem): SafeHtml {
        const attribute = NodeHelper.getAttribute(
            this.translate,
            this.config,
            data,
            item,
        );
        // sanitizer is much slower but required when attributes inject styles, so keep it in these cases
        if (attribute != null && attribute.indexOf('style=') !== -1) {
            return this.sanitizer.bypassSecurityTrustHtml(attribute);
        }
        return attribute;
    }

    private getAttributeText(data: any, item: ListItem): string {
        return NodeHelper.getAttribute(this.translate, this.config, data, item);
    }

    private getLRMIAttribute(data: any, item: ListItem): string {
        return NodeHelper.getLRMIAttribute(
            this.translate,
            this.config,
            data,
            item,
        );
    }

    private getLRMIProperty(data: any, item: ListItem): string {
        return NodeHelper.getLRMIProperty(data, item);
    }

    private getSelectedPos(selected: Node): number {
        if (!this.selectedNodes || !this.selectedNodes.length) {
            return -1;
        }
        return RestHelper.getRestObjectPositionInArray(
            selected,
            this.selectedNodes,
        );
    }

    private optionIsValid(optionItem: OptionItem, node: Node): boolean {
        if (optionItem.enabledCallback) {
            return optionItem.enabledCallback(node);
        }
        return optionItem.isEnabled;
    }

    private optionIsShown(optionItem: OptionItem, node: Node): boolean {
        if (optionItem.showCallback) {
            return optionItem.showCallback(node);
        }
        return true;
    }

    private refreshAvailableOptions(node: Node = null) {
        console.log('refreshAvailableOptions', node, this._nodes);
        this.optionsHelper.setData({
            scope: this.scope,
            activeObject: node,
            selectedObjects: this.selectedNodes,
            allObjects: this._nodes,
            parent: this.parent,
            customOptions: this._customOptions
        });
        this.optionsHelper.setListener({
            onDelete: (nodes) => this.removeNodes(nodes.error, nodes.objects)
        });
        this.optionsHelper.initComponents(this.mainNav,  this.actionbar, this);
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
        this.onSelectionChanged.emit([]);
        this.nodesChange.emit(this._nodes);
        this.refreshAvailableOptions();
    }
    addVirtualNodes(objects: Node[]|any) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        console.log(objects);
        this._nodes = objects.concat(this._nodes);
        this.nodesChange.emit(this._nodes);
        this.selectedNodes = objects;
        this.onSelectionChanged.emit(objects);
        this.refreshAvailableOptions();
    }

    showReorder() {
        this.reorderDialog = true;
        this.changes.detectChanges();
    }
}
