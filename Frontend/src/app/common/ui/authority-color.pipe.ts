import {Pipe, PipeTransform} from "@angular/core";
import {Helper} from "../helper";
@Pipe({name: 'authorityColor'})
export class AuthorityColorPipe implements PipeTransform {
  public static COLORS=['#48708e','#975B5D','#6A9663','#62998F','#769CB6','#968758'];
  transform(authority : any,args:string[]): string {
    if(!authority)
      return AuthorityColorPipe.COLORS[0];
    //if(authority.profile && authority.profile.avatar)
    //  return AuthorityColorPipe.COLORS[0];
    let colors=AuthorityColorPipe.getColors();
    let id=Math.abs(AuthorityColorPipe.hash(authority.authorityName))%AuthorityColorPipe.COLORS.length;
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
  private static hexToRgb(hex:string) : number[] {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? [
      parseInt(result[1], 16),
      parseInt(result[2], 16),
      parseInt(result[3], 16)
    ] : null;
  }
  private static componentToHex(c:number) {
    var hex = Math.max(0,Math.min(Math.round(c),255)).toString(16);
    return hex.length == 1 ? "0" + hex : hex;
  }

  private static rgbToHex(rgb:number[]) : string {
    return "#" + AuthorityColorPipe.componentToHex(rgb[0]) + AuthorityColorPipe.componentToHex(rgb[1]) + AuthorityColorPipe.componentToHex(rgb[2]);
  }

  private static getColors() {
    let colors=Helper.deepCopy(AuthorityColorPipe.COLORS);
    for(let color of AuthorityColorPipe.COLORS){
      let c=AuthorityColorPipe.hexToRgb(color);
      c[0]*=0.75;
      c[1]*=0.75;
      c[2]*=0.75;
      colors.push(AuthorityColorPipe.rgbToHex(c));
      c=AuthorityColorPipe.hexToRgb(color);
      c[0]*=1.5;
      c[1]*=1.5;
      c[2]*=1.5;
      colors.push(AuthorityColorPipe.rgbToHex(c));
    }
    console.log(colors);
    return colors;
  }
}
