import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {ActivatedRoute, Router} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {ConfigurationService} from "../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {TranslateService} from "ng2-translate";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Component, ViewChild, ElementRef} from "@angular/core";
import {
    LoginResult,
    ServerUpdate,
    CacheInfo,
    Application,
    Node,
    Authority,
    NodeList,
    NodeWrapper
} from "../../common/rest/data-object";
import {RestAdminService} from "../../common/rest/services/rest-admin.service";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {Helper} from "../../common/helper";
import {RestConstants} from "../../common/rest/rest-constants";
import {UIConstants} from "../../common/ui/ui-constants";
import {ListItem} from "../../common/ui/list-item";
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {SuggestItem} from "../../common/ui/autocomplete/autocomplete.component";
import {RestOrganizationService} from "../../common/rest/services/rest-organization.service";
import {RestSearchService} from "../../common/rest/services/rest-search.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {Observable} from "rxjs/Observable";
import {Observer} from "rxjs/Observer";


@Component({
  selector: 'admin-main',
  templateUrl: 'admin.component.html',
  styleUrls: ['admin.component.scss'],
  animations: [

  ]
})
export class AdminComponent {
  public tab : string;
  public globalProgress=true;
  public appUrl:string;
  public propertyName:string;
  public chooseDirectory=false;
  public cacheName:string;
  public cacheInfo:string;
  public oaiUrl:string;
  public oaiSet:string;
  public oaiPrefix:string;
  public oaiMetadata:string;
  public oaiFile:string;
  public oaiImporter:string;
  public repositoryVersion:string;
  public ngVersion:string;
  public updates: ServerUpdate[]=[];
  public applications: Application[]=[];
  public showWarning=false;
  public dialogTitle: string;
  public dialogMessage: string;
  public dialogButtons:DialogButton[]=[];
  public dialogParameters:any;
  public warningButtons:DialogButton[]=[];
  public xmlAppProperties:any;
  public xmlAppAdditionalPropertyName:string;
  public xmlAppAdditionalPropertyValue:string;
  private parentNode: Node;
    public selectedTemplate:string = '';
    public templates:string[];
    public eduGroupSuggestions:SuggestItem[];
    public eduGroupsSelected:SuggestItem[] = [];
  public catalina : string;
  private oaiClasses: string[];
  @ViewChild('catalinaRef') catalinaRef : ElementRef;
  @ViewChild('xmlSelect') xmlSelect : ElementRef;
  @ViewChild('excelSelect') excelSelect : ElementRef;
  @ViewChild('templateSelect') templateSelect : ElementRef;
  private excelFile: File;
  public xmlAppKeys: string[];
  public currentApp: string;
  private currentAppXml: string;
  public editableXmls=[
    {name:'HOMEAPP',file:RestConstants.HOME_APPLICATION_XML},
    {name:'CCMAIL',file:RestConstants.CCMAIL_APPLICATION_XML},
  ]
  private static MULTILINE_PROPERTIES = [
    "custom_html_headers","public_key"
  ];

  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private config: ConfigurationService,
              private title: Title,
              private translate: TranslateService,
              private storage : SessionStorageService,
              private admin : RestAdminService,
              private connector: RestConnectorService,
              private node: RestNodeService,
              private searchApi: RestSearchService,
              private organization: RestOrganizationService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
      UIHelper.setTitle('ADMIN.TITLE', this.title, this.translate, this.config);
      this.warningButtons=[
        new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>{window.history.back()}),
        new DialogButton('ADMIN.UNDERSTAND',DialogButton.TYPE_PRIMARY,()=>{this.showWarning=false})
      ];
      this.getTemplates();
      this.connector.isLoggedIn().subscribe((data: LoginResult) => {
        if (!data.isAdmin) {
          this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
          return;
        }
        this.globalProgress=false;
        this.tab='INFO';
        this.showWarning=true;
        this.admin.getServerUpdates().subscribe((data:ServerUpdate[])=>{
          this.updates=data;
        });
        this.refreshCatalina();
        this.refreshAppList();
        this.admin.getOAIClasses().subscribe((data:string[])=>{
          this.oaiClasses=data;
          this.oaiImporter=data[0];
        });
        this.admin.getRepositoryVersion().subscribe((data:string)=>{
          this.repositoryVersion=data;
        },(error:any)=>{
          this.repositoryVersion="Error accessing version information. Are you in dev mode?";
        });
        this.admin.getNgVersion().subscribe((data:string)=>{
          this.ngVersion=data;
        },(error:any)=>{
          this.ngVersion="Error accessing version information. Are you in dev mode?";
        });
      });
    });
  }
  public isMultilineProperty(key:string){
    if(AdminComponent.MULTILINE_PROPERTIES.indexOf(key)!=-1)
      return true;
    return this.xmlAppProperties[key].indexOf("\n")!=-1;
  }
  public downloadApp(app:Application){
    Helper.downloadContent(app.file,app.xml);
  }
  public updateExcelFile(event:any){
    this.excelFile=event.target.files[0];
  }
  public importExcel(){
    if(!this.excelFile){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_CSV');
      return;
    }
    if(!this.parentNode){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_DIRECTORY');
      return;
    }
    this.globalProgress=true;
    this.admin.importExcel(this.excelFile,this.parentNode.ref.id).subscribe((data:any)=>{
      this.toast.toast('ADMIN.IMPORT.CSV_IMPORTED',{rows:data.rows});
      this.globalProgress=false;
    },(error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
    });
  }
  public closeAppEditor(){
    this.xmlAppProperties=null;
    this.xmlAppAdditionalPropertyName=null;
    this.xmlAppAdditionalPropertyValue=null;
  }
  public saveApp(){
    this.globalProgress=true;
    if(this.xmlAppAdditionalPropertyName){
      this.xmlAppProperties[this.xmlAppAdditionalPropertyName]=this.xmlAppAdditionalPropertyValue;
    }
    this.admin.updateApplicationXML(this.currentAppXml,this.xmlAppProperties).subscribe(()=>{
      this.toast.toast('ADMIN.APPLICATIONS.APP_SAVED',{xml:this.currentAppXml});
      this.globalProgress=false;
      this.closeAppEditor();
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    })
  }
  public configApp(app:Application){
    window.open(app.configUrl);
  }
  public editApp(app:any){
    this.currentApp=app.name;
    this.currentAppXml=app.file;
    this.globalProgress=true;
    this.admin.getApplicationXML(app.file).subscribe((data:any[])=>{
      this.globalProgress=false;
      this.xmlAppKeys=Object.keys(data);
      this.xmlAppProperties=data;
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public removeApp(app:Application){
    this.dialogTitle='ADMIN.APPLICATIONS.REMOVE_TITLE';
    this.dialogMessage='ADMIN.APPLICATIONS.REMOVE_MESSAGE';
    let info="";
    for (let key in app) {
      if(key=="xml")
        continue;
      info+=key+": "+(app as any)[key]+"\n";
    }

    this.dialogParameters={info:info};
    this.dialogButtons=[
      new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>{this.dialogTitle=null}),
      new DialogButton('ADMIN.APPLICATIONS.REMOVE',DialogButton.TYPE_PRIMARY,()=>{
        this.dialogTitle=null;
        this.globalProgress=true;
        this.admin.removeApplication(app.id).subscribe(()=>{
          this.globalProgress=false;
          this.refreshAppList();
        },(error:any)=>{
          this.toast.error(error);
          this.globalProgress=false;
        })
      }),
    ];
  }
  public setTab(tab:string){
    this.tab=tab;
  }
  public pickDirectory(event : Node[]){
    this.parentNode=event[0];
    this.chooseDirectory=false;
  }
  public registerAppXml(event:any){
    let file=event.target.files[0];
    if(!file)
      return;
    this.globalProgress=true;
    this.admin.addApplicationXml(file).subscribe((data:any)=>{
      this.toast.toast("ADMIN.APPLICATIONS.APP_REGISTERED");
      this.refreshAppList();
      this.globalProgress=false;
      this.xmlSelect.nativeElement.value=null;
    },(error:any)=>{
      this.globalProgress=false;
      this.xmlSelect.nativeElement.value=null;
      this.toast.error(error);
    });
  }
  public registerApp(){
    this.globalProgress=true;
    this.admin.addApplication(this.appUrl).subscribe((data:any)=>{
      this.toast.toast("ADMIN.APPLICATIONS.APP_REGISTERED");
      this.refreshAppList();
      this.globalProgress=false;
      this.appUrl='';
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public getCacheInfo(){
    this.globalProgress=true;
    this.admin.getCacheInfo(this.cacheInfo).subscribe((data:CacheInfo)=>{
      this.globalProgress=false;
      this.dialogTitle=this.cacheInfo;
      this.dialogMessage="size: "+data.size+"\nstatistic hits: "+data.statisticHits;
      this.dialogButtons=DialogButton.getOk(()=>{this.dialogTitle=null;});
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public refreshAppInfo(){
    this.globalProgress=true;
    this.admin.refreshAppInfo().subscribe(()=>{
      this.globalProgress=false;
      this.toast.toast('ADMIN.TOOLKIT.APP_INFO_REFRESHED');
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public refreshEduGroupCache(){
      this.globalProgress=true;
      this.admin.refreshEduGroupCache().subscribe(()=>{
          this.globalProgress=false;
          this.toast.toast('ADMIN.TOOLKIT.EDU_GROUP_CACHE_REFRESHED');
      },(error:any)=>{
          this.globalProgress=false;
          this.toast.error(error);
      });
  }
  public refreshCache(sticky:boolean){
    this.globalProgress=true;
    this.admin.refreshCache(this.cacheName,sticky).subscribe(()=>{
      this.globalProgress=false;
      this.toast.toast('ADMIN.IMPORT.CACHE_REFRESHED');
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    })
  }
  public removeAppProperty(pos:number){
    let key=this.xmlAppKeys[pos];
    this.xmlAppKeys.splice(pos,1);
    delete this.xmlAppProperties[key];
    console.log(this.xmlAppProperties);
  }
  public oaiImport(){
    if(!this.oaiPreconditions())
      return;
    this.globalProgress=true;
    this.admin.importOAI(this.oaiUrl,this.oaiSet,this.oaiPrefix,this.oaiImporter,this.oaiMetadata,this.oaiFile).subscribe(()=>{
      this.globalProgress=false;
      this.toast.toast('ADMIN.IMPORT.OAI_STARTED');
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    })
  }

  private oaiPreconditions() {
    if(!this.oaiUrl) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_URL');
      return false;
    }
    if(!this.oaiSet) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_SET');
      return false;
    }
    if(!this.oaiPrefix) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_PREFIX');
      return false;
    }
    return true;
  }
  public removeImports(){
    if(!this.oaiPreconditions())
      return;
    this.globalProgress=true;
    this.admin.removeDeletedImports(this.oaiUrl,this.oaiSet,this.oaiPrefix).subscribe((data:any)=>{
      this.globalProgress=false;
      this.toast.toast('ADMIN.IMPORT.IMPORTS_REMOVED');
      this.appUrl='';
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public getPropertyValues(){
    this.globalProgress=true;
    this.admin.getPropertyValuespace(this.propertyName).subscribe((data:any)=>{
      this.globalProgress=false;
      this.dialogTitle='ADMIN.IMPORT.PROPERTY_VALUESPACE';
      this.dialogMessage=data.xml;
      this.dialogButtons=DialogButton.getOk(()=>{this.dialogTitle=null;});
      this.appUrl='';
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
  public runUpdate(update:ServerUpdate,execute=false){
    this.globalProgress=true;
    this.admin.runServerUpdate(update.id,execute).subscribe((data:any)=>{
      this.globalProgress=false;
      this.dialogTitle='ADMIN.UPDATE.RESULT';
      this.dialogMessage=data.result;
      this.dialogButtons=DialogButton.getOk(()=>{this.dialogTitle=null;});
      this.appUrl='';
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }

  private refreshAppList() {
    this.admin.getApplications().subscribe((data:Application[])=>{
      this.applications=data;
    });
  }

  private refreshCatalina() {
    this.admin.getCatalina().subscribe((data:string[])=>{
      this.catalina=data.reverse().join("\n");
      this.setCatalinaPosition();
    });
  }

  private setCatalinaPosition() {
    setTimeout(()=>{
      if(this.catalinaRef){
      this.catalinaRef.nativeElement.scrollTop = this.catalinaRef.nativeElement.scrollHeight;
      }
      else{
        this.setCatalinaPosition();
    }
  },50);
  }

  public getTemplates() {
      this.getTemplateFolderId().subscribe((id) => {
          this.node.getChildren(id).subscribe((data) => {
              console.log(data);
              let templates = [];
              for(let node of data.nodes) {
                  if(node.name.split('.').pop() == 'xml') {
                      templates.push(node.name);
                  }
              }
              this.templates = templates;
              this.selectedTemplate = this.templates[0];
          });
      })
  }

    public getTemplateFolderId() {
        return new Observable<string>((observer: Observer<string>) => {
        this.searchApi.searchByProperties([RestConstants.CM_NAME], ['Edu_Sharing_Sys_Template'], ['='], '', RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS).subscribe((data)=> {
            for(let node of data.nodes) {
                if (node.isDirectory) {
                   observer.next(node.ref.id);
                   observer.complete();
                   return;
                }
            }
        });
      });
    }

    public updateEduGroupSuggestions(event : any) {
        this.organization.getOrganizations(event.input).subscribe(
            (data:any)=>{
                var ret:SuggestItem[] = [];
                for (let orga of data.organizations) {
                    let item = new SuggestItem(orga.authorityName, orga.profile.displayName, 'group', '');
                    item.originalObject = orga;
                    ret.push(item);
                }
                this.eduGroupSuggestions=ret;
            });
    }

    public addEduGroup(data:any) {
        if(Helper.indexOfObjectArray(this.eduGroupsSelected, 'id', data.item.id) < 0)
            this.eduGroupsSelected.push(data.item);
    }

    public removeEduGroup(data:SuggestItem) {
        this.eduGroupsSelected.splice(Helper.indexOfObjectArray(this.eduGroupsSelected, 'id', data.id), 1);
    }

    public uploadTemplate(event:any){
        console.log(event);
        let file=event.target.files[0];
        if(!file)
            return;
        let id = '';
        this.globalProgress=true;
        this.getTemplateFolderId().subscribe((id) => {
            this.node.createNode(id,RestConstants.CCM_TYPE_IO,[],RestHelper.createNameProperty(file.name),true).subscribe(
                (data : NodeWrapper) => {
                    this.node.uploadNodeContent(data.node.ref.id,file,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
                        (data) => {
                            this.getTemplates();
                            this.toast.toast('ADMIN.FOLDERTEMPLATES.UPLOAD_DONE',{filename:JSON.parse(data.response).node.name});
                            this.globalProgress=false;
                            this.templateSelect.nativeElement.value=null;
                        }
                    );
                },(error:any)=>{
                    this.globalProgress=false;
                    this.templateSelect.nativeElement.value=null;
                    this.toast.error(error);
                });
        });
    }

    public applyTemplate(position = 0) {
        this.globalProgress = true;
        if(this.eduGroupsSelected.length < 1) {
            this.toast.error(null, 'ADMIN.FOLDERTEMPLATES.MISSING_GROUP');
            this.globalProgress = false;
            return;
        }
        if(this.selectedTemplate == '') {
            this.toast.error(null, 'ADMIN.FOLDERTEMPLATES.MISSING_TEMPLATE');
            this.globalProgress = false;
            return;
        }
        if (position >= this.eduGroupsSelected.length) {
            this.globalProgress = false;
            // done
            return;
        }
        this.admin.applyTemplate(this.eduGroupsSelected[position].id, this.selectedTemplate).subscribe(data => {
            this.toast.toast('ADMIN.FOLDERTEMPLATES.TEMPLATE_APPLIED', {templatename:this.selectedTemplate, groupname:this.eduGroupsSelected[position].id});
            this.applyTemplate(position + 1);
        }, (error: any) => {
            this.toast.error(error,'ADMIN.FOLDERTEMPLATES.TEMPLATE_NOTAPPLIED', {templatename:this.selectedTemplate, groupname:this.eduGroupsSelected[position].id});
            this.applyTemplate(position + 1);
        });
    }

    public gotoFoldertemplateFolder() {
        this.getTemplateFolderId().subscribe((id) => {
            this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"],{queryParams:{id:id}});
        });
    }
}

