import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {RequestObject} from "../request-object";
import {Node, Connector, OAuthResult, ConnectorList, Filetype, NodeLock, RestError} from "../data-object";
import {Observer} from "rxjs";
import {RestNodeService} from "./rest-node.service";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestConnectorsService extends AbstractRestService{
    private static MODE_NONE=0;
    private static MODE_CREATE=1;
    private static MODE_EDIT=2;

  private currentList: ConnectorList;
  constructor(connector : RestConnectorService,
              public nodeApi : RestNodeService) {
      super(connector);
  }

  public list = (repository=RestConstants.HOME_REPOSITORY
                  ): Observable<ConnectorList> => {
    let query=this.connector.createUrl("connector/:version/connectors/:repository/list",repository);
          return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json()).do((data)=>this.currentList=data);
  }
  public connectorSupportsEdit(node: Node,connectorList:ConnectorList=this.currentList) {
    if(connectorList==null || connectorList.connectors==null)
      return null;
    for(let connector of connectorList.connectors){
      if(RestConnectorsService.getFiletype(node,connector))
        return connector;
    }
    return null;
  }


  public static getFiletype(node:Node,connector:Connector,mode=this.MODE_NONE){
    for(let filetype of connector.filetypes){
      if(filetype.mimetype==node.mimetype && (mode==this.MODE_NONE || mode==this.MODE_EDIT && filetype.editable || mode==this.MODE_CREATE && filetype.creatable)) {
        if(filetype.mimetype=='application/zip'){
         if(   filetype.ccressourceversion==node.properties[RestConstants.CCM_PROP_CCRESSOURCEVERSION]
           && filetype.ccressourcetype==node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE]
           && filetype.ccresourcesubtype==node.properties[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE])
           return filetype;
         continue;
        }
        if(filetype.editorType && filetype.editorType!=node.properties[RestConstants.CCM_PROP_EDITOR_TYPE]){
          continue;
        }
        return filetype;
      }
    }
    return null;
  }
  public generateToolUrl(connectorList:ConnectorList,connectorType:Connector,type:Filetype,node:Node):Observable<string> {
    return new Observable<string>((observer: Observer<string>) => {
      let send: any = {};
      send["connectorId"] = connectorType.id;
      send["nodeId"] = node.ref.id;
      if(this.connector.getCordovaService().isRunningCordova()){
        send["accessToken"]=this.connector.getCordovaService().oauth.access_token;
      }
      let req = this.connector.getAbsoluteEndpointUrl()+"../eduservlet/connector?";
      let i=0;
      for (let param in send) {
        if (i > 0) {
          req += "&";
        }
        req += param + "=" + encodeURIComponent(send[param]);
        i++;
      }
      observer.next(req);
      observer.complete();
      /*
      this.connector.getOAuthToken().subscribe((oauth: OAuthResult) => {
          if (!oauth.access_token) {
            observer.error("oauth failed");
            observer.complete();
            return;
          }
          if (type == null) {
            type=RestConnectorsService.getFiletype(node,connectorType);
          }
          let send: any = {};
          send["endpoint"] = this.connector.getAbsoluteEndpointUrl();
          send["tool"] = connectorType.id;
          send["accessToken"] = oauth.access_token;
          send["refreshToken"] = oauth.refresh_token;
          send["tokenExpires"] = oauth.expires_in;
          send["filetype"] = type.filetype;
          send["mimetype"] = node.mimetype;
          send["node"] = node.ref.id;
          let i = 0;
          let req = connectorList.url.indexOf("?") != -1 ? "&" : "?";
          let params: string[] = [];
          // add mandatory params
          params.push("endpoint");
          params.push("tool");
          params.push("node");
          params.push("accessToken");
          params.push("refreshToken");
          params.push("tokenExpires");
          if(connectorType.parameters) {
            for (let param of connectorType.parameters)
              params.push(param);
          }
          for (let param of params) {
            if (!send[param]) {
              observer.error("invalid parameter " + param + " for connector, not in known list");
              observer.complete();
              return;
            }
            if (i > 0) {
              req += "&";
            }
            req += param + "=" + encodeURIComponent(send[param]);
            i++;
          }
          console.log("main request "+req);

          let url = connectorList.url + this.connector.createUrl(req, null);
          observer.next(url);
          observer.complete();
        },
        (error: any) => {
          observer.error(error);
          observer.complete();
        }
      );
      */
    });
  }

    getCurrentList() {
        return this.currentList;
    }
}
