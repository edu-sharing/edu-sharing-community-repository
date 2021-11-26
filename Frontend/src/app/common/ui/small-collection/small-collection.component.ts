import {Component, Input, OnInit} from '@angular/core';
import {Node} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {
    Collection,
    CollectionWrapper,
    ConfigurationService,
    ListItem,
    RestCollectionService
} from "../../../core-module/core.module";
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';

@Component({
  selector: 'es-small-collection',
  templateUrl: 'small-collection.component.html',
  styleUrls: ['small-collection.component.scss']
})
/**
 * this component uses the same height as the secondary bar height
 * and can be used to display a collection at this position
 */
export class SmallCollectionComponent{
  public _collection : Node;
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
  scopeItem = new ListItem('COLLECTION', 'scope');
  @Input() set collectionId(collectionId : string){
    this.collectionService.getCollection(collectionId).subscribe((data:CollectionWrapper)=>{
      this._collection=data.collection;
    });
  }
  @Input() set collection(collection : Node){
    this._collection=collection;
  }
  constructor(
    private collectionService:RestCollectionService,
    private translate:TranslateService,
    private nodeHelper: NodeHelperService,
    private config:ConfigurationService
  ) { }
}
