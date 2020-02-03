import { animate, sequence, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
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
    Repository,
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
import { OptionItem } from '../../option-item';
import { Toast } from '../../toast';
import { UIHelper } from '../../ui-helper';

@Component({
    selector: 'listTable',
    templateUrl: 'list-table.component.html',
    styleUrls: ['list-table.component.scss'],
    animations: [
        trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('openOverlayBottom', UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('orderAnimation', [
            transition(':enter', [
                sequence([
                    animate(UIAnimation.ANIMATION_TIME_SLOW + 'ms ease', style({ opacity: 0 }))
                ])
            ]),
            transition(':leave', [
                sequence([
                    animate(UIAnimation.ANIMATION_TIME_SLOW + 'ms ease', style({ opacity: 1 }))
                ])
            ])
        ])
    ],
    // Causes action menu not to align properly
    changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 * A provider to render multiple Nodes as a list
 */
export class ListTableComponent implements EventListener {
    public static VIEW_TYPE_LIST = 0;
    public static VIEW_TYPE_GRID = 1;
    public static VIEW_TYPE_GRID_SMALL = 2;

    @ViewChild('addElementRef', {static: false}) addElementRef: ElementRef;
    @ViewChild('drag', {static: false}) drag: ElementRef;
    @ViewChild('menuTrigger', {static: false}) menuTrigger: MatMenuTrigger;
    @ViewChild('dropdown', {static: false}) dropdownElement: ElementRef;
    @ViewChild('dropdownContainer', {static: false}) dropdownContainerElement: ElementRef;

    @ContentChild('itemContent', {static: false}) itemContentRef: TemplateRef<any>;

    /** Set the current list of nodes to render */
    @Input() set nodes(nodes: Node[]) {
        // remove all non-virtual nodes which are replaced by the virtual nodes (virtual have higher prio)
        // also validate that this is only enabled for regular nodes
        if (nodes && nodes.length && nodes[0].ref && nodes[0].ref.id) {
            const virtual = nodes.filter((n) => n.virtual);
            nodes = nodes.filter((n) => n.virtual || !virtual.find((v) => v.ref.id === n.ref.id));
        }
        this._nodes = nodes;
    }

    /**
     * Should the nodes be automatically linked via href.
     *
     * This is important for crawlers
     * Activate it for any kind of nodes list which is supposed to be clickable
     */
    @Input() createLink = false;

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

    /**
     * Set the options which are valid for each node, similar to the action bar options, see @OptionItem.
     */
    @Input() set options(options: OptionItem[]) {
        options = UIHelper.filterValidOptions(this.ui, options);
        if (this.selectedNodes && this.selectedNodes.length === 1) {
            options = this.filterCallbacks(options, this.selectedNodes[0]);
        }
        console.log(options);
        this._options = [];
        if (!options) {
            return;
        }
        for (const o of options) {
            if (!o.isToggle) {
                this._options.push(o);
            }
        }
        this.optionsAlways = [];
        for (const option of options) {
            if (option.showAlways) {
                this.optionsAlways.push(option);
            }
        }
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
        if (!hasCheckbox) {
            // use a timeout to prevent a ExpressionChangedAfterItHasBeenCheckedError in the parent component
            setTimeout(() => {
                this.selectedNodes = [];
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
    @Input() validatePermissions: (node: Node) => {
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
    @Input() canDelete: () => boolean;

    /**
     * Can an element be dropped on the element?
     *
     * Called with same parameters as onDrop event.
     */
    @Input() canDrop: () => boolean = () => true;

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
        node: Node;
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
        type: 'default' | 'copy';
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

    _options: OptionItem[];
    currentDrag: string;
    currentDragCount = 0;
    dropdownLeft: string;
    dropdownTop: string;
    id: number;
    reorderButtons: DialogButton[];
    reorderDialog = false;

    private _hasCheckbox: boolean;
    private _nodes: any[];
    private animateNode: Node;
    private columnsAll: ListItem[];
    /** Set the columns to show, see @ListItem */
    private columnsOriginal: ListItem[];
    private columnsVisible: ListItem[];
    private currentDragColumn: ListItem;
    private dragHover: Node;
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
        private locator: RestLocatorService,
        private route: ActivatedRoute,
        private router: Router,
        private toast: Toast,
        private frame: FrameEventsService,
        private sanitizer: DomSanitizer
    ) {
        this.reorderButtons = DialogButton.getSaveCancel(() => this.closeReorder(false), () => this.closeReorder(true));
        this.id = Math.random();
        frame.addListener(this);
        setTimeout(() => this.loadRepos());
    }

    loadRepos(): void {
        if (!this.loadRepositories) {
            return;
        }
        this.locator.setRoute(this.route).subscribe(() => {
            this.locator.locateApi().subscribe(() => {
                this.network.getRepositories().subscribe((data: NetworkRepositories) => {
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

    // @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {
        if (event.code === 'KeyA'
            && (event.ctrlKey || this.ui.isAppleCmd())
            && !KeyEvents.eventFromInputField(event)
            && !this.preventKeyevents
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
        return ColorHelper.getColorBrightness(color) > ColorHelper.BRIGHTNESS_THRESHOLD_COLLECTIONS;
    }

    toggleAll(): void {
        if (this.selectedNodes.length === this._nodes.length) {
            this.selectedNodes = [];
            this.onSelectionChanged.emit(this.selectedNodes);
        } else {
            this.selectAll();
        }
    }

    getTitle(node: Node): string {
        return RestHelper.getTitle(node);
    }

    getCollectionColor(node: any) {
        return node.collection ? node.collection.color : node.color;
    }

    getCollection(node: any) {
        return node.collection ? node.collection : node;
    }

    isHomeNode(node: any): boolean {
        // Repos not loaded or not availale. Assume true so that small images are loaded.
        if (!this.repositories) {
            return true;
        }
        return RestNetworkService.isFromHomeRepo(node, this.repositories);
    }

    getOriginalNode(node: any) {
        if (node.reference) {
            return node.reference;
        }
        return node;
    }

    getIconUrl(node: any) {
        return this.getReference(node).iconURL;
    }

    isCollection(node: any): boolean {
        return NodeHelper.isNodeCollection(node);
    }

    isReference(node: any): boolean {
        // When checking for aspects, we always detect a reference
        // BUT these references are regular nodes and DO NOT have the "originalId"
        // resulting that all references are detected as "deleted"
        return node.reference != null/* || node.aspects && node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)!=-1*/;
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
        return item.type.toLowerCase() + '_' + item.name.toLowerCase().replace(':', '_').replace('.', '_');
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

    private filterCallbacks(options: OptionItem[], node: Node): OptionItem[] {
        return options.filter((option) => !option.showCallback || option.showCallback(node));
    }

    private selectAll(): void {
        this.selectedNodes = [];
        for (const node of this._nodes) {
            this.selectedNodes.push(node);
        }
        this.onSelectionChanged.emit(this.selectedNodes);

    }

    private move(array: any[], i1: number, i2: number): void {
        const node1 = array[i1];
        const node2 = array[i2];
        array.splice(i1, 1);
        array.splice(i2, 0, node1);
    }

    private allowDrag(event: any, target: Node): void {
        if (this.orderElements) {
            event.preventDefault();
            const source = this.storage.get(TemporaryStorageService.LIST_DRAG_DATA);
            if (source.view === this.id && source.node.ref.id !== target.ref.id) {
                this.orderElementsActive = true;
                this.orderElementsActiveChange.emit(true);
                const targetPos = this._nodes.indexOf(target);
                this._nodes = Helper.deepCopy(source.list);
                this.move(this._nodes, source.offset, targetPos);
                // Inform the outer component's variable about the new order
                this.nodesChange.emit(this._nodes);
                return;
            }
        }
        if (UIHelper.handleAllowDragEvent(this.storage, this.ui, event, target, this.canDrop)) {
            event.preventDefault();
            this.dragHover = target;
        }
    }

    private noPermissions(node: Node): boolean {
        return this.validatePermissions != null && this.validatePermissions(node).status === false;
    }

    private closeReorder(save: boolean): void {
        this.reorderDialog = false;
        if (save) {
            this.columns = this.columnsAll;
            this.columnsChanged.emit(this.columnsAll);
        }
        else {
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
        this.columnsAll[this.columnsAll.indexOf(this.currentDragColumn)].visible = false;
        this.columns = this.columnsAll;
        this.currentDragColumn = null;
    }

    private drop(event: any, target: Node): void {
        this.dragHover = null;
        if (this.orderElements) {
            const source = this.storage.get(TemporaryStorageService.LIST_DRAG_DATA);
            if (source.view === this.id && source.nodes.length === 1) {
                this.onOrderElements.emit(this._nodes);
                event.preventDefault();
                event.stopPropagation();
                return;
            }
        }
        UIHelper.handleDropEvent(this.storage, this.ui, event, target, this.onDrop);
    }

    private animateIcon(node: Node, animate: boolean): void {
        if (animate) {
            if (NodeHelper.hasAnimatedPreview(node)) {
                this.animateNode = node;
            }
        }
        else {
            this.animateNode = null;
        }
    }

    private dragStart(event: any, node: Node): void {
        if (!this.dragDrop) {
            return;
        }
        if (this.getSelectedPos(node) === -1) {
            if (this.hasCheckbox) {
                this.selectedNodes.push(node);
            }
            else {
                this.selectedNodes = [node];
            }
        }
        const nodes = this.selectedNodes.length ? this.selectedNodes : [node];

        event.dataTransfer.setData('text', JSON.stringify(nodes));
        event.dataTransfer.effectAllowed = 'all';
        let name = '';
        for (const node of nodes) {
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
        this.storage.set(TemporaryStorageService.LIST_DRAG_DATA, {
            list: Helper.deepCopy(this._nodes),
            offset: this._nodes.indexOf(node),
            node,
            nodes,
            view: this.id
        });
        this.onSelectionChanged.emit(this.selectedNodes);
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
        return this.possibleSortByFields && this.possibleSortByFields.filter((p) => p.name === sortBy.name).length > 0;
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

    private setSortingIntern(sortBy: ListItem, isPrimaryElement: boolean): void {
        if (isPrimaryElement && window.innerWidth < UIConstants.MOBILE_WIDTH + UIConstants.MOBILE_STAGE * 4) {
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
        this.sortListener.emit({ sortBy: sortBy.name, sortAscending: sortBy.ascending });
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

    private showDropdown(node: Node, openMenu = false, event: any = null): void {
        // if (this._options == null || this._options.length < 1)
        //     return;
        this.select(node, 'dropdown', false, false);
        this.onUpdateOptions.emit(node);
        if (openMenu) {
            if (event) {
                console.log(event);
                if (event.clientX + event.clientY) {
                    this.dropdownLeft = event.clientX + 'px';
                    this.dropdownTop = event.clientY + 'px';
                }
                else {
                    const rect = event.srcElement.getBoundingClientRect();
                    this.dropdownLeft = rect.left + rect.width / 2 + 'px';
                    this.dropdownTop = rect.top + rect.height / 2 + 'px';
                }
            }
        }
        // Short delay to let onUpdateOptions handler run and angular menu get the correct data from
        // start.
        setTimeout(() => this.menuTrigger.openMenu());
    }

    private doubleClick(node: Node): void {
        this.doubleClickRow.emit(node);
    }

    private select(node: Node, from: string = null, fireEvent = true, unselect = true): boolean {
        if (from !== 'checkbox' && !this.isClickable) {
            return false;
        }
        if (from !== 'checkbox' && !this.selectOnClick && fireEvent) {
            this.clickRowSender(node, from);
            return false;
        }

        if (!this.hasCheckbox || from !== 'checkbox') { // Single value select
            if (this.selectedNodes.length && this.selectedNodes[0] === node && unselect) {
                this.selectedNodes = [];
            }
            else {
                this.selectedNodes = [node];
            }
            this.onSelectionChanged.emit(this.selectedNodes);
            return false;
        }
        const pos = this.getSelectedPos(node);
        // Select from-to range via shift key.
        if (from === 'checkbox' && pos === -1 && this.ui.isShiftCmd() && this.selectedNodes.length === 1) {
            const pos1 = RestHelper.getRestObjectPositionInArray(node, this._nodes);
            const pos2 = RestHelper.getRestObjectPositionInArray(this.selectedNodes[0], this._nodes);
            const start = pos1 < pos2 ? pos1 : pos2;
            const end = pos1 < pos2 ? pos2 : pos1;
            for (let i = start; i <= end; i++) {
                if (this.getSelectedPos(this._nodes[i]) === -1) {
                    this.selectedNodes.push(this._nodes[i]);
                }
            }
        }
        else {
            if (pos !== -1 && unselect) {
                this.selectedNodes.splice(pos, 1);
            }
            if (pos === -1) {
                this.selectedNodes.push(node);
            }
        }
        this.onSelectionChanged.emit(this.selectedNodes);
        this.changes.detectChanges();
        return false;
    }

    private getAttribute(data: any, item: ListItem): SafeHtml {
        const attribute = NodeHelper.getAttribute(this.translate, this.config, data, item);
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
        return NodeHelper.getLRMIAttribute(this.translate, this.config, data, item);
    }

    private getLRMIProperty(data: any, item: ListItem): string {
        return NodeHelper.getLRMIProperty(data, item);
    }

    private getSelectedPos(selected: Node): number {
        if (!this.selectedNodes || !this.selectedNodes.length) {
            return -1;
        }
        return RestHelper.getRestObjectPositionInArray(selected, this.selectedNodes);
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

}
