import {TranslateService} from "ng2-translate";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../services/configuration.service";
import {Node} from "../rest/data-object";
import {ActivatedRoute, Router} from "@angular/router";
import {UIConstants} from "./ui-constants";
import {ElementRef} from "@angular/core";
import {RestConstants} from "../rest/rest-constants";
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

  static changeQueryParameter(router: Router,route:ActivatedRoute, name: string, value: any) {
    route.queryParams.subscribe((data:any)=>{
      let queryParams:any={};
      for(let key in data){
        queryParams[key]=data[key];
      }
      queryParams[name]=value;
      router.navigate([],{queryParams:queryParams});
    });
  }

  /**
   * The materialize textarea should auto-adjust height based on content
   * however, ngmodel does not refresh the element. This method will simulate a keyboard event to refresh the state
   * It will wait for the element to come active and send a keyboard event
   * Using ng's ElementRef will not work :/ We have to use a global dom id
   * @param {ElementRef} element
   */
  static invalidateMaterializeTextarea(id:string,timeout=10) {
    setTimeout(()=> {
      if (document.getElementById(id) == null) {
        UIHelper.invalidateMaterializeTextarea(id,100);
        return;
      }
      let event = new KeyboardEvent('keyup', {
        'view': window,
        'bubbles': true,
        'cancelable': true
      });
      document.getElementById(id).dispatchEvent(event);
    },timeout);
  }

  /**
   * returns true if the given string seems to be an email
   * @param {string} email
   */
  static isEmail(email: string) {
    if(!email)
      return false;
    if(email.indexOf("@")==-1)
      return false;
    if(email.indexOf(".")==-1)
      return false;
    return true;
  }

  static routeToSearchNode(router: Router, node: Node) {
    let converted=UIHelper.convertSearchParameters(node);
    router.navigate([UIConstants.ROUTER_PREFIX+'search'],{queryParams:{query:converted.query,savedQuery:node.ref.id,repository:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY],mds:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_MDS],parameters:JSON.stringify(converted.parameters)}});
  }
  static convertSearchParameters(node: Node) {
    let parameters=JSON.parse(node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS]);
    let result:any={parameters:{},query:null};
    for(let parameter of parameters){
      if(parameter.property==RestConstants.PRIMARY_SEARCH_CRITERIA){
        if(parameter.values[0]=='*')
          parameter.values[0]='';
        result.query=parameter.values[0];
        continue;
      }
      console.log(parameter);
      result.parameters[parameter.property]=parameter.values;
    }
    return result;
  }

  static materializeSelect() {
    eval("$('select').css('display','none');$('select').material_select()");
  }
}
