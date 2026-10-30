import { trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnInit,
    ViewChild,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { filter, first, take } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    AltchaV1Service,
    AuthenticationService,
    HOME_REPOSITORY,
    NodeServiceUnwrapped,
    UserService,
} from 'ngx-edu-sharing-api';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { forkJoin } from 'rxjs';
import { NodeReportDialogData } from 'ngx-rendering-service-lib';
import { AltchaWidgetComponent } from './altcha-widget.component';

@Component({
    selector: 'es-node-report-dialog',
    templateUrl: 'node-report-dialog.component.html',
    styleUrls: ['node-report-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodeReportDialogComponent implements OnInit {
    data = inject<NodeReportDialogData>(CARD_DIALOG_DATA);
    private dialogRef = inject(CardDialogRef);
    private authenticationService = inject(AuthenticationService);
    private userService = inject(UserService);
    private translate = inject(TranslateService);
    private toast = inject(Toast);
    private nodeApi = inject(NodeServiceUnwrapped);
    private cdr = inject(ChangeDetectorRef);
    private altchaApi = inject(AltchaV1Service);

    readonly reasons = ['UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'];
    @ViewChild('formElement') formRef: ElementRef<HTMLFormElement>;
    private readonly altchaWidget = viewChild(AltchaWidgetComponent);

    /** challenge url of the ALTCHA widget, only set for guests if ALTCHA is enabled */
    readonly altchaChallengeUrl = signal<string | null>(null);
    readonly altchaMissing = signal(false);
    private altchaPayload: string | null = null;

    readonly form = new UntypedFormGroup({
        reason: new UntypedFormControl(''),
        comment: new UntypedFormControl(''),
        email: new UntypedFormControl('', [Validators.email, Validators.required]),
    });

    constructor() {
        const data = this.data;

        this.form.get('comment').clearValidators();
        this.form.get('reason').clearValidators();
        if (!data.showOptions) {
            this.form.get('comment').addValidators(Validators.required);
        } else {
            this.form.get('reason').addValidators(Validators.required);
        }
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
                new DialogButton('NODE_REPORT.REPORT', { color: 'primary' }, () => this.report()),
            ],
        });
        // Pre-fill the email field for logged-in users.
        forkJoin(
            this.authenticationService.observeLoginInfo().pipe(first()),
            this.userService.observeCurrentUser().pipe(first()),
        ).subscribe(([login, user]) => {
            if (!login.isGuest && !!user?.person?.profile?.email) {
                this.form.get('email').disable();
                this.form.patchValue({ email: user.person.profile.email });
            }
            if (login.isGuest || !login.isValidLogin) {
                this.initAltcha();
            }
        });
        // Disable close by backdrop click as soon as the user enters any value .
        this.form.valueChanges
            .pipe(
                filter((values) => Object.values(values).some((value) => !!value)),
                take(1),
            )
            .subscribe(() => this.dialogRef.patchConfig({ closable: Closable.Standard }));
    }

    cancel() {
        this.dialogRef.close();
    }

    /**
     * Show the ALTCHA widget if the backend has it enabled (the challenge endpoint returns no content otherwise)
     */
    private initAltcha() {
        this.altchaApi.getChallenge().subscribe({
            next: (challenge) => {
                if (challenge) {
                    this.altchaChallengeUrl.set(this.altchaApi.rootUrl + '/altcha/v1/challenge');
                }
            },
            error: (error) => console.warn('ALTCHA challenge could not be loaded', error),
        });
    }

    onAltchaPayloadChange(payload: string | null) {
        this.altchaPayload = payload;
        if (payload) {
            this.altchaMissing.set(false);
        }
    }

    shouldShowError(field: string) {
        const fieldControl = this.form.get(field);
        return !fieldControl.disabled && fieldControl.touched && !fieldControl.valid;
    }

    report() {
        if (this.form.valid) {
            if (this.altchaChallengeUrl() && !this.altchaPayload) {
                this.altchaMissing.set(true);
                return;
            }
            // Include value for possibly disabled email field.
            const value = this.form.getRawValue();
            this.setLoading(true);
            this.nodeApi
                .reportNode({
                    repository: HOME_REPOSITORY,
                    node: this.data.node.ref.id,
                    mode: this.data.mode === 'REVOKE_FEEDBACK' ? 'Feedback' : 'ReportProblem',
                    reason: this.getReasonAsString(value.reason),
                    userEmail: value.email,
                    userComment: value.comment,
                    'X-Altcha': this.altchaPayload ?? undefined,
                })
                .subscribe(
                    () => {
                        this.toast.toast(this.data.mode + '.DONE');
                        this.dialogRef.close();
                    },
                    (error: any) => {
                        this.setLoading(false);
                        this.toast.error(error);
                        // the payload is consumed by the backend, a new challenge is required for a retry
                        this.altchaWidget()?.reset();
                    },
                );
        } else {
            for (const field of ['reason', 'email']) {
                const control = this.form.get(field);
                if (!control.valid) {
                    control.markAsTouched();
                    this.focusField(field);
                    this.cdr.detectChanges();
                    break;
                }
            }
        }
    }

    setLoading(isLoading: boolean): void {
        this.dialogRef.patchState({ isLoading });
        if (isLoading) {
            this.form.disable();
        } else {
            this.form.enable();
        }
    }

    private getReasonAsString(reason: string) {
        return `${this.translate.instant('NODE_REPORT.REASONS.' + reason)} (${reason})`;
    }

    private focusField(field: string) {
        const form = this.formRef.nativeElement;
        const element = form.elements.namedItem(field);
        if (element instanceof HTMLElement) {
            element.focus();
        } else if (element instanceof RadioNodeList) {
            (element[0] as HTMLElement).focus();
        }
    }
}
