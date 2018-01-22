import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  NodeRef, NodeWrapper, NodePermissions, LocalPermissions, NodeVersions, NodeVersion, NodeList,
  Comments
} from "../data-object";
import {RequestObject} from "../request-object";

@Injectable()
export class RestCommentsService {

  constructor(private connector : RestConnectorService) {}

    getComments(node:string,repository = RestConstants.HOME_REPOSITORY): Observable<Comments> {
        let q=this.connector.createUrl('comment/:version/comments/:repository/:node', repository, [
          [':node',node]
        ]);
        return this.connector.get(q,this.connector.getRequestOptions()) .map((response: Response) => response.json());
    }



}
