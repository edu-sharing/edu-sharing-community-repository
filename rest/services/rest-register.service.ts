import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations, RegisterInformation, RegisterExists} from '../data-object';
import {Response} from "@angular/http";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestRegisterService extends AbstractRestService{
  constructor(connector : RestConnectorService) {
      super(connector);
  }
  public register = (data:RegisterInformation) => {
    let query = this.connector.createUrl("register/:version/register",null);
    return this.connector.post<RegisterInformation>(query, data, this.connector.getRequestOptions());
  }
    public activate = (key:string) => {
        let query = this.connector.createUrl("register/:version/activate/:key",null,[[":key",key]]);
        return this.connector.post(query, null, this.connector.getRequestOptions());
    }
    public exists = (mail:string) => {
        let query = this.connector.createUrl("register/:version/exists/:mail",null,[[":mail",mail]]);
        return this.connector.get<RegisterExists>(query, this.connector.getRequestOptions());
    }
    public resendMail = (mail:string) => {
        let query = this.connector.createUrl("register/:version/resend/:mail",null,[[":mail",mail]]);
        return this.connector.post(query, null, this.connector.getRequestOptions());
    }
    public recoverPassword = (mail:string) => {
        let query = this.connector.createUrl("register/:version/recover/:mail",null,[[":mail",mail]]);
        return this.connector.post(query, null, this.connector.getRequestOptions());
    }
    public resetPassword = (key:string,password:string) => {
        let query = this.connector.createUrl("register/:version/reset/:key/:password",null,[
            [":key",key],
            [":password",password]
        ]);
        return this.connector.post(query, null, this.connector.getRequestOptions());
    }
}
