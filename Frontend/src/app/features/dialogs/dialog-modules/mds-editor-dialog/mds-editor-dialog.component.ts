import { AfterViewInit, Component, Inject, OnInit, TemplateRef, ViewChild } from '@angular/core';
import * as rxjs from 'rxjs';
import { filter, first, map } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { JumpMark } from '../../../../services/jump-marks.service';
import { FillTypeStatus } from '../../../mds/mds-editor/input-fill-progress/input-fill-progress.component';
import { MdsEditorCardComponent } from '../../../mds/mds-editor/mds-editor-card/mds-editor-card.component';
import { MdsEditorCoreComponent } from '../../../mds/mds-editor/mds-editor-core/mds-editor-core.component';
import { MdsEditorInstanceService } from '../../../mds/mds-editor/mds-editor-instance.service';
import { EditorType } from '../../../mds/types/types';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    hasNodes,
    hasValues,
    MdsEditorDialogData,
    MdsEditorDialogResult,
} from './mds-editor-dialog-data';

@Component({
    selector: 'es-mds-editor-dialog',
    templateUrl: './mds-editor-dialog.component.html',
    styleUrls: ['./mds-editor-dialog.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class MdsEditorDialogComponent implements OnInit, AfterViewInit {
    @ViewChild('customBottomBarContent') customBottomBarContent: TemplateRef<HTMLElement>;
    @ViewChild(MdsEditorCoreComponent) mdsEditorCore: MdsEditorCoreComponent;

    // Progress indicator
    completedProperties: FillTypeStatus;
    totalProperties: FillTypeStatus;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: MdsEditorDialogData,
        private dialogRef: CardDialogRef<MdsEditorDialogData, MdsEditorDialogResult>,
        private mdsEditorInstance: MdsEditorInstanceService,
        private toast: Toast,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    async ngOnInit(): Promise<void> {
        await this.initMdsEditor();
        this.initButtons();
        this.registerProgressIndicator();
        // `SendFeedbackDialog` works similar to this component. Please update accordingly when
        // making changes here.
        this.mdsEditorInstance.mdsInflated.pipe(first()).subscribe(() => {
            this.dialogRef.patchState({ isLoading: false });
            if (this.data.immediatelyShowMissingRequiredWidgets) {
                this.mdsEditorInstance.showMissingRequiredWidgets(false);
            }
        });
    }

    ngAfterViewInit(): void {
        this.registerJumpMarks();
        this.dialogRef.patchConfig({
            customBottomBarContent: this.customBottomBarContent,
        });
    }

    onShowMissing(): void {
        this.mdsEditorInstance.showMissingRequiredWidgets();
    }

    private async initMdsEditor(): Promise<void> {
        let editorType: EditorType;
        if (hasNodes(this.data)) {
            editorType = await this.mdsEditorInstance.initWithNodes(this.data.nodes, {
                groupId: this.data.groupId,
                bulkBehavior: this.data.bulkBehavior,
                editorMode: 'nodes',
            });
        } else if (hasValues(this.data)) {
            editorType = await this.mdsEditorInstance.initWithoutNodes(
                this.data.groupId,
                this.data.setId,
                this.data.repository,
                this.data.editorMode,
                this.data.values,
            );
        }
        if (editorType !== 'angular') {
            throw new Error(
                'Called mds-editor-dialog with legacy mds. Supports only "angular" rendering.',
            );
        }
    }

    private initButtons(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
            ],
        });
        this.mdsEditorInstance.observeHasUserChanges().subscribe((hasUserChanges) =>
            this.dialogRef.patchConfig({
                closable: hasUserChanges ? Closable.Confirm : Closable.Standard,
            }),
        );
    }

    private registerJumpMarks(): void {
        rxjs.combineLatest([
            this.mdsEditorInstance.activeViews,
            this.mdsEditorInstance.shouldShowExtendedWidgets$,
        ])
            .pipe(
                map(([activeViews]) =>
                    activeViews.map((view) =>
                        this.mdsEditorCore.viewRef?.find((v) => v.view.id === view.id),
                    ),
                ),
                map((viewRef) =>
                    viewRef
                        .filter((v) => v && !v.isInHiddenState() && v.view.caption)
                        .map(
                            (v) =>
                                new JumpMark(
                                    v.view.id + MdsEditorCardComponent.JUMPMARK_POSTFIX,
                                    v.view.caption,
                                    v.view.icon,
                                ),
                        ),
                ),
            )
            .subscribe((jumpMarks) => this.dialogRef.patchConfig({ jumpMarks }));
    }

    private registerProgressIndicator(): void {
        this.mdsEditorInstance
            .observeCompletionStatus()
            .pipe(filter((completionStatus) => !!completionStatus))
            .subscribe((completionStatus) => {
                this.completedProperties = mapDict(completionStatus, (entry) => entry.completed);
                this.totalProperties = mapDict(completionStatus, (entry) => entry.total);
            });
    }

    private async save(): Promise<void> {
        if (this.mdsEditorInstance.getCanSave()) {
            this.dialogRef.patchState({ isLoading: true });
            const updatedNodesOrValues = await this.mdsEditorInstance.save();
            this.toast.toast('WORKSPACE.EDITOR.UPDATED');
            this.dialogRef.close(updatedNodesOrValues);
        } else {
            // No changes, behave like close.
            if (this.mdsEditorInstance.getIsValid()) {
                this.dialogRef.close(null);
            } else {
                this.mdsEditorInstance.showMissingRequiredWidgets();
            }
        }
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
