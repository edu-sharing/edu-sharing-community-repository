import {PipeTransform, Pipe} from '@angular/core';
import {NodeHelper} from "./node-helper";
import {TranslateService} from "@ngx-translate/core";

@Pipe({name: 'permissionName'})
export class PermissionNamePipe implements PipeTransform {
  transform(permission : any,args:string[]): string {
    if(permission.user && (permission.user.firstName || permission.user.lastName))
      return permission.user.firstName+" "+permission.user.lastName;
    if(permission.group && permission.group.displayName)
      return permission.group.displayName;
    return this.translate.instant(permission.authority.authorityName);
  }
  constructor(private translate : TranslateService){}
}
