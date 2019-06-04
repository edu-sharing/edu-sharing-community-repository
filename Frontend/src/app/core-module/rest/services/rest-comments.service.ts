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
      let query=this.connector.createUrl('comment/:version/comments/:repository/:node', repository, [
        [':node',node]
      ]);
      return this.connector.get(query,this.connector.getRequestOptions());
  }
  addComment(node:string,comment:string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> {
    let query = this.connector.createUrl('comment/:version/comments/:repository/:node', repository, [
      [':node', node]
    ]);
    return this.connector.put(query, comment, this.connector.getRequestOptions());
  }
  editComment(comment:string,text:string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> {
    let query = this.connector.createUrl('comment/:version/comments/:repository/:comment', repository, [
      [':comment', comment]
    ]);
    return this.connector.post(query,text, this.connector.getRequestOptions());
  }
  deleteComment(comment:string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> {
    let query = this.connector.createUrl('comment/:version/comments/:repository/:comment', repository, [
      [':comment', comment]
    ]);
    return this.connector.delete(query, this.connector.getRequestOptions());
  }
}
