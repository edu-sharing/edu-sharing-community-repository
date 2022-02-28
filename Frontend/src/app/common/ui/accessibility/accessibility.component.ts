import { trigger } from '@angular/animations';
import { Component, OnInit } from '@angular/core';
import { DialogButton, SessionStorageService } from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { Toast, ToastDuration, ToastType } from '../../../core-ui-module/toast';

@Component({
    selector: 'es-accessibility',
    templateUrl: 'accessibility.component.html',
    styleUrls: ['accessibility.component.scss'],
    animations: [trigger('fromBottom', UIAnimation.fromBottom(UIAnimation.ANIMATION_TIME_SLOW))],
})
export class AccessibilityComponent implements OnInit {
    readonly TOAST_DURATION = ToastDuration;
    visible = false;
    readonly buttons = DialogButton.getSaveCancel(
        () => this.hide(),
        () => this.save(),
    );
    toastMode: 'important' | 'all' = null;
    toastDuration: ToastDuration = null;

    constructor(private storage: SessionStorageService, private toast: Toast) {}

    ngOnInit() {}

    private async save() {
        this.toast.showProgressDialog();
        await this.storage.set('accessibility_toastMode', this.toastMode);
        await this.storage.set('accessibility_toastDuration', this.toastDuration);
        await this.toast.refresh();
        this.toast.closeModalDialog();
        this.toast.show({
            message: 'ACCESSIBILITY.SAVED',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
        this.visible = false;
    }

    async show() {
        this.toastMode = null;
        this.toastDuration = null;
        this.visible = true;
        this.toastMode = await this.storage.get('accessibility_toastMode', 'all').toPromise();
        this.toastDuration = await this.storage
            .get('accessibility_toastDuration', ToastDuration.Seconds_5)
            .toPromise();
    }

    hide() {
        this.visible = false;
    }
}
