import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {NodeWrapper,Node} from "../../../common/rest/data-object";
import {VCard} from "../../../common/VCard";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {Translation} from "../../../common/translation";
import {TranslateService} from "@ngx-translate/core";
import {DatepickerOptions} from "ng2-datepicker";
import {DateHelper} from "../../../common/ui/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {NgDatepickerComponent} from "ng2-datepicker";
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
