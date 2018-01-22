import { Component } from '@angular/core';
import {RestHelper} from "../../rest-helper";
import {IamGroup, IamGroups, IamAuthorities, IamUsers, IamUser} from "../../data-object";
import {RestIamService} from "../../services/rest-iam.service";

@Component({
  selector: 'app-rest-iam-test',
  templateUrl: './rest-iam-test.component.html',
})
export class RestIamTestComponent{
  public searchGroups : IamGroups;
  public searchUsers : IamUsers;
  public getGroup : IamGroup;
  public getUser : IamUser;
  public error : string;
  public getGroupMembers : IamAuthorities;
  constructor(iam : RestIamService) {
    /*
    iam.searchGroups("*").subscribe(
      data => {console.log(data);this.searchGroups=data},
      error => this.error=RestHelper.printError(error)
    );
    iam.searchUsers("*").subscribe(
      data => {console.log(data);this.searchUsers=data},
      error => this.error=RestHelper.printError(error)
    );
    iam.createGroup("testGroup",{displayName:"testDisplay"}).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.createUser("testUser3","password",{firstName:"max",lastName:"tester",email:"a@b.de",avatar:null}).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.editUser("testUser3",{firstName:"max2",lastName:"tester2",email:"a@b.de2",avatar:null}).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.editUserCredentials("testUser3",{oldPassword:"max2",newPassword:"max3"}).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.addGroupMember("testGroup","testUser3").subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    )
    iam.editGroup("testGroup",{displayName:"test"}).subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.getGroup("testGroup").subscribe(
      data => this.getGroup=data,
      error => this.error=RestHelper.printError(error)
    );
    iam.getUser("admin").subscribe(
      data => this.getUser=data,
      error => this.error=RestHelper.printError(error)
    );
    iam.getGroupMembers("testGroup").subscribe(
      data => this.getGroupMembers=data,
      error => this.error=RestHelper.printError(error)
    );

    iam.createGroup("testGroup","testDisplay").subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    iam.deleteGroup("testGroup").subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    );
    */

  }


}
