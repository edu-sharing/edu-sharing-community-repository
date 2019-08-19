import {AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Input, Output, ViewChild} from "@angular/core";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {trigger} from "@angular/animations";
import {MainNavComponent} from "../../main-nav/main-nav.component";
import {EventListener,FrameEventsService} from "../../../../core-module/rest/services/frame-events.service";
import {UIAnimation} from "../../../../core-module/ui/ui-animation";
import {RestConnectorService} from "../../../../core-module/core.module";

@Component({
  selector: 'rocketchat',
  templateUrl: 'rocketchat.component.html',
  styleUrls: ['rocketchat.component.scss'],
  animations: [
    trigger('toggle', UIAnimation.openOverlayBottom()),
  ]
})
/**
 * An edu-sharing file-picker modal dialog
 */
export class RocketchatComponent implements EventListener{
    onEvent(event: string, data: any): void {
        if(event==FrameEventsService.EVENT_USER_LOGGED_IN || event==FrameEventsService.EVENT_USER_LOGGED_OUT)
            this.initalize(true)
    }
    @ViewChild('frame') frame:ElementRef;
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if(event.code=="Escape" && this.fullscreen){
            event.preventDefault();
            event.stopPropagation();
            this.fullscreen=false;
            return;
        }
    }
    _data: any;
    src: SafeResourceUrl;
    opened=false;
    fullscreen=false;
    loaded=false;
    unread=0;
    constructor(
        private sanitizer: DomSanitizer,
        private connector: RestConnectorService,
        private events: FrameEventsService
    ){
        this.events.addSelfListener(this);
        this.initalize(false);
        window.addEventListener('message', (event:any)=>{
            if(event.source!==window.self) {
                console.log(event);
                if(event.data.eventName=='startup') {
                    this.loaded=true;
                    this.frame.nativeElement.contentWindow.postMessage({
                        externalCommand: 'login-with-token',
                        token: this._data.token
                    }, this._data.url);
                }
                if(event.data.eventName=='new-message' && !this.opened){
                    this.unread++;
                }
            }
        }, false);
    }
    getFrameUrl() {
        //return this.sanitizer.bypassSecurityTrustResourceUrl(this._data.url+'/channel/general?layout=embedded');
        return this.sanitizer.bypassSecurityTrustResourceUrl(this._data.url+'/channel/general');
    }

    private initalize(forceRenew=false) {
        this._data=null;
        this.src=null;
        this.opened=false;
        this.fullscreen=false;
        if(MainNavComponent.getPreloading()) {
            setTimeout(() => this.initalize(), 100);
            return;
        }
        this.connector.isLoggedIn(forceRenew).subscribe((login)=>{
            if(login.remoteAuthentications && login.remoteAuthentications.ROCKETCHAT){
                this._data=login.remoteAuthentications.ROCKETCHAT;
                this.src=this.getFrameUrl();
                console.log("initalizing rocketchat "+this.src);

            }
        });
    }
}
