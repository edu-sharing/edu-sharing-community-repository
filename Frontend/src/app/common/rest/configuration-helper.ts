/**
 * Different helper functions, may be used globally
 */

import {RestConstants} from "./rest-constants";
import {
  RestError, Node, Collection, Person, Permission, Permissions, User, MdsInfo,
  CollectionReference, LocalPermissions, Authority
} from "./data-object";
import {TranslateService} from "ng2-translate";
import {DateHelper} from "../ui/DateHelper";
import {ConfigurationService} from "../services/configuration.service";
import {RestMdsService} from "./services/rest-mds.service";
import {ListItem} from "../ui/list-item";
import {Helper} from "../helper";

export class ConfigurationHelper {
  public static getBanner(config: ConfigurationService){
    let banner=config.instant("banner");
    if(!banner)
      banner={};
    if(!banner.components || !banner.components.length)
      banner.components=["search"];
    console.log(banner);
    return banner;
  }
  public static hasMenuButton(config: ConfigurationService,button:string): boolean {
    let hide = config.instant("hideMainMenu");
    if(!hide)
      return true;
    // if button was not found in hide -> it has the menu button
    return hide.indexOf(button) == -1;
  }
}
