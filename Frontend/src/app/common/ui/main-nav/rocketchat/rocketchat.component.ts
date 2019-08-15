import {AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Input, Output, ViewChild} from "@angular/core";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {UIHelper} from "../../ui-helper";
import {EventListener, FrameEventsService} from "../../../services/frame-events.service";

@Component({
  selector: 'rocketchat',
  templateUrl: 'rocketchat.component.html',
  styleUrls: ['rocketchat.component.scss']
})
/**
 * An edu-sharing file-picker modal dialog
 */
export class RocketchatComponent{
    @ViewChild('frame') frame:ElementRef;
    _data: any;
    src: SafeResourceUrl;
    @Input() set data(data:any){
        this._data=data;
        this.src=this.getFrameUrl();
    }

    constructor(
        private sanitizer: DomSanitizer,
    ){
        window.addEventListener('message', (event:any)=>{
            if(event.source!==window.self) {
                console.log(event);
                console.log(this._data);
                if(event.data.eventName=='startup') {
                    this.frame.nativeElement.contentWindow.postMessage({
                        externalCommand: 'login-with-token',
                        token: this._data.token
                    }, this._data.url);
                }
            }
        }, false);
    }
    getFrameUrl() {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this._data.url+'?layout=embedded');
    }

    onEvent(event: string, data: any): void {

    }
}
