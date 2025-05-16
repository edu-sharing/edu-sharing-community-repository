import { ApplicationConfig } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { MdsWidget, SuggestionResponseDto } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Observable, Subject, of } from 'rxjs';
import { CordovaService } from '../../../services/cordova.service';
import { Toast } from '../../../services/toast';
import { InputStatus, MdsWidgetValue } from '../types/types';
import { InitialValues, MdsEditorInstanceService } from './mds-editor-instance.service';
import { ViewInstanceService } from './mds-editor-view/view-instance.service';
import { UserSimple } from 'ngx-edu-sharing-b-api';
import { CardDialogService } from '../../dialogs/card-dialog/card-dialog.service';

export const translateProvider = {
    instant: (v: string) => v,
    get: (v: string) => of(v),
    onTranslationChange: of({ lang: 'none' }),
    onDefaultLangChange: of({ lang: 'none' }),
    onLangChange: of({}),
};
export const mdsStorybookProviders: ApplicationConfig['providers'] = [
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    MdsEditorInstanceService,
    ViewInstanceService,
    CordovaService,
    Toast,
    {
        provide: TranslateService,
        useValue: translateProvider,
    },
    MatSnackBar,
    provideAnimations(),
];

export class WidgetDummy {
    readonly focusTrigger = new Subject<void>();
    readonly status = new BehaviorSubject<InputStatus>(null);
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    readonly defaultDefinition: Partial<MdsWidget> = {};

    public constructor(public definition: MdsWidget) {
        this.definition = { ...this.defaultDefinition, ...this.definition };
    }

    getInitalValuesAsync(): Promise<InitialValues> {
        return Promise.resolve(this.getInitialValues());
    }

    getInitialValues(): InitialValues {
        return {
            individualValues: [],
            jointValues: [],
        };
    }

    observeIsDisabled(): Observable<boolean> {
        return of(false);
    }

    public getInternalError(): string {
        return '';
    }

    setStatus(value: InputStatus): void {
        this.status.next(value);
    }

    getStatus(): InputStatus {
        return this.status.value;
    }

    registerShowMissingRequired() {}

    setValue() {}

    getSuggestions(): BehaviorSubject<SuggestionResponseDto[]> {
        return new BehaviorSubject([
            {
                value: 'hello world',
                propertyId: this.definition.id,
                type: 'AI',
                confidence: 1,
                description: 'description',
                version: '1',
                createdBy: {
                    authorityName: 'use',
                },
                status: 'PENDING',
                created: '',
                id: 'id',
                nodeId: 'nodeid',
            },
        ]);
    }
}
