import { Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MatTooltip } from '@angular/material/tooltip';
import { MdsWidget, MdsWidgetValue } from '../../../types/types';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { MatSelect } from '@angular/material/select';
import { ValueType } from 'ngx-edu-sharing-ui';
import { BehaviorSubject } from 'rxjs';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-mds-editor-widget-select',
    templateUrl: './mds-editor-widget-select.component.html',
    styleUrls: ['./mds-editor-widget-select.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetSelectComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    @ViewChild(MatSelect) matSelect: MatSelect;

    values: Promise<MdsWidgetValue[]>;
    formControl: UntypedFormControl;
    aiSuggestion$ = new BehaviorSubject<SuggestionResponseDto>(null);

    readonly showTooltip = (() => {
        let previousTooltip: MatTooltip;
        return (tooltip?: MatTooltip) => {
            previousTooltip?.hide();
            tooltip?.show();
            previousTooltip = tooltip;
        };
    })();

    focus() {
        this.matSelect.open();
    }

    showBulkMixedValues() {
        return this.widget.getInitialValues() && this.mdsEditorInstance.editorBulkMode?.isBulk;
    }
    async ngOnInit() {
        this.formControl = new UntypedFormControl(null, this.getStandardValidators());
        const initialValue = (await this.widget.getInitalValuesAsync()).jointValues[0];
        this.values = this.widget.getSuggestedValues();
        if (initialValue) {
            void this.values.then((values) => {
                const value = values.find((v) => v.id === initialValue);
                if (value) {
                    this.formControl.setValue(value);
                } else {
                    throw new Error(
                        `Invalid node value "${initialValue}" for ${this.widget.definition.id}`,
                    );
                }
            });
            this.formControl.valueChanges.subscribe((value) => {
                this.setValue(value ? [value.id] : [null]);
                if (this.aiSuggestion$.value && value.id !== this.aiSuggestion$.value.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
                }
            });
        } else {
            // skip first because the init state will cause a trigger
            this.formControl.valueChanges.subscribe((value) => {
                this.setValue(value ? [value.id] : [null]);
                if (this.aiSuggestion$.value && value?.id !== this.aiSuggestion$.value.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
                }
            });
        }
        this.widget.getShowAiSuggestions().subscribe(async ([show, suggestions]) => {
            const suggestion = suggestions?.find((s) => s.type === 'AI' && s.status === 'PENDING');
            if (this.aiSuggestion$.value?.status !== 'DECLINED') {
                if (!this.formControl.value && suggestion && show) {
                    const value = (await this.values).find((v) => v.id === suggestion.value);
                    if (value) {
                        this.aiSuggestion$.next(suggestion);
                        this.widget.setSuggestionState(this.aiSuggestion$, 'ACCEPTED');
                        this.setValue([suggestion.value as string]);
                        this.formControl.setValue(value, { emitEvent: false });
                    } else {
                        console.warn(
                            `Invalid suggestion value ${suggestion.value} for widget`,
                            this.widget.definition.id,
                            this.widget.definition.values,
                        );
                    }
                } else if (!initialValue && !show && this.aiSuggestion$.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'PENDING');
                    this.setValue(null);
                    this.formControl.setValue(null, { emitEvent: false });
                }
            }
        });
        this.registerValueChanges(this.formControl);
    }

    onActiveDescendantChanges(elementId: string) {
        const element = document.getElementById(elementId);
        this.showTooltip((element as any)?.tooltip);
    }

    public static mapGraphqlId(definition: MdsWidget) {
        // attach the "RangedValue" graphql Attributes
        return MdsEditorWidgetBase.attachGraphqlSelection(definition, ['id', 'value']);
    }
}
