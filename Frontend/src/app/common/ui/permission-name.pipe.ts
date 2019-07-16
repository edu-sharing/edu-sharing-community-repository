import {PipeTransform, Pipe} from '@angular/core';
import {NodeHelper} from "./node-helper";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../services/configuration.service";

@Pipe({name: 'permissionName'})
export class PermissionNamePipe implements PipeTransform {
  transform(permission : any,args:any): string {
    if(args && args['field']){
      let field=args['field'];
      if(field=='secondary'){
        field=this.config.instant('userSecondaryDisplayName',null);
      }

      if(field=='email' || field=='email-domain') {
          let email;
          if(permission.user){
              email=permission.user.email || permission.user.mailbox;
          }
          if(permission.profile){
              email=permission.profile.email || permission.profile.mailbox;
          }
          if(field=='email-domain'){
              email=email ? email.substr(email.indexOf("@")+1) : null;
          }
          return email;
      }
      if(field=='authorityName'){
          if(permission.authorityType=='USER') {
              return permission.authorityName;
          }
      }
      return "";
    }
      if(permission.user && (permission.user.firstName || permission.user.lastName))
          return permission.user.firstName+" "+permission.user.lastName;
      if(permission.group && permission.group.displayName)
          return permission.group.displayName;
      return this.translate.instant(permission.authority.authorityName);
  }
  constructor(private translate : TranslateService,private config:ConfigurationService){}
}
