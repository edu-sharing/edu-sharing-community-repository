import {Injectable} from "@angular/core";
import {Toast} from "../common/ui/toast";
import {CordovaService} from "../common/services/cordova.service";
import {DialogButton} from "../core-module/core.module";
import {Translation} from "../common/translation";

@Injectable()
export class BridgeService{
    constructor(private toast : Toast,private cordova : CordovaService){

    }
    showTemporaryMessage(type:MessageType,message:string|any,messageParameters:any=null){
       if(type==MessageType.info){
           //this.toast.toast(message,messageParameters);
       }
       if(type==MessageType.error){
           //this.toast.error(message,messageParameters);
       }
    }
    showModalDialog(title: string,message: string,buttons : DialogButton[],isCancelable=true,onCancel:Function=null,messageParamters:any=null) {
       //this.toast.showModalDialog(title,message,buttons,isCancelable,onCancel,messageParamters);
    }
    isRunningCordova(){
        return this.cordova.isRunningCordova();
    }
    getCordova(){
        return this.cordova;
    }

    getISOLanguage() {
        return Translation.getISOLanguage();
    }
}
export enum MessageType {
    info,
    error
}