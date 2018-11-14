import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from '../../common/ui/toast';
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from '@angular/router';
import {OAuthResult, LoginResult, AccessScope, Node} from '../../common/rest/data-object';
import {RouterComponent} from '../../router/router.component';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../common/translation';
import {RestConnectorService} from '../../common/rest/services/rest-connector.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {ConfigurationService} from '../../common/services/configuration.service';
import {FrameEventsService} from '../../common/services/frame-events.service';
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../common/ui/ui-helper';
import {SessionStorageService} from '../../common/services/session-storage.service';
import {Scope} from '@angular/core/src/profile/wtf_impl';
import {UIConstants} from '../../common/ui/ui-constants';
import {Helper} from '../../common/helper';
import {RestHelper} from '../../common/rest/rest-helper';
import {PlatformLocation} from '@angular/common';

import {CordovaService} from "../../common/services/cordova.service";
import {InputPasswordComponent} from "../../common/ui/input-password/input-password.component";
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {NodeHelper} from '../../common/ui/node-helper';
import {TemporaryStorageService} from '../../common/services/temporary-storage.service';

@Component({
  selector: 'app-file-upload',
  templateUrl: 'file-upload.component.html',
  styleUrls: ['file-upload.component.scss']
})
export class FileUploadComponent{
    private filesToUpload: FileList;
    private loading = true;
    private _showUploadSelect: boolean;

    private set showUploadSelect(showUploadSelect: boolean){
        if(!showUploadSelect){
            //@TODO: Tell the LMS to close?
            console.log("Close requested");
            window.close();
        }
        this._showUploadSelect=showUploadSelect;
    }
    private get showUploadSelect(){
        return this._showUploadSelect;
    }
    private parent: Node;
    private reurl: string;
   constructor(
       private translate : TranslateService,
       private configService : ConfigurationService,
       private storage : SessionStorageService,
       private temporaryStorage : TemporaryStorageService,
       private router : Router,
       private route : ActivatedRoute,
       private node : RestNodeService,
       private title : Title
   ){
       Translation.initialize(this.translate,this.configService,this.storage,this.route).subscribe(()=> {
           UIHelper.setTitle('WORKSPACE.ADD_OBJECT_TITLE', title, translate, configService);
           this.node.getNodeMetadata(RestConstants.USERHOME).subscribe((node)=>{
               this.parent=node.node;
               this.route.queryParams.subscribe((params)=>{
                   this.reurl=params['reurl'];
               });
               this._showUploadSelect=true;
               this.loading=false;
           });
       });
   }

    uploadNodes(event: any) {
        this._showUploadSelect=false;
        this.filesToUpload=event;
    }
    onDone(node: Node){
       if(node==null){
           // canceled;
           this._showUploadSelect=true;
           return;
       }
       NodeHelper.addNodeToLms(this.router,this.temporaryStorage,node,this.reurl);
    }
}
