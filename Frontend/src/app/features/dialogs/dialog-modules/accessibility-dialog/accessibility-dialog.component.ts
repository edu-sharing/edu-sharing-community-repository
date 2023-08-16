import { Component, OnDestroy, OnInit } from '@angular/core';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { AccessibilityService, AccessibilitySettings, ToastDuration } from 'ngx-edu-sharing-ui';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';

@Component({
    selector: 'es-accessibility-dialog',
    templateUrl: './accessibility-dialog.component.html',
    styleUrls: ['./accessibility-dialog.component.scss'],
})
export class AccessibilityDialogComponent implements OnInit, OnDestroy {
    readonly TOAST_DURATION = ToastDuration;
    readonly destroyed$ = new Subject<void>();
    settings: AccessibilitySettings;

    private saveTrigger = new Subject<void>();

    constructor(
        private accessibility: AccessibilityService,
        private dialogRef: CardDialogRef<void, void>,
    ) {}

    ngOnInit() {
        this.accessibility
            .observe()
            .pipe(takeUntil(this.destroyed$))
            .subscribe({
                next: (settings) => (this.settings = { ...settings }),
            });
        this.saveTrigger
            .pipe(
                tap(() => this.dialogRef.patchState({ autoSavingState: 'saving' })),
                switchMap(() => rxjs.from(this.accessibility.set(this.settings))),
            )
            .subscribe({
                next: () => this.dialogRef.patchState({ autoSavingState: 'saved' }),
                error: () => this.dialogRef.patchState({ autoSavingState: 'error' }),
            });
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    updateSettings() {
        this.saveTrigger.next();
    }
}
