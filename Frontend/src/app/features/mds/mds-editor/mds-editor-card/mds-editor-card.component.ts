import { takeUntil, delay, map, filter } from 'rxjs/operators';
import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    OnDestroy,
    ViewChild,
} from '@angular/core';
import { DialogButton, Node } from '../../../../core-module/core.module';
import { JumpMark } from '../../../../services/jump-marks.service';
import { Toast } from '../../../../core-ui-module/toast';
import { FillTypeStatus } from '../input-fill-progress/input-fill-progress.component';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { ReplaySubject, Observable, combineLatest } from 'rxjs';
import { MdsEditorCoreComponent } from '../mds-editor-core/mds-editor-core.component';

/**
 * @deprecated replaced with MdsEditorDialogComponent
 */
@Component({
    selector: 'es-mds-editor-card',
    templateUrl: './mds-editor-card.component.html',
    styleUrls: ['./mds-editor-card.component.scss'],
})
export class MdsEditorCardComponent implements OnInit, OnDestroy {
    @ViewChild('core') coreRef: MdsEditorCoreComponent;
    constructor(private mdsEditorInstance: MdsEditorInstanceService, private toast: Toast) {
        this.mdsEditorInstance.isEmbedded = false;
    }
    public static JUMPMARK_POSTFIX = '_header';
    @Input() title: string;
    @Input() set labels(labels: { [key in 'NEGATIVE' | 'POSITIVE']: string }) {
        this.buttons[0].label = labels.NEGATIVE;
        this.buttons[1].label = labels.POSITIVE;
    }
    @Input() priority = 0;
    @Output() cancel = new EventEmitter();
    @Output() save = new EventEmitter();

    nodes: Node[];
    jumpMarks: JumpMark[];
    readonly buttons = [
        new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel.emit()),
        new DialogButton('SAVE', { color: 'primary' }, () => this.save.emit()),
    ];

    // Progress indicator
    completedProperties: FillTypeStatus;
    totalProperties: FillTypeStatus;

    private readonly destroyed = new ReplaySubject<void>(1);

    ngOnInit(): void {
        this.nodes = this.mdsEditorInstance.nodes$.value;
        this.getJumpMarks()
            .pipe(takeUntil(this.destroyed))
            .subscribe((jumpMarks) => (this.jumpMarks = jumpMarks));
        this.mdsEditorInstance
            .observeCompletionStatus()
            .pipe(delay(0))
            .subscribe((completionStatus) => {
                this.completedProperties = mapDict(completionStatus, (entry) => entry.completed);
                this.totalProperties = mapDict(completionStatus, (entry) => entry.total);
            });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    confirmDiscard(): void {
        if (this.mdsEditorInstance.getHasUserChanges()) {
            this.toast.showModalDialog(
                'DIALOG.CONFIRM_DISCARD_TITLE',
                'DIALOG.CONFIRM_DISCARD_MESSAGE',
                [
                    new DialogButton('CANCEL', { color: 'standard' }, () => {
                        this.toast.closeModalDialog();
                    }),
                    new DialogButton('DISCARD', { color: 'primary' }, () => {
                        this.cancel.emit();
                        this.toast.closeModalDialog();
                    }),
                ],
                true,
            );
        } else {
            this.cancel.emit();
        }
    }

    onShowMissing(): void {
        this.mdsEditorInstance.showMissingRequiredWidgets();
    }

    private getJumpMarks(): Observable<JumpMark[]> {
        return combineLatest([
            this.mdsEditorInstance.activeViews,
            this.mdsEditorInstance.shouldShowExtendedWidgets$,
        ]).pipe(
            map(([activeViews]) =>
                activeViews.map((view) =>
                    this.coreRef?.viewRef?.find((v) => v.view.id === view.id),
                ),
            ),
            map((viewRef) =>
                viewRef
                    .filter((v) => v && !v.isInHiddenState())
                    .map(
                        (v) =>
                            new JumpMark(
                                v.view.id + MdsEditorCardComponent.JUMPMARK_POSTFIX,
                                v.view.caption,
                                v.view.icon,
                            ),
                    ),
            ),
        );
    }
}

function mapDict<K extends string, T, R>(
    dict: { [key in K]: T },
    f: (element: T) => R,
): { [key in K]: R } {
    return Object.entries(dict).reduce((acc, [key, value]) => {
        acc[key as K] = f(value as T);
        return acc;
    }, {} as { [key in K]: R });
}
