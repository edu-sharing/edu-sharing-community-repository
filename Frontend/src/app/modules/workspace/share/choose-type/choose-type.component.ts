import {
    Component,
    Input,
    EventEmitter,
    Output,
    ViewChild,
    ElementRef,
    AfterViewInit,
} from '@angular/core';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';

@Component({
    selector: 'es-workspace-share-choose-type',
    templateUrl: 'choose-type.component.html',
    styleUrls: ['choose-type.component.scss'],
})
export class WorkspaceShareChooseTypeComponent implements AfterViewInit {
    publish: boolean;
    publishDisabled: boolean;
    ngAfterViewInit(): void {
        setTimeout(() => UIHelper.setFocusOnDropdown(this.dropdownElement));
    }
    private _selected: string[];
    @ViewChild('dropdown') dropdownElement: ElementRef;
    @Input() set selected(selected: string[]) {
        this._selected = selected;
        setTimeout(() => this.checkPublish(), 10);
    }
    @Input() isDirectory = false;
    @Input() canPublish = true;
    @Output() onCancel = new EventEmitter();
    @Output() onType = new EventEmitter();

    public cancel() {
        this.onCancel.emit();
    }
    public setType(type: string) {
        let types = ['Consumer', 'Collaborator', 'Coordinator'];
        for (let type of types) {
            if (this.contains(type)) this._selected.splice(this._selected.indexOf(type), 1);
        }
        this._selected.push(type);
        setTimeout(() => this.checkPublish(), 10);
        this.onType.emit({ permissions: this._selected, wasMain: true });
    }
    public contains(type: string) {
        return this._selected.indexOf(type) != -1;
    }
    public checkPublish() {
        this.publish = this.contains('CCPublish');
        this.publishDisabled = this.contains('Coordinator');
        if (this.contains('Coordinator')) this.publish = true;
    }
    public setPublish(event: MatSlideToggleChange) {
        if (event.checked) {
            if (this.contains('CCPublish')) return;
            this._selected.push('CCPublish');
        } else {
            if (!this.contains('CCPublish')) return;
            this._selected.splice(this._selected.indexOf('CCPublish'), 1);
        }
        this.onType.emit({ permissions: this._selected, wasMain: false });
    }
}
