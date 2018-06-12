import { Injectable } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  ArchiveRestore, ArchiveSearch, Node, NodeList, IamGroup, IamGroups, IamAuthorities, GroupProfile,
  IamUsers, IamUser, UserProfile, UserCredentials
} from "../data-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestIamService extends AbstractRestService{
    constructor(connector : RestConnectorService) {
        super(connector);
    }
  public searchAuthorities = (pattern="*",global=true,groupType:string="",request : any = null,repository=RestConstants.HOME_REPOSITORY): Observable<IamAuthorities> => {
    let query=this.connector.createUrlNoEscape("iam/:version/authorities/:repository?pattern=:pattern&global=:global&groupType=:groupType&:request",repository,[
      [":pattern",encodeURIComponent(pattern)],
      [":global",global+""],
      [":groupType",encodeURIComponent(groupType)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public searchGroups = (pattern="*",global=true,groupType="",request : any = null,repository=RestConstants.HOME_REPOSITORY): Observable<IamGroups> => {
    let query=this.connector.createUrlNoEscape("iam/:version/groups/:repository?pattern=:pattern&global=:global&groupType=:groupType&:request",repository,[
      [":pattern",encodeURIComponent(pattern)],
      [":global",global+""],
      [":groupType",encodeURIComponent(groupType)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public getGroup = (group : string,repository=RestConstants.HOME_REPOSITORY): Observable<IamGroup> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group",repository,[[":group",group]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public deleteGroup = (group : string,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group",repository,[[":group",group]]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  public createGroup = (group : string,profile : GroupProfile,parent="",repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group?parent=:parent",repository,[
      [":group",group],
      [":parent",parent]
    ]);
    return this.connector.post(query,JSON.stringify(profile),this.connector.getRequestOptions());
  }
  public editGroup = (group : string,profile : GroupProfile,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group/profile",repository,[[":group",group]]);
    return this.connector.put(query,JSON.stringify(profile),this.connector.getRequestOptions());
  }
  public getGroupMembers = (group : string,pattern="",authorityType="",request : any = null,repository=RestConstants.HOME_REPOSITORY): Observable<IamAuthorities> => {
    let query=this.connector.createUrlNoEscape("iam/:version/groups/:repository/:group/members/?pattern=:pattern&authorityType=:authorityType&:request",repository,[
      [":group",encodeURIComponent(group)],
      [":pattern",encodeURIComponent(pattern)],
      [":authorityType",encodeURIComponent(authorityType)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public deleteGroupMember = (group : string,member : string,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group/members/:member",repository,
      [
        [":group",group],
        [":member",member]
      ]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  public addGroupMember = (group : string,member : string,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/groups/:repository/:group/members/:member",repository,
      [
        [":group",group],
        [":member",member]
      ]);
    return this.connector.put(query,"",this.connector.getRequestOptions());
  }
  public searchUsers = (pattern="*",global=true,request:any=null,repository=RestConstants.HOME_REPOSITORY): Observable<IamUsers> => {
    let query=this.connector.createUrlNoEscape("iam/:version/people/:repository?pattern=:pattern&global=:global&:request",repository,[
      [":pattern",encodeURIComponent(pattern)],
      [":global",global+""],
      [":request",this.connector.createRequestString(request)]
    ]);
    console.log(query);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public deleteUser = (user : string,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user",repository,[[":user",user]]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  public getNodeList = (list : string,request:any=null,user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<NodeList> => {
    let query=this.connector.createUrlNoEscape("iam/:version/people/:repository/:user/nodeList/:list?:request",repository,
      [
        [":user",encodeURIComponent(user)],
        [":list",encodeURIComponent(list)],
        [":request",this.connector.createRequestString(request)],
      ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public removeNodeList = (list : string,node:string,user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/nodeList/:list/:node",repository,
      [
        [":user",user],
        [":list",list],
        [":node",node],
      ]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  public addNodeList = (list : string,node:string,user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/nodeList/:list/:node",repository,
      [
        [":user",user],
        [":list",list],
        [":node",node],
      ]);
    return this.connector.put(query,null,this.connector.getRequestOptions());
  }
  public getUser = (user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<IamUser> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user",repository,[[":user",user]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public getUserPreferences = (user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<any> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/preferences",repository,[[":user",user]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => JSON.parse(response.json().preferences));
  }
  public setUserPreferences = (preferences:any,user=RestConstants.ME,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/preferences",repository,[[":user",user]]);
    return this.connector.put(query,JSON.stringify(preferences),this.connector.getRequestOptions());
  }
  public createUser = (user : string,password : string,profile : UserProfile,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/?password=:password",repository,[
      [":user",user],
      [":password",password]
    ]);
    return this.connector.post(query,JSON.stringify(profile),this.connector.getRequestOptions());
  }
  public editUser = (user : string,profile : UserProfile,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/profile",repository,[[":user",user]]);
    return this.connector.put(query,JSON.stringify(profile),this.connector.getRequestOptions());
  }
  public editUserCredentials = (user : string,credentials : UserCredentials,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("iam/:version/people/:repository/:user/credential",repository,[[":user",user]]);
    return this.connector.put(query,JSON.stringify(credentials),this.connector.getRequestOptions());
  }
}
