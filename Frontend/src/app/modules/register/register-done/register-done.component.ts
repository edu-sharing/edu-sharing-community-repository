import {Component} from '@angular/core';

@Component({
  selector: 'app-register-done',
  templateUrl: 'register-done.component.html',
  styleUrls: ['register-done.component.scss']
})
export class RegisterDoneComponent{
    private editMail() {
        //TODO: @Simon
        // Zum Bearbeitung vom E-Mail
    }
    private sendMail(){
        //TODO: @Simon
        // E-Mail erneut versenden
        console.log("E-Mail erneut versendet");
    }

}