import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestNodeService, RestSearchService, VCardResult} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper,Node} from "../../../core-module/core.module";
import {VCard} from "../../../core-module/ui/VCard";
import {Toast} from "../../../core-ui-module/toast";
import {Translation} from "../../../core-ui-module/translation";
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../../../core-ui-module/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {debounceTime, filter, startWith, switchMap} from 'rxjs/operators';
import {BehaviorSubject, Observable} from 'rxjs';
import {FormControl} from '@angular/forms';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';

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
  public contributorLifecycle:any={};
  public contributorMetadata:any={};
  public rolesLifecycle=RestConstants.CONTRIBUTOR_ROLES_LIFECYCLE;
  public rolesMetadata=RestConstants.CONTRIBUTOR_ROLES_METADATA;

  private _nodeId: string;
  public loading=true;
  public edit: VCard;
  public editMode: string;
  public editType: number;
  public more = false;
  public persistentIds = false;
  public editScopeNew: string;
  private editScopeOld: string;
  editOriginal: VCard;
  public dialogTitle: string;
  public dialogMessage: string;
  public dialogButtons: DialogButton[];
  public dialogParameters: any;
  public node: Node;
  public date : Date;
  buttons: DialogButton[];
  private editButtons: DialogButton[];
  private static TYPE_PERSON = 0;
  private static TYPE_ORG = 1;
  private fullName = new BehaviorSubject('');
  private orgName = new BehaviorSubject('');
  suggestionPersons$: Observable<VCardResult[]>;
  suggestionOrgs$: Observable<VCardResult[]>;
  editDisabled = false;
  @Input() set nodeId(nodeId : string){
    this._nodeId=nodeId;
    this.loading=true;
    this.nodeService.getNodeMetadata(nodeId,[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
      this.node=data.node;
      for(let role of this.rolesLifecycle){
        this.contributorLifecycle[role]=[];
        let list=data.node.properties[RestConstants.CONTRIBUTOR_LIFECYCLE_PREFIX+role];
        if(!list)
          continue;
        for(let vcard of list){
          if(vcard && new VCard(vcard).isValid())
            this.contributorLifecycle[role].push(new VCard(vcard));
        }
      }
      for(let role of this.rolesMetadata){
        this.contributorMetadata[role]=[];
        let list=data.node.properties[RestConstants.CONTRIBUTOR_METADATA_PREFIX+role];
        if(!list)
          continue;
        for(let vcard of list){
          if(vcard && new VCard(vcard).isValid())
            this.contributorMetadata[role].push(new VCard(vcard));
        }
      }
      this.loading=false;
    });

  }
  @Output() onClose=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  givenname = new FormControl('');
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
  public addVCard(mode = this.editMode) {
    this.date=null;
    this.editType=WorkspaceContributorComponent.TYPE_PERSON;
    this.editMode=mode;
    this.edit=new VCard();
    this.editDisabled = false;
    this.editOriginal=null;
    this.editScopeOld=null;
    this.editScopeNew=this.editMode=='lifecycle' ? this.rolesLifecycle[0] : this.rolesMetadata[0];

  }
  openDatepicker(){
      this.date=new Date();
      //setTimeout(()=>this.datepicker.toggle());
  }
  public editVCard(mode:string,vcard : VCard,scope:string){
    this.editMode=mode;
    this.editOriginal=vcard;
    this.edit=vcard.copy();
    this.editDisabled = !!(vcard.orcid || vcard.gnduri || vcard.ror || vcard.wikidata);
    this.editScopeOld=scope;
    this.editScopeNew=scope;
    this.editType=vcard.givenname||vcard.surname ? WorkspaceContributorComponent.TYPE_PERSON : WorkspaceContributorComponent.TYPE_ORG;
    this.date=null;
    let contributeDate=vcard.contributeDate;
    if(contributeDate) {
      //this.date.formatted=contributeDate;
      //this.dateOptions.initialDate=new Date(contributeDate);
      this.date=new Date(contributeDate);
      /*
      let split=contributeDate.split("-");
      if(split.length==3){
        this.dateOptions.initialDate=new Date(parseInt(split[0]),parseInt(split[1]),parseInt(split[2]),0,0,0,0);
      }
      */
    }
  }
  public saveEdits(){
    if(this.editType==WorkspaceContributorComponent.TYPE_PERSON && (!this.edit.givenname || !this.edit.surname)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_PERSON_NAME');
      return;
    }
    if(this.editType==WorkspaceContributorComponent.TYPE_ORG && (!this.edit.org)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_ORG_NAME');
      return;
    }
    if(this.editType==WorkspaceContributorComponent.TYPE_ORG){
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
    this.edit=null;
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
    this.nodeService.editNodeMetadataNewVersion(this._nodeId,RestConstants.COMMENT_CONTRIBUTOR_UPDATE,properties).subscribe(()=>{
      this.toast.toast('WORKSPACE.TOAST.CONTRIBUTOR_UPDATED');
      this.onClose.emit();
      this.onLoading.emit(false);
    },(error:any)=>{
      this.toast.error(error);
      this.onLoading.emit(false);
    });
  }
  public cancel(){
    if(this.edit!=null){
      this.edit=null;
      return;
    }
    this.onClose.emit();
  }
  public constructor(
    private nodeService:RestNodeService,
    private searchService:RestSearchService,
    private translate:TranslateService,
    private toast:Toast,
  ) {
    this.suggestionPersons$ = this.fullName.pipe(
        startWith(''),
        filter((v) => v !== ''),
        debounceTime(200),
        switchMap((v) => this.searchService.searchContributors(v.trim(), 'PERSON')),
    );
    this.suggestionOrgs$ = this.orgName.pipe(
        startWith(''),
        filter((v) => v !== ''),
        debounceTime(200),
        switchMap((v) => this.searchService.searchContributors(v.trim(), 'ORGANIZATION')),
    );
    this.buttons=[
        new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
        new DialogButton('APPLY',DialogButton.TYPE_PRIMARY,()=>this.saveContributor())
    ];
    this.editButtons=[
        new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
        new DialogButton('APPLY',DialogButton.TYPE_PRIMARY,()=>this.saveEdits())
    ];
  }

    updatePersonSuggestions() {

    }

  setFullName() {
    this.fullName.next(this.edit.givenname + ' ' + this.edit.surname);
  }
  setOrgName() {
    this.orgName.next(this.edit.org);
  }

  useVCardSuggestion(event: MatAutocompleteSelectedEvent) {
    console.log(event);
    this.edit = event.option.value.copy();
    this.editDisabled = true;
  }
}
