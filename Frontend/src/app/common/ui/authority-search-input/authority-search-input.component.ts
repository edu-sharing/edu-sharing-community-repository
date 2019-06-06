import {Component, Input, Output, EventEmitter, OnInit, SimpleChanges} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {SuggestItem} from "../autocomplete/autocomplete.component";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {PermissionNamePipe} from '../permission-name.pipe';
import {Authority, IamAuthorities, RestConstants, RestIamService} from '../../../core-module/core.module';

@Component({
  selector: 'authority-search-input',
  templateUrl: 'authority-search-input.component.html',
  styleUrls: ['authority-search-input.component.scss']
})


export class AuthoritySearchInputComponent{
  public authoritySuggestions : SuggestItem[];
  @Input() globalSearch = false;
  /**
   * Do allow any entered authority (not recommended for general use)
   * @type {boolean}
   */
  @Input() allowAny = false;
  /**
   * Group type to filter the groups searched for
   */
  @Input() groupType = "";
  @Input() disabled = false;
  @Input() maxSuggestions = 10;
  @Input() placeholder = 'WORKSPACE.INVITE_FIELD';
  @Input() hintBottom = "";
  @Output() onChooseAuthority = new EventEmitter();
  private lastSuggestionSearch: string;
  public addSuggestion(data: any) {
    this.onChooseAuthority.emit(data.item.originalObject)
  }
  public addAny(data:string){
    let authority=new Authority();
    authority.authorityName=data;
    authority.authorityType=RestConstants.AUTHORITY_TYPE_UNKNOWN;
    this.onChooseAuthority.emit(authority);
  }
  constructor(private iam : RestIamService,private namePipe : PermissionNamePipe){

  }
  public updateSuggestions(event : any){
    this.lastSuggestionSearch = event.input;
    this.iam.searchAuthorities(event.input,this.globalSearch,this.groupType).subscribe(
      (authorities:IamAuthorities)=>{
        if(this.lastSuggestionSearch!=event.input)
          return;
        let ret:SuggestItem[] = [];
        for (let user of authorities.authorities) {
          let group = user.profile.displayName != null;
          let item = new SuggestItem(user.authorityName, group ? user.profile.displayName : NodeHelper.getUserDisplayName(user), group ? 'group' : 'person', '');
          item.originalObject = user;
          item.secondaryTitle = this.namePipe.transform(user,{field:'secondary'});
          ret.push(item);
        }
        this.authoritySuggestions=ret;
      });
    /*
        this.iam.searchUsers(event.input,this.globalSearch).subscribe(
          (users:IamUsers) => {
            var ret:SuggestItem[] = [];
            for (let user of users.users){
              let item=new SuggestItem(user.authorityName,user.profile.firstName+" "+user.profile.lastName, 'person', '');
              item.originalObject=user;
              ret.push(item);
            }
            this.iam.searchGroups(event.input,this.globalSearch).subscribe(
              (groups:IamGroups) => {
                for (let group of groups.groups){
                  let item=new SuggestItem(group.authorityName,group.profile.displayName, 'group', '');
                  item.originalObject=group;
                  ret.push(item);
                }
                this.authoritySuggestions=ret;
              });
          },
          error => console.log(error));
          */

  }
}
