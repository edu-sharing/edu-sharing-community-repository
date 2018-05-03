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

        if (this.cordova.isRunningCordova()){

            // wait until cordova device init is ready
            this.cordova.subscribeServiceReady().subscribe(()=>{

                // per default go to app
                this.router.navigate(['app'],{replaceUrl:true});

            });

        }
        else{
            this.router.navigate([UIConstants.ROUTER_PREFIX+'login'],{replaceUrl:true});
        }
    }
}

