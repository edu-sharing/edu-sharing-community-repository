import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from '../../core-ui-module/toast';
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from '@angular/router';
import {OAuthResult, LoginResult, AccessScope, Node} from '../../core-module/core.module';
import {RouterComponent} from '../../router/router.component';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../core-ui-module/translation';
import {RestConnectorService} from '../../core-module/core.module';
import {RestConstants} from '../../core-module/core.module';
import {ConfigurationService} from '../../core-module/core.module';
import {FrameEventsService} from '../../core-module/core.module';
import {SessionStorageService} from '../../core-module/core.module';
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
        if(!showUploadSelect){
            //@TODO: Tell the LMS to close?
            window.close();
        }
        this._showUploadSelect=showUploadSelect;
    }
    get showUploadSelect(){
        return this._showUploadSelect;
    }
    parent: Node;
    private reurl: string;
   constructor(
       private translate : TranslateService,
       private nodeHelper: NodeHelperService,
       private connector: RestConnectorService,
       private configService : ConfigurationService,
       private storage : SessionStorageService,
       private temporaryStorage : TemporaryStorageService,
       private router : Router,
       private route : ActivatedRoute,
       private node : RestNodeService,
   ){
       Translation.initialize(this.translate,this.configService,this.storage,this.route).subscribe(()=> {
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
    }
}
