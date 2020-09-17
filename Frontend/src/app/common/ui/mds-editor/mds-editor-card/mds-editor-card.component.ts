import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DialogButton, Node } from '../../../../core-module/core.module';
import { CardJumpmark } from '../../../../core-ui-module/components/card/card.component';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { FillTypeStatus } from '../../input-fill-progress/input-fill-progress.component';
import { delay } from 'rxjs/operators';

@Component({
    selector: 'app-mds-editor-card',
    templateUrl: './mds-editor-card.component.html',
    styleUrls: ['./mds-editor-card.component.scss'],
})
export class MdsEditorCardComponent implements OnInit {
    @Input() title: string;
    @Output() cancel = new EventEmitter();
    @Output() save = new EventEmitter();

    nodes: Node[];
    jumpMarks: CardJumpmark[];
    readonly buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel.emit()),
        new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => this.save.emit()),
    ];

    // Progress indicator
    completedProperties: FillTypeStatus;
    totalProperties: FillTypeStatus;

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.nodes = this.mdsEditorInstance.nodes.value;
        this.jumpMarks = this.getJumpMarks();
        this.mdsEditorInstance
            .getCanSave()
            .pipe(delay(0))
            .subscribe((value) => {
                this.buttons[1].disabled = !value;
            });
        this.mdsEditorInstance
            .getCompletionStatus()
            .pipe(delay(0))
            .subscribe((completionStatus) => {
                this.completedProperties = map(completionStatus, (entry) => entry.completed);
                this.totalProperties = map(completionStatus, (entry) => entry.total);
            });
    }

    private getJumpMarks(): CardJumpmark[] {
        return this.mdsEditorInstance.views.map(
            (view) => new CardJumpmark(view.id + '_header', view.caption, view.icon),
        );
    }
}

function map<K extends string, T, R>(
    dict: { [key in K]: T },
    f: (element: T) => R,
): { [key in K]: R } {
    return Object.entries(dict).reduce((acc, [key, value]) => {
        acc[key as K] = f(value as T);
        return acc;
    }, {} as { [key in K]: R });
}
