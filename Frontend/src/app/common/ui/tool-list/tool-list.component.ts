import {
  Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ApplicationRef,
  ChangeDetectorRef, ViewChild, ElementRef, HostListener, ViewEncapsulation
} from '@angular/core';
import {BrowserModule, DomSanitizer} from '@angular/platform-browser';
import {TranslateService} from "ng2-translate";
import {Node} from "../../rest/data-object";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestConstants} from "../../rest/rest-constants";
import {NodeHelper} from "../../ui/node-helper";
import {Translation} from "../../translation";
import {OptionItem} from "../actionbar/actionbar.component";
import {UIAnimation} from "../ui-animation";
import {TemporaryStorageService} from "../../services/temporary-storage.service";
import {Toast} from "../toast";
import {UIService} from "../../services/ui.service";
import {KeyEvents} from "../key-events";
import {FrameEventsService,EventListener} from "../../services/frame-events.service";
import {ConfigurationService} from "../../services/configuration.service";
import {RestHelper} from "../../rest/rest-helper";
import {trigger} from "@angular/animations";

@Component({
  selector: 'toolList',
  templateUrl: 'tool-list.component.html',
  styleUrls: ['tool-list.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('openOverlayBottom', UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
/**
 * A provider to render multiple Nodes as a list
 */
export class ToolListComponent{
  public _nodes : Node[];
  /**
   * Set the current list of nodes to render
   * @param nodes
   */
  @Input() set nodes(nodes : Node[]){
    this._nodes=nodes;
  };

  /**
   * Shall a "click" cursor be shown?
   * @type {boolean}
   */
  @Input() clickable=true;
  @Output() onClick=new EventEmitter();
  public click(node:Node){
    this.onClick.emit(node);
  }
}

