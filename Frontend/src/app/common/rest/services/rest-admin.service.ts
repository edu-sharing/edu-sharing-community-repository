import { Injectable } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  ArchiveRestore, ArchiveSearch, Node, IamGroup, IamGroups, IamAuthorities, GroupProfile,
  IamUsers, IamUser, UserProfile, UserCredentials, ServerUpdate, CacheInfo, NetworkRepositories, Application
} from "../data-object";
import {Observer} from "rxjs";

@Injectable()
export class RestAdminService {
  constructor(private connector : RestConnectorService) {}

  public addApplication = (url:string): Observable<any> => {
    let query=this.connector.createUrl("admin/:version/applications?url=:url",null,[
      [":url",url],
    ]);
    return this.connector.put(query,null,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public importExcel = (file : File,parent:string) : Observable<any> => {
    let query=this.connector.createUrl("admin/:version/import/excel?parent=:parent",null,[
      [":parent",parent]
    ]);
    let options=this.connector.getRequestOptions();

    return this.connector.sendDataViaXHR(query,file,"POST","excel")
      .map((response:XMLHttpRequest) => {return JSON.parse(response.response)});
  }
  public addApplicationXml = (file : File) : Observable<any> => {
    let query=this.connector.createUrl("admin/:version/applications/xml",null);
    let options=this.connector.getRequestOptions();

    return this.connector.sendDataViaXHR(query,file,"PUT","xml")
      .map((response:XMLHttpRequest) => {return JSON.parse(response.response)});
  }
  public getApplications = (): Observable<Application[]> => {
    let query=this.connector.createUrl("admin/:version/applications",null);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public removeApplication = (id:string): Observable<Response> => {
    let query=this.connector.createUrl("admin/:version/applications/:id",null,[
      [":id",id]
    ]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  public getServerUpdates = (): Observable<ServerUpdate[]> => {
    let query=this.connector.createUrl("admin/:version/serverUpdate/list",null);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public getNgVersion = (): Observable<string> => {
    let query = this.connector.createUrl("../version.txt", null);
    return this.connector.get(query, this.connector.getRequestOptions())
      .map((response: Response) => response.text());
  }
  public getRepositoryVersion = (): Observable<string> => {
    let query=this.connector.createUrl("../version.html",null);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => this.readRepositoryVersion(response.text()));
  }

  public getOAIClasses = (): Observable<string[]> => {
    let query=this.connector.createUrl("admin/:version/import/oai/classes",null);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public getCatalina = (): Observable<string[]> => {
    let query=this.connector.createUrl("admin/:version/catalina",null);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public importOAI = (baseUrl:string,set:string,metadataPrefix:string,className:string,metadataset="",fileUrl=""): Observable<Response> => {
    let query=this.connector.createUrl("admin/:version/import/oai?baseUrl=:baseUrl&set=:set&metadataPrefix=:metadataPrefix&className=:className&metadataset=:metadataset&fileUrl=:fileUrl",null,[
      [":baseUrl",baseUrl],
      [":set",set],
      [":metadataPrefix",metadataPrefix],
      [":className",className],
      [":metadataset",metadataset],
      [":fileUrl",fileUrl]
    ]);
    return this.connector.post(query,null,this.connector.getRequestOptions());
  }
  public refreshCache = (rootFolder:string,sticky=false): Observable<Response> => {
    let query=this.connector.createUrl("admin/:version/import/refreshCache/:rootFolder?sticky=:sticky",null,[
      [":rootFolder",rootFolder],
      [":sticky",""+sticky],
    ]);
    return this.connector.post(query,null,this.connector.getRequestOptions());
  }
  public getCacheInfo = (id : string): Observable<CacheInfo> => {
    let query=this.connector.createUrl("admin/:version/cacheInfo/:id",null,[[":id",id]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public refreshAppInfo = (): Observable<Response> => {
    let query=this.connector.createUrl("admin/:version/refreshAppInfo",null);
    return this.connector.post(query,null,this.connector.getRequestOptions());
  }
  public refreshEduGroupCache = (): Observable<Response> => {
      let query=this.connector.createUrl("admin/:version/refreshEduGroupCache",null);
      return this.connector.post(query,null,this.connector.getRequestOptions());
  }
  public getPropertyValuespace = (property:string): Observable<any> => {
    let query=this.connector.createUrl("admin/:version/propertyToMds?properties=:property",null,[
      [":property",property],
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public runServerUpdate = (id:string,execute=false): Observable<any> => {
    let query=this.connector.createUrl("admin/:version/serverUpdate/run/:id?execute=:execute",null,[
      [":id",id],
      [":execute",""+execute]
    ]);
    return this.connector.post(query,null,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public removeDeletedImports = (baseUrl:string,set:string,metadataPrefix:string): Observable<any> => {
    let query=this.connector.createUrl("admin/:version/import/oai/?baseUrl=:baseUrl&set=:set&metadataPrefix=:metadataPrefix",null,[
      [":baseUrl",baseUrl],
      [":set",set],
      [":metadataPrefix",metadataPrefix],
    ]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }

  private readRepositoryVersion(s: string) {
    return "build.number"+s.split("build.number")[1].split("</body>")[0].replace(/<br\/>/g,"");
  }

  public getApplicationXML(xml:string) {
    let query=this.connector.createUrl("admin/:version/applications/:xml",null,[[":xml",xml]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }

  public updateApplicationXML(xml:string,homeAppProperties: any[]) {
    let query=this.connector.createUrl("admin/:version/applications/:xml",null,[[":xml",xml]]);
    return this.connector.put(query,JSON.stringify(homeAppProperties),this.connector.getRequestOptions());
  }

  public applyTemplate = (groupName:string, templateName:string) :Observable<any> => {
      let query=this.connector.createUrl("admin/:version/applyTemplate",null,[]);
      let params = JSON.stringify({template:templateName,group:groupName});
      return this.connector.post(query,params,this.connector.getRequestOptions());
  }
}

