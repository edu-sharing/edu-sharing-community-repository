import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {RequestObject} from "../request-object";
import {ArchiveRestore, ArchiveSearch, Node, Collection, UsageList} from "../data-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestUsageService extends AbstractRestService{
    constructor(connector : RestConnectorService) {
        super(connector);
    }

    public getNodeUsages = (node : string,
                   repository=RestConstants.HOME_REPOSITORY
                  ): Observable<UsageList> => {
    // TODO does it need a repro?
    let query=this.connector.createUrl("usage/:version/usages/node/:node",repository,
      [
        [":node",node]
      ]);
    return this.connector.get(query,this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
    public getNodeUsagesCollection = (node : string,
                            repository=RestConstants.HOME_REPOSITORY
    ): Observable<Collection[]> => {
        // TODO does it need a repro?
        let query=this.connector.createUrl("usage/:version/usages/node/:node/collections",repository,
            [
                [":node",node]
            ]);
        return this.connector.get(query,this.connector.getRequestOptions())
            .map((response: Response) => response.json());
    }
}
