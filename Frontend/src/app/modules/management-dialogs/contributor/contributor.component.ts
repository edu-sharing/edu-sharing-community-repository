import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestIamService, RestNodeService, RestSearchService, VCardResult} from "../../../core-module/core.module";
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
  selector: 'es-workspace-contributor',
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

  public loading=true;
  public edit: VCard;
  public editMode: string;
  public editType: number | string; // FIXME: weird type
  public more = false;
  public showPersistentIds = false;
  public editScopeNew: string;
  private editScopeOld: string;
  editOriginal: VCard;
  _node: Node;
  @Input() set node (node: Node){
      this._node = node;
      for(let role of this.rolesLifecycle){
          this.contributorLifecycle[role]=[];
          let list=node.properties[RestConstants.CONTRIBUTOR_LIFECYCLE_PREFIX+role];
          if(!list)
              continue;
          for(let vcard of list){
              if(vcard && new VCard(vcard).isValid())
                  this.contributorLifecycle[role].push(new VCard(vcard));
          }
      }
      for(let role of this.rolesMetadata){
          this.contributorMetadata[role]=[];
          let list=node.properties[RestConstants.CONTRIBUTOR_METADATA_PREFIX+role];
          if(!list)
              continue;
          for(let vcard of list){
              if(vcard && new VCard(vcard).isValid())
                  this.contributorMetadata[role].push(new VCard(vcard));
          }
      }
      this.loading=false;
  }
  public date : Date;
  buttons: DialogButton[];
  editButtons: DialogButton[];
  private fullName = new BehaviorSubject('');
  private orgName = new BehaviorSubject('');
  suggestionPersons$: Observable<VCardResult[]>;
  suggestionOrgs$: Observable<VCardResult[]>;
  editDisabled = false;
  @Input() set nodeId(nodeId : string){
    this.loading=true;
    this.nodeService.getNodeMetadata(nodeId,[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
      this.node=data.node;
    });

  }
  @Output() onClose=new EventEmitter<Node>();
  @Output() onLoading=new EventEmitter();
  givenname = new FormControl('');
  userAuthor = false;
  public remove(data:any[],pos:number){
      this.toast.showConfigurableDialog({
          title: 'WORKSPACE.CONTRIBUTOR.DELETE_TITLE',
          message: 'WORKSPACE.CONTRIBUTOR.DELETE_MESSAGE',
          messageParameters: {name:data[pos].getDisplayName()},
          isCancelable: true,
          buttons: DialogButton.getYesNo(()=>{
              this.toast.closeModalDialog();
          },()=>{
              data.splice(pos,1);
              this.toast.closeModalDialog();
          })
      });
  }
  public resetVCard(){
      this.date = null;
      this.edit=new VCard();
      this.editType=this.edit.getType();
      this.editDisabled = false;
  }
  public addVCard(mode = this.editMode) {
    this.resetVCard();
    this.userAuthor = false;
    this.editMode=mode;
    this.editOriginal=null;
    this.editScopeOld=null;
    this.editScopeNew=this.editMode=='lifecycle' ? this.rolesLifecycle[0] : this.rolesMetadata[0];

  }
  openDatepicker(){
      this.date=new Date();
      //setTimeout(()=>this.datepicker.toggle());
  }
  public async editVCard(mode: string, vcard: VCard, scope: string) {
      this.editMode = mode;
      this.editOriginal = vcard;
      this.edit = vcard.copy();
      this.editDisabled = !!(vcard.orcid || vcard.gnduri || vcard.ror || vcard.wikidata);
      this.editScopeOld = scope;
      this.editScopeNew = scope;
      this.editType = vcard.getType();
      if (vcard.uid === (await this.iamService.getCurrentUserVCard()).uid) {
          this.userAuthor = true;
          this.editDisabled = true;
      }
      this.date = null;
      let contributeDate = vcard.contributeDate;
      if (contributeDate) {
          //this.date.formatted=contributeDate;
          //this.dateOptions.initialDate=new Date(contributeDate);
          this.date = new Date(contributeDate);
          /*
          let split=contributeDate.split("-");
          if(split.length==3){
            this.dateOptions.initialDate=new Date(parseInt(split[0]),parseInt(split[1]),parseInt(split[2]),0,0,0,0);
          }
          */
      }
  }
  public saveEdits(){
    if(this.editType==VCard.TYPE_PERSON && (!this.edit.givenname || !this.edit.surname)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_PERSON_NAME');
      return;
    }
    if(this.editType==VCard.TYPE_ORG && (!this.edit.org)){
      this.toast.error(null,'WORKSPACE.CONTRIBUTOR.ERROR_ORG_NAME');
      return;
    }
    if(this.editType==VCard.TYPE_ORG){
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
    this.nodeService.editNodeMetadataNewVersion(this._node.ref.id,RestConstants.COMMENT_CONTRIBUTOR_UPDATE,properties).subscribe(({node})=>{
      this.toast.toast('WORKSPACE.TOAST.CONTRIBUTOR_UPDATED');
      this.onClose.emit(node);
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
    private iamService:RestIamService,
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
    this.edit = event.option.value.copy();
    this.editDisabled = true;
  }

  async setVCardAuthor(set: boolean) {
      if (set) {
          this.edit = await this.iamService.getCurrentUserVCard();
          this.userAuthor = true;
          this.editDisabled = true;
      } else {
          this.resetVCard();
      }

  }
}
