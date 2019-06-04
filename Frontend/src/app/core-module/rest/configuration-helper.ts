/**
 * Different helper functions, may be used globally
 */

import {MdsInfo, Repository} from "./data-object";
import {RestConstants} from "./rest-constants";
import {RestHelper} from "./rest-helper";
import {RestNetworkService} from "./services/rest-network.service";
import {ConfigurationService} from "../core.module";

export class ConfigurationHelper {
  public static getBanner(config: ConfigurationService){
    let banner=config.instant("banner");
    if(!banner)
      banner={};
    if(!banner.components || !banner.components.length)
      banner.components=["search"];
    return banner;
  }
  public static hasMenuButton(config: ConfigurationService,button:string): boolean {
    let hide = config.instant("hideMainMenu");
    if(!hide)
      return true;
    // if button was not found in hide -> it has the menu button
    return hide.indexOf(button) == -1;
  }
  static getPersonWithConfigDisplayName(person: any, config: ConfigurationService) {
    let field=config.instant("userDisplayName","fullName");
    if(field=="authorityName"){
      if(person.authorityName==null)
        field="fullName";
      else
        return person.authorityName;
    }
    if(field=="fullName"){
      if(person.profile){
        return ((person.profile.firstName ? person.profile.firstName : "")+" "+(person.profile.lastName ? person.profile.lastName : "")).trim();
      }
      return ((person.firstName ? person.firstName : "")+" "+(person.lastName ? person.lastName : "")).trim();
    }
    if(field=="firstName" || field=="lastName"){
      if(person.profile){
        return person.profile[field];
      }
      return person[field];
    }
    if(field=="email"){
      if(person.profile && person.profile.email)
        return person.profile.email;
      if(person.email==null)
        return person.mailbox;
      return person.email;
    }
    return person[field];
  }
  public static filterValidMds(repository:string|Repository,metadatasets: MdsInfo[], config: ConfigurationService) {
    let validMds=config.instant("availableMds");
    if(validMds && validMds.length){
      for(let mds of validMds){
        if(!(mds.repository==repository || mds.repository==RestConstants.HOME_REPOSITORY && (repository as Repository).isHomeRepo))
          continue;
        for(let i=0;i<metadatasets.length;i++){
          if(mds.mds.indexOf(metadatasets[i].id)==-1){
            metadatasets.splice(i,1);
            i--;
          }
        }
      }
    }
    return metadatasets;
  }
  public static filterValidRepositories(repositories: Repository[], config: ConfigurationService,onlyLocal : boolean) {
    let validRepositories = config.instant("availableRepositories");
    if (validRepositories && validRepositories.length) {
      for (let i = 0; i < repositories.length; i++) {
        if(validRepositories.indexOf(RestConstants.HOME_REPOSITORY)!=-1 && repositories[i].isHomeRepo)
          continue;
        if (validRepositories.indexOf(repositories[i].id) == -1) {
          repositories.splice(i, 1);
          i--;
        }
      }
    }
      if (onlyLocal) {
          for (let i = 0; i < repositories.length; i++) {
              if (!repositories[i].isHomeRepo) {
                  repositories.splice(i, 1);
                  i--;
              }
          }
      }
    return repositories;
  }
}
