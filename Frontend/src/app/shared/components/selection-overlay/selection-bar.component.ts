import { LiveAnnouncer } from '@angular/cdk/a11y';
import {
    Component,
    computed,
    effect,
    ElementRef,
    HostListener,
    inject,
    input,
    model,
    output,
    untracked,
    viewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { ColumnType, NodeEntriesDisplayType, NodeTitlePipe } from 'ngx-edu-sharing-ui';

let nextPanelId = 0;

/**
 * Bar at the bottom of a node list that shows how many nodes are selected (or on the clipboard),
 * lets the user clear them and, via its toggle button, opens a panel listing those nodes.
 *
 * The panel follows the toggle button in the DOM, so it comes right after it in reading and tab
 * order, and is laid out above the bar's controls. It is a non-modal dialog: Escape inside it or
 * on the toggle closes it and returns focus to the toggle, a click outside the bar closes it, and
 * opening and closing are announced to screen readers.
 *
 * Actions for the nodes (typically an `es-actionbar`) are projected and placed after the panel.
 * The host carries the class `selection-action-bar`; add `visible` to show the bar.
 */
@Component({
    selector: 'es-selection-bar',
    templateUrl: './selection-bar.component.html',
    styleUrls: ['./selection-bar.component.scss'],
    standalone: false,
    host: {
        class: 'selection-action-bar',
    },
})
export class SelectionBarComponent {
    private readonly announcer = inject(LiveAnnouncer);
    private readonly translate = inject(TranslateService);
    private readonly nodeTitle = inject(NodeTitlePipe);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    /** Nodes counted on the toggle button and listed in the panel. */
    readonly nodes = input<Node[]>([]);
    /** Columns of the panel's list. */
    readonly columns = input<ColumnType>();
    /** Display type of the related node list; the panel uses a table or small cards to match. */
    readonly displayType = input<NodeEntriesDisplayType>(NodeEntriesDisplayType.SmallGrid);
    /** Translation key of the toggle button's label; receives `count`. */
    readonly countLabel = input('SEARCH.N_SELECTED');
    /** Translation key of the clear button's label. */
    readonly clearLabel = input('SEARCH.CLEAR_SELECTION');
    /** Translation key of the panel's accessible name. */
    readonly panelLabel = input('SEARCH.SELECTION_PANEL');
    /** Whether the panel is open; it only shows while there are nodes. */
    readonly open = model(false);
    /** Emits when the user clears all nodes via the clear button. */
    readonly clear = output<void>();
    /** Emits a node the user unchecked in the panel. */
    readonly deselect = output<Node>();

    /** DOM id of the panel, referenced by the toggle button's `aria-controls`. */
    readonly panelId = `es-selection-bar-panel-${nextPanelId++}`;
    readonly panelOpen = computed(() => this.open() && !!this.nodes()?.length);

    private readonly toggleButton = viewChild('toggleRef', { read: ElementRef });
    private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

    constructor() {
        // Without nodes the bar is hidden; a later selection starts with the panel closed. Nothing
        // is announced since the whole bar disappears.
        effect(() => {
            if (!this.nodes()?.length && untracked(this.open)) {
                this.open.set(false);
            }
        });
    }

    /** Opens or closes the panel and announces the change to screen readers. */
    toggle(): void {
        if (!this.nodes()?.length) {
            return;
        }
        this.setOpen(!this.panelOpen());
    }

    /**
     * Forwards a node unchecked in the panel.
     *
     * The unchecked entry leaves the panel, so focus moves to the toggle button, which reads the
     * new count; without remaining nodes the whole bar disappears and focus moves to the node's
     * checkbox in the page's list instead. Without focus in the panel (pointer use), the new count
     * is announced.
     */
    onDeselect(node: Node): void {
        const focusWasInPanel = !!this.panel()?.nativeElement.contains(document.activeElement);
        this.deselect.emit(node);
        // Wait for the host page to pass the reduced node list.
        setTimeout(() => {
            const remaining = this.nodes()?.length ?? 0;
            if (focusWasInPanel) {
                if (remaining) {
                    this.toggleButton()?.nativeElement.focus();
                } else {
                    this.focusListCheckbox(node);
                }
            } else {
                void this.announcer.announce(
                    this.translate.instant(this.countLabel(), { count: remaining }),
                );
            }
        });
    }

    /** Closes the panel on a click outside the bar (which contains it). */
    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        if (this.panelOpen() && !event.composedPath().includes(this.host.nativeElement)) {
            this.setOpen(false);
        }
    }

    /**
     * Closes the panel on Escape pressed inside the panel or on its toggle button and returns
     * focus to the toggle button. Other Escape presses are left to the page.
     */
    @HostListener('keydown.escape', ['$event'])
    onEscape(event: Event): void {
        if (!this.panelOpen()) {
            return;
        }
        const target = event.target as HTMLElement;
        const toggle: HTMLElement = this.toggleButton()?.nativeElement;
        const inPanel = !!this.panel()?.nativeElement.contains(target);
        if (!inPanel && !toggle?.contains(target)) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        this.setOpen(false);
        toggle?.focus();
    }

    private setOpen(open: boolean): void {
        if (this.panelOpen() === open) {
            return;
        }
        this.open.set(open);
        const name = this.translate.instant(this.panelLabel());
        void this.announcer.announce(
            this.translate.instant(
                open ? 'SEARCH.SELECTION_PANEL_SHOWN' : 'SEARCH.SELECTION_PANEL_HIDDEN',
                { name },
            ),
        );
    }

    /**
     * Focuses the checkbox of the given node in the page's node list, matched by its accessible
     * name; checkboxes inside the bar itself are skipped.
     */
    private focusListCheckbox(node: Node): void {
        const label = this.translate.instant('SELECT', {
            element: this.nodeTitle.transform(node),
        });
        const checkbox = Array.from(
            document.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'),
        ).find(
            (input) =>
                input.getAttribute('aria-label') === label &&
                !this.host.nativeElement.contains(input),
        );
        checkbox?.focus();
    }
}
