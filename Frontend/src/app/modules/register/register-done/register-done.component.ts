import {Component} from '@angular/core';

@Component({
  selector: 'app-register-done',
  templateUrl: 'register-done.component.html',
  styleUrls: ['register-done.component.scss']
})
export class RegisterDoneComponent{

    loading=false;
    email = '';

    private _keyUrl = '';
    get keyUrl(){
        return this._keyUrl;
    }
    set keyUrl(keyUrl:string){
        this._keyUrl=keyUrl;
        this.loading=true;
        this.activate(keyUrl);
    }

    public editMail() {
        //TODO: @Simon
        // Zum Bearbeitung vom E-Mail
    }
    public sendMail(){
        //TODO: @Simon
        // E-Mail erneut versenden
        console.log("E-Mail erneut versendet");

    }

    private activate(keyUrl: string) {

    }
}