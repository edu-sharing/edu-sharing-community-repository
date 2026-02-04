import {
    ApplicationRef,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    EventEmitter,
    Injector,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { MdsEditorInstanceService, Widget } from '../mds-editor-instance.service';
import { EditorMode, MdsWidget } from '../../types/types';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { WidgetComponents } from '../../types/mds-types';
import { MdsService } from 'ngx-edu-sharing-api';
import { first } from 'rxjs/operators';
import { MdsEditorViewComponent } from '../mds-editor-view/mds-editor-view.component';
import { MdsEditorWidgetBase } from '../widgets/mds-editor-widget-base';
import {
    MdsEditorInstanceServiceAbstract,
    MdsViewerService,
    MdsWidgetComponent,
    MdsWidgetType,
    UIService,
    ViewInstanceService,
} from 'ngx-edu-sharing-ui';
import { mapExtendedValues } from '../mds-editor-wrapper/extended-values-mapper';

export interface MdsEditInterface {
    injectEditField(
        mdsWidgetComponent: MdsWidgetComponent,
        targetElement: Element,
    ): Promise<{
        htmlElement: HTMLElement;
        instance: MdsEditorWidgetBase;
    }>;
}

@Component({
    selector: 'es-mds-editor-single-widget',
    templateUrl: './mds-editor-single-widget.component.html',
    styleUrls: ['./mds-editor-single-widget.component.scss'],
    providers: [
        MdsEditorInstanceService,
        { provide: MdsEditorInstanceServiceAbstract, useExisting: MdsEditorInstanceService },
        MdsViewerService,
        ViewInstanceService,
    ],
    standalone: false,
})
export class MdsEditorSingleWidgetComponent implements OnChanges, OnDestroy, MdsEditInterface {
    @ViewChild('widget') ref: ElementRef<HTMLDivElement>;
    @Input() editorMode = 'inline' as EditorMode;
    @Input() ngModel: string[];
    @Output() ngModelChange = new EventEmitter<string[]>();
    @Input() repository = RestConstants.HOME_REPOSITORY;
    @Input() mds = RestConstants.DEFAULT;
    @Input() widgetId: string;
    /**
     * custom attributes to override
     * i.e. {type: 'textarea'}
     *
     * */
    @Input() customAttributes: Partial<MdsWidget>;
    hasExtendedWidgets$: Observable<boolean>;
    readonly EditorMode: EditorMode;
    readonly shouldShowExtendedWidgets$: BehaviorSubject<boolean>;
    private value$: BehaviorSubject<string[]>;
    private destroyed = new Subject<void>();
    private instanceExists: boolean = false;
    private widget: Widget;

    constructor(
        public mdsEditorInstance: MdsEditorInstanceService,
        public mdsService: MdsService,
        private applicationRef: ApplicationRef,
        private factoryResolver: ComponentFactoryResolver,
        private injector: Injector,
        private containerRef: ViewContainerRef,
    ) {}

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (this.mds && this.widgetId && !this.instanceExists) {
            const mdsDefinition = await this.mdsService
                .getMetadataSet({ metadataSet: this.mds, repository: this.repository })
                .toPromise();
            let definition = mdsDefinition.widgets.find(
                (w) => w.id === this.widgetId && !w.template,
            );
            if (this.customAttributes) {
                definition = { ...definition, ...this.customAttributes };
            }
            this.widget = this.mdsEditorInstance.createWidget(definition, null, this.repository);
            /*
             */
            this.mdsEditorInstance.editorMode = this.editorMode;
            this.mdsEditorInstance.values$.next({ [this.widgetId]: this.ngModel });
            if (['inline', 'viewer'].includes(this.editorMode)) {
                const bindings: { [p: string]: any } = {
                    widget: this.widget,
                    showCaption: false,
                    view: this,
                };
                if (this.editorMode === 'inline') {
                    bindings.inlineEditing = 'always';
                }
                UIService.injectAngularComponent(
                    this.factoryResolver,
                    this.containerRef,
                    MdsWidgetComponent,
                    this.ref.nativeElement,
                    bindings,
                    {},
                    this.injector,
                );
            } else {
                setTimeout(
                    () =>
                        void this.injectEditField(
                            this.widget as unknown as MdsWidgetComponent,
                            this.ref.nativeElement,
                        ),
                );
            }
            this.instanceExists = true;
        }
    }
    async injectEditField(mdsWidgetComponent: MdsWidgetComponent, targetElement: Element) {
        const component = WidgetComponents[this.widget.definition.type as MdsWidgetType];
        const injected = this.mdsEditorInstance.injectWidget(
            this.widget,
            targetElement,
            component,
            'replace',
            { injector: this.injector } as MdsEditorViewComponent,
        );
        this.widget.initWithValues({ [this.widgetId]: this.ngModel });
        /*combineLatest([
            this.widget.instance.widget.observeValue(),
            this.widget.instance.widget.observeHasChanged(),
        ]).pipe(takeUntil(this.destroyed), filter(([_, changes]) => changes)).subscribe(([v, _]) => {
            console.log('change', v);
            this.ngModelChange.emit(v)
        });*/
        void this.mdsEditorInstance.fetchDisplayValues(this.widget);
        // timeout to wait for view inflation and set the focus
        await this.applicationRef.tick();
        setTimeout(() => {
            injected.instance.focus();
            if (this.editorMode === 'inline') {
                injected.instance.onBlur.pipe(first()).subscribe(() => {
                    this.ngModelChange.emit(injected.instance.widget.getValue());
                    this.instanceExists = false;
                    void mdsWidgetComponent.finishEdit(injected.instance, false);
                });
            } else {
                this.widget
                    .observeValue()
                    .subscribe((v) => this.ngModelChange.emit(mapExtendedValues(v)));
            }
        });
        return injected;
    }
}
