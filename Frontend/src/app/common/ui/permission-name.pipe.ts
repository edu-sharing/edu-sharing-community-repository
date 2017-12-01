import {PipeTransform, Pipe} from '@angular/core';
import {NodeHelper} from "./node-helper";
import {TranslateService} from "ng2-translate";

@Pipe({name: 'permissionName'})
export class PermissionNamePipe implements PipeTransform {
  transform(permission : any,args:string[]): string {
    if(permission.user)
      return permission.user.firstName+" "+permission.user.lastName;
    if(permission.group)
      return permission.group.displayName;
    return this.translate.instant(permission.authority.authorityName);
  }
  constructor(private translate : TranslateService){}
}
