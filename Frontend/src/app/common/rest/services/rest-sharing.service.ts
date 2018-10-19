import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
    NodeRef, NodeWrapper, NodePermissions, LocalPermissions, NodeVersions, NodeVersion, NodeList,
    SharingInfo
} from "../data-object";
import {RequestObject} from "../request-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestSharingService extends AbstractRestService{

    constructor(connector : RestConnectorService) {
        super(connector);
    }

    getInfo(node:string, token:string, password = "", repository = RestConstants.HOME_REPOSITORY) {
        let query=this.connector.createUrl('sharing/:version/sharing/:repository/:node/:token?password=:password', repository,
            [
                [":node",node],
                [":token",token],
                [":password",password]
            ]);
        return this.connector.get<SharingInfo>(query,this.connector.getRequestOptions());
    }
    getChildren(node:string, token:string, password = "",request:any={}, repository = RestConstants.HOME_REPOSITORY) {
        let query=this.connector.createUrlNoEscape('sharing/:version/sharing/:repository/:node/:token/children?password=:password&:request', repository,
            [
                [":node",encodeURIComponent(node)],
                [":token",encodeURIComponent(token)],
                [":password",encodeURIComponent(password)],
                [":request",this.connector.createRequestString(request)],
            ]);
        return this.connector.get<NodeList>(query,this.connector.getRequestOptions());
    }

  
}
