import {Pipe, PipeTransform} from "@angular/core";
@Pipe({name: 'authorityName'})
export class AuthorityNamePipe implements PipeTransform {
  transform(authority : any,args:string[]): string {
    if(!authority)
      return "invalid";
    if(authority.profile)
      return authority.profile.firstName+" "+authority.profile.lastName;
    if(authority.authorityName)
      return authority.authorityName;
    if(authority.firstName || authority.lastName)
      return authority.firstName+" "+authority.lastName;
    return "invalid";
  }
}
