import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from '../../core-ui-module/toast';
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from '@angular/router';
import {OAuthResult, LoginResult, AccessScope, Node} from '../../core-module/core.module';
import {RouterComponent} from '../../router/router.component';
import {TranslateService} from '@ngx-translate/core';
import { TranslationsService } from '../../translations/translations.service';
import {RestConnectorService} from '../../core-module/core.module';
import {RestConstants} from '../../core-module/core.module';
import {FrameEventsService} from '../../core-module/core.module';
import {RestNodeService} from '../../core-module/core.module';
import {TemporaryStorageService} from '../../core-module/core.module';
import {NodeHelperService} from '../../core-ui-module/node-helper.service';

@Component({
  selector: 'es-file-upload',
  templateUrl: 'file-upload.component.html',
  styleUrls: ['file-upload.component.scss']
})
export class FileUploadComponent{
    filesToUpload: FileList;
    loading = true;
    _showUploadSelect: boolean;

    set showUploadSelect(showUploadSelect: boolean){
        this._showUploadSelect=showUploadSelect;
    }
    get showUploadSelect(){
        return this._showUploadSelect;
    }
    parent: Node;
    private reurl: string;
   constructor(
       private translations: TranslationsService,
       private nodeHelper: NodeHelperService,
       private connector: RestConnectorService,
       private temporaryStorage : TemporaryStorageService,
       private events : FrameEventsService,
       private router : Router,
       private route : ActivatedRoute,
       private node : RestNodeService,
   ){
       this.translations.waitForInit().subscribe(()=> {
           this.connector.isLoggedIn(false).subscribe((login) => {
               if(login.statusCode === RestConstants.STATUS_CODE_OK) {
                   this.nodeHelper.getDefaultInboxFolder().subscribe((n) => {
                       this.parent = n;
                       this.route.queryParams.subscribe((params)=>{
                           this.reurl=params['reurl'];
                       });
                       this._showUploadSelect=true;
                       this.loading=false;
                   });
               }
           });
       });
   }

    uploadNodes(event: FileList) {
        this._showUploadSelect=false;
        this.filesToUpload=event;
    }
    onDone(node: Node[]){
        if(node==null){
            // canceled;
            this._showUploadSelect=true;
            return;
        }
        this.nodeHelper.addNodeToLms(node[0],this.reurl);
        window.close();
    }

    cancel() {
        this.events.broadcastEvent(FrameEventsService.EVENT_UPLOAD_CANCELED);
        window.close();
    }
}
