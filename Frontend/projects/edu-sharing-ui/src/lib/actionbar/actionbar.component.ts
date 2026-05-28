import { trigger } from '@angular/animations';
import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UIAnimation } from '../util/ui-animation';
import { UIConstants } from '../util/ui-constants';
import { OptionItem } from '../types/option-item';
import { UIService } from '../services/ui.service';
import { Helper } from '../util/helper';
import { TooltipPosition } from '@angular/material/tooltip';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-actionbar',
    templateUrl: 'actionbar.component.html',
    styleUrls: ['actionbar.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
    standalone: false,
})
/**
 * The action bar provides several icons, usually at the top right, with actions for a current context
 */
export class ActionbarComponent implements OnChanges, AfterViewInit, OnDestroy {
    /**
     * The amount of options which are not hidden inside an overflow menu
     * (default: depending on mobile (1) or not (2))
     * Also use numberOfAlwaysVisibleOptionsMobile to control the amount of mobile options visible
     */
    @Input() numberOfAlwaysVisibleOptions = 2;
    @Input() numberOfAlwaysVisibleOptionsMobile = 1;
    /**
     * Controls label visibility for always-visible buttons.
     *
     * - `'auto'` (default): labels shown on wide screens, hidden via CSS media query below ~900 px.
     * - `true`: labels always shown regardless of screen size.
     * - `false`: labels never shown (icon-only).
     * - `'ifRoom'`: labels shown only when the actionbar container has enough horizontal space;
     *   hides automatically when buttons would overflow, and reappears when space is available again.
     *   Responds to the actual rendered width, not a fixed breakpoint.
     */
    @Input() showCaptions: 'auto' | false | true | 'ifRoom' = 'auto';
    /**
     * Visual style of the actionbar
     *
     * Values:
     *  - 'button': material buttons with icons and text on medium to large screens
     *  - 'icon-button': material buttons without text
     *  - 'round': circular buttons without text
     * @default 'button'
     */
    @Input() appearance: 'round' | 'button' | 'icon-button' = 'button';
    /**
     * dropdownPosition is for position of dropdown (default = left)
     * Values 'left' or 'right'
     */
    @Input() dropdownPosition: 'left' | 'right' = 'left';

    /**
     * backgroundType for color matching, either bright, dark or primary
     */
    @Input() backgroundType: 'bright' | 'dark' | 'primary' = 'bright';
    /**
     * Style, currently default or 'flat' if all always visible icons should get a flat look
     */
    @Input() style: 'default' | 'flat' = 'default';
    /**
     * Highlight one or more of the always-visible buttons as primary action.
     *
     * - `first`, `last`: The first / last of `optionsAlways` by order.
     * - `manual`: Highlight all options that set `isPrimary = true`.
     */
    @Input() highlight: 'first' | 'last' | 'manual' | 'none' = 'none';
    /**
     * Should disabled ("greyed out") options be shown or hidden?
     */
    @Input() showDisabled = true;

    /**
     * the position of the mat tooltips
     */
    @Input() tooltipPosition: TooltipPosition = 'below';
    /**
     * fixed button width (used for ifRoom caption calculation)
     */
    readonly FixedButtonWidth = 200;

    /**
     * Set the options, see @OptionItem
     */
    @Input() set options(options: OptionItem[]) {
        this.optionsIn = options;
        this.prepareOptions(options);
    }

    /**
     * breakpoint width at which point the mobile display count is used
     */
    @Input() mobileBreakpoint = UIConstants.MOBILE_WIDTH;
    @HostBinding('class.labels-hidden-for-room') labelsHiddenForRoom = false;
    @ViewChild('actionbarDiv') private actionbarDiv: ElementRef<HTMLElement>;

    constructor(
        private uiService: UIService,
        private translate: TranslateService,
        private cdr: ChangeDetectorRef,
        private elementRef: ElementRef<HTMLElement>,
    ) {}

    optionsIn: OptionItem[] = [];
    optionsAlways$ = new BehaviorSubject<OptionItem[]>([]);
    optionsMenu$ = new BehaviorSubject<OptionItem[]>([]);
    optionsToggleBefore: OptionItem[] = [];
    optionsToggleAfter: OptionItem[] = [];

    private resizeObserver: ResizeObserver | null = null;

    private prepareOptions(options: OptionItem[]) {
        options = this.uiService.filterValidOptions(Helper.deepCopyArray(options));
        if (options == null) {
            this.optionsAlways$.next([]);
            this.optionsMenu$.next([]);
            return;
        }
        this.optionsToggleBefore = this.uiService.filterToggleOptions(options, true, 'before');
        this.optionsToggleAfter = this.uiService.filterToggleOptions(options, true, 'after');
        this.optionsAlways$.next(
            this.sortByActionOptions(this.uiService.filterToggleOptions(options, false)).slice(
                0,
                this.getNumberOptions(),
            ),
        );
        if (!this.optionsAlways$.value.length) {
            this.optionsAlways$.next(
                this.uiService
                    .filterToggleOptions(options, false)
                    .slice(0, this.getNumberOptions()),
            );
        }
        this.optionsMenu$.next(
            this.hideActionOptions(
                this.uiService.filterToggleOptions(options, false),
                this.optionsAlways$.value,
            ),
        );
        void this.uiService.updateOptionEnabledState(this.optionsAlways$);
        void this.uiService.updateOptionEnabledState(this.optionsMenu$);
        // may causes weird looking
        /*if(this.optionsMenu.length<2) {
      this.optionsAlways = this.optionsAlways.concat(this.optionsMenu);
      this.optionsMenu = [];
    }*/
    }

    public getNumberOptions() {
        if (window.innerWidth < this.mobileBreakpoint) {
            return this.numberOfAlwaysVisibleOptionsMobile;
        }
        return this.numberOfAlwaysVisibleOptions;
    }

    click(option: OptionItem) {
        if (!option.isEnabled) {
            if (option.disabledCallback) {
                option.disabledCallback();
            }
            return;
        }
        option.callback();
    }

    private sortByActionOptions(options: OptionItem[]) {
        const result: OptionItem[] = [];
        for (const option of options) {
            if (option.showAsAction === true) result.push(option);
        }
        for (const option of options) {
            if (option.showAsAction === 'auto') result.push(option);
        }
        return result;
    }

    private hideActionOptions(options: OptionItem[], optionsAlways: OptionItem[]) {
        const result: OptionItem[] = [];
        for (const option of options) {
            if (optionsAlways.indexOf(option) === -1) result.push(option);
        }
        return result;
    }

    /**
     * Invalidate / refreshes all options based on their current callbacks
     */
    public invalidate() {
        this.prepareOptions(this.optionsIn);
    }

    private filterDisabled(options: OptionItem[]) {
        if (options == null) return null;
        const filtered = [];
        for (const option of options) {
            if (option.isEnabled || this.showDisabled) filtered.push(option);
        }
        return filtered;
    }

    canShowDropdown() {
        if (!this.optionsMenu$.value.length) {
            return false;
        }
        return this.optionsMenu$.value.filter((o) => o.isEnabled).length > 0;
    }

    shouldHighlight(optionIndex: number, option: OptionItem): boolean {
        switch (this.highlight) {
            case 'none':
                return false;
            case 'first':
                return optionIndex === 0;
            case 'last':
                return optionIndex === this.optionsAlways$.value.length - 1;
            case 'manual':
                return option.isPrimary;
        }
    }

    ngAfterViewInit(): void {
        this.setupResizeObserver();
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.invalidate();
        if (changes.showCaptions) {
            this.setupResizeObserver();
        }
    }

    ngOnDestroy(): void {
        this.resizeObserver?.disconnect();
    }

    private setupResizeObserver(): void {
        this.resizeObserver?.disconnect();
        this.resizeObserver = null;
        if (this.showCaptions !== 'ifRoom' || !this.actionbarDiv) {
            this.labelsHiddenForRoom = false;
            return;
        }
        // Observe the parent element, not the host — the host is content-sized and changes
        // width when labels toggle, which would re-trigger the observer and cause flickering.
        const parent = this.elementRef.nativeElement.parentElement;
        this.resizeObserver = new ResizeObserver(() => this.checkLabelsRoom());
        this.resizeObserver.observe(parent);
    }

    private checkLabelsRoom(): void {
        const availableWidth =
            this.elementRef.nativeElement.parentElement.getBoundingClientRect().width;
        const requiredWidth = this.optionsAlways$.value.length * this.FixedButtonWidth;
        this.labelsHiddenForRoom = availableWidth < requiredWidth;
        this.cdr.markForCheck();
    }

    protected readonly UIService = UIService;
}
