import { Component } from '@angular/core';
import {RestArchiveService} from "../../services/rest-archive.service";
import {RestConstants} from "../../rest-constants";
import {RestHelper} from "../../rest-helper";
import {
  ArchiveSearch, CollectionWrapper, CollectionData, MdsMetadatasets, MdsMetadataset,
  MdsValueList
} from "../../data-object";
import {RestMdsService} from "../../services/rest-mds.service";

@Component({
  selector: 'app-rest-mds-test',
  templateUrl: './rest-mds-test.component.html',
})
export class RestMdsTestComponent {
  public getSets : MdsMetadatasets;
  public getSet : MdsMetadataset;
  public getValues : MdsValueList;
  public error : string;
  constructor(mds : RestMdsService) {
    mds.getSets().subscribe(
      data => this.getSets=data,
      error => this.error=RestHelper.printError(error)
    )
    mds.getSet().subscribe(
      data => this.getSet=data,
      error => this.error=RestHelper.printError(error)
    )
    mds.getValues({query:"*",property:"{http://www.campuscontent.de/model/1.0}remotenodeid",pattern:"*"}).subscribe(
      data => this.getValues=data,
      error => this.error=RestHelper.printError(error)
    )
  }

}
