
import {Component, Input, Output, EventEmitter, HostListener, ChangeDetectorRef, ApplicationRef} from "@angular/core";
import {
    Node, Group, IamGroups, IamUsers, NodeList, IamUser, IamAuthorities,
    Authority, OrganizationOrganizations, Organization, Person, User, HomeFolder, SharedFolder, ListItem, DialogButton
} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {ActivatedRoute, Router} from "@angular/router";
import {RestIamService} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {RestConnectorService} from "../../../core-module/core.module";
import {OptionItem} from "../../../core-ui-module/option-item";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {SuggestItem} from "../../../common/ui/autocomplete/autocomplete.component";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {RestConstants} from "../../../core-module/core.module";
import {RestOrganizationService} from "../../../core-module/core.module";
import {RestNodeService} from "../../../core-module/core.module";
import {ConfigurationService} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";
import {trigger} from "@angular/animations";
import {UIHelper} from "../../../core-ui-module/ui-helper";
@Component({
  selector: 'permissions-authorities',
  templateUrl: 'authorities.component.html',
  styleUrls: ['authorities.component.scss'],
  animations:[
	trigger('fromRight',UIAnimation.fromRight()),
	trigger('fade',UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class PermissionsAuthoritiesComponent {
  public GROUP_TYPES=RestConstants.VALID_GROUP_TYPES;
  public SCOPE_TYPES=RestConstants.VALID_SCOPE_TYPES;
  public ORG_TYPES=RestConstants.VALID_GROUP_TYPES_ORG;
  public list : any[]=[];
  public edit : any;
  private editDetails : any;
  private editId : string;
  private offset = 0
  public columns : ListItem[]=[];
  public addMemberColumns : ListItem[]=[];
  public editGroupColumns : ListItem[]=[];
  public sortBy : string;
  public sortAscending = true;
  public loading = true;
  public _searchQuery: string;
  private manageMemberSearch:string;
  public options:OptionItem[]=[];
  public toolpermissionAuthority:any;
  public optionsActionbar:OptionItem[];
  private orgs: OrganizationOrganizations;
  public addMembers: any;
  public editGroups: User;
  private memberOptions: OptionItem[];
  private addToList: any[];
  private isAdmin = false;
  private embeddedQuery:string;
  editButtons: DialogButton[];
  memberButtons: DialogButton[];
  @Input() set searchQuery(searchQuery : string){
    this._searchQuery=searchQuery;
    if(this._mode)
      this.search();
  }
  private selected : any[]=[];

  public _mode : string;
  public loadingTitle:string;
  private loadingMessage:string;
  public dialogTitle:string;
  public dialogMessage:string;
  public dialogButtons : DialogButton[];
  public dialogParameters : any;
  public dialogCancelable : boolean;
  public addTo : any;
  private addToSelection : any;
  public globalProgress=false;
  @Input() org : Organization;
  @Input() embedded = false;
  @Output() onSelection = new EventEmitter();
  @Output() setTab = new EventEmitter();
  public editMembers : any;
  private memberList : Authority[];
  private selectedMembers : Authority[]=[];
  private memberSugesstions : SuggestItem[];
  private memberListOffset : number;
  private updateMemberSuggestions(event : any){
    if(this.editMembers==this.org || this.org==null){
      this.iam.searchUsers(event.input).subscribe(
        (users:IamUsers) => {
          var ret:SuggestItem[] = [];
          for (let user of users.users){
            let item=new SuggestItem(user.authorityName,user.profile.firstName+" "+user.profile.lastName, 'person', '');
            item.originalObject=user;
            ret.push(item);
          }
          this.memberSugesstions=ret;
        },
        error => console.log(error));
    }
    else {
      this.iam.getGroupMembers(this.org.authorityName, event.input, 'USER').subscribe(
        (users: IamAuthorities) => {
          var ret: SuggestItem[] = [];
          for (let user of users.authorities) {
            let item = new SuggestItem(user.authorityName, user.profile.firstName + " " + user.profile.lastName, 'person', '');
            item.originalObject = user;
            ret.push(item);
          }
          this.memberSugesstions = ret;
        },
        error => console.log(error));
    }

  }
  private addMember(event:any){
    if(this.editMembers=="ALL"){
      this.iam.addGroupMember(this.org.authorityName, event.item.id).subscribe(() => {
        this.memberList = [];
        this.memberListOffset = 0;
        this.searchMembers();
      }, (error: any) => this.handleError(error));
    }
    else {
      this.iam.addGroupMember((this.editMembers as Group).authorityName, event.item.id).subscribe(() => {
        this.memberList = [];
        this.memberListOffset = 0;
        this.searchMembers();
      }, (error: any) => this.handleError(error));
    }
  }
  @Input() set mode(mode : string){
   this._mode=mode;
   if(mode=='USER'){
     this.sortBy="firstName";

   }
   else {
     this.sortBy="displayName";
   }
   this.columns=this.getColumns(mode,this.embedded);
    this.addMemberColumns=this.getColumns('USER',true);
    this.editGroupColumns=this.getColumns('GROUP',true);
   this.loadAuthorities();
  }
  private getMemberOptions() : OptionItem[]{
    let options:OptionItem[]=[];
    if(this.editGroups){
        if (this.selectedMembers.length) {
            options.push(new OptionItem("PERMISSIONS.MENU_REMOVE_MEMBERSHIP", "delete", (data: any) => this.deleteMembership()))
        }
    }
    else {
        if (this.selectedMembers.length) {
            options.push(new OptionItem("PERMISSIONS.MENU_REMOVE_MEMBER", "delete", (data: any) => this.deleteMember()))
        }
    }
    return options;
  }

  private getColumns(mode : string,fromDialog=false){
    let columns : ListItem[]=[];
    if(mode=='USER'){
      columns.push(new ListItem(mode, RestConstants.AUTHORITY_NAME));
      columns.push(new ListItem(mode, RestConstants.AUTHORITY_FIRSTNAME));
      columns.push(new ListItem(mode, RestConstants.AUTHORITY_LASTNAME));
      if(!fromDialog)
        columns.push(new ListItem(mode, RestConstants.AUTHORITY_EMAIL));
    }
    else if(mode=='GROUP'){
      columns.push(new ListItem(mode, RestConstants.AUTHORITY_DISPLAYNAME));
      if(!fromDialog)
        columns.push(new ListItem(mode, RestConstants.AUTHORITY_GROUPTYPE));
    }
    else {
      columns.push(new ListItem(mode, RestConstants.AUTHORITY_DISPLAYNAME));
    }
    return columns;
  }
  constructor(private toast: Toast,
              private node : RestNodeService,
              private config : ConfigurationService,
              private router : Router,
              private translate : TranslateService,
              private organization : RestOrganizationService,
              private connector : RestConnectorService,
              private iam: RestIamService) {
    this.updateButtons();
    this.organization.getOrganizations().subscribe((data: OrganizationOrganizations) => {
      this.isAdmin = data.canCreate;
    });
  }
  private search(){
    this.refresh();
  }
  public changeSort(event : any){
    //this.sortBy=event.sortBy;
    if(this._mode=='GROUP'){
      this.sortBy=event.sortBy;
    }
    this.sortAscending=event.sortAscending;
    this.offset=0;
    this.list=[];
    this.loadAuthorities();
  }
  public selection(data : any){
    this.selected=data;
    this.onSelection.emit(data);
    this.updateOptions(false);
  }
  private getList<T>(data:T) : T[]{
    if(data)
      return [data];
    return this.selected;
  }
  private updateOptions(all:boolean) {
    if(this.embedded)
      return;
    let options:OptionItem[]=[];
    let list=this.getList(null);
    if(!all && this.isAdmin && this._mode=='ORG' && !list.length){
      options.push(new OptionItem("PERMISSIONS.MENU_TOOLPERMISSIONS_GLOBAL","playlist_add_check",(data:any)=>{this.toolpermissionAuthority=RestConstants.getAuthorityEveryone()}));
    }
    if(!all && !list.length){
      if(this._mode=='GROUP') {
        options.push(new OptionItem("PERMISSIONS.MENU_CREATE_GROUP", "add", (data: any) => this.createGroup()))
      }
      if(this._mode=='USER'){
        if(this.org)
          options.push(new OptionItem("PERMISSIONS.MENU_ADD_GROUP_MEMBERS", "person_add", (data: any) => this.addMembersFunction(this.org)));
        if(this.orgs && this.orgs.canCreate){
          options.push(new OptionItem("PERMISSIONS.MENU_CREATE_USER", "add", (data: any) => this.createAuthority()));
        }
        let download = new OptionItem("PERMISSIONS.EXPORT_MEMBER", "cloud_download", (data: any) => this.downloadMembers())
        download.onlyDesktop=true;
        options.push(download);
      }
    }
    if(!all && this._mode=='ORG' && this.orgs && this.orgs.canCreate){
      options.push(new OptionItem("PERMISSIONS.ADD_ORG", "add", (data: any) => this.createOrg()));
    }

    if(all || list.length){
      if(this._mode=='USER' && !all){
        options.push(new OptionItem("PERMISSIONS.MENU_ADD_TO_GROUP","group_add",(data:any)=>this.addToGroup(data)))
        options.push(new OptionItem("PERMISSIONS.MENU_EDIT_GROUPS", "group", (data: any) => this.openEditGroups(data)));
      }
      if(list.length==1 || all) {
        if(this._mode=='GROUP') {
          options.push(new OptionItem("PERMISSIONS.MENU_ADD_GROUP_MEMBERS", "group_add", (data: any) => this.addMembersFunction(data)));
          options.push(new OptionItem("PERMISSIONS.MENU_MANAGE_GROUP", "group", (data: any) => this.manageMembers(data)));
        }
        if(this._mode=='GROUP' || this.orgs && this.orgs.canCreate)
          options.push(new OptionItem("PERMISSIONS.MENU_EDIT", "edit", (data: any) => this.editAuthority(data)));

        if(this.isAdmin)
          options.push(new OptionItem("PERMISSIONS.MENU_TOOLPERMISSIONS","playlist_add_check",(data:any)=>{this.toolpermissionAuthority=this.getList(data)[0]}));
      }
      if(this._mode=='GROUP')
        options.push(new OptionItem("PERMISSIONS.MENU_DELETE","delete",(data:any)=>this.deleteAuthority(data,(list:any)=>this.startDelete(list))));
      if(this._mode=='USER' && this.org)
        options.push(new OptionItem("PERMISSIONS.MENU_EXCLUDE","delete",(data:any)=>this.deleteAuthority(data,(list:any)=>this.startExclude(list))));
      if(this._mode=='ORG' && this.orgs && this.orgs.canCreate)
        options.push(new OptionItem("PERMISSIONS.MENU_DELETE","delete",(data:any)=>this.deleteAuthority(data,(list:any)=>this.deleteOrg(list))));
    }
    console.log(all);
    console.log(list);
    console.log(options);
    if(all){
      this.options=options;
    }
    else {
      this.optionsActionbar = options;
    }
  }
  private cancelEdit(){
    this.edit=null;
  }
  private cancelAddTo(){
    this.addTo=null;
  }
  private cancelEditMembers(){
    this.editMembers=null;
    this.addMembers=null;
    this.editGroups=null;
    //this.refresh();
  }
  private addMembersToGroup(){
    this.globalProgress=true;
    this.addToSelection=[this.addMembers];
    this.addToList=this.selectedMembers;
    this.addMembers=null;

    this.addToSingle(()=>this.refresh());
  }
  private checkOrgExists(orgName:string){
    this.organization.getOrganizations(orgName).subscribe((data:OrganizationOrganizations)=>{
      if(data.organizations.length){
        this.loadingTitle=null;
        this.toast.toast("PERMISSIONS.ORG_CREATED");
        this.refresh();
      }
      else{
        setTimeout(()=>this.checkOrgExists(orgName),2000);
      }
    });

  }
  private saveEdits(){
    console.log(this._mode+" save");
    if(this._mode=='GROUP' || this._mode=='ORG'){
      if(this.editId==null){
        let name=this.edit.profile.displayName;
        if(this._mode=='ORG'){
          this.organization.createOrganization(name).subscribe(() => {
            this.edit = null;
            this.loadingTitle="PERMISSIONS.ORG_CREATING";
            this.loadingMessage="PERMISSIONS.ORG_CREATING_INFO";
            setTimeout(()=>this.checkOrgExists(name),2000);

          },
            (error:any)=>this.toast.error(error));
        }
        else {
          this.globalProgress=true;
          console.log("groupescope:" , this.edit.profile);
          this.iam.createGroup(name, this.edit.profile, this.org ? this.org.groupName : "").subscribe(() => {
            this.edit = null;
            this.globalProgress=false;
            this.toast.toast("PERMISSIONS.GROUP_CREATED");
            this.refresh();
          }, (error: any) =>{
            this.toast.error(error);
            this.globalProgress=false;
          });
        }
        return;
      }
      this.iam.editGroup(this.editId,this.edit.profile).subscribe(() => {
        this.edit=null;
        this.toast.toast("PERMISSIONS.GROUP_EDITED");
        this.refresh();
      },
        (error : any)=>this.toast.error(error));
    }
    else{
      let editStore=Helper.deepCopy(this.edit);
      editStore.profile.sizeQuota*=1024*1024;
      this.globalProgress=true;
      if(this.editId==null){
        let name=this.editDetails.authorityName;
        let password=this.editDetails.password;
        this.iam.createUser(name,password,editStore.profile).subscribe(() => {
            this.edit=null;
            this.globalProgress=false;
            if(this.org){
              this.iam.addGroupMember(this.org.authorityName,name).subscribe(()=>{
                this.toast.toast("PERMISSIONS.USER_CREATED");
                this.refresh();
              },(error:any)=>this.toast.error(error));
            }
            else{
              this.toast.toast("PERMISSIONS.USER_CREATED");
              this.refresh();
            }

          },
          (error : any)=>{
            this.toast.error(error);
            this.globalProgress=false;
          });
      }
      else {
        this.iam.editUser(this.editId, editStore.profile).subscribe(() => {
            this.edit = null;
            this.toast.toast("PERMISSIONS.USER_EDITED");
            this.refresh();
            this.globalProgress=false;
          },
          (error: any) => {
            this.toast.error(error);
            this.globalProgress=false;
          });
      }
    }
  }
  public loadAuthorities() {
    this.loading=true;
    let sort=RestConstants.AUTHORITY_NAME;
    if(this._mode=='ORG')
      sort=RestConstants.CM_PROP_AUTHORITY_AUTHORITYNAME;
    if(this._mode=='GROUP' && !this.org) {
      sort=this.sortBy;
      if(sort==RestConstants.AUTHORITY_DISPLAYNAME){
        sort = RestConstants.AUTHORITY_NAME;
      }
      if(sort==RestConstants.AUTHORITY_GROUPTYPE) {
        sort = RestConstants.CCM_PROP_AUTHORITY_GROUPTYPE;
      }
    }
    if(this._mode=='USER' && !this.org)
      sort="firstName";

    let request={sortBy:[sort],sortAscending:this.sortAscending,offset:this.offset};
    let query=this._searchQuery? this._searchQuery : "";
    this.updateOptions(false);
    this.updateOptions(true);
    this.organization.getOrganizations(query).subscribe((orgs: OrganizationOrganizations) => {
      this.orgs = orgs;
      this.updateOptions(false);
      this.updateOptions(true);
    });
    if(this._mode=='ORG') {
      this.organization.getOrganizations(query, request).subscribe((orgs: OrganizationOrganizations) => {
        this.offset += this.connector.numberPerRequest;
        for (let org of orgs.organizations) {
          if(org.administrationAccess)
            this.list.push(org);
        }
        this.loading = false;
      });
    }/*
    else if(this._mode=='USER'){
      this.iam.searchUsers(this.query,request).subscribe((users : IamUsers) => {
        console.log(users);
        this.offset+=this.connector.numberPerRequest;
        for(let user of users.users)
          this.list.push(user);
        this.loading=false;
      });
    }
    else{
      this.iam.searchGroups(this.query,request).subscribe((groups : IamGroups) => {
        this.offset+=this.connector.numberPerRequest;
        for(let group of groups.groups)
          this.list.push(group);
        this.loading=false;
      });
    }*/
    else{
      if(this.org) {
        this.offset += this.connector.numberPerRequest;
        this.iam.getGroupMembers(this.org.authorityName, query, this._mode, request).subscribe((data: IamAuthorities) => {
          for (let auth of data.authorities)
            this.list.push(auth);
          this.loading = false;
        });
      }
      else if(this._mode=='GROUP'){
        this.offset += this.connector.numberPerRequest;
        this.iam.searchGroups(query,true,"",request).subscribe((data:IamGroups)=>{
          for (let auth of data.groups)
            this.list.push(auth);
          this.loading = false;
        });
      }
      else if(this._mode=='USER'){
        this.offset += this.connector.numberPerRequest;
        this.iam.searchUsers(query,true,request).subscribe((data:IamUsers)=>{
          for (let auth of data.users)
            this.list.push(auth);
          this.loading = false;
        });
      }
    }
  }

  private editAuthority(data: any) {
    let list=this.getList(data);
    console.log(list);

    if(this._mode=='ORG'){
      this.node.getNodeParents(list[0].sharedFolder.id,true).subscribe((data:NodeList)=>{
        this.edit = Helper.deepCopy(list[0]);
        this.edit.folder="";
        data.nodes=data.nodes.reverse().slice(1);
        for(let node of data.nodes){
          this.edit.folder += node.name + "/";
        }
        this.editId = this.edit.authorityName;
      },(error:any)=>this.toast.error(error));
    }
    else if(this._mode=='USER'){
      this.iam.getUser(list[0].authorityName).subscribe((user)=>{
          this.edit = Helper.deepCopy(user.person);
          this.edit.profile.sizeQuota=user.person.quota.sizeQuota/1024/1024;
          this.editId = this.edit.authorityName;
      });
    }
    else {
      this.edit = Helper.deepCopy(list[0]);
      this.editId = this.edit.authorityName;
    }

  }
  private addToGroup(data: any) {
    let list=this.getList(data);
    console.log("addToGroup");
    console.log(list);

    this.addTo=list;
    this.addToSelection=null;
  }
  private openEditGroups(data: User) {
      let list=this.getList(data);
      this.editGroups=list[0];
      this.manageMemberSearch='';
      this.memberList = [];
      this.memberListOffset = 0;
      this.searchMembers();
  }
  private addToSelect(){
    this.addToList=this.selected;
    this.addToSingle();
  }
  private addToSingle(callback:Function=null,position = 0,groupPosition=0,errors=0){
    if(position==this.addToList.length){
      if(groupPosition<this.addToSelection.length-1){
        this.addToSingle(callback,0,groupPosition+1,errors);
      }
      else {
        if(groupPosition==0) {
          if (errors)
            this.toast.toast("PERMISSIONS.USER_ADDED_FAILED", {
              count: position - errors,
              error: errors,
              group: this.addToSelection[0].profile.displayName
            });
          else
            this.toast.toast("PERMISSIONS.USER_ADDED_TO_GROUP", {
              count: position,
              group: this.addToSelection[0].profile.displayName
            });
        }
        else{
          let count=this.addToList.length*this.addToSelection.length;
          if (errors)
            this.toast.toast("PERMISSIONS.USER_ADDED_FAILED_MULTI", {
              count: count - errors,
              error: errors,
            });
          else
            this.toast.toast("PERMISSIONS.USER_ADDED_TO_GROUP_MULTI", {
              count: count,
            });
        }

        this.addTo = null;
        this.globalProgress = false;
        if (callback)
          callback();
      }
      return;
    }
    this.globalProgress=true;
    this.iam.addGroupMember(this.addToSelection[groupPosition].authorityName,this.addToList[position].authorityName).subscribe(()=> {
      this.addToSingle(callback,position + 1,groupPosition,errors)
    },
      (error:any)=> {
        if (error.status == RestConstants.DUPLICATE_NODE_RESPONSE) {
          errors++;
        }
        else {
          this.toast.error(error);
        }
        this.addToSingle(callback, position + 1,groupPosition, errors);
      }
    );

  }
  private deleteAuthority(data: any,callback:Function) {
    let list=this.getList(data);
    if(this._mode=='GROUP' && list.filter((l)=>l.groupType==RestConstants.GROUP_TYPE_ADMINISTRATORS).length){
        this.toast.error(null,'PERMISSIONS.DELETE_ERROR_ADMINISTRATORS');
        return;
    }
    this.dialogTitle="PERMISSIONS.DELETE_TITLE";
    this.dialogMessage="PERMISSIONS.DELETE_"+this._mode;
    this.dialogCancelable=true;
    if(list.length==1)
      this.dialogMessage+="_SINGLE";
    this.dialogParameters={name:this._mode=='USER' ? list[0].authorityName : list[0].profile.displayName};
    this.dialogButtons=[
      new DialogButton("CANCEL",DialogButton.TYPE_CANCEL,()=>this.closeDialog()),
      new DialogButton("PERMISSIONS.MENU_DELETE",DialogButton.TYPE_PRIMARY,()=>callback(list))
    ];
  }
  private refresh() {
    this.offset=0;
    this.list=[];
    this.selected=[];
    this.loadAuthorities();
  }

  private closeDialog() {
    this.dialogTitle=null;
  }

  private startDelete(data:any,position=0,error=false) {
    this.dialogTitle = null;
    if(position==data.length) {
      this.globalProgress=false;
      this.refresh();
      if(!error)
        this.toast.toast("PERMISSIONS.DELETED_"+this._mode);
      return;
    }
    this.globalProgress=true;
    if(this._mode=='USER'){
      this.iam.deleteUser(data[position].authorityName).subscribe(()=>this.startDelete(data,position+1,error),(error:any)=>{
        this.toast.error(error);
        this.startDelete(data,position+1,true);
      });
    }
    else{
      this.iam.deleteGroup(data[position].authorityName).subscribe(()=>this.startDelete(data,position+1,error),(error:any)=>{
        this.toast.error(error);
        this.startDelete(data,position+1,true);
    });
    }

  }
  private startExclude(data:any,position=0) {
    this.dialogTitle = null;
    if(position==data.length) {
      this.globalProgress=false;
      this.refresh();
      this.toast.toast("PERMISSIONS.DELETED_"+this._mode);
      return;
    }
    this.globalProgress=true;
    this.organization.removeMember(this.org.groupName,data[position].authorityName).subscribe(()=>this.startExclude(data,position+1),(error:any)=>this.toast.error(error));
  }
  private createAuthority() {
    this.edit={profile:{}};
    this.editDetails={};
    this.editId=null;
  }
  private createGroup(){
      this.createAuthority();
      this.edit.profile.groupType=null;
      this.edit.profile.scopeType=null;
  }
  private createOrg() {
    this.createGroup();
  }

  private addMembersFunction(data: any) {
    if(data=='ALL')
      this.addMembers=this.org;
    else {
      let list = this.getList(data);
      this.addMembers = list[0];
    }
    this.manageMemberSearch='';
    this.searchMembers();
  }
  private updateSelectedMembers(data:Authority[]){
    console.log(data);
    this.selectedMembers=data;
    this.memberOptions=this.getMemberOptions();
    this.updateButtons();
  }
  private manageMembers(data: any) {
    if(data=='ALL')
      this.editMembers=this.org;
    else {
      let list = this.getList(data);
      this.editMembers = list[0];
    }
    this.manageMemberSearch='';
    this.searchMembers();
  }

  private deleteMember(position=0) {
    if(this.selectedMembers.length==position){
      this.toast.toast("PERMISSIONS.MEMBER_REMOVED");
      this.selectedMembers=[];
      this.memberOptions=this.getMemberOptions();
      this.memberList=[];
      this.memberListOffset=0;
      this.searchMembers();
      this.globalProgress=false;
      return;
    }
    this.globalProgress=true;
    this.iam.deleteGroupMember(this.editMembers=='ALL' ? this.org.authorityName : (this.editMembers as Group).authorityName,this.selectedMembers[position].authorityName).subscribe(()=>{
      this.deleteMember(position+1);
    },(error:any)=>this.toast.error(error));
  }
    private deleteMembership(position=0) {
        if(this.selectedMembers.length==position){
            this.toast.toast("PERMISSIONS.MEMBERSHIP_REMOVED");
            this.selectedMembers=[];
            this.memberOptions=this.getMemberOptions();
            this.memberList=[];
            this.memberListOffset=0;
            this.searchMembers();
            this.globalProgress=false;
            return;
        }
        this.globalProgress=true;
        this.iam.deleteGroupMember(this.selectedMembers[position].authorityName,this.editGroups.authorityName).subscribe(()=>{
            this.deleteMembership(position+1);
        },(error:any)=>this.toast.error(error));
    }
  private searchMembers(){
    this.selectedMembers=[];
    this.memberOptions=this.getMemberOptions();
    this.memberList=[];
    this.memberListOffset=0;
    this.refreshMemberList();
  }
  private refreshMemberList() {
    if(this.addMembers){
      this.selectedMembers=[];
      if(this.org && this.addMembers.authorityName!=this.org.authorityName){
        let request:any={
          sortBy: ["authorityName"],
          offset: this.memberListOffset
        };
        this.memberListOffset+=this.connector.numberPerRequest;
        this.iam.getGroupMembers(this.org.authorityName,this.manageMemberSearch, RestConstants.AUTHORITY_TYPE_USER, request).subscribe((data: IamAuthorities) => {
          this.memberList=this.memberList.concat(data.authorities);
          this.memberList=Helper.deepCopy(this.memberList);
        });
      }else {
        let request:any={
          sortBy: ["firstName"],
          offset: this.memberListOffset
        };
        this.memberListOffset+=this.connector.numberPerRequest;
        this.iam.searchUsers(this.manageMemberSearch, true, request).subscribe((data) => {
            this.memberList=this.memberList.concat(data.users);
            this.memberList=Helper.deepCopy(this.memberList);
        });
      }
    }
    else if(this.editGroups){
        let request:any={
            sortBy: ["authorityName"],
            offset: this.memberListOffset
        };
        this.memberListOffset+=this.connector.numberPerRequest;
        this.iam.getUserGroups(this.editGroups.authorityName,this.manageMemberSearch, request).subscribe((data) => {
            this.memberList=this.memberList.concat(data.groups);
            this.memberList=Helper.deepCopy(this.memberList);
        });
    }
    else {
      let request:any={
        sortBy: ["authorityName"],
        offset: this.memberListOffset
      };
      this.memberListOffset+=this.connector.numberPerRequest;
      this.iam.getGroupMembers((this.editMembers as Group).authorityName, this.manageMemberSearch, RestConstants.AUTHORITY_TYPE_USER, request).subscribe((data) => {
          this.memberList=this.memberList.concat(data.authorities);
          this.memberList=Helper.deepCopy(this.memberList);
      });
    }
  }

  private handleError(error: any) {
    if (error.status == RestConstants.DUPLICATE_NODE_RESPONSE)
        this.toast.error(null,"PERMISSIONS.USER_EXISTS_IN_GROUP");
      else
        this.toast.error(error);
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if (event.key == "Escape") {
      if (this.addTo) {
        this.addTo = null;
        return;
      }
      if(this.edit){
        this.edit=null;
        return;
      }
      if(this.editMembers){
        this.cancelEditMembers();
        return;
      }
    }
  }

  private deleteOrg(list: any) {
    this.globalProgress=true;
    let org=list[0];
    this.organization.deleteOrganization(org.groupName).subscribe(()=>{
      this.toast.toast("PERMISSIONS.ORG_REMOVED");
      this.globalProgress=false;
      this.dialogTitle = null;
      this.refresh();
    },
      (error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
      this.refresh();
    });
  }
  private deselectOrg(){
    this.org=null;
    this.refresh();
  }
  private setOrgTab(){
    this.setTab.emit('ORG');
  }

  private downloadMembers() {
    let data='';
    let i=0;
    for(let column of this.columns) {
      if(i)
        data+=";";
      data+=this.translate.instant(this._mode+"."+column.name);
      i++;
    }
    for(let entry of this.list){
      data+="\n";
      let i=0;
      for(let column of this.columns) {
        if(i)
          data+=";";
        data+=NodeHelper.getAttribute(this.translate,this.config,entry,column);
        i++;
      }
    }
    Helper.downloadContent(this.translate.instant("PERMISSIONS.DOWNLOAD_MEMBER_FILENAME"),data);
  }
  openFolder(folder:SharedFolder){
      UIHelper.goToWorkspaceFolder(this.node,this.router,this.connector.getCurrentLogin(),folder.id);
  }

    private updateButtons() {
        this.editButtons=[
            new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancelEdit()),
            new DialogButton('SAVE',DialogButton.TYPE_PRIMARY,()=>this.saveEdits()),
        ];
        /**
         *
         <div class="card-action" *ngIf="editMembers">
         <a class="waves-effect waves-light btn" (click)="cancelEditMembers()">{{'CLOSE' | translate }}</a>
         </div>
         <div class="card-action" *ngIf="addMembers">
         <a class="waves-effect waves-light btn" [class.disabled]="selectedMembers.length==0" (click)="addMembersToGroup()">{{'ADD' | translate }}</a>
         <a class="waves-effect waves-light btn-flat" (click)="cancelEditMembers()">{{'CLOSE' | translate }}</a>
         </div>
         </div>
         */
        let add=new DialogButton('ADD',DialogButton.TYPE_PRIMARY,()=>this.addMembersToGroup());
        add.disabled=this.selectedMembers.length==0;
        this.memberButtons=[
            new DialogButton('CLOSE',DialogButton.TYPE_CANCEL,()=>this.cancelEditMembers()),
            add
        ];
    }
}
