import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper,Node} from "../../../core-module/core.module";
import {VCard} from "../../../core-module/ui/VCard";
import {Toast} from "../../../core-ui-module/toast";
import {Translation} from "../../../core-ui-module/translation";
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../../../core-ui-module/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {MdsComponent} from "../../../common/ui/mds/mds.component";

@Component({
  selector: 'node-template',
  templateUrl: 'node-template.component.html',
  styleUrls: ['node-template.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation()),
  ]
})
export class NodeTemplateComponent  {
    @ViewChild('mds') mdsRef : MdsComponent;
    loading : boolean;
    _nodeId: string;
    node: Node;
    enabled: boolean;
  @Input() set nodeId(nodeId : string){
    this._nodeId=nodeId;
    this.loading=true;
    this.nodeService.getNodeMetadata(nodeId).subscribe((parent)=>{
        this.nodeService.getNodeTemplate(nodeId).subscribe((data)=>{
            this.node=data.node;
            this.enabled=data.enabled;
            if(!data.enabled){
              // check if this is the first time opening -> activate it
              if(parent.node.aspects.indexOf(RestConstants.CCM_ASPECT_METADATA_PRESETTING)==-1)
                this.enabled=true;
            }
            this.loading=false;
            console.log(data);
        });

    });

  }
  @Output() onClose=new EventEmitter();


  constructor(
      private nodeService : RestNodeService,
      private toast : Toast
  ){

  }
  save(){
    let data = this.enabled ? this.mdsRef.getValues() : {};
    this.loading=true;
    this.nodeService.setNodeTemplate(this._nodeId,this.enabled,data).subscribe(()=>{
      this.cancel();
      this.toast.toast('WORKSPACE.TOAST.METADATA_TEMPLATE_UPDATED');
    },(error)=>{
      this.toast.error(error);
    });
  }
  cancel(){
    this.onClose.emit();
  }
}
