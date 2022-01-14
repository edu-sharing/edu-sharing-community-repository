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
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../core-ui-module/ui-helper';
import {SessionStorageService} from '../../core-module/core.module';
import {RestNodeService} from '../../core-module/core.module';
import {NodeHelper} from '../../core-ui-module/node-helper';
import {TemporaryStorageService} from '../../core-module/core.module';

@Component({
  selector: 'app-file-upload',
  templateUrl: 'file-upload.component.html',
  styleUrls: ['file-upload.component.scss']
})
export class FileUploadComponent{
    filesToUpload: Node[];
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
    onDone(node: Node[]){
       if(node==null){
           // canceled;
           this._showUploadSelect=true;
           return;
       }
       NodeHelper.addNodeToLms(this.router,this.temporaryStorage,node[0],this.reurl);
    }
}
