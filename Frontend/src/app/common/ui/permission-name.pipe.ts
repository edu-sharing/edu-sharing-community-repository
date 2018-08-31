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
        field=this.config.instant('userSecondaryDisplayName','email')
      }
      console.log(field);
      console.log(permission);

      if(field=='email') {
          if(permission.user){
            return permission.user.email || permission.user.mailbox;
          }
          if(permission.profile){
              return permission.profile.email || permission.profile.mailbox;
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
