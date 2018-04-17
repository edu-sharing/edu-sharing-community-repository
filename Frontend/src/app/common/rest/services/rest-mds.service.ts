import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  ArchiveRestore, ArchiveSearch, Node, MdsMetadatasets, MdsMetadataset, MdsValues,
  MdsValueList
} from "../data-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestMdsService extends AbstractRestService{
    constructor(connector : RestConnectorService) {
        super(connector);
    }
  public getSets = (repository=RestConstants.HOME_REPOSITORY): Observable<MdsMetadatasets> => {
    let query=this.connector.createUrl("mds/:version/metadatasetsV2/:repository",repository);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());

  }
  public getSet = (metadataset=RestConstants.DEFAULT,repository=RestConstants.HOME_REPOSITORY): Observable<MdsMetadataset> => {
    let query=this.connector.createUrl("mds/:version/metadatasetsV2/:repository/:metadataset",repository,[[":metadataset",metadataset]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public getValues = (values : MdsValues,metadataset=RestConstants.DEFAULT,repository=RestConstants.HOME_REPOSITORY): Observable<MdsValueList> => {
    let query=this.connector.createUrl("mds/:version/metadatasetsV2/:repository/:metadataset/values",repository,[[":metadataset",metadataset]]);
    return this.connector.post(query,JSON.stringify(values),this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }

}
