import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    AfterViewInit,
    ApplicationRef,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    HostBinding,
    Injector,
    Input,
    NgZone,
    OnChanges,
    OnDestroy,
    OnInit,
    Optional,
    SimpleChanges,
    Type,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BehaviorSubject, Observable, ReplaySubject, Subject } from 'rxjs';
import { filter, first, map, take, takeUntil } from 'rxjs/operators';
import { JUMP_MARK_POSTFIX } from '../../../dialogs/card-dialog/card-dialog-container/jump-marks-handler.directive';
import { NativeWidgets, WidgetComponents } from '../../types/mds-types';
import {
    Constraints,
    EditorMode,
    GeneralWidget,
    InputStatus,
    MdsView,
    NativeWidgetType,
    Values,
} from '../../types/types';
import { Node } from 'ngx-edu-sharing-api';
import { MdsEditorCoreComponent } from '../mds-editor-core/mds-editor-core.component';
import { MdsEditorInstanceService, Widget } from '../mds-editor-instance.service';
import { Attributes, getAttributesArray } from '../util/parse-attributes';
import { MdsEditorWidgetErrorComponent } from '../widgets/mds-editor-widget-error/mds-editor-widget-error.component';
import { MdsEditorWidgetSuggestionChipsComponent } from '../widgets/mds-editor-widget-suggestion-chips/mds-editor-widget-suggestion-chips.component';
import { JumpMark, JumpMarksService } from '../../../../services/jump-marks.service';
import { MdsEditInterface } from '../mds-editor-single-widget/mds-editor-single-widget.component';
import {
    MdsWidgetComponent,
    MdsWidgetType,
    replaceElementWithDiv,
    UIAnimation,
    UIService,
    ViewInstanceService,
} from 'ngx-edu-sharing-ui';

export interface NativeWidgetComponent {
    hasChanges: BehaviorSubject<boolean>;
    onSaveNode?: (nodes: Node[]) => Promise<Node[]>;
    getValues?: (values: Values, node: Node) => Promise<Values>;
    status?: Observable<InputStatus>;
    focus?: () => void;
}

type NativeWidgetClass = {
    constraints: Constraints;
} & Type<NativeWidgetComponent>;

@Component({
    selector: 'es-mds-editor-view',
    templateUrl: './mds-editor-view.component.html',
    styleUrls: ['./mds-editor-view.component.scss'],
    animations: [
        trigger('expandContent', [
            state('collapsed', style({ height: 0, overflow: 'hidden' })),
            transition('collapsed => expanded', [
                animate(UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease', style({ height: '*' })),
            ]),
            transition('expanded => collapsed', [
                style({ overflow: 'hidden' }),
                animate(UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease', style({ height: 0 })),
            ]),
        ]),
    ],
    providers: [ViewInstanceService],
})
export class MdsEditorViewComponent
    implements OnInit, AfterViewInit, OnChanges, OnDestroy, MdsEditInterface
{
    private static readonly nativeWidgets = NativeWidgets;
    private static readonly suggestionWidgetComponents: {
        [type in MdsWidgetType]?: Type<object>;
    } = {
        [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetSuggestionChipsComponent,
        [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetSuggestionChipsComponent,
        [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetSuggestionChipsComponent,
    };

    @HostBinding('class.hidden') isHidden: boolean;
    @ViewChild('container') container: ElementRef<HTMLDivElement>;
    @Input() core: MdsEditorCoreComponent;
    @Input() view: MdsView;
    @Input() overrideWidget?: Type<object>;
    html: SafeHtml;
    isEmbedded: boolean;
    isExpanded: boolean;
    readonly isExpanded$ = this.viewInstance.isExpanded$;

    private knownWidgetTags: string[];
    private destroyed = new ReplaySubject<void>(1);
    private allWidgetsHidden = false;
    private expandContentDone = new Subject<void>();

    constructor(
        private sanitizer: DomSanitizer,
        private factoryResolver: ComponentFactoryResolver,
        private containerRef: ViewContainerRef,
        private applicationRef: ApplicationRef,
        private mdsEditorInstance: MdsEditorInstanceService,
        private ngZone: NgZone,
        private viewInstance: ViewInstanceService,
        private uiService: UIService,
        public injector: Injector,
        @Optional() private jumpMarks: JumpMarksService,
    ) {
        this.isEmbedded = this.mdsEditorInstance.isEmbedded;
        this.knownWidgetTags = [
            ...Object.values(NativeWidgetType),
            ...this.mdsEditorInstance.mdsDefinition$.value.widgets.map((w) => w.id),
        ];
    }

    getWidgets() {
        return (this.mdsEditorInstance.widgets.value as GeneralWidget[])
            .concat(this.mdsEditorInstance.nativeWidgets.value)
            .filter((w) => w.viewId === this.view.id);
    }
    ngOnInit(): void {
        this.html = this.getHtml();
        this.mdsEditorInstance.activeViews
            .pipe(
                takeUntil(this.destroyed),
                map((activeViews) => activeViews.some((view) => view.id === this.view.id)),
            )
            .subscribe((isActive) => (this.isHidden = !isActive));

        this.jumpMarks?.beforeScrollToJumpMark
            .pipe(
                takeUntil(this.destroyed),
                filter((jumpMark) => jumpMark.id === this.view.id + JUMP_MARK_POSTFIX),
            )
            .subscribe((jumpMark) => {
                if (!this.isExpanded$.value) {
                    this.expandAndScrollToTop(jumpMark);
                }
            });
        this.isExpanded$
            .pipe(takeUntil(this.destroyed))
            .subscribe((isExpanded) => (this.isExpanded = isExpanded));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.view) {
            this.isExpanded$.next(!this.view.isExtended);
        }
    }

    ngAfterViewInit(): void {
        // Wait for the change-detection cycle to finish.
        setTimeout(() => this.injectWidgets());
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private expandAndScrollToTop(jumpMark?: JumpMark) {
        const height = this.container.nativeElement.scrollHeight;
        this.isExpanded$.next(true);
        this.jumpMarks.triggerScrollToJumpMark.next({
            jumpMark: jumpMark ?? this.view.id + JUMP_MARK_POSTFIX,
            expandAnimation: {
                height,
                done: this.expandContentDone.pipe(take(1)).toPromise(),
            },
        });
    }

    onDoneExpandContent() {
        this.expandContentDone.next();
    }

    private getHtml(): SafeHtml {
        // Close any known tags and additionally any tags that include a colon (which indicates the
        // user probably meant to define the respective widget) as these would mess up the HTML
        // structure if left unclosed.
        const html = closeTags(
            this.view.html,
            (tagName) => this.knownWidgetTags.includes(tagName) || tagName.includes(':'),
        );
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }

    private injectWidgets(): void {
        const elements = this.container.nativeElement.getElementsByTagName('*');
        for (const element of Array.from(elements)) {
            const tagName = element.localName;

            const widgets = this.mdsEditorInstance.getWidgetsByTagName(tagName, this.view.id);
            if (Object.values(NativeWidgetType).includes(tagName as NativeWidgetType)) {
                const widgetName = tagName as NativeWidgetType;
                // Native widgets don't support dynamic conditions yet and don't necessarily have a
                // `widget` object.
                const WidgetComponent = MdsEditorViewComponent.nativeWidgets[widgetName];
                if (
                    ['inline'].includes(this.mdsEditorInstance.editorMode) &&
                    !WidgetComponent?.constraints?.supportsInlineEditing
                ) {
                    // native widgets not (yet) supported for inline editing
                    continue;
                } else {
                    const attributes = getAttributesArray(this.view.html, widgetName);
                    this.injectNativeWidget(widgets[0], widgetName, element, attributes);
                }
            } else {
                if (widgets.length >= 1) {
                    // Possibly inject multiple widgets to allow dynamic switching via conditions.
                    for (const widget of widgets) {
                        const widgetComponent = this.getWidgetComponent(
                            widget,
                            this.mdsEditorInstance.editorMode,
                        );
                        this.mdsEditorInstance.injectWidget(
                            widget,
                            element,
                            widgetComponent,
                            widgets.length === 1 ? 'replace' : 'append',
                            this,
                            this.container,
                        );
                    }
                    if (element.parentNode) {
                        // remove the dummy element because it is now replaced with div containers
                        element.parentNode.removeChild(element);
                    }
                } else if (this.knownWidgetTags.includes(tagName)) {
                    // The widget is defined, but was disabled due to unmet conditions.
                    continue;
                } else if (tagName.includes(':')) {
                    this.injectMissingWidgetWarning(tagName, element);
                }
            }
        }
    }

    private injectMissingWidgetWarning(widgetName: string, element: Element): void {
        // Property names are no valid names for autonomous custom elements as by the W3C
        // specification.
        console.warn(
            'Widget definition missing in MDS',
            widgetName,
            this.mdsEditorInstance.mdsId,
            this.mdsEditorInstance.widgets.value.map((w) => w.definition),
        );
        element = replaceElementWithDiv(element);
        UIService.injectAngularComponent(
            this.factoryResolver,
            this.containerRef,
            MdsEditorWidgetErrorComponent,
            element,
            {
                widgetName,
                reason: 'Widget definition missing in MDS',
            },
            {},
            this.injector,
        );
    }

    private injectNativeWidget(
        widget: Widget,
        widgetName: NativeWidgetType,
        element: Element,
        attributes: Attributes,
    ): void {
        element = replaceElementWithDiv(element);
        const WidgetComponent = NativeWidgets[widgetName];
        if (!WidgetComponent) {
            this.uiService.injectAngularComponent(
                this.containerRef,
                MdsEditorWidgetErrorComponent,
                element,
                {
                    widgetName,
                    reason: 'Not implemented',
                },
                {},
                this.injector,
            );
            return;
        }
        const constraintViolation = this.violatesConstraints(WidgetComponent.constraints);
        if (constraintViolation) {
            if (
                !WidgetComponent.constraints.onConstraintFailed ||
                WidgetComponent.constraints.onConstraintFailed === 'showError'
            ) {
                this.uiService.injectAngularComponent(
                    this.containerRef,
                    MdsEditorWidgetErrorComponent,
                    element,
                    {
                        widgetName,
                        reason: constraintViolation,
                    },
                    { replace: false },
                    this.injector,
                );
            } else {
                console.info(
                    'Widget ' + widgetName + ' violates constrain: ' + constraintViolation,
                );
            }
            return;
        }
        this.ngZone.runOutsideAngular(() => {
            const nativeWidget = this.uiService.injectAngularComponent(
                this.containerRef,
                WidgetComponent,
                element,
                {
                    widgetName,
                    widget,
                    attributes,
                },
                { replace: false },
                this.injector,
            );
            this.mdsEditorInstance.registerNativeWidget(nativeWidget.instance, this.view.id);
        });
    }

    private getWidgetComponent(widget: Widget, editorMode: EditorMode): Type<object> {
        if (this.overrideWidget) {
            return this.overrideWidget;
        } else if (this.view.rel === 'suggestions') {
            return MdsEditorViewComponent.suggestionWidgetComponents[
                widget.definition.type as MdsWidgetType
            ];
        } else if (
            widget.definition.interactionType === 'None' ||
            editorMode === 'inline' ||
            editorMode === 'viewer'
        ) {
            // if inline editing -> we don't hide any widget so it can be edited
            if (editorMode === 'inline' && widget.definition.interactionType !== 'None') {
                widget.definition.hideIfEmpty = false;
            }
            return MdsWidgetComponent;
        } else {
            return WidgetComponents[widget.definition.type as MdsWidgetType];
        }
    }

    private violatesConstraints(constraints: Constraints): string | null {
        if (constraints.requiresNode === true) {
            if (
                !this.mdsEditorInstance.nodes$.value &&
                !this.mdsEditorInstance.suggestionMetadata$.value
            ) {
                return 'Only supported if a node object is available';
            }
        }
        if (constraints.supportsBulk === false) {
            if (this.mdsEditorInstance.editorBulkMode.isBulk) {
                return 'Not supported in bulk mode';
            }
        }
        return null;
    }

    toggleShow() {
        if (this.isExpanded) {
            this.isExpanded$.next(false);
        } else {
            this.expandAndScrollToTop();
        }
    }
    async injectEditField(mdsWidgetComponent: MdsWidgetComponent, targetElement: Element) {
        const widget = this.mdsEditorInstance.createWidget(
            mdsWidgetComponent.widget.definition,
            this.view.id,
        );
        const widgetComponent = this.getWidgetComponent(widget, 'nodes');
        const injected = this.mdsEditorInstance.injectWidget(
            widget,
            targetElement,
            widgetComponent,
            'replace',
            this,
            this.container,
        );
        widget.initWithNodes(this.mdsEditorInstance.nodes$.value);
        void this.mdsEditorInstance.fetchDisplayValues(widget);
        // timeout to wait for view inflation and set the focus
        await this.applicationRef.tick();
        setTimeout(() => {
            injected.instance.focus();
            injected.instance.onBlur.pipe(first()).subscribe(() => {
                void mdsWidgetComponent.finishEdit(injected.instance, true);
            });
        });
        return injected;
    }

    isInHiddenState() {
        if (this.isHidden) {
            return true;
        }
        if (this.mdsEditorInstance.shouldShowExtendedWidgets$.value) {
            return false;
        }
        return this.getWidgets().filter((w) => !(w as Widget).definition?.isExtended).length === 0;
    }
}

/**
 * Closes any tags satisfying `predicate` and returns the resulting HTML.
 */
function closeTags(html: string, predicate: (tag: string) => boolean): string {
    let index = 0;
    while (true) {
        index = html.indexOf('<', index);
        if (index === -1) {
            break;
        }
        const endIndex = html.indexOf('>', index + 1);
        if (endIndex === -1) {
            throw new Error('Invalid template html: ' + html);
        }
        let tagNameEndIndex = endIndex;
        const tag = html.substring(index, endIndex + 1); // The complete tag, e.g. '<foo bar="baz">'
        const whiteSpaceIndex = tag.search(/\s/);
        if (whiteSpaceIndex !== -1) {
            tagNameEndIndex = index + whiteSpaceIndex;
        }
        const tagName = html.substring(index + 1, tagNameEndIndex); // The tag name, e.g. 'foo'
        if (predicate(tagName)) {
            const htmlProcessed = html.substring(0, endIndex + 1) + '</' + tagName + '>';
            html = htmlProcessed + html.substring(endIndex + 1);
            index = htmlProcessed.length;
        } else {
            index = endIndex + 1;
        }
    }
    return html;
}
