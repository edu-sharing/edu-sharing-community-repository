import {
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChange,
    ViewContainerRef,
} from '@angular/core';
import { ActionbarComponent } from '../../../common/ui/actionbar/actionbar.component';
import { MainNavComponent } from '../../../common/ui/main-nav/main-nav.component';
import {
    Node,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import {OptionItem} from "../../../core-ui-module/option-item";

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
    selector: 'app-custom-node-list-wrapper',
    template: '',
})
export class CustomNodeListWrapperComponent implements OnChanges {
    @Input() parent: Node;
    @Input() nodes: Node[];
    @Input() hasMore: boolean;
    @Input() isLoading: boolean;
    @Input() mainNav: MainNavComponent;
    @Input() actionbar: ActionbarComponent;
    @Input() optionItems: OptionItem[];

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
        private elementRef: ElementRef,
    ) {}

    ngOnChanges(changes: { [key: string]: SimpleChange }) {
        if (!this.componentRef) {
            this.init();
        }
        // Pass changes to the wrapped custom-node-list component and trigger an
        // update.
        for (const key in changes) {
            this.componentRef.instance[key] = changes[key].currentValue;
        }
        if (this.componentRef.instance.ngOnChanges) {
            this.componentRef.instance.ngOnChanges(changes);
        }
    }

    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        const customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_LIST_COMPONENT,
        );
        this.componentRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings(),
        );
    }

    /**
     * Creates a simple map of the output bindings defined in this component.
     */
    private getOutputBindings(): { [key: string]: EventEmitter<any> } {
        const outputBindings: { [key: string]: EventEmitter<any> } = {};
        for (const key in this) {
            const value = this[key];
            if (value instanceof EventEmitter) {
                outputBindings[key] = value;
            }
        }
        return outputBindings;
    }
}
