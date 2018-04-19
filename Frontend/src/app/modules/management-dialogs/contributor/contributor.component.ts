import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {NodeWrapper,Node} from "../../../common/rest/data-object";
import {VCard} from "../../../common/VCard";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {Translation} from "../../../common/translation";
import {TranslateService} from "@ngx-translate/core";
import {DatepickerOptions} from "ng2-datepicker";
import {DateHelper} from "../../../common/ui/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {NgDatepickerComponent} from "ng2-datepicker";

@Component({
  selector: 'workspace-contributor',
  templateUrl: 'contributor.component.html',
  styleUrls: ['contributor.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation()),
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
export class WorkspaceContributorComponent  {
  @ViewChild('datepicker') datepicker : NgDatepickerComponent;
  public contributorLifecycle:any={};
  public contributorMetadata:any={};
  public rolesLifecycle=["publisher","author","unknown","initiator","terminator","validator",
    "editor","graphical_designer","technical_implementer","content_provider",
    "educational_validator","script_writer","instructional_designer","subject_matter_expert"];
  public rolesMetadata=["creator","validator","provider"];

  private _nodeId: string;
  public loading=true;
  public edit: VCard;
  public editMode: string;
  public editType: string;
  public more = false;
  public editScopeNew: string;
  private editScopeOld: string;
  private editOriginal: VCard;
  public dialogTitle: string;
  public dialogMessage: string;
  public dialogButtons: DialogButton[];
  public dialogParameters: any;
  public node: Node;
  public date : Date;
  public dateOptions: DatepickerOptions;
  @Input() set nodeId(nodeId : string){
    this._nodeId=nodeId;
    this.loading=true;
    this.nodeService.getNodeMetadata(nodeId,[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
      this.node=data.node;
      for(let role of this.rolesLifecycle){
        this.contributorLifecycle[role]=[];
        let list=data.node.properties["ccm:lifecyclecontributer_"+role];
        if(!list)
          continue;
        for(let vcard of list){
          if(vcard && new VCard(vcard).isValid())
            this.contributorLifecycle[role].push(new VCard(vcard));
        }
      }
      for(let role of this.rolesMetadata){
        this.contributorMetadata[role]=[];
        let list=data.node.properties["ccm:metadatacontributer_"+role];
        if(!list)
          continue;
        for(let vcard of list){
          if(vcard && new VCard(vcard).isValid())
            this.contributorMetadata[role].push(new VCard(vcard));
        }
      }
      this.loading=false;
      console.log(this.contributorMetadata);
    });

  }
  @Output() onClose=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  public remove(data:any[],pos:number){
    this.dialogTitle='WORKSPACE.CONTRIBUTOR.DELETE_TITLE';
    this.dialogMessage='WORKSPACE.CONTRIBUTOR.DELETE_MESSAGE';
    this.dialogParameters={name:data[pos].getDisplayName()};
    this.dialogButtons=DialogButton.getYesNo(()=>{
      this.dialogTitle=null;
    },()=>{
      data.splice(pos,1);
      this.dialogTitle=null;
    });

  }
  public addVCard(mode:string) {
    this.date=null;
    this.editType='person';
    this.editMode=mode;
    this.edit=new VCard();
    this.editOriginal=null;
    this.editScopeOld=null;
    this.editScopeNew=this.editMode=='lifecycle' ? this.rolesLifecycle[0] : this.rolesMetadata[0];

  }
  openDatepicker(){
      this.date=new Date();
      setTimeout(()=>this.datepicker.toggle());
  }
  public editVCard(mode:string,vcard : VCard,scope:string){
    this.editMode=mode;
    this.editOriginal=vcard;
    this.edit=vcard.copy();
    this.editScopeOld=scope;
    this.editScopeNew=scope;
    this.editType=vcard.givenname||vcard.surname ? 'person' : 'org';
    this.date=null;
    let contributeDate=vcard.contributeDate;
    console.log(contributeDate);
    if(contributeDate) {
      //this.date.formatted=contributeDate;
      //this.dateOptions.initialDate=new Date(contributeDate);
      this.date=new Date(contributeDate);
      /*
      let split=contributeDate.split("-");
      if(split.length==3){
        console.log(split);
        this.dateOptions.initialDate=new Date(parseInt(split[0]),parseInt(split[1]),parseInt(split[2]),0,0,0,0);
      }
      */
    }
  }
  public saveEdits(){
    if(this.editType=='person' && (!this.edit.givenname || !this.edit.surname)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_PERSON_NAME');
      return;
    }
    if(this.editType=='org' && (!this.edit.org)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_ORG_NAME');
      return;
    }
    if(this.editType=='org'){
      this.edit.givenname='';
      this.edit.surname='';
      this.edit.title='';
    }
    this.edit.contributeDate=this.date ? DateHelper.getDateFromDatepicker(this.date).toISOString() : null;
    let array=this.editMode=='lifecycle' ? this.contributorLifecycle : this.contributorMetadata;
    if(this.editScopeOld) {
      let pos = array[this.editScopeOld].indexOf(this.editOriginal);
      array[this.editScopeOld].splice(pos, 1);
      if(this.editScopeOld==this.editScopeNew){
        array[this.editScopeOld].splice(pos,0,this.edit);
      }
      else{
        array[this.editScopeNew].push(this.edit);
      }
    }
    else{
      array[this.editScopeNew].push(this.edit);
    }
    console.log(array);
    this.edit=null;
  }
  public setTab(tab:string){
    this.editType=tab;
  }
  public saveContributor(){
    this.onLoading.emit(true);
    let properties:any={};
    for(let role of this.rolesLifecycle){
      let prop=[];
      for(let vcard of this.contributorLifecycle[role]){
        prop.push(vcard.toVCardString());
      }
      properties["ccm:lifecyclecontributer_"+role]=prop;
    }
    for(let role of this.rolesMetadata){
      let prop=[];
      for(let vcard of this.contributorMetadata[role]){
        prop.push(vcard.toVCardString());
      }
      properties["ccm:metadatacontributer_"+role]=prop;
    }

    console.log(properties);
    this.nodeService.editNodeMetadata(this._nodeId,properties).subscribe(()=>{
      this.toast.toast('WORKSPACE.TOAST.CONTRIBUTOR_UPDATED');
      this.onClose.emit();
      this.onLoading.emit(false);
    },(error:any)=>{
      this.toast.error(error);
      this.onLoading.emit(false);
    });
  }
  public cancel(){
    this.onClose.emit();
  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private toast:Toast,
  ){
    this.dateOptions={};
    //this.dateOptions.format="DD.MM.YYYY";
    Translation.applyToDateOptions(this.translate,this.dateOptions);

  }

}
