import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {RequestObject} from "../request-object";
import {ArchiveRestore, ArchiveSearch, Node} from "../data-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestArchiveService extends AbstractRestService{
    constructor(connector : RestConnectorService) {
        super(connector);
    }
  /**
   * Searches for nodes in the Archive that contain the specific pattern
   * @param pattern Pattern to contain, or "*" to show all
   * @param person Show only nodes archived by this person
   * @param repository
   */
  public search = (pattern="*",
                   person="",
                   request : any = null,
                   repository=RestConstants.HOME_REPOSITORY
                  ) => {
    let query=this.connector.createUrlNoEscape("archive/:version/search/:repository/:pattern/:person?:request",repository,
      [
        [":pattern",encodeURIComponent(pattern)],
        [":person",encodeURIComponent(person)],
        [":request",this.connector.createRequestString(request)]
      ]);
    return this.connector.get<ArchiveSearch>(query,this.connector.getRequestOptions());
  }
  /**
   * Delete node(s) from the repository
   * @param nodes The array of nodes to delete
   * @param repository &archivedNodeIds=1&archivedNodeIds=2&archivedNodeIds=n...
   */
  public delete = (nodes : Node[]|string[],repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrlNoEscape("archive/:version/purge/:repository/?:nodes",repository,
      [[":nodes",RestHelper.getQueryStringForList("archivedNodeIds",nodes)]]);

    return this.connector.delete(query,this.connector.getRequestOptions())
      .map((response: Response) => response);

  }
  /**
   * Restore node(s) from the repository
   * @param nodes The array of nodes to restore
   * @param toPath A path to restore node which parents are missing. A missing value won't restore these nodes
   * @param repository
   */
  public restore = (nodes : Node[]|string[],toPath:string="",repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("archive/:version/restore/:repository/?:nodes&target=:target",repository,
      [
        [":nodes",RestHelper.getQueryStringForList("archivedNodeIds",nodes)],
        [":target",encodeURIComponent(toPath)]
      ]);
    return this.connector.post<ArchiveRestore>(query,"",this.connector.getRequestOptions());
  }
}
