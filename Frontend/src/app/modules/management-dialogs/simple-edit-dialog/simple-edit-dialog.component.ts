import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {DialogButton, RestConnectorService} from '../../../core-module/core.module';
import {Toast} from '../../../core-ui-module/toast';
import {RestNodeService} from '../../../core-module/core.module';
import {Connector, Node} from '../../../core-module/core.module';
import {ConfigurationService} from '../../../core-module/core.module';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {RestIamService} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {RestConstants} from '../../../core-module/core.module';
import {Router} from '@angular/router';
import {RestHelper} from '../../../core-module/core.module';
import {RestConnectorsService} from '../../../core-module/core.module';
import {FrameEventsService} from '../../../core-module/core.module';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {MdsComponent} from '../../../common/ui/mds/mds.component';
import {SimpleEditMetadataComponent} from './simple-edit-metadata/simple-edit-metadata.component';
import {SimpleEditInviteComponent} from './simple-edit-invite/simple-edit-invite.component';

@Component({
  selector: 'app-simple-edit-dialog',
  templateUrl: 'simple-edit-dialog.component.html',
  styleUrls: ['simple-edit-dialog.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class SimpleEditDialogComponent  {
  @ViewChild('metadata') metadata : SimpleEditMetadataComponent;
  @ViewChild('invite') invite : SimpleEditInviteComponent;
  _nodes: Node[];
  buttons: DialogButton[];
  /**
   * was this dialog called directly after upload
   * if true, the ui will behave a bit differently
   */
  @Input() fromUpload = false;
  @Input() set nodes(nodes : Node[]) {
    this._nodes = nodes;
    this.updateButtons();
  }
  @Output() onCancel=new EventEmitter<void>();
  @Output() onDone=new EventEmitter<void>();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private translate : TranslateService,
    private connectors : RestConnectorsService,
    private config : ConfigurationService,
    private toast : Toast,
    private bridge : BridgeService,
    private events : FrameEventsService,
    private router : Router,
    private nodeApi : RestNodeService) {
      this.updateButtons();
  }
  public cancel() {
    this.onCancel.emit();
  }
    updateButtons(): any {
        this.buttons=[
            new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
            new DialogButton('SAVE',DialogButton.TYPE_PRIMARY,()=>this.save())
        ]
    }

    save() {
      this.toast.showProgressDialog();
        this.metadata.save().subscribe(() => {
          this.invite.save().subscribe(() => {
            this.onDone.emit();
            this.toast.closeModalDialog();
          }, error => {
            this.toast.error(error);
            this.toast.closeModalDialog();
          });
        },error => {
          this.toast.error(error);
          this.toast.closeModalDialog();
      });
    }
}
