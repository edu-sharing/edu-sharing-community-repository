import {Component, Input, EventEmitter, Output} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {DialogButton, RestArchiveService, Node} from "../../../../core-module/core.module";
import {TemporaryStorageService} from "../../../../core-module/core.module";

@Component({
  selector: 'es-recycle-delete-confirmation',
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
            new DialogButton('RECYCLE.DELETE.CANCEL',{ color: 'standard' },()=>this.cancel()),
            new DialogButton('RECYCLE.DELETE.YES',{ color: 'primary' },()=>this.confirm()),
        ];
    }

    cancel() : void{
        this.onClose.emit();
    }
    private confirm() : void{
        this.service.set("recycleSkipDeleteConfirmation",this.skipDeleteConfirmation);
        this.onDelete.emit();
        this.cancel();
    }
}
