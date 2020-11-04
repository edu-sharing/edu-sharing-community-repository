import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {DialogButton, RestConnectorService} from "../../../../core-module/core.module";
import {Toast} from "../../../../core-ui-module/toast";
import {RestNodeService} from "../../../../core-module/core.module";
import {Connector, Node} from "../../../../core-module/core.module";
import {ConfigurationService} from "../../../../core-module/core.module";
import {UIHelper} from "../../../../core-ui-module/ui-helper";
import {RestIamService} from "../../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../../core-module/ui/ui-animation";
import {RestConstants} from '../../../../core-module/core.module';
import {Router} from '@angular/router';
import {RestHelper} from '../../../../core-module/core.module';
import {RestConnectorsService} from "../../../../core-module/core.module";
import {FrameEventsService} from "../../../../core-module/core.module";
import {NodeHelper} from "../../../../core-ui-module/node-helper";
import {OPEN_URL_MODE} from "../../../../core-module/ui/ui-constants";
import {BridgeService} from '../../../../core-bridge-module/bridge.service';
import {BulkBehaviour, MdsComponent} from '../../../../common/ui/mds/mds.component';
import {BehaviorSubject, Observable, Observer} from 'rxjs';

@Component({
  selector: 'app-simple-edit-metadata',
  templateUrl: 'simple-edit-metadata.component.html',
  styleUrls: ['simple-edit-metadata.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class SimpleEditMetadataComponent  {
  readonly BulkBehaviour = BulkBehaviour;
  @ViewChild('mds') mds : MdsComponent;
  @Input() nodes : Node[];
  @Input() fromUpload : boolean;
  @Output() onError = new EventEmitter<void>();
  isInited = new BehaviorSubject(false);

  constructor(
    private nodeApi : RestNodeService,
    private toast : Toast,
  ) {
  }
  isDirty(){
    return this.mds.isDirty();
  }
  save() {
    return new Observable<void>((observer) => {
      if (!this.isDirty()) {
        observer.next();
        observer.complete();
        return;
      }
      Observable.forkJoin(this.nodes.map((n) => {
        const props = this.mds.getValues(n.properties);
        delete props[RestConstants.CM_NAME];
        return this.nodeApi.editNodeMetadataNewVersion(n.ref.id, RestConstants.COMMENT_METADATA_UPDATE, props);
      })).subscribe(() => {
        observer.next();
        observer.complete();
      }, (error) => {
        observer.error(error);
        observer.complete();
      })
    });
  }
}
