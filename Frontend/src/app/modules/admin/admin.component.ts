import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {ActivatedRoute, Router} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {ConfigurationService} from "../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {TranslateService} from "@ngx-translate/core";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Component, ViewChild, ElementRef} from "@angular/core";
import {LoginResult, ServerUpdate, CacheInfo, Application, Node} from "../../common/rest/data-object";
import {RestAdminService} from "../../common/rest/services/rest-admin.service";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {Helper} from "../../common/helper";
import {RestConstants} from "../../common/rest/rest-constants";
import {UIConstants} from "../../common/ui/ui-constants";
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
  public chooseCollection=false;
  public cacheName:string;
  public cacheInfo:string;
  public oai:any={};
  public oaiSave=true;
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
  private parentCollection: Node;
  private parentCollectionType = "root";
  public catalina : string;
  private oaiClasses: string[];
  @ViewChild('catalinaRef') catalinaRef : ElementRef;
  @ViewChild('xmlSelect') xmlSelect : ElementRef;
  @ViewChild('excelSelect') excelSelect : ElementRef;
  private excelFile: File;
  private collectionsFile: File;
  private uploadTempFile: File;
  public xmlAppKeys: string[];
  public currentApp: string;
  private currentAppXml: string;
  public editableXmls=[
    {name:'HOMEAPP',file:RestConstants.HOME_APPLICATION_XML},
    {name:'CCMAIL',file:RestConstants.CCMAIL_APPLICATION_XML},
  ]

  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private config: ConfigurationService,
              private title: Title,
              private translate: TranslateService,
              private storage : SessionStorageService,
              private admin : RestAdminService,
              private connector: RestConnectorService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
      UIHelper.setTitle('ADMIN.TITLE', this.title, this.translate, this.config);
      this.warningButtons=[
        new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>{window.history.back()}),
        new DialogButton('ADMIN.UNDERSTAND',DialogButton.TYPE_PRIMARY,()=>{this.showWarning=false})
      ];
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
          this.oai.className=data[0];
          this.storage.get("admin_oai").subscribe((data:any)=>{
            if(data)
              this.oai=data;
          });
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
  public downloadApp(app:Application){
    Helper.downloadContent(app.file,app.xml);
  }
  public updateExcelFile(event:any){
    this.excelFile=event.target.files[0];
  }
  public updateUploadTempFile(event:any){
    this.uploadTempFile=event.target.files[0];
  }
  public updateCollectionsFile(event:any){
    this.collectionsFile=event.target.files[0];
  }
  public importCollections(){
    if(!this.collectionsFile){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_COLLECTIONS_XML');
      return;
    }
    if(!this.parentCollection && this.parentCollectionType=="choose"){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_COLLECTION');
      return;
    }
    this.globalProgress=true;
    this.admin.importCollections(this.collectionsFile,this.parentCollectionType=="root" ? RestConstants.ROOT : this.parentCollection.ref.id).subscribe((data:any)=>{
      this.toast.toast('ADMIN.IMPORT.COLLECTIONS_IMPORTED',{count:data.count});
      this.globalProgress=false;
      this.collectionsFile=null;
    },(error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
    });
  }
  public startUploadTempFile(){
    if(!this.uploadTempFile){
      this.toast.error(null,'ADMIN.TOOLKIT.CHOOSE_UPLOAD_TEMP');
      return;
    }
    this.globalProgress=true;
    this.admin.uploadTempFile(this.uploadTempFile).subscribe((data:any)=>{
      this.toast.toast('ADMIN.TOOLKIT.UPLOAD_TEMP_DONE',{filename:data.file});
      this.globalProgress=false;
      this.uploadTempFile=null;
    },(error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
    });
  }
  public importExcel(){
    if(!this.excelFile){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_EXCEL');
      return;
    }
    if(!this.parentNode){
      this.toast.error(null,'ADMIN.IMPORT.CHOOSE_DIRECTORY');
      return;
    }
    this.globalProgress=true;
    this.admin.importExcel(this.excelFile,this.parentNode.ref.id).subscribe((data:any)=>{
      this.toast.toast('ADMIN.IMPORT.EXCEL_IMPORTED',{rows:data.rows});
      this.globalProgress=false;
      this.excelFile=null;
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
        this.refreshAppList();
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
  public pickCollection(event : Node[]){
    this.parentCollection=event[0];
    this.chooseCollection=false;
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
    if(this.oaiSave){
      this.storage.set("admin_oai",this.oai);
    }
    this.admin.importOAI(this.oai.url,this.oai.set,this.oai.prefix,this.oai.className,this.oai.importerClassName,this.oai.recordHandlerClassName,this.oai.binaryHandlerClassName,this.oai.metadata,this.oai.file).subscribe(()=>{      this.globalProgress=false;
      this.toast.toast('ADMIN.IMPORT.OAI_STARTED');
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    })
  }

  private oaiPreconditions() {
    if(!this.oai.url) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_URL');
      return false;
    }
    if(!this.oai.set) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_SET');
      return false;
    }
    if(!this.oai.prefix) {
      this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_PREFIX');
      return false;
    }
    return true;
  }
  public removeImports(){
    if(!this.oaiPreconditions())
      return;
    this.globalProgress=true;
    this.admin.removeDeletedImports(this.oai.url,this.oai.set,this.oai.prefix).subscribe((data:any)=>{
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
}

