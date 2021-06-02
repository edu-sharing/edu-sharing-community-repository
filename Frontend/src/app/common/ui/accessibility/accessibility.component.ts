import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {DialogButton, SessionStorageService} from '../../../core-module/core.module';
import {Toast, ToastDuration, ToastType} from '../../../core-ui-module/toast';
import {Options} from 'ng5-slider/options';
import {TranslateService} from '@ngx-translate/core';

@Component({
    selector: 'app-accessibility',
    templateUrl: 'accessibility.component.html',
    styleUrls: ['accessibility.component.scss'],
    animations: [
        trigger('fromBottom', UIAnimation.fromBottom(UIAnimation.ANIMATION_TIME_SLOW)),
    ]
})


export class AccessibilityComponent implements OnInit {
    readonly TOAST_DURATION = ToastDuration;
    visible = false;
    buttons = DialogButton.getSaveCancel(
        () => this.hide(),
        () => this.save()
    );
    toastMode: 'important' | 'all' = null;
    toastDuration: ToastDuration = null;

    sliderOptions: Options = {
        floor: 0,
        ceil: ToastDuration.Infinite,
        animate: true,
        step: 1,
        draggableRange: true,
        minRange: 1,
        translate: (value) => this.getDuration(value)
    };

    constructor(
        private storage: SessionStorageService,
        private toast: Toast,
        private translate: TranslateService
    ) {
    }
    ngOnInit() {
    }

    private async save() {
        this.toast.showProgressDialog();
        await this.storage.set('accessibility_toastMode', this.toastMode).toPromise();
        await this.storage.set('accessibility_toastDuration', this.toastDuration).toPromise();
        await this.toast.refresh();
        this.toast.closeModalDialog();
        this.toast.show({
            message: 'ACCESSIBILITY.SAVED',
            type: 'info',
            subtype: ToastType.InfoSimple
        });
        this.visible = false;
    }

    getDuration(value = this.toastDuration) {
        const time = Toast.convertDuration(value);
        if(time) {
            return this.translate.instant('ACCESSIBILITY.TOAST_DURATION_SECONDS', {seconds: time});
        } else {
            return this.translate.instant('ACCESSIBILITY.TOAST_DURATION_INFINITE', {seconds: time});
        }
    }

    async show() {
        this.toastMode = null;
        this.toastDuration = null;
        this.visible = true;
        this.toastMode = await this.storage.get('accessibility_toastMode', 'all').toPromise();
        this.toastDuration = await this.storage.get('accessibility_toastDuration', ToastDuration.Seconds_5).toPromise();
    }
    hide() {
        this.visible = false;
    }
}
