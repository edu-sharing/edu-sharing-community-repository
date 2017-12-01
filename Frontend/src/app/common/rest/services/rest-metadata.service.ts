import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import { NodeRef, NodeWrapper, NodePermissions, LocalPermissions, NodeVersions, NodeVersion, NodeList} from "../data-object";
import {RequestObject} from "../request-object";

@Injectable()
export class RestMetadataService {

  constructor(private connector : RestConnectorService) {}

    getMetadataValues(metadataset:string, query:string, prop: string, pattern: string, repository = RestConstants.HOME_REPOSITORY): Observable<any> {
        let q=this.connector.createUrl('mds/:version/metadatasets/:repository/'+metadataset+'/values', repository, []);
        let body = '{"query": "'+query+'","property": "'+prop+'","pattern": "'+pattern+'"}';
        return this.connector.post(q,body,this.connector.getRequestOptions()) .map((response: Response) => response.json());
    }
  

  
}
