import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Tool } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../../core-module/ui/dialog-button';

@Component({
    selector: 'es-create-ltitool',
    templateUrl: './create-ltitool.component.html',
    styleUrls: ['./create-ltitool.component.scss'],
})
export class CreateLtitoolComponent implements OnInit {
    public _tool: Tool;
    buttons: DialogButton[];
    @Output() onCancel = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    public _name = '';

    constructor() {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('CREATE', { color: 'primary' }, () => this.create()),
        ];
    }

    ngOnInit(): void {}

    @Input() set tool(tool: Tool) {
        this._tool = tool;
    }

    public cancel() {
        this.onCancel.emit();
    }

    public create() {
        if (!this._name.trim()) {
            return;
        }
        this.onCreate.emit({ name: this._name, appId: this.tool.appId });
    }
}
