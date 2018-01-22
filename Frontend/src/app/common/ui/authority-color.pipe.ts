import {Pipe, PipeTransform} from "@angular/core";
@Pipe({name: 'authorityColor'})
export class AuthorityColorPipe implements PipeTransform {
  public static COLORS=['#48708e','#975B5D','#6A9663','#62998F','#769CB6','#966196'];
  transform(authority : any,args:string[]): string {
    if(!authority)
      return AuthorityColorPipe.COLORS[0];
    //if(authority.profile && authority.profile.avatar)
    //  return AuthorityColorPipe.COLORS[0];

    let id=AuthorityColorPipe.hash(authority.authorityName)%AuthorityColorPipe.COLORS.length;
    return AuthorityColorPipe.COLORS[id];
  }
  // https://stackoverflow.com/questions/6122571/simple-non-secure-hash-function-for-javascript
  private static hash(data:string) {
    let hash = 0;
    if (data.length == 0) {
      return hash;
    }
    for (let i = 0; i < data.length; i++) {
      let char = data.charCodeAt(i);
      hash = ((hash<<5)-hash)+char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
  }
}
