import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import { NodeRef, Node, NodeWrapper, NodePermissions, LocalPermissions, NodeVersions, NodeVersion, NodeList} from "../data-object";
import {RequestObject} from "../request-object";
import {AbstractRestService} from "./abstract-rest-service";
import {MdsComponent} from "../../ui/mds/mds.component";
import {Helper} from "../../helper";

@Injectable()
export class RestSearchService extends AbstractRestService{
    static convertCritierias(properties:any[],mdsRef:MdsComponent){
        let criterias=[];
        properties=Helper.deepCopy(properties);
        for (let property in properties) {
            let widget=mdsRef.getWidget(property);
            if(widget && widget.type=='multivalueTree'){
                let attach=RestSearchService.unfoldTreeChilds(properties[property],widget);
                if(attach)
                    properties[property]=properties[property].concat(attach);
            }
            console.log(properties[property]);
            if(properties[property] && properties[property].length)
                criterias.push({'property':property,'values':properties[property]});
        }
        return criterias;
    }
    static unfoldTreeChilds(props:string[], widget:any) {
        let attach:string[] = [];
        if(props) {
            for (let prop of props) {
                for (let child of widget.values) {
                    let copy = child;
                    for(let i=0;i<=100 && copy.parent;i++) {
                        if (copy.parent == prop && attach.indexOf(child.id) == -1) {
                            attach.push(child.id);
                        }
                        if (copy.parent) {
                            copy = widget.values.find((v: any) => v.id == copy.parent);
                        } else
                            break;
                        if(i==100){
                            console.warn("possible tree recursion detected in valuespace for widget "+widget.id);
                        }
                    }
                }
            }
            return attach;
        }
        return null;
    }
  constructor(connector : RestConnectorService) {
      super(connector);
  }

  searchByProperties(properties:string[],values:string[],comparators:string[],combineMode=RestConstants.COMBINE_MODE_AND,contentType=RestConstants.CONTENT_TYPE_FILES,request: any=null, repository = RestConstants.HOME_REPOSITORY) {
    let url=this.connector.createUrlNoEscape('search/:version/custom/:repository?contentType=:contentType&combineMode=:combineMode&:properties&:values&:comparators&:request',repository,[
      [":contentType",contentType],
      [":combineMode",combineMode],
      [":properties",RestHelper.getQueryString("property",properties)],
      [":values",RestHelper.getQueryString("value",values)],
      [":comparators",RestHelper.getQueryString("comparator",comparators)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get<NodeList>(url,this.connector.getRequestOptions());

  }
  saveSearch(name:string,query:string,criterias:any[],repository = RestConstants.HOME_REPOSITORY, metadataset = RestConstants.DEFAULT,replace=false) {
    let url=this.connector.createUrl('search/:version/queriesV2/:repository/:metadataset/:query/save?name=:name&replace=:replace',repository,[
      [":name",name],
      [":query",query],
      [":metadataset",metadataset],
      [":replace",""+replace],
    ]);
    return this.connector.post<NodeWrapper>(url,JSON.stringify(criterias),this.connector.getRequestOptions());

  }
  searchSimple(queryId = 'ngsearch',criterias: any[]=[],query:string=null,request: any=null,type=RestConstants.CONTENT_TYPE_FILES) {
    if(query){
      criterias.push({property:RestConstants.PRIMARY_SEARCH_CRITERIA,values:[query]});
    }
    return this.search(criterias,null,request,type, RestConstants.HOME_REPOSITORY,RestConstants.DEFAULT,[],queryId);
  }
    search(criterias: any[],facettes:string[]=[], request: any=null,contentType=RestConstants.CONTENT_TYPE_FILES, repository = RestConstants.HOME_REPOSITORY, metadataset = RestConstants.DEFAULT,propertyFilter:string[]=[], query = RestConstants.DEFAULT_QUERY_NAME) {
        let body={
            criterias:criterias,
            facettes:facettes
        };

      let q=this.connector.createUrlNoEscape('search/:version/queriesV2/:repository/:metadataset/:query/?contentType=:contentType&:request&:propertyFilter',repository,[
        [":metadataset",encodeURIComponent(metadataset)],
        [":query",encodeURIComponent(query)],
        [":contentType",contentType],
        [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)],
        [":request",this.connector.createRequestString(request)]
      ]);
      return this.connector.post<NodeList>(q,body,this.connector.getRequestOptions());
    }
}
