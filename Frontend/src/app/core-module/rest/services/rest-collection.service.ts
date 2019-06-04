import { Injectable } from '@angular/core';

import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";

import * as EduData from "../data-object";
import {CollectionSubcollections, CollectionWrapper} from '../data-object';
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestCollectionService extends AbstractRestService{
    constructor(connector : RestConnectorService) {
        super(connector);
    }
  public deleteCollection = (collection : string,repository=RestConstants.HOME_REPOSITORY): Observable<void> => {
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collection",repository,[[":collection",collection]]);
    return this.connector.delete(query,this.connector.getRequestOptions());
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
  public getCollection = (collection : string,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection",repository,[
      [":collection",collection],
    ]);
    return this.connector.get<CollectionWrapper>(query,this.connector.getRequestOptions());
  }
  public search = (
      query="*",
      request:any=null,
      repository=RestConstants.HOME_REPOSITORY
  )=>{
      let http=this.connector.createUrlNoEscape("collection/:version/collections/:repository/search?query=:query&:request",repository,[
        [":query",encodeURIComponent(query)],
        [":request",this.connector.createRequestString(request)]
      ]);
      return this.connector.get<CollectionSubcollections>(http,this.connector.getRequestOptions());
  }


  public getCollectionSubcollections = (
      collection : string,
      scope = RestConstants.COLLECTIONSCOPE_ALL,
      propertyFilter : string[] = [],
      request:any = null,
      repository=RestConstants.HOME_REPOSITORY
    ) => {
    let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection/children/collections?scope=:scope&:propertyFilter&:request",repository,[
      [":collection",encodeURIComponent(collection)],
      [":scope",encodeURIComponent(scope)],
      [":request",this.connector.createRequestString(request)],
      [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)]
    ]);
    return this.connector.get<EduData.CollectionSubcollections>(query,this.connector.getRequestOptions());
  }
    public getCollectionReferences = (
        collection : string,
        propertyFilter : string[] = [],
        request:any = null,
        repository=RestConstants.HOME_REPOSITORY
    ) => {
        let query=this.connector.createUrlNoEscape("collection/:version/collections/:repository/:collection/children/references?:propertyFilter&:request",repository,[
            [":collection",encodeURIComponent(collection)],
            [":request",this.connector.createRequestString(request)],
            [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)]
        ]);
        return this.connector.get<EduData.CollectionReferences>(query,this.connector.getRequestOptions());
    }

  public getCollectionMetadata = (collectionId:string, repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("collection/:version/collections/:repository/:collectionid",repository,[[":collectionid",collectionId]]);
    return this.connector.get<EduData.Collection>(query,this.connector.getRequestOptions());
  }

  public createCollection = (
      collection:EduData.Collection,
      parentCollectionId:string=RestConstants.ROOT, repository:string=RestConstants.HOME_REPOSITORY
    ) => {

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid/children",repository,[[":collectionid",parentCollectionId]]);
    let options = this.connector.getRequestOptions();
    return this.connector.post<EduData.CollectionWrapper>(query, JSON.stringify(collection), options);
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

    public deleteCollectionImage = (collectionId:string, repository:string = RestConstants.HOME_REPOSITORY):Observable<XMLHttpRequest> => {
        let query=this.connector.createUrl("collection/:version/collections/:repository/:collectionid/icon",repository,
            [
                [":collectionid",collectionId],
            ]);
        let options=this.connector.getRequestOptions();

        return this.connector.delete(query,options);
    };

  public updateCollection = (collection:EduData.Collection) => {

    let repo:string = RestConstants.HOME_REPOSITORY;
    if ((collection.ref.repo!=null) && (collection.ref.repo!="local")) repo = collection.ref.repo;

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid",repo,[[":collectionid",collection.ref.id]]);

    let body:string = JSON.stringify(collection);

    let options = this.connector.getRequestOptions();
    return this.connector.put(query, body, options);

  };

  public removeFromCollection = (referenceId:string, collectionId:string, repository:string = RestConstants.HOME_REPOSITORY) => {

    let query:string = this.connector.createUrl("collection/:version/collections/:repository/:collectionid/references/:refid",repository,[[":collectionid",collectionId],[":refid",referenceId]]);

    return this.connector.delete(query, this.connector.getRequestOptions());
  };
}
