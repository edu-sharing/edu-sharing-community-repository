/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, Input} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {Comment, Person, UserProfile, UserSimple} from '../../rest/data-object';
import {RestConstants} from '../../rest/rest-constants';
import {Router} from '@angular/router';
import {UIConstants} from '../ui-constants';
import {DomSanitizer} from "@angular/platform-browser";
import {AuthorityNamePipe} from "../authority-name.pipe";
import {TranslateService} from "@ngx-translate/core";

@Component({
    selector: 'user-avatar',
    templateUrl: 'user-avatar.component.html',
    styleUrls: ['user-avatar.component.scss'],
})
export class UserAvatarComponent {
    /**
     * Automatically link to the given user profile
     * @type {boolean}
     */
    @Input() link=false;
    @Input() user : UserSimple;
    /**
     * either xsmall, small, medium or large
     */
    @Input() size = 'large';

    // random view id
    public id=Math.random();
    public _customImage:any;
    @Input() set customImage(customImage:File){
        if(customImage==null){
            this._customImage=null;
            return;
        }
        this._customImage=this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(customImage));
    };
    constructor(private router : Router,
                private translate : TranslateService,
                private sanitizer : DomSanitizer) {
    }
    isEditorialUser(){
        return this.user.profile && this.user.profile.types && this.user.profile.types.indexOf(RestConstants.GROUP_TYPE_EDITORIAL)!=-1;
    }
    openProfile(){
        this.router.navigate([UIConstants.ROUTER_PREFIX+"profiles",this.user.authorityName]);
    }

    getLetter(user: UserSimple) {
        return new AuthorityNamePipe(this.translate).transform(user,null).substring(0,1).toUpperCase();
    }
}
