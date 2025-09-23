import {
    Component,
    ComponentFactoryResolver,
    ElementRef,
    Injector,
    Input,
    OnChanges,
    QueryList,
    signal,
    SimpleChanges,
    ViewChildren,
    ViewContainerRef,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
    InitialValues,
    MdsValueList,
    MdsViewerWidget,
    MdsWidgetComponent,
} from './widget/mds-widget.component';
import {
    HOME_REPOSITORY,
    MdsDefinition,
    MdsService,
    MdsView,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { Values } from '../services/search-helper.service';
import { replaceElementWithDiv } from './replace-element-with-div';
import { UIService } from '../services/ui.service';
import { MdsViewerService } from './mds-viewer.service';
import { ViewInstanceService } from './view-instance.service';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-mds-viewer',
    templateUrl: 'mds-viewer.component.html',
    styleUrls: ['mds-viewer.component.scss'],
    providers: [MdsViewerService, ViewInstanceService],
    standalone: false,
})
export class MdsViewerComponent implements OnChanges {
    @ViewChildren('container') container: QueryList<ElementRef>;

    @Input() mdsEditorInstanceService: any;
    @Input() groupId: string;
    @Input() setId: string;
    @Input() data: Values;
    @Input() mds: MdsDefinition;
    templates = signal<
        {
            view: MdsView;
            html: SafeHtml;
        }[]
    >(null);

    /**
     * show group headings (+ icons) for the individual templates
     */
    @Input() showGroupHeadings = true;

    /**
     * The heading level from 1 to 6 to use for widget labels, equivalent to `h1` to `h6`.
     *
     * If not set, widget labels are not marked as headings and an invisible colon is added between
     * labels and values, that will be read out by screen readers.
     */
    @Input()
    set headingLevel(value: number | null) {
        this.viewInstance.headingLevel = value;
    }
    get headingLevel() {
        return this.viewInstance.headingLevel;
    }

    constructor(
        private mdsService: MdsService,
        private mdsViewerService: MdsViewerService,
        private factoryResolver: ComponentFactoryResolver,
        private injector: Injector,
        private containerRef: ViewContainerRef,
        private sanitizer: DomSanitizer,
        private viewInstance: ViewInstanceService,
    ) {}

    getGroup() {
        return this.mds.groups.find((g: any) => g.id == this.groupId);
    }
    getView(id: string) {
        return this.mds.views.find((v: any) => v.id == id);
    }

    public async inflate() {
        if (!this.mds) {
            setTimeout(() => this.inflate(), 1000 / 60);
            return;
        }
        try {
            this.mdsViewerService.values$.next(this.data);
            this.mdsViewerService.mds$.next(this.mds);
            if (this.mdsEditorInstanceService) {
                const editor = await this.mdsEditorInstanceService.initWithoutNodes(
                    this.groupId,
                    this.setId,
                    HOME_REPOSITORY,
                    'viewer',
                    this.data,
                );
                if (!editor) {
                    // Initialization was interrupted. Probably, this method was called again before it
                    // could finish.
                    return;
                }
            }
        } catch (e) {
            return;
        }
        const templates = [];
        for (const view of this.getGroup().views) {
            const v = this.getView(view);
            templates.push({
                view: v,
                html: this.sanitizer.bypassSecurityTrustHtml(this.prepareHTML(v.html)),
            });
        }
        this.templates.set(templates);
        // wait for angular to inflate the new binding
        setTimeout(() => {
            for (const widget of (this.mdsEditorInstanceService?.widgets.value ||
                this.mds.widgets.map((definition) => {
                    return {
                        definition,
                        getInitalValuesAsync: async () => {
                            return {
                                jointValues: this.data[definition.id!!],
                            } as InitialValues;
                        },
                        getInitialDisplayValues: () => new BehaviorSubject<MdsValueList>(null),
                    } as MdsViewerWidget;
                })) as MdsViewerWidget[]) {
                this.container.toArray().forEach((c) => {
                    let element: HTMLElement = c.nativeElement.getElementsByTagName(
                        widget.definition.id,
                    )?.[0];
                    if (element) {
                        // MdsEditorViewComponent.updateWidgetWithHTMLAttributes(element, w);
                        element = replaceElementWithDiv(element);

                        UIService.injectAngularComponent(
                            this.factoryResolver,
                            this.containerRef,
                            MdsWidgetComponent,
                            element,
                            {
                                widget,
                            },
                            {},
                            this.injector,
                        );
                    }
                    setTimeout(() => this.hideEmpty(c), 1);
                });
            }
        });
    }

    /**
     * close all custom tags inside the html which are not closed
     * e.g. <cm:name>
     *     -> <cm:name></cm:name>
     * @param html
     */
    private prepareHTML(html: string) {
        for (const w of this.mds.widgets) {
            const start = html.indexOf('<' + w.id);
            if (start == -1) {
                continue;
            }
            const end = html.indexOf('>', start) + 1;
            html = html.substring(0, end) + '</' + w.id + '>' + html.substring(end);
        }
        return html;
    }

    async ngOnChanges(changes: SimpleChanges) {
        let inflate = false;
        if (changes.setId) {
            this.mds = await this.mdsService
                .getMetadataSet({ metadataSet: this.setId })
                .toPromise();
            inflate = true;
        }
        if (changes.data) {
            if (this.data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] != null) {
                this.mds = await this.mdsService
                    .getMetadataSet({
                        metadataSet:
                            this.data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET][0],
                    })
                    .toPromise();
            } else if (!this.mds) {
                this.mds = await this.mdsService.getMetadataSet({}).toPromise();
            }
            inflate = true;
        }
        if (inflate) {
            void this.inflate();
        }
    }

    private hideEmpty(c: ElementRef) {
        for (let emptyGroup of c.nativeElement.getElementsByTagName('hideifempty')) {
            console.log(emptyGroup);
            if (!emptyGroup.getElementsByTagName('hideifempty-content')?.[0]?.innerText?.trim()) {
                emptyGroup.parentElement.removeChild(emptyGroup);
            }
        }
    }
}
