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

@Component({
  selector: 'app-simple-edit-invite',
  templateUrl: 'simple-edit-invite.component.html',
  styleUrls: ['simple-edit-invite.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class SimpleEditInviteComponent {
  @ViewChild('orgGroup') orgGroup: MatButtonToggleGroup;
  @ViewChild('globalGroup') globalGroup: MatButtonToggleGroup;
  _nodes: Node[];
  multipleParents: boolean;
  parentPermissions: Permission[];
  parentAuthorities: Authority[] = [];
  organization: {organization: Group, groups?: any};
  /**
   * When true, we know that we only handling with simple permissions
   * this will cause that editing permission will REPLACE the permissions, rather than EXPAND them
   */
  stablePermissionState = false;
  private organizationGroups: string[];
  private globalGroups: Group[]|any = [];
  private nodesPermissions: Permissions[];
  @Input() set nodes (nodes : Node[]) {
    this._nodes = nodes;
    this.prepare();
  }
  @Input() fromUpload : boolean;
  orgValue = 'unset';
  globalValue: any = null;
  constructor(
    private nodeApi : RestNodeService,
    private connector : RestConnectorService,
    private configService : ConfigurationService,
    private iamApi : RestIamService,
    private organizationApi : RestOrganizationService,
    private toast : Toast,
  ) {
    this.configService.get('simpleEdit.organization.groupTypes',
        [RestConstants.GROUP_TYPE_ADMINISTRATORS]).subscribe((data) => this.organizationGroups = data);
    this.configService.get('simpleEdit.globalGroups',
        ['GROUP_ORG_baum',RestConstants.AUTHORITY_EVERYONE]).subscribe((data) => {
          this.loadGlobalGroups(data);
    });
  }
  save() {
    return new Observable<void>((observer) => {
      let authority:Group = null;
      if (this.orgGroup.value) {
        if (this.orgGroup.value === 'unset') {
          console.log('unset');
          // do nothing
        } else if(this.orgGroup.value === 'org') {
          authority = this.organization.organization;
        } else {
          authority = this.organization.groups[this.orgGroup.value];
        }
      } else if(this.globalGroup.value) {
        authority = this.globalGroups.find((g: Group) => g.authorityName === this.globalGroup.value);
      } else {
        console.warn('invalid value for button toggle in simple invite dialog');
      }
      // auth not to set, we can skip tasks
      console.log(authority);
      if (authority == null) {
        observer.next(null);
        observer.complete();
      } else {
        const addPermission = new Permission();
        addPermission.authority = {
          authorityName: authority.authorityName,
          authorityType: authority.authorityType,
        };
        addPermission.permissions = [RestConstants.PERMISSION_CONSUMER];
        Observable.forkJoin(this._nodes.map((n, i) => {
          const permissions = RestHelper.copyAndCleanPermissions(this.nodesPermissions[i].localPermissions.permissions,
              this.nodesPermissions[i].localPermissions.inherited);
          if(this.stablePermissionState) {
            permissions.permissions = [addPermission];
           } else {
            permissions.permissions =
                WorkspaceShareComponent.mergePermissionsWithHighestPermission(permissions.permissions, [addPermission]);
          }
          return this.nodeApi.setNodePermissions(n.ref.id, permissions, false);
        })).subscribe(() => {
          observer.next(null);
          observer.complete();
        }, error => {
          observer.error(error);
          observer.complete();
        });
      }
    });
  }

  private prepare() {
    const parents=Array.from(new Set(this._nodes.map((n) => n.parent.id)));
    this.multipleParents = parents.length > 1;
    console.log(parents);
    if(this.multipleParents) {
      return;
    }
    this.nodeApi.getNodePermissions(parents[0]).subscribe((parent) => {
      this.parentPermissions = parent.permissions.localPermissions.permissions.concat(parent.permissions.inheritedPermissions);
      // filter and distinct them first
      const authorities = Array.from(new Set(this.parentPermissions.
          map((p) => p.authority.authorityName).
          filter((a) => a !== this.connector.getCurrentLogin().authorityName)
      ));
      // now, conver them back to objects
      this.parentAuthorities = authorities.map((a) =>
        this.parentPermissions.find((p) => p.authority.authorityName === a).authority
      );
      console.log(this.parentAuthorities);
    });
    Observable.forkJoin((this._nodes.map((n) => this.nodeApi.getNodePermissions(n.ref.id)))).
      subscribe((permissions) => {
        this.nodesPermissions = permissions.map((p) => p.permissions);
      this.organizationApi.getOrganizations().subscribe((orgs) => {
        if(orgs.organizations.length === 1 || true) {
          this.organization = {
            organization: orgs.organizations[0],
            groups: {}
          };
          this.iamApi.getGroupMembers(this.organization.organization.authorityName,'', RestConstants.AUTHORITY_TYPE_GROUP)
              .subscribe((group) => {
                for (const auth of group.authorities) {
                  this.organization.groups[auth.profile.groupType] = auth;
                }
                console.log(this.organization);
                this.detectPermissionState();
              });
        }
      });
    }, error => {
        this.toast.error(error);
    });
  }

  private loadGlobalGroups(data: string[]) {
    this.globalGroups = [];
    // filter group everyone and handle it seperately
    if(data.find((d) => d === RestConstants.AUTHORITY_EVERYONE)) {
      data = data.filter((d) => d !== RestConstants.AUTHORITY_EVERYONE);
      this.globalGroups.push({
        authorityName: RestConstants.AUTHORITY_EVERYONE,
        authorityType: RestConstants.AUTHORITY_TYPE_EVERYONE
      });
    }
    Observable.forkJoin(data.map((d) => this.iamApi.getGroup(d))).
    subscribe((groups) =>
        this.globalGroups = groups.map((g) => g.group).concat(this.globalGroups)
    );
  }

  updateValue(mode: 'org' | 'global') {
    if(mode === 'org') {
      this.globalGroup.value = null;
    } else {
        this.orgGroup.value = null;
    }
  }

  private detectPermissionState() {
    let group: Authority = null;
    let unset = false;
    let invalid = false;
    for (const perm of this.nodesPermissions) {
      const list = perm.localPermissions.permissions.
                filter((p) => p.authority.authorityName !== this.connector.getCurrentLogin().authorityName);
      if(list.length === 1) {
        if(Helper.arrayEquals(list[0].permissions, [RestConstants.PERMISSION_CONSUMER]) &&
            (group==null || list[0].authority.authorityName === group.authorityName)) {
          group = list[0].authority;
        } else {
          invalid = true;
        }
      } else if(list.length > 0) {
        console.log('node has unmatching permissions for simple invite', list);
        invalid = true;
      } else {
        unset = true;
      }
    }
    this.stablePermissionState = !invalid;
    console.log(unset, invalid, group);
    if (unset || invalid) {
      this.orgGroup.value = 'unset';
    } else {
      if (this.organization) {
          if (group.authorityName === this.organization.organization.authorityName) {
            console.log('set org active');
            this.orgGroup.value = 'org';
            return;
          }
          for (const key of Object.keys(this.organization.groups)) {
            if(group.authorityName === this.organization.groups[key].authorityName) {
              this.orgGroup.value = key;
              return;
            }
          }
      }
      if (this.globalGroups) {
        for (const globalGroup of this.globalGroups) {
          if (group.authorityName === globalGroup.authorityName) {
            this.globalGroup.value = group.authorityName;
            return;
          }
        }
      }
      this.orgGroup.value = 'unset';
    }
  }
}
