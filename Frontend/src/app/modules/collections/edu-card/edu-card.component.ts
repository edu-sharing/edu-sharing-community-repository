import {Component, OnInit, Input, EventEmitter, Output} from '@angular/core';

import {TranslateService, TranslatePipe} from 'ng2-translate/ng2-translate';

import * as EduData from "../../../common/rest/data-object";
import { RestConstants } from "../../../common/rest/rest-constants";
import { RestHelper } from "../../../common/rest/rest-helper";
import {NodeHelper} from "../../../common/ui/node-helper";
import {VCard} from "../../../common/VCard";

@Component({
  selector: 'edu-card',
  templateUrl: 'edu-card.component.html',
  styleUrls: ['edu-card.component.scss']
})
export class EduCardComponent implements OnInit {

  // possible input - one must be given
  @Input() node:EduData.Node;
  @Input() reference:EduData.CollectionReference;
  @Input() collection:EduData.Collection;
  @Output() onClick=new EventEmitter();
  @Output() onDelete=new EventEmitter();
  private isCollectionReference:boolean = false;
  private isCollection:boolean = false;
  private isContent:boolean = false;
  private isFolder:boolean = false;
  public isNewCollectionPlaceholder:boolean = false;

  constructor(private translationService:TranslateService) {
    if (typeof this.node == "undefined") this.node = null;
    if (typeof this.reference == "undefined") this.reference = null;
    if ((this.node==null) && (this.reference!=null)) {
      this.node.preview.url = this.reference.previewUrl;
      this.node = this.reference.reference;
    }
  }
  click(){
    this.onClick.emit();
  }
  delete(event:any){
    this.onDelete.emit();
    event.stopPropagation();
  }
  originalDeleted(){
    return this.reference && !this.reference.originalId;
  }
  isAllowedToDelete() : boolean {
    if (RestHelper.hasAccessPermission(this.reference,'Delete')) return true;
    return false;
  }

  getCollectionPrivacyScope():string {
    if (this.collection==null) return RestConstants.COLLECTIONSCOPE_ALL;
    return this.collection.scope;
    //return RestHelper.getPrivacyScope(this.collection);
  }

  getName() : string {
    return RestHelper.getTitle(this.node);
  }

  getCreatorName() : string {
    if(!this.node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR])
      return "";
    let vcard=new VCard(this.node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR][0]);
    if(!vcard.isValid())
      return "";
    return vcard.getDisplayName();
    //return RestHelper.getCreatorName(this.node);
  }

  getCreateTime() : string {
    return RestHelper.getModifiedTime(this.translationService,this.node);
  }
  getLicense() : string {
    return NodeHelper.getLicenseHtml(this.translationService,this.node);
  }

  ngOnInit() {

    // if collection reference is input - use original as node
    if (this.reference!=null) {
      this.isCollectionReference = true;
      this.node = this.reference.reference;
    }

    // check input data and decide to be a collection, content or a folder
    if (this.collection!=null) {
      // its a collection
      if (this.collection.ref!=null) {
          this.isCollection = true;

          // default values for collection
          if ((typeof this.collection.color == "undefined") || (this.collection.color==null)) this.collection.color = "#759CB7";

      } else {
        this.isNewCollectionPlaceholder = true;
      }
    } else
    if (this.node!=null) {

      if (RestHelper.isContentItem(this.node)) {
          // its content
          this.isContent = true;
      } else
      if (RestHelper.isFolder(this.node)) {
          // its a folder
          this.isFolder = true;
      } else {
        console.warn("EduCard: not supported node data type: "+JSON.stringify(this.node));
      }
    } else {
      console.warn('EduCard: has no input data');
    }

  }

}
