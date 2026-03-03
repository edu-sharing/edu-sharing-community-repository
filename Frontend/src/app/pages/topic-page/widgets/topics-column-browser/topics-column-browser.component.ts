import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    input,
    Input,
    InputSignal,
    Output,
    signal,
    WritableSignal,
} from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { CollectionEntries, Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { scrollIntoView } from '../../shared/utils/dom-util';
import { retrieveNodeId } from '../../shared/utils/template-util';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { LifecycleDirective } from './lifecycle.directive';
import { RemoteTreeDataSource, TreeNode } from './remote-tree-data-source';
import { WrapObservablePipe } from './wrap-observable.pipe';

@Component({
    selector: 'es-topics-column-browser',
    imports: [
        EduSharingUiCommonModule,
        EduSharingUiModule,
        LifecycleDirective,
        MatListModule,
        SharedModule,
        WidgetConfigurationButtonsComponent,
        WrapObservablePipe,
    ],
    templateUrl: './topics-column-browser.component.html',
    styleUrls: ['./topics-column-browser.component.scss'],
})
export class TopicsColumnBrowserComponent implements WidgetComponentInterface {
    // CONSTANTS
    private readonly MOBILE_WIDTH: number = 860;

    // INPUTS + OUTPUTS
    @Input() contextNodeId!: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() @HostBinding('style.height') height?: string;
    @Input() pageVariantNode?: Node;
    @Input() sidebarEmbedding: boolean = false;
    @Input() swimlaneIndex: number = -1;

    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    customUrl: (node: Node) => string;
    readonly dataSource: RemoteTreeDataSource<Node> = new RemoteTreeDataSource<Node>();
    initialized: WritableSignal<boolean> = signal(false);
    path: TreeNode<Node>[] = [];
    updateInProgress: WritableSignal<boolean> = signal(false);
    private width$: BehaviorSubject<number> = new BehaviorSubject<number>(window.innerWidth);

    @HostListener('window:resize') onResize(): void {
        this.width$.next(window.innerWidth);
    }

    constructor(
        private readonly elementRef: ElementRef<HTMLElement>,
        private topicPageGlobalService: TopicPageGlobalService,
        private topicPageHelperService: TopicPageHelperService,
    ) {
        if (this.topicPageGlobalService.getCustomUrlFunction()) {
            this.customUrl = this.topicPageGlobalService.getCustomUrlFunction();
        }
    }

    /**
     * Checks whether the current view is a mobile view.
     */
    isMobile(): Observable<boolean> {
        return this.width$.pipe(map((width: number): boolean => width < this.MOBILE_WIDTH));
    }

    /**
     * Reacts to a list option being selected.
     *
     * @param value
     */
    onSelectedChange(value: TreeNode<Node> | null): void {
        if (value) {
            this.path = [...this.path.slice(0, value.level), value];
        }
    }

    /**
     * Scrolls the view to the left.
     */
    scrollLeft(): void {
        this.elementRef.nativeElement.scroll({ left: 0, behavior: 'smooth' });
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Preloads and populates the list of sub-collections.
     */
    async preLoadAction(): Promise<void> {
        this.path = [];
        this.dataSource.setFetchChildNodes((nodeId: string) => {
            return this.topicPageHelperService
                .getSubcollections(nodeId ?? this.contextNodeId, true)
                .pipe(
                    map((nodes: CollectionEntries) =>
                        nodes.collections
                            .filter(
                                (node: Node): boolean =>
                                    !node.properties?.[
                                        RestConstants.CCM_PROP_IO_EDITORIAL_STATE
                                    ]?.includes('deactivated'),
                            )
                            .map((node: Node) => ({ id: retrieveNodeId(node), data: node })),
                    ),
                );
        });
    }

    protected readonly scrollIntoView = scrollIntoView;
}
