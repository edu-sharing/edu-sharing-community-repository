import {Component} from "@angular/core";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {Collection} from "../../rest/data-object";
import {RestCollectionService} from "../../rest/services/rest-collection.service";
import {RestConnectorService} from "../../rest/services/rest-connector.service";

@Component({
  selector: 'app-mds-test',
  templateUrl: './mds-test.component.html',
})
export class MdsTestComponent {
  public collections: Array<Collection>;
  constructor(private collectionsService:RestCollectionService,private connector:RestConnectorService){
    connector.login("admin","admin").subscribe(()=>{
      collectionsService.search("*").subscribe((list)=>{
        console.log(list);
        this.collections=list.collections;
      });
    });

  }
}
