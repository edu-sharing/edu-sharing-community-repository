import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {Permission, Usage} from '../../../../core-module/core.module';
import {RestConstants} from "../../../../core-module/core.module";
import {UIAnimation} from '../../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {TranslateService} from '@ngx-translate/core';

@Component({
  selector: 'es-workspace-share-usages',
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
  @Input() deleteList : any[];
  @Output() deleteListChange = new EventEmitter();
  @Input() showDelete = true;
  @Output() onRemoveAll = new EventEmitter();
  @Output() onRemove = new EventEmitter();
  showAll = false;
    isDeleted(usage:any){
        return this.deleteList.indexOf(usage)!=-1;
    }
    getIcon(){
        let map=WorkspaceUsageComponent.ICON_MAP[this.name.toUpperCase()];
        if(map)
            return map;
        return 'extension';
    }
    getName(usage:any){
        // may be a collection
        if(usage.collection)
            return usage.collection.title;

        let info = usage.usageXmlParams;
        if(info && info.general.referencedInName!=null) {
            const typeStr = 'WORKSPACE.SHARE.USAGE_SUBTYPE.'+info.general.referencedInType.toUpperCase();
            let type = this.translation.instant(typeStr);
            if(type === typeStr) {
                type =  info.general.referencedInType;
            }
            return this.translation.instant('WORKSPACE.SHARE.USAGE_INFO',{
                instance: info.general.referencedInInstance,
                type,
                name: info.general.referencedInName
            });
        }
        return usage.courseId;
    }
    public remove(usage:any){
        if(this.showDelete && usage.type!='INDIRECT') {
            if(this.isDeleted(usage))
                this.deleteList.splice(this.deleteList.indexOf(usage),1);
            else
                this.deleteList.push(usage);
            this.deleteListChange.emit(this.deleteList);
        }
    }
    constructor(private translation : TranslateService){

    }
}
