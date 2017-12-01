import { Component } from '@angular/core';
import {RestArchiveService} from "../../services/rest-archive.service";
import {RestConstants} from "../../rest-constants";
import {RestHelper} from "../../rest-helper";
import {ArchiveSearch, CollectionWrapper, CollectionData} from "../../data-object";
import {RestCollectionService} from "../../services/rest-collection.service";

@Component({
  selector: 'app-rest-collection-test',
  templateUrl: './rest-collection-test.component.html',
})
export class RestCollectionTestComponent {
  public getCollection : CollectionWrapper;
  public error : string;
  constructor(collection : RestCollectionService) {

    /*
    collection.getCollection("test").subscribe(
      data => {console.log(data);this.getCollection=data},
      error => this.error=RestHelper.printError(error)
    );
    */

  }

}
