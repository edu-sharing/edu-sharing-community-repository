import { Component } from '@angular/core';
import {RestArchiveService} from "../../services/rest-archive.service";
import {RestConstants} from "../../rest-constants";
import {RestHelper} from "../../rest-helper";
import {ArchiveSearch} from "../../data-object";

@Component({
  selector: 'app-rest-archive-test',
  templateUrl: './rest-archive-test.component.html',
})
export class RestArchiveTestComponent {
  public data : ArchiveSearch;
  public error : string;
  constructor(archive : RestArchiveService) {
    archive.search("*").subscribe(
      data => this.data=data,
      error => this.error=RestHelper.printError(error)
    );
    /*
     archive.delete(["e166f3db-9891-416a-97b7-c3c53b7a457a"]).subscribe(
     data => null,
     error => this.error=RestHelper.printError(error)
     );
     */
    archive.restore(["edfdb68b-afa5-48b1-8154-0616dc63b4c6"]).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );

  }

}
