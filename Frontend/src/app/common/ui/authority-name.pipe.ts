import {Pipe, PipeTransform} from "@angular/core";
@Pipe({name: 'authorityName'})
export class AuthorityNamePipe implements PipeTransform {
  transform(authority : any,args:string[]): string {
    if(!authority)
      return "invalid";
    if(authority.profile && authority.profile.displayName)
      return authority.profile.displayName;
    if(authority.profile)
      return authority.profile.firstName+" "+authority.profile.lastName;
    if(authority.authorityName)
      return authority.authorityName;
    return "invalid";
  }
}
