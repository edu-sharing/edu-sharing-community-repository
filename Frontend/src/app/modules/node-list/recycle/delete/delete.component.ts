import {Component, Input, EventEmitter, Output} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {RestArchiveService} from "../../../../core-module/core.module";
import {TemporaryStorageService} from "../../../../core-module/core.module";

@Component({
  selector: 'recycle-delete-confirmation',
  templateUrl: 'delete.component.html'
})
export class RecycleDeleteComponent {
  @Input() node: Node[];
  @Output() onDelete=new EventEmitter();
  @Output() onClose=new EventEmitter();
	private skipDeleteConfirmation : boolean;
	constructor(private service : TemporaryStorageService) {
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
