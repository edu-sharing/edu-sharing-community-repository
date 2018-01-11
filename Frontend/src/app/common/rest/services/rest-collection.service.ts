import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptionsArgs } from '@angular/http';

import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";

import * as EduData from "../data-object";
import {CollectionWrapper} from "../data-object";

@Injectable()
export class RestCollectionService {
  constructor(private connector : RestConnectorService) {}

  public deleteCollection = (collection : string,repository=RestConstants.HOME_REPOSITORY): Observable<void> => {
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collection",repository,[[":collection",collection]]);
    return this.connector.delete(query,this.connector.getRequestOptions())
      .map((response: Response) => {});
  }
  public addNodeToCollection = (collection : string,node:string,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collection/references/:node",repository,[
      [":collection",collection],
      [":node",node],
    ]);
    return this.connector.put(query,null,this.connector.getRequestOptions());
  }
  public setPinning = (collections : string[],repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/pinning",repository);
    return this.connector.post(query,JSON.stringify(collections),this.connector.getRequestOptions());
  }
  public setOrder = (collection : string,nodes : string[]=[],repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection/order",repository,[
      [":collection",collection],
    ]);
    return this.connector.post(query,JSON.stringify(nodes),this.connector.getRequestOptions());
  }
  public getCollection = (collection : string,repository=RestConstants.HOME_REPOSITORY): Observable<CollectionWrapper> => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection",repository,[
      [":collection",collection],
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public search = (
      query="*",
      request:any=null,
      repository=RestConstants.HOME_REPOSITORY
  ): Observable<EduData.CollectionContent>=>{
      let http=this.connector.createUrlNoEscape("collection/:version/collections/:repository/search?query=:query&:request",repository,[
        [":query",encodeURIComponent(query)],
        [":request",this.connector.createRequestString(request)]
      ]);
      return this.connector.get(http,this.connector.getRequestOptions())
     .map((response: Response) => response.json());
  }


  public getCollectionContent = (
      collection : string,
      scope = RestConstants.COLLECTIONSCOPE_ALL,
      propertyFilter : string[] = [],
      request:any = null,
      repository=RestConstants.HOME_REPOSITORY
    ): Observable<EduData.CollectionContent> => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection/children?scope=:scope&:propertyFilter&:request",repository,[
      [":collection",encodeURIComponent(collection)],
      [":scope",encodeURIComponent(scope)],
      [":request",this.connector.createRequestString(request)],
      [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)]
    ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }

  public getCollectionMetadata = (collectionId:string, repository=RestConstants.HOME_REPOSITORY) : Observable<EduData.Collection> => {
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collectionid",repository,[[":collectionid",collectionId]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }

  public createCollection = (
      collection:EduData.Collection,
      parentCollectionId:string=RestConstants.ROOT, repository:string=RestConstants.HOME_REPOSITORY
    ) : Observable<EduData.CollectionWrapper> => {

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid/children",repository,[[":collectionid",parentCollectionId]]);
    let options:RequestOptionsArgs = this.connector.getRequestOptions();
    options.headers.append('Accept', 'text/html');

    return this.connector.post(query, JSON.stringify(collection), options)
      .map((response: Response) => response.json());

  }

  public uploadCollectionImage = (collectionId:string, file:File, mimetype:string, repository:string = RestConstants.HOME_REPOSITORY):Observable<XMLHttpRequest> => {

    if(mimetype=="auto")
      mimetype=file.type;
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collectionid/icon?mimetype=:mime",repository,
      [
        [":collectionid",collectionId],
        [":mime",mimetype]
      ]);
    let options=this.connector.getRequestOptions();

    return this.connector.sendDataViaXHR(query,file);
  };


  // TODO: put into a rest-organization service later
  public getOrganizations = (repository:string = RestConstants.HOME_REPOSITORY) : Observable<EduData.Organizations> => {
    let query=this.connector.createUrl("organization/:version/organizations/:repository",repository,[]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  };

  public updateCollection = (collection:EduData.Collection) : Observable<void> => {

    var repo:string = RestConstants.HOME_REPOSITORY;
    if ((collection.ref.repo!=null) && (collection.ref.repo!="local")) repo = collection.ref.repo;

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid",repo,[[":collectionid",collection.ref.id]]);

    let body:string = JSON.stringify(collection);

    let options:RequestOptionsArgs = this.connector.getRequestOptions();
    options.headers.append('Accept', 'text/html');

    return this.connector.put(query, body, options).map((response: Response) => {});

  };

  public removeFromCollection = (referenceId:string, collectionId:string, repository:string = RestConstants.HOME_REPOSITORY) : Observable<void> => {

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid/references/:refid",repository,[[":collectionid",collectionId],[":refid",referenceId]]);

    return this.connector.delete(query, this.connector.getRequestOptions())
      .map((response: Response) => {});

  };

  // TPDP move this to rest-render-service later
  public getRenderSnippetForContent = (content:EduData.CollectionReference, repository:string = RestConstants.HOME_REPOSITORY) : Observable<string> => {
    let query=this.connector.createUrl("rendering/:version/details/:repository/:referenceid",repository,[[":referenceid",content.ref.id]]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json().detailsSnippet);
  };

   public httpGET = (url:string) : Observable<string> => {
    return this.connector.get(url,this.connector.getRequestOptions())
      .map((response: Response) => response.text());
  };

}
