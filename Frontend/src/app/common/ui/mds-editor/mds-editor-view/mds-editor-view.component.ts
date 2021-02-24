import {
    AfterViewInit,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    Input, OnChanges,
    OnInit, SimpleChanges,
    Type,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BehaviorSubject } from 'rxjs';
import { Node } from '../../../../core-module/core.module';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { MdsEditorInstanceService, Widget } from '../mds-editor-instance.service';
import {
    Constraints,
    InputStatus,
    MdsEditorWidgetComponent,
    MdsView,
    MdsWidgetType,
    NativeWidgetType,
    Values,
} from '../types';
import { MdsEditorWidgetAuthorComponent } from '../widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { MdsEditorWidgetCheckboxComponent } from '../widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import { MdsEditorWidgetCheckboxesComponent } from '../widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import { MdsEditorWidgetChildobjectsComponent } from '../widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import { MdsEditorWidgetChipsComponent } from '../widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import { MdsEditorWidgetDurationComponent } from '../widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import { MdsEditorWidgetErrorComponent } from '../widgets/mds-editor-widget-error/mds-editor-widget-error.component';
import { MdsEditorWidgetLicenseComponent } from '../widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import { MdsEditorWidgetLinkComponent } from '../widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import { MdsEditorWidgetPreviewComponent } from '../widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import { MdsEditorWidgetRadioButtonComponent } from '../widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import { MdsEditorWidgetSelectComponent } from '../widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import { MdsEditorWidgetSliderComponent } from '../widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import { MdsEditorWidgetSuggestionChipsComponent } from '../widgets/mds-editor-widget-suggestion-chips/mds-editor-widget-suggestion-chips.component';
import { MdsEditorWidgetTextComponent } from '../widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import { MdsEditorWidgetTreeComponent } from '../widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import { MdsEditorWidgetVersionComponent } from '../widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import { MdsEditorWidgetFileUploadComponent } from '../widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../../core-module/ui/ui-animation';

export interface NativeWidget {
    hasChanges: BehaviorSubject<boolean>;
    onSaveNode?: (nodes: Node[]) => Promise<Node[]>;
    getValues?: (values: Values, node: Node) => Promise<Values>;
    getStatus?: () => InputStatus;
    focus?: () => void;
}
type NativeWidgetClass = {
    constraints: Constraints;
} & Type<NativeWidget>;

@Component({
    selector: 'app-mds-editor-view',
    templateUrl: './mds-editor-view.component.html',
    styleUrls: ['./mds-editor-view.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_NORMAL))],
})
export class MdsEditorViewComponent implements OnInit, AfterViewInit, OnChanges {
    private static readonly nativeWidgets: {
        [widgetType in NativeWidgetType]: NativeWidgetClass;
    } = {
        preview: MdsEditorWidgetPreviewComponent,
        author: MdsEditorWidgetAuthorComponent,
        version: MdsEditorWidgetVersionComponent,
        childobjects: MdsEditorWidgetChildobjectsComponent,
        maptemplate: MdsEditorWidgetLinkComponent,
        license: MdsEditorWidgetLicenseComponent,
        fileupload: MdsEditorWidgetFileUploadComponent,
        workflow: null as null,
    };
    private static readonly widgetComponents: {
        [type in MdsWidgetType]: MdsEditorWidgetComponent;
    } = {
        [MdsWidgetType.Text]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Number]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Email]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Date]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Month]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Color]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Textarea]: MdsEditorWidgetTextComponent,
        [MdsWidgetType.Checkbox]: MdsEditorWidgetCheckboxComponent,
        [MdsWidgetType.RadioHorizontal]: MdsEditorWidgetRadioButtonComponent,
        [MdsWidgetType.RadioVertical]: MdsEditorWidgetRadioButtonComponent,
        [MdsWidgetType.CheckboxHorizontal]: MdsEditorWidgetCheckboxesComponent,
        [MdsWidgetType.CheckboxVertical]: MdsEditorWidgetCheckboxesComponent,
        [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetChipsComponent,
        [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetChipsComponent,
        [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetChipsComponent,
        [MdsWidgetType.Singleoption]: MdsEditorWidgetSelectComponent,
        [MdsWidgetType.Slider]: MdsEditorWidgetSliderComponent,
        [MdsWidgetType.Range]: MdsEditorWidgetSliderComponent,
        [MdsWidgetType.Duration]: MdsEditorWidgetDurationComponent,
        [MdsWidgetType.SingleValueTree]: MdsEditorWidgetTreeComponent,
        [MdsWidgetType.MultiValueTree]: MdsEditorWidgetTreeComponent,
        [MdsWidgetType.DefaultValue]: null,
    };
    private static readonly suggestionWidgetComponents: {
        [type in MdsWidgetType]?: Type<object>;
    } = {
        [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetSuggestionChipsComponent,
        [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetSuggestionChipsComponent,
        [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetSuggestionChipsComponent,
    };

    @ViewChild('container') container: ElementRef<HTMLDivElement>;
    @Input() view: MdsView;
    html: SafeHtml;
    isEmbedded: boolean;

    private knownWidgetTags: string[];
    show = true;

    constructor(
        private sanitizer: DomSanitizer,
        private factoryResolver: ComponentFactoryResolver,
        private containerRef: ViewContainerRef,
        private mdsEditorInstance: MdsEditorInstanceService,
    ) {
        this.isEmbedded = this.mdsEditorInstance.isEmbedded;
        this.knownWidgetTags = [
            ...Object.values(NativeWidgetType),
            ...this.mdsEditorInstance.mdsDefinition$.value.widgets.map((w) => w.id),
        ];
    }

    ngOnInit(): void {
        this.html = this.getHtml();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if(this.view) {
            this.show = !this.view.isExtended;
        }
    }
    ngAfterViewInit(): void {
        // Wait for the change-detection cycle to finish.
        setTimeout(() => this.injectWidgets());
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
            const widget = this.mdsEditorInstance.getWidgetByTagName(tagName, this.view.id);
            if (Object.values(NativeWidgetType).includes(tagName as NativeWidgetType)) {
                const widgetName = tagName as NativeWidgetType;
                this.injectNativeWidget(widget, widgetName, element);
            } else {
                if (widget) {
                    this.injectWidget(widget, element);
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
        UIHelper.injectAngularComponent(
            this.factoryResolver,
            this.containerRef,
            MdsEditorWidgetErrorComponent,
            element,
            {
                widgetName,
                reason: 'Widget definition missing in MDS',
            },
        );
    }

    private injectNativeWidget(
        widget: Widget,
        widgetName: NativeWidgetType,
        element: Element,
    ): void {
        const WidgetComponent = MdsEditorViewComponent.nativeWidgets[widgetName];
        if (!WidgetComponent) {
            UIHelper.injectAngularComponent(
                this.factoryResolver,
                this.containerRef,
                MdsEditorWidgetErrorComponent,
                element,
                {
                    widgetName,
                    reason: 'Not implemented',
                },
            );
            return;
        }
        const constraintViolation = this.violatesConstraints(WidgetComponent.constraints);
        if (constraintViolation) {
            UIHelper.injectAngularComponent(
                this.factoryResolver,
                this.containerRef,
                MdsEditorWidgetErrorComponent,
                element,
                {
                    widgetName,
                    reason: constraintViolation,
                },
            );
            return;
        }
        const nativeWidget = UIHelper.injectAngularComponent(
            this.factoryResolver,
            this.containerRef,
            WidgetComponent,
            element,
            {
                widgetName,
                widget,
            },
        );
        this.mdsEditorInstance.registerNativeWidget(nativeWidget.instance);
    }

    private injectWidget(widget: Widget, element: Element): void {
        this.updateWidgetWithHTMLAttributes(widget);
        this.mdsEditorInstance.updateWidgetDefinition();
        const WidgetComponent = this.getWidgetComponent(widget);
        if (WidgetComponent === undefined) {
            UIHelper.injectAngularComponent(
                this.factoryResolver,
                this.containerRef,
                MdsEditorWidgetErrorComponent,
                element,
                {
                    widgetName: widget.definition.caption,
                    reason: `Widget for type ${widget.definition.type} is not implemented`,
                },
            );
            return;
        } else if (WidgetComponent === null) {
            return;
        }
        UIHelper.injectAngularComponent(
            this.factoryResolver,
            this.containerRef,
            WidgetComponent,
            element,
            {
                widget,
            },
        );
    }

    private getWidgetComponent(widget: Widget): Type<object> {
        if (this.view.rel === 'suggestions') {
            return MdsEditorViewComponent.suggestionWidgetComponents[widget.definition.type];
        } else {
            return MdsEditorViewComponent.widgetComponents[widget.definition.type];
        }
    }

    /**
     * Overrides widget definition using inline parameters.
     */
    private updateWidgetWithHTMLAttributes(widget: Widget): void {
        const htmlRef = this.container.nativeElement.querySelector(
            widget.definition.id.replace(':', '\\:'),
        );
        if (htmlRef) {
            for (let attribute of htmlRef.getAttributeNames()) {
                // map the extended attribute
                const value = htmlRef.getAttribute(attribute);
                if (attribute === 'isextended' || attribute === 'extended') {
                    attribute = 'isExtended';
                } else if(attribute === 'bottomcaption') {
                    attribute = 'bottomCaption';
                } else if(attribute === 'defaultmin') {
                    attribute = 'defaultMin';
                } else if(attribute === 'defaultmax') {
                    attribute = 'defaultMax';
                }
                (widget.definition as any)[attribute] = value;
            }
        }
    }

    private violatesConstraints(constraints: Constraints): string | null {
        if (constraints.requiresNode === true) {
            if (!this.mdsEditorInstance.nodes$.value) {
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
