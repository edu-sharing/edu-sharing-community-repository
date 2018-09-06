import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {Permission, Usage} from '../../../../common/rest/data-object';
import {RestConstants} from "../../../../common/rest/rest-constants";
import {UIAnimation} from '../../../../common/ui/ui-animation';
import {trigger} from '@angular/animations';

@Component({
  selector: 'workspace-share-usages',
  templateUrl: 'usage.component.html',
  styleUrls: ['usage.component.scss'],
    animations: [
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ]
})
export class WorkspaceUsageComponent  {
    static ICON_MAP:any={
        'MOODLE':'school',
        'COLLECTION':'layers'
    };
  @Input() name : string;
  @Input() usages : any[];
  @Input() showDelete = false;
  @Output() onRemoveAll = new EventEmitter();
  @Output() onRemove = new EventEmitter();
  showAll = false;

    getIcon(){
        let map=WorkspaceUsageComponent.ICON_MAP[this.name.toUpperCase()];
        if(map)
            return map;
        return 'extension';
    }
    getName(usage:any){
        // may be a collection
        if(usage.title)
            return usage.title;
        return usage.courseId;
    }
  public removeAll(){
    if(this.showDelete)
      this.onRemoveAll.emit();
  }
    public remove(usage:any){
        if(this.showDelete)
            this.onRemove.emit(usage);
    }
}
