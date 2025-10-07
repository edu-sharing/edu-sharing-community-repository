import { FormControl, UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { InputStatus, MdsWidget, RequiredMode } from '../../types/types';
import { Directive, EventEmitter } from '@angular/core';
import { MdsEditorWidgetCore } from '../mds-editor-widget-core.directive';
import { SuggestionResponseDto, SuggestionStatus } from 'ngx-edu-sharing-api';
import { DisplayValue } from './DisplayValues';
import { AuthorityNamePipe, ValueType } from 'ngx-edu-sharing-ui';
import { map, Observable } from 'rxjs';
import { Toast, ToastType } from '../../../../services/toast';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { TranslateService } from '@ngx-translate/core';

@Directive()
export abstract class MdsEditorWidgetBase extends MdsEditorWidgetCore {
    abstract readonly valueType: ValueType;

    /**
     * triggered when the input focus is lost
     */
    onBlur = new EventEmitter<void>();

    /**
     * this method should set focus on the primary input of the widget
     */
    focus(): void {
        // default implementation will do nothing
    }
    constructor(
        protected toast: Toast,
        public mdsEditorInstance: MdsEditorInstanceService,
        protected translate: TranslateService,
    ) {
        super(mdsEditorInstance, translate);
    }
    protected setValue(value: string[], dirty?: boolean): void {
        this.widget.setValue(value, dirty);
    }

    protected setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
    }

    /**
     * register the form control that should be updated when external value changes received
     */
    protected registerValueChanges(formControl: FormControl) {
        this.widget.setValueExternal.subscribe((value) => {
            formControl.setValue(value);
            this.setValue(value, true);
        });
    }

    protected getStandardValidators(
        overrides: { requiredValidator?: ValidatorFn } = {},
    ): ValidatorFn[] {
        const validators: ValidatorFn[] = [];
        const widgetDefinition = this.widget.definition;
        // Marking both `Mandatory` and `MandatoryForPublish` fields as required is needed for the
        // hint texts to be shown when revealing unfilled fields or leaving the field without
        // entering a value.
        if (
            this.mdsEditorInstance.editorMode !== 'search' &&
            (widgetDefinition.isRequired === RequiredMode.Mandatory ||
                widgetDefinition.isRequired === RequiredMode.MandatoryForPublish)
        ) {
            validators.push(overrides.requiredValidator ?? Validators.required);
        }
        return validators;
    }
    protected static attachGraphqlSelection(definition: MdsWidget, fields: string[]) {
        const id = MdsEditorWidgetBase.mapGraphqlId(definition);
        if (id) {
            const originalId = id[0];
            return fields.map((f) => originalId + '.' + f);
        }
        return null;
    }
    public static mapGraphqlId(definition: MdsWidget) {
        // @TODO: make types!
        const id = (definition as any).ids?.graphql;
        if (id) {
            return [(definition as any).ids?.graphql];
        }
        return null;
    }
    public static mapGraphqlSuggestionId(definition: MdsWidget): string[] {
        return [];
    }
}

@Directive()
/**
 * used by chips or tree based widget that might show suggestion chips
 */
export abstract class MdsEditorWidgetChipsSuggestionBase extends MdsEditorWidgetBase {
    // holds suggestions from users or automatic generated data
    chipsSuggestionsSubject: Observable<SuggestionResponseDto[]>;
    chipsControl: UntypedFormControl;

    abstract add(value: DisplayValue): void;
    abstract toDisplayValue(value: string): DisplayValue;
    initSuggestions(): void {
        this.chipsSuggestionsSubject = this.widget
            .getSuggestions()
            .pipe(map((suggestions) => suggestions?.filter((s) => s.status === 'PENDING')));
    }
    removeSuggestion(toBeRemoved: SuggestionResponseDto): void {
        this.updateSuggestionState(toBeRemoved, 'DECLINED');
    }
    addSuggestion(suggestion: SuggestionResponseDto) {
        this.add(this.toDisplayValue(suggestion.value as string));
        this.updateSuggestionState(suggestion, 'ACCEPTED');
    }
    getSuggestionTooltip(suggestion: SuggestionResponseDto): string | null {
        return `${this.translate.instant('MDS.SUGGESTION_TOOLTIP', {
            value: this.toDisplayValue(suggestion.value as string).label,
            // @TODO
            creator: new AuthorityNamePipe(this.translate).transform(suggestion.createdBy),
        })}`;
    }
    updateSuggestionState(modified: SuggestionResponseDto, status: SuggestionStatus) {
        modified.status = status;
        this.mdsEditorInstance.updateSuggestionState(this.widget.definition.id, modified);
        //this.chipsSuggestions.splice(this.chipsSuggestions.indexOf(suggestion), 1);
        this.widget.getSuggestions();
        this.widget.markSuggestionChanged();
        this.mdsEditorInstance.updateHasChanges();
        const suggestion = this.widget.getSuggestions().value.find((s) => s.id === modified.id);
        suggestion.status = status;
        this.widget.getSuggestions().next(this.widget.getSuggestions().value);
        this.toast.show({
            type: 'info',
            subtype: ToastType.InfoSimple,
            message: 'AI.TOAST.' + status,
        });
    }

    getSuggestions(): Observable<SuggestionResponseDto[]> {
        // console.log(this.chipsSuggestions, this.chipsControl);
        return this.chipsSuggestionsSubject?.pipe(
            map((suggestions) =>
                suggestions?.filter(
                    (s) => !this.chipsControl.value.some((s1: DisplayValue) => s1.key === s.value),
                ),
            ),
        );
    }
}
