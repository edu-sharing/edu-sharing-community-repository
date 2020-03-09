import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {Authority, DialogButton, Group, LocalPermissions, Permission, Permissions, RestConnectorService, RestOrganizationService} from '../../../../core-module/core.module';
import {Toast} from '../../../../core-ui-module/toast';
import {RestNodeService} from '../../../../core-module/core.module';
import {Connector, Node} from '../../../../core-module/core.module';
import {ConfigurationService} from '../../../../core-module/core.module';
import {UIHelper} from '../../../../core-ui-module/ui-helper';
import {RestIamService} from '../../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../../core-module/ui/ui-animation';
import {RestConstants} from '../../../../core-module/core.module';
import {Router} from '@angular/router';
import {RestHelper} from '../../../../core-module/core.module';
import {RestConnectorsService} from '../../../../core-module/core.module';
import {FrameEventsService} from '../../../../core-module/core.module';
import {NodeHelper} from '../../../../core-ui-module/node-helper';
import {OPEN_URL_MODE} from '../../../../core-module/ui/ui-constants';
import {BridgeService} from '../../../../core-bridge-module/bridge.service';
import {BulkBehaviour, MdsComponent} from '../../../../common/ui/mds/mds.component';
import {Observable, Observer} from 'rxjs';
import {MatButtonToggleGroup} from '@angular/material/button-toggle';
import {WorkspaceShareComponent} from '../../../workspace/share/share.component';
import {Helper} from '../../../../core-module/rest/helper';
import {CollectionChooserComponent} from '../../../../core-ui-module/components/collection-chooser/collection-chooser.component';
import {VCard} from '../../../../core-module/ui/VCard';

@Component({
  selector: 'app-simple-edit-license',
  templateUrl: 'simple-edit-license.component.html',
  styleUrls: ['simple-edit-license.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class SimpleEditLicenseComponent {
  @ViewChild('modeGroup') modeGroup: MatButtonToggleGroup;
  @ViewChild('licenseGroup') licenseGroup: MatButtonToggleGroup;
  @Input() fromUpload : boolean;
  @Output() onInitFinished = new EventEmitter<void>();
  @Output() onError = new EventEmitter<any>();

  _nodes: Node[];
  private allowedLicenses: string[];
  authorFreetext: string;
  invalid: boolean;
  wasInvalid: boolean;
  private initialLicense: string;
  private initalAuthorFreetext: string;
  private initialMode: string;
  @Input() set nodes (nodes : Node[]) {
    this._nodes = nodes;
    this.prepare(true);
  }
  constructor(
    private nodeApi : RestNodeService,
    private connector : RestConnectorService,
    private configService : ConfigurationService,
    private iamApi : RestIamService,
    private organizationApi : RestOrganizationService,
    private toast : Toast,
  ) {
    // just for init
    this.iamApi.getUser().subscribe(() => {
      if(!this.getESUID()){
        console.warn('Current user has no esuid, detecting owner of license is impossible');
      }
    });
    this.configService.get('simpleEdit.licenses',['NONE', 'COPYRIGHT_FREE', 'CC_BY', 'CC_0'])
        .subscribe((licenses) => this.allowedLicenses = licenses);
  }
  getESUID() {
    return this.iamApi.getCurrentUserVCard().uid;
  }
  isDirty() {
    // state is untouched -> so not dirty
    if (this.invalid) {
      return false;
    }
    // when was initialy invalid -> the invalid was changed, so it is touched
    if (this.wasInvalid) {
      return true;
    }
    return this.initialLicense !== this.licenseGroup.value ||
           this.initalAuthorFreetext !== this.authorFreetext ||
           this.initialMode !== this.modeGroup.value;
  }
  save() : Observable<any> {
    if (!this.isDirty()) {
      return new Observable<void>((observer) => {
        observer.next();
        observer.complete();
        return;
      });
    }
    const properties = this.getProperties();
    return Observable.forkJoin(this._nodes.map((n, i) => {
      return this.nodeApi.editNodeMetadataNewVersion(n.ref.id, RestConstants.COMMENT_LICENSE_UPDATE, properties);
    }));
  }

  private prepare(updateInvalid = false) {
    Observable.forkJoin(this._nodes.map((n) => this.nodeApi.getNodeMetadata(n.ref.id,[RestConstants.ALL])))
        .subscribe((nodes) => {
          this._nodes = nodes.map((n) => n.node);
          const license = NodeHelper.getValueForAll(this._nodes, RestConstants.CCM_PROP_LICENSE, null, 'NONE',false);
          this.authorFreetext = NodeHelper.getValueForAll(this._nodes, RestConstants.CCM_PROP_AUTHOR_FREETEXT, '', '',false);
          const vcard = new VCard(NodeHelper.getValueForAll(this._nodes, RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR, '', '',false));
          console.log(license);
          let isValid = true;
          if(license) {
            if (license.startsWith('CC_BY')) {
              const version = NodeHelper.getValueForAll(this._nodes, RestConstants.CCM_PROP_LICENSE_CC_VERSION, null, null, false);
              isValid = version === '4.0';
            }
            if(this.allowedLicenses.indexOf(license) === -1) {
              isValid = false;
            }
          } else {
            isValid = false;
          }
          this.initialLicense = license;
          this.initalAuthorFreetext = this.authorFreetext;
          setTimeout(()=> {
            if(updateInvalid) {
              this.invalid = !this.fromUpload && !isValid;
              this.wasInvalid = this.invalid;
            }
            if (this.fromUpload) {
              this.modeGroup.value = 'own';
            } else {
              if (this.getESUID() && this.getESUID() === vcard.uid) {
                this.modeGroup.value = 'own';
              } else {
                this.modeGroup.value = 'foreign';
              }
            }
            this.initialMode = this.modeGroup.value;
            if (isValid) {
              this.licenseGroup.value = license;
            } else {
              this.licenseGroup.value = 'NONE';
            }
            this.onInitFinished.emit();
          });
        }, error => this.onError.emit(error));

  }

  private getProperties() {
    const properties:any = {};
    if (this.modeGroup.value === 'own') {
      const vcard = this.iamApi.getCurrentUserVCard();
      properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = [vcard.toVCardString()];
      properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = null;
    } else {
      properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = null;
      properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [this.authorFreetext];
    }
    properties[RestConstants.CCM_PROP_LICENSE] = [this.licenseGroup.value];
    properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION]  = this.licenseGroup.value === 'CC_BY' ? ['4.0'] : null;
    return properties;
  }
}
