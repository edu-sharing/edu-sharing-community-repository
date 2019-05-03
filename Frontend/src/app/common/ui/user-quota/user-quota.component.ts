import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {RestIamService} from "../../rest/services/rest-iam.service";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {Node, NodeList, IamUsers, IamUser, CollectionContent, UserQuota} from "../../rest/data-object";
import {RestConstants} from "../../rest/rest-constants";
import {RestCollectionService} from "../../rest/services/rest-collection.service";
import {Toast} from "../toast";
import {ListItem} from "../list-item";
import {UIHelper} from '../ui-helper';

@Component({
  selector: 'user-quota',
  templateUrl: 'user-quota.component.html',
  styleUrls: ['user-quota.component.scss'],
})
/**
 * A quota info component
 */
export class UserQuotaComponent{
  @Input() quota : UserQuota;

    /**
     * returns the "health" of the available space
     * 0 = good
     * 1 = medium
     * 2 = bad
     */
  getHealth(){
    const fac=this.quota.sizeCurrent / this.quota.sizeQuota;
    if(fac<0.75)
      return 0;
    if(fac<0.9)
      return 1;
    return 2;
  }
}
