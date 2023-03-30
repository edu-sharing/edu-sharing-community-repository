import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { ColorHelper, PreferredColor } from '../../../core-module/ui/color-helper';
import { Node } from 'ngx-edu-sharing-api';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { Permission } from '../../../core-module/rest/data-object';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { ActionbarComponent } from '../../../../../projects/edu-sharing-ui/src/lib/actionbar/actionbar.component';
import { MdsViewerComponent } from '../../../features/mds/mds-viewer/mds-viewer.component';

@Component({
    selector: 'es-collection-info-bar',
    templateUrl: 'collection-info-bar.component.html',
    styleUrls: ['collection-info-bar.component.scss'],
})
export class CollectionInfoBarComponent {
    @ViewChild('actionbar') actionbar: ActionbarComponent;
    @ViewChild('mds') mds: MdsViewerComponent;
    @Input() collection: Node;
    @Input() permissions: Permission[];
    @Output() edit = new EventEmitter<void>();

    constructor(private nodeHelper: NodeHelperService) {}

    isBrightColor() {
        return (
            ColorHelper.getPreferredColor(this.collection?.collection?.color) ===
            PreferredColor.White
        );
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
