import {Component, Input, Output, EventEmitter, OnInit, SimpleChanges} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {SuggestItem} from "../autocomplete/autocomplete.component";
import {NodeHelper} from "../node-helper";
import {IamAuthorities} from "../../rest/data-object";
import {RestIamService} from "../../rest/services/rest-iam.service";

@Component({
  selector: 'authority-search-input',
  templateUrl: 'authority-search-input.component.html',
  styleUrls: ['authority-search-input.component.scss']
})


export class AuthoritySearchInputComponent{
  public authoritySuggestions : SuggestItem[];
  @Input() globalSearch = false;
  @Input() disabled = false;
  @Input() maxSuggestions = 10;
  @Input() placeholder = 'WORKSPACE.INVITE_FIELD';
  @Output() onChooseAuthority = new EventEmitter();
  private lastSuggestionSearch: string;
  public addSuggestion(data: any) {
    this.onChooseAuthority.emit(data.item.originalObject)
  }
  constructor(private iam : RestIamService){

  }
  public updateSuggestions(event : any){
    this.lastSuggestionSearch = event.input;
    this.iam.searchAuthorities(event.input,this.globalSearch).subscribe(
      (authorities:IamAuthorities)=>{
        if(this.lastSuggestionSearch!=event.input)
          return;
        var ret:SuggestItem[] = [];
        for (let user of authorities.authorities) {
          let group = user.profile.displayName != null;
          let item = new SuggestItem(user.authorityName, group ? user.profile.displayName : NodeHelper.getUserDisplayName(user), group ? 'group' : 'person', '');
          item.originalObject = user;
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
