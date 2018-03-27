import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, AfterViewInit} from '@angular/core';
import {UIHelper} from "../../../../common/ui/ui-helper";

@Component({
  selector: 'workspace-share-choose-type',
  templateUrl: 'choose-type.component.html',
  styleUrls: ['choose-type.component.scss']
})
export class WorkspaceShareChooseTypeComponent implements AfterViewInit {
  ngAfterViewInit(): void {
    setTimeout(()=>UIHelper.setFocusOnDropdown(this.dropdownElement));
  }
  private _selected : string[];
  @ViewChild('publish') publish : ElementRef;
  @ViewChild('dropdown') dropdownElement : ElementRef;
  @Input() set selected (selected : string[]){
    this._selected=selected;
    setTimeout(()=>this.checkPublish(),10);
  }
  @Input() isDirectory=false;
  @Input() canPublish=true;
  @Output() onCancel = new EventEmitter();
  @Output() onType = new EventEmitter();

  public cancel(){
    this.onCancel.emit();
  }
  public setType(type : string){
    let types=['Consumer','Collaborator','Coordinator'];
    for(let type of types){
      if(this.contains(type))
        this._selected.splice(this._selected.indexOf(type),1);
    }
    this._selected.push(type);
    setTimeout(()=>this.checkPublish(),10);
    this.onType.emit({permissions:this._selected,wasMain:true});
  }
  public contains(type:string){
    return this._selected.indexOf(type)!=-1;
  }
  public checkPublish() {
    if(!this.publish)
      return;
    this.publish.nativeElement.checked=this.contains('CCPublish');
    this.publish.nativeElement.disabled=this.contains('Coordinator');
    if(this.contains('Coordinator'))
      this.publish.nativeElement.checked=true;
  }
  public setPublish(event : any){
    if(this.publish.nativeElement.checked){
      if(this.contains('CCPublish'))
        return;
      this._selected.push('CCPublish');
    }
    else{
      if(!this.contains('CCPublish'))
        return;
      this._selected.splice(this._selected.indexOf('CCPublish'),1);
    }
    this.onType.emit({permissions:this._selected,wasMain:false});

  }
}
