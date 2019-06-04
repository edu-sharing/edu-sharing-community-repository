import {Component} from "@angular/core";
import {Collection, RestCollectionService, RestConnectorService} from "../../../core-module/core.module";

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
