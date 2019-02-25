import {Component, Input, EventEmitter, Output} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {RestArchiveService} from "../../../../common/rest/services/rest-archive.service";
import {TemporaryStorageService} from "../../../../common/services/temporary-storage.service";
import {DialogButton} from "../../../../common/ui/modal-dialog/modal-dialog.component";

@Component({
  selector: 'recycle-delete-confirmation',
  templateUrl: 'delete.component.html'
})
export class RecycleDeleteComponent {
  @Input() node: Node[];
  @Output() onDelete=new EventEmitter();
  @Output() onClose=new EventEmitter();
	skipDeleteConfirmation : boolean;
    buttons: DialogButton[];
	constructor(private service : TemporaryStorageService) {
	  this.buttons=[
	      new DialogButton('RECYCLE.DELETE.CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
	      new DialogButton('RECYCLE.DELETE.YES',DialogButton.TYPE_PRIMARY,()=>this.confirm()),
      ];
  }

  private cancel() : void{
    this.onClose.emit();
  }
  private confirm() : void{
    this.service.set("recycleSkipDeleteConfirmation",this.skipDeleteConfirmation);
    this.onDelete.emit();
    this.cancel();
  }
}
