import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {RequestObject} from "../request-object";
import {ArchiveRestore, ArchiveSearch, Node, Collection, UsageList, CollectionUsage} from '../data-object';
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestUsageService extends AbstractRestService{
    public static getNodeUsagesByRepositoryType(list : UsageList){
        let groups:any={};
        for(let l of list.usages){
            let type=l.appSubtype;
            if(!groups[type])
                groups[type]=[];
            groups[type].push(l);
        }
        return groups;
    }

    constructor(connector : RestConnectorService) {
        super(connector);
    }

    public getNodeUsages = (node : string,
                   repository=RestConstants.HOME_REPOSITORY
                  ) => {
    let query=this.connector.createUrl("usage/:version/usages/node/:node",repository,
      [
        [":node",node]
      ]);
    return this.connector.get<UsageList>(query,this.connector.getRequestOptions());
  }
    public getNodeUsagesCollection = (node : string,
                            repository=RestConstants.HOME_REPOSITORY
    ) => {
        let query=this.connector.createUrl("usage/:version/usages/node/:node/collections",repository,
            [
                [":node",node]
            ]);
        return this.connector.get<CollectionUsage[]>(query,this.connector.getRequestOptions());
    }
    public deleteNodeUsage = (node : string, usage : string,
                                      repository=RestConstants.HOME_REPOSITORY
    ): Observable<Response> => {
        let query=this.connector.createUrl("usage/:version/usages/node/:node/:usage",repository,
            [
                [":node",node],
                [":usage",usage]
            ]);
        return this.connector.delete(query,this.connector.getRequestOptions());
    }
}
