import { DatePipe } from '@angular/common';
import {
    Component,
    DestroyRef,
    EventEmitter,
    forwardRef,
    Input,
    OnInit,
    Output,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validator,
    Validators,
} from '@angular/forms';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { DateAdapter } from '@angular/material/core';
import { provideDateFnsAdapter } from '@angular/material-date-fns-adapter';
import { de, enUS, fr, it, type Locale } from 'date-fns/locale';
import { merge } from 'rxjs';
import { SharedModule } from '../../../../../../shared/shared.module';

@Component({
    selector: 'es-share-dialog-choose-date',
    templateUrl: 'choose-date.component.html',
    styleUrls: ['choose-date.component.scss'],
    imports: [SharedModule],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ShareDialogChooseDateComponent),
            multi: true,
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ShareDialogChooseDateComponent),
            multi: true,
        },
        provideDateFnsAdapter(),
    ],
})
export class ShareDialogChooseDateComponent implements OnInit, ControlValueAccessor, Validator {
    private translationsService = inject(TranslationsService);
    private dateAdapter = inject<DateAdapter<any>>(DateAdapter);

    /**
     * The edited timestamp. Kept as a two-way bindable input/output for consumers that use
     * `[(dateTime)]` (e.g. the admin messages page); the same value is also propagated through the
     * `ControlValueAccessor` when the component is bound via `formControlName`.
     */
    @Input() set dateTime(value: number) {
        this._dateTime = value;
        this.setControlsFromTimestamp(value);
    }

    get dateTime(): number {
        return this._dateTime;
    }
    @Input() from?: number;
    @Input() to?: number;
    @Input() editable = false;
    /** custom errors if needed, e.g. the component is used as a single date selection */
    @Input() minError = 'WORKSPACE.SHARE.TIMEBASED.INVALID_RANGE';
    @Input() maxError = 'WORKSPACE.SHARE.TIMEBASED.INVALID_RANGE';
    @Output() dateTimeChange = new EventEmitter<number>();

    readonly dateControl = new FormControl<Date | null>(null);
    readonly timeControl = new FormControl('', [Validators.pattern(/^\d{1,2}:\d{2}$/)]);

    private _dateTime: number;
    private _onChange?: (value: number) => void;
    private _onTouched?: () => void;
    private _onValidatorChange?: () => void;
    private readonly destroyRef = inject(DestroyRef);

    private static readonly DATE_FNS_LOCALES: Record<string, Locale> = { de, en: enUS, fr, it };

    constructor() {
        const lang = this.translationsService.getLanguage() ?? '';
        this.dateAdapter.setLocale(ShareDialogChooseDateComponent.DATE_FNS_LOCALES[lang] ?? de);
    }

    ngOnInit(): void {
        merge(this.dateControl.valueChanges, this.timeControl.valueChanges)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this._onValidatorChange?.();
                this.emitCombined();
            });
    }

    isValid(from: number): boolean {
        return this.timeControl.valid && this.dateControl.valid && this.dateTime > (from ?? 0);
    }

    toDate(value: number): Date | null {
        return value ? new Date(value) : null;
    }

    /** Lower datepicker bound, normalized to the start of the day so the `from` day itself stays selectable. */
    get minDate(): Date | null {
        if (!this.from) {
            return null;
        }
        const date = new Date(this.from);
        date.setHours(0, 0, 0, 0);
        return date;
    }

    get maxDate(): Date | null {
        return this.to ? new Date(this.to) : null;
    }

    onBlur(): void {
        this._onTouched?.();
    }

    // --- ControlValueAccessor -------------------------------------------------

    writeValue(value: number): void {
        this._dateTime = value;
        this.setControlsFromTimestamp(value);
    }

    registerOnChange(fn: (value: number) => void): void {
        this._onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this._onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.dateControl.disable({ emitEvent: false });
            this.timeControl.disable({ emitEvent: false });
        } else {
            this.dateControl.enable({ emitEvent: false });
            this.timeControl.enable({ emitEvent: false });
        }
    }

    // --- Validator ------------------------------------------------------------

    validate(): ValidationErrors | null {
        const errors: ValidationErrors = {};
        if (this.timeControl.invalid) {
            errors['invalidTime'] = true;
        }
        if (this.dateControl.invalid && this.dateControl.errors) {
            Object.assign(errors, this.dateControl.errors);
        }
        return Object.keys(errors).length ? errors : null;
    }

    registerOnValidatorChange(fn: () => void): void {
        this._onValidatorChange = fn;
    }

    // --- internals ------------------------------------------------------------

    private setControlsFromTimestamp(value: number): void {
        const date = this.toDate(value);
        this.dateControl.setValue(date, { emitEvent: false });
        this.timeControl.setValue(date ? new DatePipe('en').transform(date, 'HH:mm') : '', {
            emitEvent: false,
        });
    }

    private emitCombined(): void {
        if (this.dateControl.invalid || this.timeControl.invalid) {
            // keep the previous value; invalidity is surfaced through validate()
            return;
        }
        const date = this.dateControl.value;
        if (!date) {
            return;
        }
        const combined = new Date(date.getTime());
        const time = this.timeControl.value;
        if (time && /^\d{1,2}:\d{2}$/.test(time)) {
            const [hours, minutes] = time.split(':');
            combined.setHours(parseInt(hours, 10), parseInt(minutes, 10), 0, 0);
        }
        this._dateTime = combined.getTime();
        this.dateTimeChange.emit(this._dateTime);
        this._onChange?.(this._dateTime);
    }
}
