import {
    Component,
    ComponentFactoryResolver,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewContainerRef,
} from '@angular/core';
import {
    CollectionReference,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';

@Component({
    selector: 'app-custom-node-list-wrapper',
    template: '',
})
export class CustomNodeListWrapperComponent implements OnInit {
    @Input() nodes: CollectionReference[];
    @Output() clickRow = new EventEmitter<{ node: CollectionReference }>();

    constructor(
        private temporaryStorageService: TemporaryStorageService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private elementRef: ElementRef,
    ) {}

    ngOnInit() {
        this.init();
    }

    private init(): void {
        const customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_LIST_COMPONENT,
        );
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            customNodeListComponent,
            this.elementRef.nativeElement,
            {
                nodes: this.nodes,
                clickRow: (event: { node: CollectionReference }) =>
                    this.clickRow.emit(event),
            },
        );
    }
}
