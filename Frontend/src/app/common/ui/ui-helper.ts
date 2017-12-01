import {TranslateService} from "ng2-translate";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../services/configuration.service";
import {Router} from "@angular/router";
import {UIConstants} from "./ui-constants";
export class UIHelper{
  static MOBILE_WIDTH = 600;
  public static setTitleNoTranslation(name:string,title:Title,config:ConfigurationService) {
    config.get("branding").subscribe((data:any)=>{
      let t=name;
      if(data==true){
        t+=" - edu-sharing";
      }
      title.setTitle(t);
    });
  }
  public static setTitle(name:string,title:Title,translate:TranslateService,config:ConfigurationService){
    translate.get(name).subscribe((name:string)=>{
      this.setTitleNoTranslation(name,title,config);
    });
  }
  public static goToLogin(router : Router,config:ConfigurationService,scope="",next=window.location.href) {
    config.get("loginUrl").subscribe((url:string)=> {
      if(url && !scope){
        window.location.href=url;
        return;
      }
      router.navigate([UIConstants.ROUTER_PREFIX + "login"], {
        queryParams: {
          scope: scope,
          next: next
        }
      });
    });
  }

  static materializeSelect() {
    eval("$('select').css('display','none');$('select').material_select()");
  }
}
