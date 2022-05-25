import {
    ApplicationRef,
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    ComponentRef, ContentChild,
    ElementRef,
    EventEmitter, Injector,
    Input, NgZone,
    OnChanges,
    Output,
    SimpleChange, SkipSelf, TemplateRef,
    ViewContainerRef,
} from '@angular/core';
import { ActionbarComponent } from '../../../common/ui/actionbar/actionbar.component';
import { MainNavComponent } from '../../../main/navigation/main-nav/main-nav.component';
import {
    ListItem,
    Node,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import { UIHelper } from '../../ui-helper';
import {CustomOptions, OptionItem, Scope} from '../../option-item';
import {DropData} from "../../directives/drag-nodes/drag-nodes";
import {ListTableComponent} from "../list-table/list-table.component";

export interface CustomNodeListWrapperInterface {
    // Inputs
    parent: Node;
    nodes: Node[];
    hasMore: boolean;
    isLoading: boolean;
    mainNav: MainNavComponent;
    actionbar: ActionbarComponent;
    // Outputs
    clickRow: EventEmitter<{ node: Node }>;
    loadMore: EventEmitter<null>;
    requestRefresh: EventEmitter<null>;
}

/**
 * Replaces itself with a configurable custom-node-list component.
 *
 * The custom-node-list component is configured by setting
 * `TemporaryStorageService.CUSTOM_NODE_LIST_COMPONENT` to its class. The
 * configured component class must implement `CustomNodeListWrapperInterface`.
 *
 * Any new input / output parameters have to be added to 1) this wrapper, 2)
 * `CustomNodeListWrapperInterface`, and 3) the configured component.
 */
@Component({
    selector: 'es-custom-node-list-wrapper',
    template: '',
})
export class CustomNodeListWrapperComponent implements OnChanges {
    @ContentChild('itemContent') itemContentRef: TemplateRef<any>;
    @Input() parent: Node;
    @Input() nodes: Node[];
    @Output() nodesChange =new EventEmitter<Node[]>();
    @Input() hasMore: boolean;
    @Input() hasHeading = true;
    @Input() dragDrop = false;
    @Input() hasIcon: boolean;
    @Input() orderElements = false;
    @Input() orderElementsActive = false;
    @Output() orderElementsActiveChange = new EventEmitter();
    @Input() columns: ListItem[];
    @Input() scope: Scope;
    @Input() createLink = false;
    @Input() isClickable: boolean;
    @Input() hasCheckbox: boolean;
    @Input() isLoading: boolean;
    @Input() canDrop: (arg0: DropData) => boolean = () => true;
    @Input() canDelete: (node: Node | any) => boolean;


    @Input() actionbar: ActionbarComponent;
    @Input() optionItems: OptionItem[];
    @Input() customOptions: CustomOptions;
    @Input() viewType: 0 | 1 | 2;
    @Input() validatePermissions: (
        node: Node,
    ) => {
        status: boolean;
        message?: string;
        button?: {
            click: () => void;
            caption: string;
            icon: string;
        };
    };
    @Input() sortBy: string[];
    @Input() sortAscending = true;


    @Output() onRequestRefresh = new EventEmitter<void>();
    @Output() onDelete = new EventEmitter();
    @Output() doubleClickRow = new EventEmitter();
    @Output() onDrop = new EventEmitter<{
        target: Node;
        source: Node[];
        event: any;
        type: 'move' | 'copy';
    }>();
    @Output() clickRow = new EventEmitter<{ node: Node }>();
    @Output() loadMore = new EventEmitter<null>();
    @Output() requestRefresh = new EventEmitter<null>();

    /**
     * The wrapped custom-node-list component.
     *
     * The actual instance is accessible via `componentRef.instance`.
     */
    private componentRef: ComponentRef<any>;

    constructor(
        private temporaryStorageService: TemporaryStorageService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private ngZone: NgZone,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.itemContentRef = this.itemContentRef)
        );
    }

    ngOnChanges(changes: { [key: string]: SimpleChange }) {
        if (!this.componentRef) {
            this.init();
        }
        // Pass changes to the wrapped custom-node-list component and trigger an
        // update.
        // tslint:disable-next-line:forin
        for (const key in changes) {
            this.componentRef.instance[key] = changes[key].currentValue;
        }
        if (this.componentRef.instance.ngOnChanges) {
            this.componentRef.instance.ngOnChanges(changes);
        }
        // attach the template ref
        this.componentRef.instance.itemContentRef = this.itemContentRef;
        // force change detection on list table component
        this.componentRef.instance.changeDetectorRef?.detectChanges();
    }
    getListTable(): ListTableComponent {
        return this.componentRef?.instance;
    }

    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        const customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_LIST_COMPONENT,
            ListTableComponent
        );
        this.componentRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings());
    }

    /**
     * Creates a simple map of the output bindings defined in this component.
     */
    private getOutputBindings(): { [key: string]: EventEmitter<any> } {
        const outputBindings: { [key: string]: any } = {};
        for (const key in this) {
            const value = this[key];
            if (value instanceof EventEmitter) {
                outputBindings[key] = value;
            }
        }
        return outputBindings;
    }
}
