import {RestAdminService} from "../../../core-module/rest/services/rest-admin.service";
import {Component, EventEmitter, Output} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";
import {NodeStatistics, Node, Statistics, IamGroup, Group} from "../../../core-module/rest/data-object";
import {ListItem} from "../../../core-module/ui/list-item";
import {RestConstants} from "../../../core-module/rest/rest-constants";
import {RestHelper} from "../../../core-module/rest/rest-helper";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {ConfigurationService} from "../../../core-module/rest/services/configuration.service";
import {DialogButton, RestConnectorService, RestIamService, RestMediacenterService} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";
import {Toast} from "../../../core-ui-module/toast";
import {OptionItem} from "../../../core-ui-module/option-item";

// Charts.js
declare var Chart:any;

@Component({
  selector: 'app-admin-mediacenter',
  templateUrl: 'mediacenter.component.html',
  styleUrls: ['mediacenter.component.scss']
})
export class AdminMediacenterComponent {
  // @TODO: declare the mediacenter type when it is finalized in backend
  mediacenters: any[];
  // original link to mediacenter object (contained in mediacenters[])
  currentMediacenter:any;
  // copy of the current mediacenter for (temporary) edits
  currentMediacenterCopy:any;

  addGroup: Group;
  mediacenterGroups: IamGroup[];
  groupColumns:ListItem[];
  groupActions:OptionItem[];
  currentTab=0;
  private isAdmin: boolean;
  public mediacentersFile: File;
  public organisationsFile: File;
  public globalProgress=false;


    constructor(
      private mediacenterService: RestMediacenterService,
      private translate: TranslateService,
      private connector: RestConnectorService,
      private iamService: RestIamService,
      private toast: Toast,
  ){
    this.isAdmin=this.connector.getCurrentLogin().isAdmin;
    this.refresh();
    this.groupColumns=[
      new ListItem('GROUP', RestConstants.AUTHORITY_DISPLAYNAME),
      new ListItem('GROUP', RestConstants.AUTHORITY_GROUPTYPE)
    ];
    this.groupActions=[
        new OptionItem('ADMIN.MEDIACENTER.GROUPS.REMOVE','delete',(authority:Group)=>{
          this.toast.showModalDialog('ADMIN.MEDIACENTER.GROUPS.REMOVE_TITLE','ADMIN.MEDIACENTER.GROUPS.REMOVE_MESSAGE',
              DialogButton.getYesNo(()=>this.toast.closeModalDialog(),()=>{
                this.toast.closeModalDialog();
                this.deleteGroup(authority)
              }),true, ()=>this.toast.closeModalDialog(),{name:authority.profile.displayName});
        })
    ];
  }


  setMediacenter(mediacenter:any){
    this.currentMediacenter=mediacenter;
    this.currentMediacenterCopy=Helper.deepCopy(mediacenter);
    this.mediacenterGroups=null;
    if(mediacenter){
      this.mediacenterService.getManagedGroups(mediacenter.authorityName).subscribe((groups)=> {
        this.mediacenterGroups=groups;
      });
    }
  }
  removeCatalog(catalog:any){
    this.currentMediacenterCopy.profile.mediacenter.catalogs.splice(this.currentMediacenterCopy.profile.mediacenter.catalogs.indexOf(catalog),1);
  }
  addCatalog() {
    if(!this.currentMediacenterCopy.profile.mediacenter.catalogs){
      this.currentMediacenterCopy.profile.mediacenter.catalogs=[];
    }
    this.currentMediacenterCopy.profile.mediacenter.catalogs.push({name:'',url:''});
  }
  addMediacenter(){
    this.toast.showInputDialog('ADMIN.MEDIACENTER.ADD_MEDIACENTER_TITLE','ADMIN.MEDIACENTER.ADD_MEDIACENTER_MESSAGE','ADMIN.MEDIACENTER.ADD_MEDIACENTER_LABEL',
        DialogButton.getOkCancel(()=>this.toast.closeModalDialog(),()=>{
          let id=this.toast.dialogInputValue;
          let profile={
              displayName:this.translate.instant('ADMIN.MEDIACENTER.UNNAMED_MEDIACENTER',{id:id}),
              mediacenter:{
                  id:id
              }
          };
          this.toast.showProgressDialog();
          this.mediacenterService.addMediacenter(id,profile).subscribe((result)=>{
            RestHelper.waitForResult(()=>this.mediacenterService.getMediacenters(),(list:any[])=>{
              return list.filter((r)=>Helper.objectEquals(r,result)).length==1;
            },()=>{
              this.toast.closeModalDialog();
              this.toast.toast('ADMIN.MEDIACENTER.CREATED',{name:id});
              this.setMediacenter(null);
              this.refresh();
            });
          },(error:any)=>{
            this.toast.error(error);
            this.toast.closeModalDialog();
          })
        }),
        true,()=>this.toast.closeModalDialog());
  }

  saveChanges() {
    this.toast.showProgressDialog();
    this.mediacenterService.editMediacenter(this.currentMediacenterCopy.authorityName,this.currentMediacenterCopy.profile).subscribe(()=>{
      this.toast.toast('ADMIN.MEDIACENTER.UPDATED',{name:this.currentMediacenterCopy.profile.displayName});
      this.toast.closeModalDialog();
      this.refresh();
    },(error:any)=>{
      this.toast.error(error);
      this.toast.closeModalDialog();
      this.refresh();
    })
  }
  refresh() {
    this.mediacenters=null;
    this.mediacenterService.getMediacenters().subscribe((m)=>{
      this.mediacenters=m.filter((m)=>m.administrationAccess);
    });
  }
  addCurrentGroup() {
    this.toast.showProgressDialog();
    this.mediacenterService.addManagedGroup(this.currentMediacenterCopy.authorityName, this.addGroup.authorityName).subscribe((groups) => {
      this.mediacenterGroups = groups;
      this.toast.toast('ADMIN.MEDIACENTER.GROUPS.ADDED',{name:this.addGroup.profile.displayName});
      this.toast.closeModalDialog();
      this.addGroup=null;
    },(error)=>{
      this.toast.error(error);
      this.toast.closeModalDialog();
    });
  }
  deleteMediacenter(){
    this.toast.showModalDialog('ADMIN.MEDIACENTER.DELETE_TITLE','ADMIN.MEDIACENTER.DELETE_MESSAGE',
        DialogButton.getYesNo(()=>this.toast.closeModalDialog(),()=>{
          this.toast.showProgressDialog();
          this.iamService.deleteGroup(this.currentMediacenter.authorityName).subscribe(()=>{
            this.toast.closeModalDialog();
            this.toast.toast('ADMIN.MEDIACENTER.DELETED',{name:this.currentMediacenterCopy.profile.displayName});
            this.setMediacenter(null);
            this.refresh();
          },(error:any)=>{
            this.toast.error(error);
            this.toast.closeModalDialog();
          })
        }),true,()=>this.toast.closeModalDialog(),{name:this.currentMediacenterCopy.profile.displayName});
  }

  private deleteGroup(authority: Group) {
    this.toast.showProgressDialog();
    this.mediacenterService.removeManagedGroup(this.currentMediacenterCopy.authorityName, authority.authorityName).subscribe((groups) => {
      this.mediacenterGroups = groups;
      this.toast.toast('ADMIN.MEDIACENTER.GROUPS.REMOVED',{name:authority.profile.displayName});
      this.toast.closeModalDialog();
    },(error)=>{
      this.toast.error(error);
      this.toast.closeModalDialog();
    });
  }

    public updateMediacentersFile(event:any){
        this.mediacentersFile=event.target.files[0];
    }

    public updateOrganisationsFile(event:any){
        this.organisationsFile=event.target.files[0];
    }

    public importMediacenters(){
        if(!this.mediacentersFile){
            this.toast.error(null,'ADMIN.MEDIACENTER.IMPORT.CHOOSE_MEDIACENTERS');
            return;
        }
        this.globalProgress=true;
        this.mediacenterService.importMediacenters(this.mediacentersFile).subscribe((data:any)=>{
            this.toast.toast('ADMIN.MEDIACENTER.IMPORT.IMPORTED',{rows:data.rows});
            this.globalProgress=false;
            this.mediacentersFile=null;
        },(error:any)=>{
            this.toast.error(error);
            this.globalProgress=false;
        });
    }

    public importOrganisations(){
        if(!this.organisationsFile){
            this.toast.error(null,'ADMIN.MEDIACENTER.ORGIMPORT.CHOOSE_ORGANISATIONS');
            return;
        }
        this.globalProgress=true;
        this.mediacenterService.importOrganisations(this.organisationsFile).subscribe((data:any)=>{
            this.toast.toast('ADMIN.MEDIACENTER.ORGIMPORT.IMPORTED',{rows:data.rows});
            this.globalProgress=false;
            this.organisationsFile=null;
        },(error:any)=>{
            this.toast.error(error);
            this.globalProgress=false;
        });
    }
}
