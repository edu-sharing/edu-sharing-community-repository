import { trigger } from '@angular/animations';
import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    Output,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    Node,
    RestHelper,
    UIService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { UIHelper } from '../../ui-helper';

/**
 * A common edu-sharing modal card
 */
@Component({
    selector: 'card',
    templateUrl: 'card.component.html',
    styleUrls: ['card.component.scss'],
    animations: [trigger('cardAnimation', UIAnimation.cardAnimation())],
})
export class CardComponent implements AfterViewInit, OnDestroy {
    @ViewChild('cardContainer', { static: false })
    cardContainer: ElementRef<HTMLElement>;
    @ViewChild('jumpmarksRef', { static: false })
    jumpmarksRef: ElementRef;
    @ViewChild('cardActions', { static: false })
    cardActions: ElementRef<HTMLElement>;

    /**
     * the title of the card. Should be pre-translated
     */
    @Input() title: string;
    /**
     * The subtitle of the card (optional)
     * You may also use the "node" binding to automatically fill this field
     */
    @Input() subtitle: string;
    /**
     * Should a "x" appear in the top right (don't forget to bind onCancel as an event)
     */
    @Input() isCancelable = true;
    /**
     * An optional image href that should be appear in the top left corner
     */
    @Input() avatar: string;
    /**
     * An optional icon that should be appear in the top left corner (use either avatar or icon!)
     */
    @Input() icon: string;
    @Input() width = 'normal';
    @Input() height = 'normal';
    /**
     * Hint that the layout contains mat-tab-group (relevant for correct scrolling, tabs will be fixed at top)
     */
    @Input() tabbed = false;
    /**
     * Should the dialog be modal (a dark background)
     * allowed values: always (default), auto, never
     * auto: Automatically make the dialog modal when viewed on very tiny screens (e.g. mobile), otherwise use non-modal view
     */
    @Input() modal: 'always' | 'auto' = 'always';
    /**
     * Should the heading be shown
     */
    @Input() header = true;
    /**
     * Jumpmarks for the left side (used for the mds dialog)
     */
    @Input() jumpmarks: CardJumpmark[];
    @Input() priority = 0;

    /**
     * Optional, bind a Node or Node-Array to this element
     * If this is used, the subtitle and avatar is automatically set depending on the given data
     */
    @Input() set node(node: Node | Node[]) {
        if (!node) {
            return;
        }
        let nodes: Node[] = node as any;
        if (!Array.isArray(nodes)) {
            nodes = [node as any];
        }
        if (nodes && nodes.length) {
            if (nodes.length === 1 && nodes[0]) {
                this.avatar = nodes[0].iconURL;
                this.subtitle = RestHelper.getTitle(nodes[0]);
            } else {
                this.avatar = null;
                this.subtitle = this.translate.instant(
                    'CARD_SUBTITLE_MULTIPLE',
                    { count: nodes.length },
                );
            }
        }
    }
    @Input() set buttons(buttons: DialogButton[]) {
        this._buttons = buttons;
    }

    @Output() onCancel = new EventEmitter();
    @Output() onScrolled = new EventEmitter();

    /** A list of buttons, see @DialogButton
     * Also use the DialogButton.getYesNo() and others if applicable!
     */
    _buttons: DialogButton[];
    jumpmarkActive: CardJumpmark;

    private static modalCards: CardComponent[] = [];

    static getNumberOfOpenCards() {
        return CardComponent.modalCards.length;
    }

    constructor(
        private uiService: UIService,
        private translate: TranslateService,
    ) {
        CardComponent.modalCards.splice(0, 0, this);
        document.body.style.overflow = 'hidden';
        UIHelper.waitForComponent(this, 'jumpmarksRef').subscribe(() => {
            console.log('jumpmarks ready');
            setInterval(() => {
                try {
                    const jump = this.jumpmarksRef;
                    const height =
                        this.cardContainer.nativeElement.getBoundingClientRect()
                            .bottom -
                        this.cardContainer.nativeElement.getBoundingClientRect()
                            .top;
                    const pos =
                        this.cardContainer.nativeElement.scrollTop -
                        height -
                        200;
                    let closest = 999999;
                    for (const jumpmark of this.jumpmarks) {
                        const element = document.getElementById(jumpmark.id);
                        const top = element.getBoundingClientRect().top;
                        if (Math.abs(top - pos) < closest) {
                            closest = Math.abs(top - pos);
                            this.jumpmarkActive = this.jumpmarks[
                                Helper.indexOfObjectArray(
                                    this.jumpmarks,
                                    'id',
                                    element.id,
                                )
                            ];
                        }
                    }
                } catch (e) {}
            }, 1000 / 20); // 20 FPS
        });
    }

    ngAfterViewInit() {
        // Delay focus processing to not interfere with Angular's
        // initialization.
        setTimeout(() => this.setInitialFocus());
    }

    ngOnDestroy() {
        CardComponent.modalCards.splice(
            CardComponent.modalCards.indexOf(this),
            1,
        );
        if (CardComponent.modalCards.length === 0) {
            document.body.style.overflow = null;
        }
    }

    @HostListener('window:resize')
    onResize() {
        if (
            document.activeElement &&
            this.cardContainer &&
            this.cardContainer.nativeElement
        ) {
            UIHelper.scrollSmoothElementToChild(
                document.activeElement,
                this.cardContainer.nativeElement,
            );
        }
    }
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        for (const card of CardComponent.modalCards) {
            if (card.handleEvent(event)) {
                return;
            }
        }
    }

    handleEvent(event: any) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            event.preventDefault();
            this.cancel();
            return true;
        }
        return false;
    }

    click(btn: DialogButton) {
        btn.callback();
    }

    cancel() {
        this.onCancel.emit();
    }

    scrolled() {
        this.onScrolled.emit();
    }

    private scrollSmooth(jumpmark: CardJumpmark) {
        const pos = document.getElementById(jumpmark.id).offsetTop;
        UIHelper.scrollSmoothElement(pos, this.cardContainer.nativeElement, 2);
    }

    private setInitialFocus() {
        const inputs = Array.from(
            this.cardContainer.nativeElement.getElementsByTagName('input'),
        );
        if (inputs.some(el => el.autofocus)) {
            // Focus the first input field that sets `autofocus`.
            inputs.find(el => el.autofocus).focus();
        } else if (inputs.length) {
            // Else, focus the first input field.
            inputs[0].focus();
        } else {
            // Else, focus the right-most action button that is not disabled.
            const actionButtons = Array.from(
                this.cardActions.nativeElement.children,
            ).map(el => el.children[0] as HTMLButtonElement);
            const lastButton = actionButtons.reverse().find(el => !el.disabled);
            if (lastButton) {
                lastButton.focus();
            }
        }
        // Else, focus will default to the 'X' button on the header bar.
        //
        // If this happens although there are buttons or inputs on the dialog,
        // make sure these are there from the beginning and not inserted later
        // on.
    }
}

export class CardJumpmark {
    /**
     *
     * @param id the id (as in html)
     * @param label the pre-translated label
     * @param icon the icon
     */
    constructor(public id: string, public label: string, public icon: string) {}
}
