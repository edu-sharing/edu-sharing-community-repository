import {
    Component,
    ElementRef,
    Inject,
    Input,
    OnDestroy,
    signal,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { LoadingScreenService } from '../../../../main/loading-screen/loading-screen.service';
import { Toast } from '../../../../services/toast';
import {
    ConfigurationService,
    Mds,
    NodeList,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestMdsService,
    RestNodeService,
    UIService,
} from '../../../../core-module/core.module';
import { ActivatedRoute, Router } from '@angular/router';
import { LocalEventsService, OptionsHelperDataService } from 'ngx-edu-sharing-ui';
import { takeUntil } from 'rxjs/operators';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { NodePreviewMediaDialogData } from './node-preview-media-dialog-data';
import { RenderHelperService } from '../../../../pages/render-legacy-page/render-helper.service';
import { SharedModule } from '../../../../shared/shared.module';
import { RenderWrapperComponent } from '../../../../pages/render2-page/render-wrapper-component/render-wrapper.component';

@Component({
    selector: 'es-node-preview-media-dialog',
    imports: [SharedModule, RenderWrapperComponent],
    templateUrl: './node-preview-media-dialog.component.html',
    styleUrls: ['./node-preview-media-dialog.component.scss'],
    providers: [RenderHelperService, OptionsHelperDataService],
    standalone: true,
})
export class NodePreviewMediaDialogComponent {
    node = signal<Node>(null);
    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodePreviewMediaDialogData,
        viewContainerRef: ViewContainerRef,
    ) {
        this.node.set(data.node);
    }
}
