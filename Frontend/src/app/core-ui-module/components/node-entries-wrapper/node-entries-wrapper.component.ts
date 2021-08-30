import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ComponentFactoryResolver, ComponentRef, ContentChild, ElementRef, EventEmitter, Input, NgZone,
    OnChanges,
    SimpleChange,
    SimpleChanges, TemplateRef, Type, ViewContainerRef
} from '@angular/core';
import {NodeEntriesService} from '../../node-entries.service';
import {TemporaryStorageService} from '../../../core-module/rest/services/temporary-storage.service';
import {ListTableComponent} from '../list-table/list-table.component';
import {UIHelper} from '../../ui-helper';
import {NodeEntriesComponent} from '../node-entries/node-entries.component';
import {DataSource} from '@angular/cdk/collections';
import {NodeDataSource} from './node-data-source';
import {Node} from '../../../core-module/rest/data-object';
import {ListItem} from '../../../core-module/ui/list-item';
export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid
}
@Component({
    selector: 'app-node-entries-wrapper',
    template: `
        <app-node-entries
            *ngIf="!customNodeListComponent"
        ></app-node-entries>`,
    providers: [
        NodeEntriesService,
    ]
})
export class NodeEntriesWrapperComponent<T extends Node> implements OnChanges {
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    @Input() dataSource: NodeDataSource<T>;
    @Input() columns: ListItem[];
    @Input() displayType = NodeEntriesDisplayType.Grid;
    private componentRef: ComponentRef<any>;
    public customNodeListComponent: Type<NodeEntriesComponent<T>>;

    constructor(
        private temporaryStorageService: TemporaryStorageService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private ngZone: NgZone,
        private entriesService: NodeEntriesService<T>,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        /*
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.emptyRef = this.emptyRef)
        );
        */
    }
    ngOnChanges(changes: { [key: string]: SimpleChange }) {
        if (!this.componentRef) {
            this.init();
        }
        console.log(this.columns);
        this.entriesService.dataSource = this.dataSource;
        this.entriesService.columns = this.columns;
        this.entriesService.displayType = this.displayType;

        if (this.componentRef) {
            this.componentRef.instance.changeDetectorRef?.detectChanges();
        }
    }
    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        this.customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_ENTRIES_COMPONENT,
            null
        );
        if(this.customNodeListComponent == null) {
            return;
        }
        this.componentRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            this.customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings()
        );
    }
    /**
     * Creates a simple map of the output bindings defined in this component.
     */
    private getOutputBindings(): { [key: string]: EventEmitter<any> } {
        const outputBindings: { [key: string]: any } = {};
        for (const key of Object.keys(this)) {
            const value = (this as any)[key];
            if (value instanceof EventEmitter) {
                outputBindings[key] = value;
            }
        }
        return outputBindings;
    }
}
