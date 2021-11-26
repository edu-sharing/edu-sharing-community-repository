import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {Connector, DialogButton} from '../../../core-module/core.module';

@Component({
  selector: 'es-workspace-create-connector',
  templateUrl: 'create-connector.component.html',
  styleUrls: ['create-connector.component.scss']
})
export class WorkspaceCreateConnector  {
  public type = 0;
  public _name= '';
  public _connector: Connector;
  buttons: DialogButton[];
  @Input() set connector(connector: Connector){
    for (let i = 0; i < connector.filetypes.length; i++) {
      if (!connector.filetypes[i].creatable) {
        connector.filetypes.splice(i, 1);
      }
    }
    this._connector = connector;
  }
  @Input() set name(name: string){
    this._name = name;
  }
  @Output() onCancel= new EventEmitter();
  @Output() onCreate= new EventEmitter();

  public cancel() {
    this.onCancel.emit();
  }
  public create() {
    if (!this._name.trim()) {
      return;
    }
    this.onCreate.emit({name: this._name, type: this.getType()});
  }
  constructor() {
    this.buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
        new DialogButton('CREATE', DialogButton.TYPE_PRIMARY, () => this.create()),
    ];
  }
  getType() {
    return this._connector.filetypes[this.type];
  }
}
