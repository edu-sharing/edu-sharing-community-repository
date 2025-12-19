import { Component, OnInit } from '@angular/core';
import { Options } from '@angular-slider/ngx-slider';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { ValueType } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, firstValueFrom, timer } from 'rxjs';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-mds-editor-widget-duration',
    templateUrl: './mds-editor-widget-duration.component.html',
    styleUrls: ['./mds-editor-widget-duration.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetDurationComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;

    sliderOptions: Options = {
        floor: 0,
        ceil: 0,
        animate: true,
        step: 1,
        draggableRange: true,
        minRange: 1,
        translate: (value: number): string => this.format(value),
    };

    currentValue: number; // Total minutes
    hours: string;
    minutes: string;
    aiSuggestion$ = new BehaviorSubject<SuggestionResponseDto>(null);

    async ngOnInit() {
        await this.initCurrentValue();
        this.sliderOptions.floor = this.widget.definition.min ?? 0;
        this.sliderOptions.ceil = this.widget.definition.max ?? 599;
        this.updateInput();
        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            this.sliderOptions = {
                ...this.sliderOptions,
                disabled: isDisabled,
            };
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
        this.widget.getShowAiSuggestions().subscribe(async ([show, suggestions]) => {
            const suggestion = suggestions?.find((s) => s.type === 'AI' && s.status === 'PENDING');
            if (this.aiSuggestion$.value?.status !== 'DECLINED') {
                if (this.widget.getIsDirty()) {
                    return;
                }
                if (suggestion && show) {
                    this.currentValue = this.msToMin(
                        parseInt(suggestion.value as string, 10),
                    ).valueMin;
                    // delay so 'slider' can trigger its own event first
                    await firstValueFrom(timer(1));
                    this.aiSuggestion$.next(suggestion);
                    this.widget.setSuggestionState(this.aiSuggestion$, 'ACCEPTED');
                    this.update('suggestion');
                } else if (!show && this.aiSuggestion$.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'PENDING');
                    void this.initCurrentValue();
                }
            }
        });
    }

    update(src: 'suggestion' | 'slider' | 'input') {
        if (src === 'slider' || src === 'suggestion') {
            this.updateInput();
        } else {
            this.currentValue = parseInt(this.hours, 10) * 60 + parseInt(this.minutes, 10);
        }
        if (src !== 'suggestion' && this.aiSuggestion$.value?.status === 'ACCEPTED') {
            this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
        }
        this.setValue_(src !== 'suggestion');
    }

    updateInput() {
        this.hours = Math.floor(this.currentValue / 60) + '';
        this.minutes = (this.currentValue % 60) + '';
    }

    private format(value: number): string {
        return value + ' ' + this.translate.instant('INPUT_MINUTES');
    }

    private async initCurrentValue() {
        const initialValues = (await this.widget.getInitalValuesAsync()).jointValues;
        let value = parseInt(initialValues[0] ?? '0', 10);
        // Internally values are saved as [ms].
        const { valueMin, wasMin } = this.msToMin(value);
        this.currentValue = valueMin;
        // Update legacy values, that were stored as [min].
        if (wasMin) {
            this.setValue_();
        }
    }

    /** Convert milliseconds to minutes. */
    private msToMin(valueMs: number): { valueMin: number; wasMin?: boolean } {
        if (!valueMs) {
            return { valueMin: valueMs };
        }
        // Graceful migration of values falsely saved as [min].
        if (valueMs > 0 && valueMs < 1000) {
            return { valueMin: valueMs, wasMin: true };
        }
        return { valueMin: valueMs / 60000 };
    }

    private setValue_(dirty = true): void {
        this.setValue([(this.currentValue * 60000).toString()], dirty);
    }
}
