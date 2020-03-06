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
import {SimpleEditLicenseComponent} from './simple-edit-license/simple-edit-license.component';
import {Observable} from 'rxjs';

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
  @ViewChild('license') license : SimpleEditLicenseComponent;
  _nodes: Node[];
  buttons: DialogButton[];
  /**
   * was this dialog called directly after upload
   * if true, the ui will behave a bit differently
   */
  @Input() fromUpload = false;
  initState: { license: boolean; metadata: boolean; invite: boolean };
  @Input() set nodes(nodes : Node[]) {
    this._nodes = nodes;
    this.initState = {
      metadata: false,
      invite: false,
      license: false
    };
    this.toast.showProgressDialog()
    this.updateButtons();
  }
  @Output() onCancel=new EventEmitter<void>();
  @Output() onDone=new EventEmitter<Node[]>();
  @Output() onOpenMetadata=new EventEmitter<Node[]>();
  @Output() onOpenInvite=new EventEmitter<Node[]>();
  @Output() onOpenLicense=new EventEmitter<Node[]>();
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

  save(callback: () => void = null) {
    this.toast.showProgressDialog();
    this.metadata.save().subscribe( () => {
      this.invite.save().subscribe(() => {
        this.license.save().subscribe(() => {
          Observable.forkJoin(this._nodes.map((n) => this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL])))
              .subscribe((nodes) => {
                this.onDone.emit(nodes.map((n) => n.node));
                if (callback) {
                  callback();
                }
                this.toast.closeModalDialog();
              });
        }, error => {
          this.toast.error(error);
          this.toast.closeModalDialog();
        });
      },error => {
        this.toast.error(error);
        this.toast.closeModalDialog();
      });
    },error => {
      this.toast.error(error);
      this.toast.closeModalDialog();
    });
  }
  checkIsDirty() {
    console.log('mds dirty: '+this.metadata.isDirty()+' invite: '+this.invite.isDirty()+' license: '+this.license.isDirty());
    return this.metadata.isDirty() || this.invite.isDirty() || this.license.isDirty();
  }
  openDialog(callback: () => void, force = false) {
    if (this.checkIsDirty() && !force) {
      this.showDirtyDialog(() => this.openDialog(callback,true));
      return;
    }
    callback();
    this.onDone.emit();
  }
  openMetadata(force = false) {
    this.openDialog(() => this.onOpenMetadata.emit(this._nodes));
  }
  openInvite(force = false) {
    this.openDialog(() => this.onOpenInvite.emit(this._nodes));
  }
  openLicense(force = false) {
    this.openDialog(() => this.onOpenLicense.emit(this._nodes));
  }

  private showDirtyDialog(callback: () => void) {
    this.toast.showConfigurableDialog({
      title: 'SIMPLE_EDIT.DIRTY.TITLE',
      message: 'SIMPLE_EDIT.DIRTY.MESSAGE',
      isCancelable: true,
      buttons: [
          new DialogButton('DISCARD',DialogButton.TYPE_CANCEL, () => {
            this.toast.closeModalDialog();
            callback();
          }),
          new DialogButton('SAVE',DialogButton.TYPE_PRIMARY, () => {
            this.toast.closeModalDialog();
            this.save(callback);
          }),
      ],
    })
  }
  updateInitState() {
    console.log(this.initState);
    if(this.initState.metadata && this.initState.invite && this.initState.license) {
      this.toast.closeModalDialog();
    }
  }
}
