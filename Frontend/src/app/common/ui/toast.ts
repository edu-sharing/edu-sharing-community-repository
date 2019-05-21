import {Injectable} from "@angular/core";
import {ToastyService, ToastData} from "ngx-toasty";
import {RestConstants} from "../rest/rest-constants";
import {RouterComponent} from "../../router/router.component";
import {ConfigurationService} from "../services/configuration.service";
import {Router} from "@angular/router";
import {TemporaryStorageService} from "../services/temporary-storage.service";
import {DialogButton} from "./modal-dialog/modal-dialog.component";
import {UIConstants} from "./ui-constants";
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "./ui-animation";
import {CordovaService} from "../services/cordova.service";
import {RestHelper} from "../rest/rest-helper";

@Injectable()
export class Toast{
  private onShowModal: Function;
  private dialogTitle: string;
  private dialogMessage: string;
  private dialogParameters: any;
  private lastToastMessage: string;
  private lastToastMessageTime: number;
  private lastToastError: string;
  private lastToastErrorTime: number;
  private static MIN_TIME_BETWEEN_TOAST = 2000;
  private linkCallback: Function;
  constructor(private toasty : ToastyService,
              private router : Router,
              private storage : TemporaryStorageService,
              private cordova : CordovaService,
              private translate : TranslateService){
    (window as any)['toastComponent']=this;
  }
  /**
   * Generates a toast message
   * @param message Translation-String of message
   * @param parameters Parameter bindings for translation
   * @param additional: additional parameter objects {link:{caption:string,callback:Function}}
   */
  public toast(message : string,parameters : Object = null,dialogTitle:string=null,dialogMessage:string=null,additional : any = null) : void {
    if(this.lastToastMessage==message && (Date.now()-this.lastToastMessageTime)<Toast.MIN_TIME_BETWEEN_TOAST)
      return;
    this.lastToastMessage=message;
    this.lastToastMessageTime=Date.now();
    this.translate.get(message, parameters).subscribe((text: any) => {
      if(dialogTitle){
        text+='<br /><a onclick="window[\'toastComponent\'].openDetails()">'+this.translate.instant("DETAILS")+'</a>';
      }
      text=this.handleAdditional(text,additional);
      this.dialogParameters=parameters;
      this.toasty.info(this.getToastOptions(text));
      this.dialogTitle=dialogTitle;
      this.dialogMessage=dialogMessage;
    });
  }
  private openDetails(buttons:DialogButton[]=null){
    this.onShowModal({title:this.dialogTitle,message:this.dialogMessage,translation:this.dialogParameters,buttons:buttons,isCancelable:true});
  }

  private getToastOptions(text: string) {
    let timeout=8000 + UIAnimation.ANIMATION_TIME_NORMAL;
    return {
      title: "",
      msg: text,
      showClose: true,
      animate:'scale',
      timeout: timeout,
      onAdd: (toast:ToastData) => {
        setTimeout(()=> {
          let elements = document.getElementsByClassName("toasty-theme-default");
          let element:any = elements[elements.length - 1];
          element.style.opacity = 1;
          element.style.transform = "translateY(0)";
          setTimeout(()=>{
            element.style.opacity=0;
          },timeout-UIAnimation.ANIMATION_TIME_NORMAL);
        },10);
      },
      onRemove: function(toast:ToastData) {
      }
    };
  }

  /**
   * Generates a toast error message
   */
  public error(errorObject : any,message="COMMON_API_ERROR",parameters: any = null,dialogTitle='',dialogMessage='',additional : any = null) : void {
    let error=errorObject;
    let errorInfo="";
    let json:any=null;
    if(errorObject)
        json=errorObject.error;

    try {
      error=json.error+": "+json.message;
    }catch(e){}
    this.dialogTitle=dialogTitle;
    this.dialogMessage=dialogMessage;
    if(message=="COMMON_API_ERROR") {
      this.dialogMessage = '';
      this.dialogTitle = 'COMMON_API_ERROR_TITLE';
      console.log(errorObject);
      try {
        let error = json.error;
        if (error.stacktraceArray) {
          errorInfo = json.stacktraceArray.join('\n');
        }
        if(json.message.indexOf(RestConstants.CONTENT_QUOTA_EXCEPTION)!=-1){
          message = 'GENERIC_QUOTA_ERROR_TITLE';
          this.dialogTitle = '';
        }
        else if (json.error.indexOf("DAOToolPermissionException") != -1) {
          this.dialogTitle = 'TOOLPERMISSION_ERROR_TITLE';
          message = 'TOOLPERMISSION_ERROR';
          let permission = (json ? json.message : error).split(' ')[0];
          this.dialogMessage = this.translate.instant('TOOLPERMISSION_ERROR_HEADER') + "\n- " +
            this.translate.instant('TOOLPERMISSION.' + permission) + "\n\n" +
            this.translate.instant('TOOLPERMISSION_ERROR_FOOTER', {permission: permission});
        }
        else if (json.error.indexOf("SystemFolderDeleteDeniedException") != -1) {
          message = 'SYSTEM_FOLDER_DELETE_ERROR';
          this.dialogTitle = '';
        }
        else {
          this.dialogMessage = '';
          this.dialogTitle = 'COMMON_API_ERROR_TITLE';
          if (errorObject)
            errorInfo = JSON.stringify(json);
          try {
            if (json.stacktraceArray) {
              errorInfo = json.stacktraceArray.join("\n");
            }
            if (json.error.indexOf("DAOToolPermissionException") != -1) {
              this.dialogTitle = 'TOOLPERMISSION_ERROR_TITLE';
              message = 'TOOLPERMISSION_ERROR';
              let permission = error.split(' ')[0];
              this.dialogMessage = this.translate.instant('TOOLPERMISSION_ERROR_HEADER') + "\n- " +
                this.translate.instant('TOOLPERMISSION.' + permission) + "\n\n" +
                this.translate.instant('TOOLPERMISSION_ERROR_FOOTER', {permission: permission});
            }
            else if (json.error.indexOf("SystemFolderDeleteDeniedException") != -1) {
              message = 'SYSTEM_FOLDER_DELETE_ERROR';
              this.dialogTitle = '';
            }
            else if(json.message.indexOf("InvalidLogLevel")!=-1){
              error=json.error;
              errorInfo=json.message;
            }
          } catch (e) {
          }

        }
      } catch (e) {}
      if (errorInfo == undefined)
        errorInfo = '';
      if (errorObject.status == RestConstants.DUPLICATE_NODE_RESPONSE) {
        message = "WORKSPACE.TOAST.DUPLICATE_NAME";
        parameters = {name: name};
      }
      else if (errorObject.status == RestConstants.HTTP_FORBIDDEN) {
        message = "TOAST.API_FORBIDDEN";
        this.dialogTitle = null;

        let login=this.storage.get(TemporaryStorageService.SESSION_INFO);
        if(login && login.isGuest){
          this.toast('TOAST.API_FORBIDDEN_LOGIN');
          this.goToLogin();
          return;
        }
      }
      else if (errorObject.status == RestConstants.HTTP_UNAUTHORIZED) {
        this.toast('TOAST.API_FORBIDDEN_LOGIN');
        return;
      }
      else {
        if (!this.dialogMessage)
          this.dialogMessage = error + "\n\n" + errorInfo;
        if (!parameters)
          parameters = {};
        parameters["error"] = error;
      }
    }
    if(error && error.status==0 && this.cordova.isRunningCordova()){
        message='TOAST.NO_CONNECTION';
        this.dialogTitle = null;
    }
    if(this.lastToastError==message+JSON.stringify(parameters) && (Date.now()-this.lastToastErrorTime)<Toast.MIN_TIME_BETWEEN_TOAST)
      return;
    this.lastToastError=message+JSON.stringify(parameters);
    this.lastToastErrorTime=Date.now();
    console.log(message);
    this.translate.get(message, parameters).subscribe((text: any) => {
      if (this.dialogTitle) {
        text += '<br /><a onclick="window[\'toastComponent\'].openDetails()">' + this.translate.instant("DETAILS") + '</a>';
      }
      text=this.handleAdditional(text,additional);
      this.toasty.error(this.getToastOptions(text));
    });

  }
  public goToLogin(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"login"],{queryParams:{next:window.location}});
  }
  onShowModalDialog(param:Function) {
    this.onShowModal=param;
  }

  private handleAdditional(text:string,additional: any) {
      if(additional && additional.link){
          text+='<br /><a onclick="window[\'toastComponent\'].linkCallback()">'+this.translate.instant(additional.link.caption)+'</a>';
          this.linkCallback=additional.link.callback;
      }
      return text;
  }

    showModalDialog(title: string,message: string,buttons : DialogButton[],isCancelable=true,onCancel:Function=null,messageParamters:any=null) {
        this.onShowModal({title:title,message:message,isCancelable:isCancelable,translation:messageParamters,onCancel:onCancel,buttons:buttons});
    }
}
