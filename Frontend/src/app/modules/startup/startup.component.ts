import {
  Component,
  OnInit
} from '@angular/core';
import { CordovaService } from "../../common/services/cordova.service";
import {UIConstants} from '../../common/ui/ui-constants';
import {Router} from '@angular/router';

@Component({
  selector: 'app-startup',
  template: ''
})
export class StartupComponent {
    constructor(private cordova : CordovaService,private router:Router) {

        console.log("CONSTRUCTOR StartupComponent");

        if(this.cordova.isRunningCordova()){
            this.router.navigate(['app']);
        }
        else{
            this.router.navigate([UIConstants.ROUTER_PREFIX+'login']);
        }
    }
}

