import {Component, Input, OnInit} from '@angular/core';
import {RestNodeService} from "../../../core-module/rest/services/rest-node.service";
import {Node} from "../../../core-module/rest/data-object"
import {RestConstants} from "../../../core-module/rest/rest-constants";

@Component({
  selector: 'video-controls',
  templateUrl: 'video-controls.component.html',
  styleUrls: ['video-controls.component.scss']
})
export class VideoControlsComponent{

  @Input() video:HTMLVideoElement;
  @Input() node:Node;
  constructor(private nodeService : RestNodeService) {
  }
  isReadOnly(){
    return this.node.access.indexOf(RestConstants.ACCESS_WRITE)==-1;
  }
}
