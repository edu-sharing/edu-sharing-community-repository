import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {Connector} from "../../../core-module/core.module";

@Component({
  selector: 'workspace-create-connector',
  templateUrl: 'create-connector.component.html',
  styleUrls: ['create-connector.component.scss']
})
export class WorkspaceCreateConnector  {
  @ViewChild('input') input : ElementRef;
  public type = 0;
  public _name="";
  public _connector : Connector;
  @Input() set connector  (connector : Connector){
    for(let i=0;i<connector.filetypes.length;i++){
      if(!connector.filetypes[i].creatable)
        connector.filetypes.splice(i,1);
    }
    this._connector=connector;
  }
  @Input() set name(name : string){
    this._name=name;
    this.input.nativeElement.focus();
  }
  @Output() onCancel=new EventEmitter();
  @Output() onCreate=new EventEmitter();

  public cancel(){
    this.onCancel.emit();
  }
  public create(){
    if(!this._name.trim())
      return;
    this.onCreate.emit({name:this._name,type:this.getType()});
  }
  constructor(){
  }
  private getType() {
    //return NodeHelper.OPEN_OFFICE_SUBTYPES[this.type];
    return this._connector.filetypes[this.type];
  }
}
