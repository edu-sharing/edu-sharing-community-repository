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
export class RestSearchService {

  constructor(private connector : RestConnectorService) {}
  searchByProperties(properties:string[],values:string[],comparators:string[],combineMode=RestConstants.COMBINE_MODE_AND,contentType=RestConstants.CONTENT_TYPE_FILES,request: any=null, repository = RestConstants.HOME_REPOSITORY) : Observable<NodeList> {
    let url=this.connector.createUrlNoEscape('search/:version/custom/:repository?contentType=:contentType&combineMode=:combineMode&:properties&:values&:comparators&:request',repository,[
      [":contentType",contentType],
      [":combineMode",combineMode],
      [":properties",RestHelper.getQueryString("property",properties)],
      [":values",RestHelper.getQueryString("value",values)],
      [":comparators",RestHelper.getQueryString("comparator",comparators)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get(url,this.connector.getRequestOptions()).map((response: Response) => response.json());

  }

  search(criterias: Array<any>,facettes:string[]=[], request: any=null,contentType=RestConstants.CONTENT_TYPE_FILES, repository = RestConstants.HOME_REPOSITORY, metadataset = RestConstants.DEFAULT,propertyFilter:string[]=[], query = 'ngsearch') : Observable<NodeList> {
        let properties = '';
        for(var i = 0; i < criterias.length; i++) {
            if(i > 0)
              properties += ',';
            properties += '{"property":"'+criterias[i]['property']+'","values":'+JSON.stringify(criterias[i]['values'])+'}';
        }
        let body = '{"criterias":[' + properties + ']' + ',"facettes":'+JSON.stringify(facettes)+'}';

      let q=this.connector.createUrlNoEscape('search/:version/queriesV2/:repository/:metadataset/:query/?contentType=:contentType&:request&:propertyFilter',repository,[
        [":metadataset",encodeURIComponent(metadataset)],
        [":query",encodeURIComponent(query)],
        [":contentType",contentType],
        [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)],
        [":request",this.connector.createRequestString(request)]
      ]);
      return this.connector.post(q,body,this.connector.getRequestOptions()).map((response: Response) => response.json());
    }

}
