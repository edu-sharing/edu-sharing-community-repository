import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    inject,
} from '@angular/core';
import { Node, NodeService, NodeStats } from 'ngx-edu-sharing-api';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { Permission } from '../../../core-module/rest/data-object';
import { NodeHelperService } from '../../../services/node-helper.service';
import {
    ActionbarComponent,
    ColorHelper,
    MdsViewerComponent,
    PreferredColor,
} from 'ngx-edu-sharing-ui';
import { MdsEditorInstanceService } from '../../../features/mds/mds-editor/mds-editor-instance.service';

@Component({
    selector: 'es-collection-info-bar',
    templateUrl: 'collection-info-bar.component.html',
    styleUrls: ['collection-info-bar.component.scss'],
    providers: [MdsEditorInstanceService],
    standalone: false,
})
export class CollectionInfoBarComponent implements OnChanges {
    private nodeHelper = inject(NodeHelperService);
    private nodeService = inject(NodeService);
    mdsEditorInstanceService = inject(MdsEditorInstanceService);

    @ViewChild('actionbar') actionbar: ActionbarComponent;
    @ViewChild('mds') mds: MdsViewerComponent;
    @Input() collection: Node;
    @Input() permissions: Permission[];
    @Output() edit = new EventEmitter<void>();
    stats: NodeStats;

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.collection?.currentValue) {
            if (this.collection.access.includes(RestConstants.ACCESS_CHANGE_PERMISSIONS)) {
                this.stats = await this.nodeService.getStats(this.collection.ref.id).toPromise();
            }
        }
    }

    hasNonIconPreview(): boolean {
        const preview = this.collection?.preview;
        return preview && !preview.isIcon;
    }

    isAllowedToEditCollection() {
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_WRITE);
    }

    getScopeInfo() {
        return this.nodeHelper.getCollectionScopeInfo(this.collection);
    }
}
