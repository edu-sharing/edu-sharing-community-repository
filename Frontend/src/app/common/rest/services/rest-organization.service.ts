import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations} from "../data-object";
import {Response} from "@angular/http";

@Injectable()
export class RestOrganizationService {
  constructor(private connector: RestConnectorService) {
  }
  public removeMember = (organization : string,member : string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization/member/:member", repository,
      [
        [":organization",organization],
        [":member",member]
      ]);
    return this.connector.delete(query, this.connector.getRequestOptions());
  }
  public deleteOrganization = (organization : string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization", repository,
      [
        [":organization",organization],
      ]);
    return this.connector.delete(query, this.connector.getRequestOptions());
  }
  public createOrganization = (organization : string,repository = RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization", repository,
      [
        [":organization",organization],
      ]);
    return this.connector.put(query,null, this.connector.getRequestOptions());
  }
  public getOrganizations = (pattern = "", request:any=null,repository = RestConstants.HOME_REPOSITORY): Observable<OrganizationOrganizations> => {
    let query = this.connector.createUrlNoEscape("organization/:version/organizations/:repository/?pattern=:pattern&:request", repository,
    [
      [":pattern",encodeURIComponent(pattern)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.get(query, this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
}
