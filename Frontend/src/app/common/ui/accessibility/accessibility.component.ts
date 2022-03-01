import { trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DialogButton } from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { Toast, ToastDuration, ToastType } from '../../../core-ui-module/toast';
import { AccessibilityService, AccessibilitySettings } from './accessibility.service';

@Component({
    selector: 'es-accessibility',
    templateUrl: 'accessibility.component.html',
    styleUrls: ['accessibility.component.scss'],
    animations: [trigger('fromBottom', UIAnimation.fromBottom(UIAnimation.ANIMATION_TIME_SLOW))],
})
export class AccessibilityComponent implements OnInit, OnDestroy {
    readonly TOAST_DURATION = ToastDuration;
    readonly buttons = DialogButton.getSaveCancel(
        () => this.hide(),
        () => this.save(),
    );
    readonly destroyed$ = new Subject<void>();
    visible = false;
    settings: AccessibilitySettings;

    constructor(private accessibility: AccessibilityService, private toast: Toast) {}

    ngOnInit() {
        this.accessibility
            .observe()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((settings) => (this.settings = { ...settings }));
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private async save() {
        this.toast.showProgressDialog();
        await this.accessibility.set(this.settings);
        this.toast.closeModalDialog();
        this.toast.show({
            message: 'ACCESSIBILITY.SAVED',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
        this.visible = false;
    }

    async show() {
        this.visible = true;
    }

    hide() {
        this.visible = false;
    }
}
