import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations} from "../data-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestOrganizationService extends AbstractRestService{
  constructor(connector : RestConnectorService) {
      super(connector);
  }
  public removeMember = (organization : string,member : string,repository = RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization/member/:member", repository,
      [
        [":organization",organization],
        [":member",member]
      ]);
    return this.connector.delete(query, this.connector.getRequestOptions());
  }
  public deleteOrganization = (organization : string,repository = RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization", repository,
      [
        [":organization",organization],
      ]);
    return this.connector.delete(query, this.connector.getRequestOptions());
  }
  public createOrganization = (organization : string,repository = RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("organization/:version/organizations/:repository/:organization", repository,
      [
        [":organization",organization],
      ]);
    return this.connector.put(query,null, this.connector.getRequestOptions());
  }
  public getOrganizations = (pattern = "",onlyMemberships=true, request:any=null,repository = RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrlNoEscape("organization/:version/organizations/:repository/?pattern=:pattern&:request", repository,
    [
      [":pattern",encodeURIComponent(pattern)],
      [":request",this.connector.createRequestString(request)],
      [":onlyMemberships",""+onlyMemberships]]);
    return this.connector.get<OrganizationOrganizations>(query, this.connector.getRequestOptions());
  }
}
