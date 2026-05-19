import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    input,
    Input,
    InputSignal,
    Output,
    signal,
    WritableSignal,
} from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { CollectionEntries, Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { SharedModule } from '../../../../shared/shared.module';
import { TooltipAriaLabelDirective } from '../../shared/directives/tooltip-aria-label.directive';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { scrollIntoView } from '../../shared/utils/dom-util';
import { retrieveNodeId } from '../../shared/utils/template-util';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { LifecycleDirective } from './lifecycle.directive';
import { RemoteTreeDataSource, TreeNode } from './remote-tree-data-source';
import { WrapObservablePipe } from './wrap-observable.pipe';

// CSS selector for focusable list items in the children lists.
const CHILD_ITEM_SELECTOR = ':scope > .child-list-item';

@Component({
    selector: 'es-topics-column-browser',
    imports: [
        EduSharingUiCommonModule,
        EduSharingUiModule,
        LifecycleDirective,
        MatListModule,
        SharedModule,
        TooltipAriaLabelDirective,
        WidgetConfigurationButtonsComponent,
        WrapObservablePipe,
    ],
    templateUrl: './topics-column-browser.component.html',
    styleUrls: ['./topics-column-browser.component.scss'],
})
export class TopicsColumnBrowserComponent implements WidgetComponentInterface {
    // CONSTANTS
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.TOPICS_COLUMN_BROWSER.';
    private readonly MOBILE_WIDTH: number = 860;

    // INPUTS + OUTPUTS
    @Input() contextNodeId!: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() @HostBinding('style.height') height?: string;
    @Input() pageVariantNode?: Node;
    @Input() sidebarEmbedding: boolean = false;
    @Input() swimlaneIndex: number = -1;

    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    customUrl: (node: Node) => string;
    customUrlTarget: '_self' | '_blank' = '_self';
    readonly dataSource: RemoteTreeDataSource<Node> = new RemoteTreeDataSource<Node>();
    initialized: WritableSignal<boolean> = signal(false);
    path: TreeNode<Node>[] = [];
    updateInProgress: WritableSignal<boolean> = signal(false);
    private width$: BehaviorSubject<number> = new BehaviorSubject<number>(window.innerWidth);

    @HostListener('window:resize') onResize(): void {
        this.width$.next(window.innerWidth);
    }

    constructor(
        private readonly elementRef: ElementRef<HTMLElement>,
        private topicPageGlobalService: TopicPageGlobalService,
        private topicPageHelperService: TopicPageHelperService,
    ) {
        if (this.topicPageGlobalService.getCustomUrlFunction()) {
            this.customUrl = this.topicPageGlobalService.getCustomUrlFunction();
        }
        this.customUrlTarget = this.topicPageGlobalService.getCustomUrlTarget();
    }

    /**
     * Checks whether the current view is a mobile view.
     */
    isMobile(): Observable<boolean> {
        return this.width$.pipe(map((width: number): boolean => width < this.MOBILE_WIDTH));
    }

    /**
     * Reacts to a list option being selected.
     *
     * @param value
     */
    onSelectedChange(value: TreeNode<Node> | null): void {
        if (value) {
            this.path = [...this.path.slice(0, value.level), value];
        }
    }

    /**
     * Scrolls the view to the left.
     */
    scrollLeft(): void {
        this.elementRef.nativeElement.scroll({ left: 0, behavior: 'smooth' });
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Preloads and populates the list of sub-collections.
     */
    async preLoadAction(): Promise<void> {
        this.path = [];
        this.dataSource.setFetchChildNodes((nodeId: string) => {
            return this.topicPageHelperService
                .getSubcollections(nodeId ?? this.contextNodeId, true)
                .pipe(
                    map((nodes: CollectionEntries) =>
                        nodes.collections
                            .filter(
                                (node: Node): boolean =>
                                    !node.properties?.[
                                        RestConstants.CCM_PROP_IO_EDITORIAL_STATE
                                    ]?.includes('deactivated'),
                            )
                            .map((node: Node) => ({ id: retrieveNodeId(node), data: node })),
                    ),
                );
        });
    }

    /**
     * Keyboard dispatcher for the first-level mat-selection-list items.
     *
     * IMPORTANT: mat-selection-list already provides arrow/home/end navigation
     * via its internal FocusKeyManager. We must NOT interfere with those keys,
     * otherwise the focus jumps unpredictably (e.g. always to first/last).
     * We only handle the keys that add custom behavior (Enter, Space, ArrowRight).
     */
    onFirstLevelKeydown(event: KeyboardEvent, node: TreeNode<Node>): void {
        const hasChildren: boolean = !!node.data.collection.childCollectionsCount;
        const currentItem: HTMLElement = event.currentTarget as HTMLElement;

        switch (event.key) {
            case 'Enter': {
                event.preventDefault();
                event.stopPropagation();
                this.triggerLink(currentItem);
                break;
            }
            case ' ': // Space
            case 'ArrowRight': {
                if (hasChildren) {
                    event.preventDefault();
                    event.stopPropagation();
                    this.onSelectedChange(node);
                    // The column we just opened belongs to `node` as parent.
                    this.focusAfterDrilldown(node);
                }
                break;
            }
            // All other keys (ArrowUp, ArrowDown, Home, End, ...) are left to
            // mat-selection-list's own FocusKeyManager.
        }
    }

    /**
     * Keyboard dispatcher for the children list items (custom <ul role="listbox">).
     * Implements the full WAI-ARIA listbox keyboard pattern with a roving
     * tabindex, because the children lists are plain HTML and do not have
     * Material's built-in keyboard handling.
     *
     * @param event KeyboardEvent originating from the focused list item
     * @param node The TreeNode the item represents
     * @param columnIndex 1-based index of the children column
     */
    onChildItemKeydown(event: KeyboardEvent, node: TreeNode<Node>, columnIndex: number): void {
        const hasChildren: boolean = !!node.data.collection.childCollectionsCount;
        const currentItem: HTMLElement = event.currentTarget as HTMLElement;

        switch (event.key) {
            case 'Enter': {
                event.preventDefault();
                event.stopPropagation();
                this.triggerLink(currentItem);
                break;
            }
            case ' ': // Space
            case 'ArrowRight': {
                if (hasChildren) {
                    event.preventDefault();
                    event.stopPropagation();
                    this.onSelectedChange(node);
                    // The column we just opened belongs to `node` as parent.
                    this.focusAfterDrilldown(node);
                }
                break;
            }
            case 'ArrowLeft': {
                event.preventDefault();
                if (this.getCurrentMobileState()) {
                    // On mobile only one column is visible at a time. Going back
                    // means popping the last path entry; the focus should land on
                    // the parent node that opened the column we are leaving.
                    const focusTarget: TreeNode<Node> | undefined = this.path[this.path.length - 1];
                    this.path = this.path.slice(0, this.path.length - 1);
                    setTimeout(() => this.focusNodeAfterMobileBack(focusTarget));
                } else if (columnIndex > 1) {
                    // Focus the parent node that opened the current column.
                    this.focusChildColumnItem(columnIndex - 1, this.path[columnIndex - 2]);
                } else {
                    // Move back to the first-level list and focus the currently
                    // selected root node (path[0]) instead of the first element.
                    this.focusFirstLevelItem(this.path[0]);
                }
                break;
            }
            case 'ArrowDown': {
                event.preventDefault();
                this.moveChildFocus(currentItem, 1);
                break;
            }
            case 'ArrowUp': {
                event.preventDefault();
                this.moveChildFocus(currentItem, -1);
                break;
            }
            case 'Home': {
                event.preventDefault();
                this.moveChildFocusToEdge(currentItem, 'first');
                break;
            }
            case 'End': {
                event.preventDefault();
                this.moveChildFocusToEdge(currentItem, 'last');
                break;
            }
        }
    }

    /** Activates the link element rendered inside a list item. */
    private triggerLink(item: HTMLElement): void {
        const link: HTMLAnchorElement | null = item.querySelector<HTMLAnchorElement>(
            'a.item-link, es-node-url a',
        );
        link?.click();
    }

    /**
     * Moves the roving focus to a sibling item in the same children column.
     */
    private moveChildFocus(current: HTMLElement, delta: 1 | -1): void {
        const items: HTMLElement[] = this.getChildSiblings(current);
        if (items.length === 0) {
            return;
        }
        const idx: number = items.indexOf(current);
        const nextIdx: number = Math.max(0, Math.min(items.length - 1, idx + delta));
        this.setRovingFocus(items, nextIdx);
    }

    /**
     * Moves the focus to the first or last item of the current children column.
     */
    private moveChildFocusToEdge(current: HTMLElement, edge: 'first' | 'last'): void {
        const items: HTMLElement[] = this.getChildSiblings(current);
        if (items.length === 0) {
            return;
        }
        this.setRovingFocus(items, edge === 'first' ? 0 : items.length - 1);
    }

    /**
     * Focuses an item in the nth children column.
     *
     * @param columnIndex 1-based index of the children column
     * @param target Either the TreeNode to focus or a numeric item index
     */
    private focusChildColumnItem(columnIndex: number, target: TreeNode<Node> | number): void {
        const host: HTMLElement = this.elementRef.nativeElement;
        const containers: NodeListOf<Element> = host.querySelectorAll('.children-list-container');
        const list: Element | null | undefined =
            containers[columnIndex - 1]?.querySelector('.child-level-list');
        const items: HTMLElement[] = list
            ? Array.from(list.querySelectorAll<HTMLElement>(CHILD_ITEM_SELECTOR))
            : [];
        if (items.length === 0) {
            return;
        }
        const targetIdx: number = this.resolveTargetIndex(items, target);
        this.setRovingFocus(items, targetIdx);
    }

    /**
     * Focuses an item in the first-level mat-selection-list.
     * Material manages its own roving tabindex, so we only have to call focus().
     *
     * @param target Either the TreeNode to focus or a numeric item index
     */
    private focusFirstLevelItem(target: TreeNode<Node> | number): void {
        const host: HTMLElement = this.elementRef.nativeElement;
        const items: HTMLElement[] = Array.from(
            host.querySelectorAll<HTMLElement>('.first-level-list mat-list-option'),
        );
        if (items.length === 0) {
            return;
        }
        const targetIdx: number = this.resolveTargetIndex(items, target);
        items[targetIdx]?.focus();
    }

    /**
     * Resolves a target argument to a concrete item index.
     * - If a TreeNode is passed, the item with the matching data-node-id is used.
     * - If a number is passed, it is clamped to the valid range.
     * - Falls back to 0 when no match is found.
     */
    private resolveTargetIndex(items: HTMLElement[], target: TreeNode<Node> | number): number {
        if (typeof target === 'number') {
            return Math.max(0, Math.min(items.length - 1, target));
        }
        const idx: number = items.findIndex(
            (el: HTMLElement): boolean => el.getAttribute('data-node-id') === target?.id,
        );
        return idx >= 0 ? idx : 0;
    }

    /**
     * After collapsing one path level on mobile, places the focus on the
     * element that previously opened the column we just left.
     *
     * - If the path is now empty, the first-level list is shown and we focus
     *   the corresponding root node.
     * - Otherwise the last children column is shown and we focus the parent
     *   node within it.
     */
    private focusNodeAfterMobileBack(focusTarget: TreeNode<Node> | undefined): void {
        if (!focusTarget) {
            return;
        }
        if (this.path.length === 0) {
            this.focusFirstLevelItem(focusTarget);
        } else {
            // The visible column on mobile is the last children container.
            this.focusChildColumnItem(this.path.length, focusTarget);
        }
    }

    /**
     * Places the focus into a newly opened column after a drilldown.
     *
     * The new column is identified by the parent node (data-parent-id) rather
     * than by a positional index, which would be unreliable while Angular is
     * still tearing down a previously opened column.
     *
     * @param parent TreeNode whose children form the new column
     */
    private focusAfterDrilldown(parent: TreeNode<Node>): void {
        this.waitForColumnReady(parent).then((container: Element | null): void => {
            if (!container) {
                return;
            }
            const items: HTMLElement[] = Array.from(
                container.querySelectorAll<HTMLElement>('.child-level-list > .child-list-item'),
            );
            if (items.length > 0) {
                this.setRovingFocus(items, 0);
                return;
            }
            // Empty column – on mobile fall back to back button.
            if (this.getCurrentMobileState()) {
                const backButton: HTMLElement | null =
                    container.querySelector<HTMLElement>('.back-button');
                backButton?.focus();
            }
        });
    }

    /**
     * Waits until the column belonging to `parent` has finished loading,
     * i.e. has rendered its list items or shows an empty/error state.
     *
     * The container is matched via its `data-parent-id` attribute, so that
     * stale containers from a previous path are not picked up while Angular
     * is still re-rendering.
     *
     * @param parent TreeNode whose container we are waiting for
     * @param timeoutMs Maximum time to wait before resolving anyway
     */
    private waitForColumnReady(
        parent: TreeNode<Node>,
        timeoutMs: number = 5000,
    ): Promise<Element | null> {
        return new Promise<Element | null>((resolve) => {
            const host: HTMLElement = this.elementRef.nativeElement;

            const getContainer = (): Element | null =>
                host.querySelector(
                    `.children-list-container[data-parent-id="${CSS.escape(parent.id)}"]`,
                );

            /**
             * "Ready" means actual rendered content – items, error notice or
             * empty-state info message – is present. Just the absence of a
             * spinner is not enough, because during re-render the container
             * can briefly exist without any inner content.
             */
            const isReady = (container: Element | null): boolean => {
                if (!container) {
                    return false;
                }
                return !!(
                    container.querySelector('.child-level-list > .child-list-item') ||
                    container.querySelector(':scope > .error-notice') ||
                    container.querySelector('.info-message')
                );
            };

            let observer: MutationObserver | null = null;

            const finish = (container: Element | null): void => {
                observer?.disconnect();
                clearTimeout(timeoutHandle);
                resolve(container);
            };

            const tryResolve = (): boolean => {
                const container: Element | null = getContainer();
                if (isReady(container)) {
                    finish(container);
                    return true;
                }
                return false;
            };

            observer = new MutationObserver(() => {
                tryResolve();
            });
            observer.observe(host, { childList: true, subtree: true });

            const timeoutHandle: ReturnType<typeof setTimeout> = setTimeout(() => {
                finish(getContainer());
            }, timeoutMs);

            // Initial check (cached data may already be there).
            tryResolve();
        });
    }

    /** Returns all sibling child-list items in the same column. */
    private getChildSiblings(current: HTMLElement): HTMLElement[] {
        const parent: HTMLElement | null = current.parentElement;
        return parent ? Array.from(parent.querySelectorAll<HTMLElement>(CHILD_ITEM_SELECTOR)) : [];
    }

    /**
     * Applies the roving tabindex pattern: only the target item is reachable
     * via Tab (tabindex="0"); all others are removed from the tab sequence
     * (tabindex="-1"). The target is then focused programmatically.
     */
    private setRovingFocus(items: HTMLElement[], targetIdx: number): void {
        items.forEach((el: HTMLElement, i: number) =>
            el.setAttribute('tabindex', i === targetIdx ? '0' : '-1'),
        );
        items[targetIdx]?.focus();
    }

    /** Synchronously reads the latest mobile state from the width subject. */
    private getCurrentMobileState(): boolean {
        return this.width$.value < this.MOBILE_WIDTH;
    }

    /**
     * Pops the last entry from the navigation path and restores the focus to
     * the parent node that opened the column we are leaving. Used by both the
     * mobile "back" button (click) and the keyboard handlers (ArrowLeft).
     */
    goBackOneLevel(): void {
        if (this.path.length === 0) {
            return;
        }
        const focusTarget: TreeNode<Node> = this.path[this.path.length - 1];
        this.path = this.path.slice(0, this.path.length - 1);
        setTimeout(() => this.focusNodeAfterMobileBack(focusTarget));
    }

    /**
     * Keyboard handler for the mobile "back" button. Allows navigating into
     * the list with ArrowDown / ArrowRight and going back one level with
     * ArrowLeft, mirroring the list-item behavior.
     */
    onBackButtonKeydown(event: KeyboardEvent): void {
        switch (event.key) {
            case 'ArrowDown':
            case 'ArrowRight': {
                event.preventDefault();
                // The currently visible column belongs to the last path entry as parent.
                const currentParent: TreeNode<Node> | undefined = this.path[this.path.length - 1];
                if (currentParent) {
                    this.focusAfterDrilldown(currentParent);
                }
                break;
            }
            case 'ArrowLeft': {
                event.preventDefault();
                this.goBackOneLevel();
                break;
            }
        }
    }

    protected readonly scrollIntoView = scrollIntoView;
}
