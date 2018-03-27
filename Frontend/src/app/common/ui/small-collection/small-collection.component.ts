import {Component, Input, OnInit} from '@angular/core';
import {Collection, CollectionWrapper} from "../../rest/data-object";
import {RestCollectionService} from "../../rest/services/rest-collection.service";
import {ConfigurationService} from "../../services/configuration.service";
import {ListItem} from "../list-item";
import {NodeHelper} from "../node-helper";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'small-collection',
  templateUrl: 'small-collection.component.html',
  styleUrls: ['small-collection.component.scss']
})
/**
 * this component uses the same height as the secondary bar height
 * and can be used to display a collection at this position
 */
export class SmallCollectionComponent{
  public _collection : Collection;
  /**
   * Custom title rendering. Use {{title}}in your string to replace it with the title
   * @type {string}
   */
  @Input() titleLabel:string;
  /**
   * Custom title rendering for mobile / small layout. Use {{title}}in your string to replace it with the title
   * @type {string}
   */
  @Input() titleLabelShort:string;
  @Input() set collectionId(collectionId : string){
    this.collectionService.getCollection(collectionId).subscribe((data:CollectionWrapper)=>{
      this._collection=data.collection;
    });
  }
  @Input() set collection(collection : Collection){
    this._collection=collection;
  }
  constructor(
    private collectionService:RestCollectionService,
    private translate:TranslateService,
    private config:ConfigurationService
  ) { }
  public getAttribute(attribute:string){
    return NodeHelper.getAttribute(this.translate,this.config,this._collection,new ListItem('COLLECTION',attribute));
  }
}
