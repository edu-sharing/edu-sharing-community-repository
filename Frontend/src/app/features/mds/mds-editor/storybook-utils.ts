import { ApplicationConfig } from '@angular/core';

import { HttpClient, HttpHandler, HttpXhrBackend } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimations } from '@angular/platform-browser/animations';
import {
    DEFAULT_LANGUAGE,
    FakeMissingTranslationHandler,
    MissingTranslationHandler,
    TranslateCompiler,
    TranslateDefaultParser,
    TranslateFakeCompiler,
    TranslateFakeLoader,
    TranslateLoader,
    TranslateParser,
    TranslateService,
    TranslateStore,
    USE_DEFAULT_LANG,
    USE_EXTEND,
    USE_STORE,
} from '@ngx-translate/core';
import { EduSharingApiModule, MdsWidget } from 'ngx-edu-sharing-api';
import { CordovaService } from '../../../common/services/cordova.service';
import { Toast } from '../../../core-ui-module/toast';
import { InitialValues, MdsEditorInstanceService } from './mds-editor-instance.service';
import { ViewInstanceService } from './mds-editor-view/view-instance.service';
import { Subject, BehaviorSubject, Observable, of } from 'rxjs';
import { InputStatus, MdsWidgetValue } from '../types/types';

export const mdsStorybookProviders: ApplicationConfig['providers'] = [
    HttpClient,
    {
        provide: HttpHandler,
        useValue: new HttpXhrBackend({ build: () => new XMLHttpRequest() }),
    },
    importProvidersFrom(
        EduSharingApiModule.forRoot({
            rootUrl: '/api',
        }),
    ),
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    MdsEditorInstanceService,
    ViewInstanceService,
    CordovaService,
    Toast,
    TranslateService,
    TranslateStore,
    {
        provide: TranslateLoader,
        useClass: TranslateFakeLoader,
    },
    {
        provide: TranslateCompiler,
        useClass: TranslateFakeCompiler,
    },
    {
        provide: TranslateParser,
        useClass: TranslateDefaultParser,
    },
    {
        provide: MissingTranslationHandler,
        useClass: FakeMissingTranslationHandler,
    },
    {
        provide: USE_DEFAULT_LANG,
        useValue: true,
    },
    {
        provide: USE_STORE,
        useValue: true,
    },
    {
        provide: USE_EXTEND,
        useValue: true,
    },
    {
        provide: DEFAULT_LANGUAGE,
        useValue: 'de',
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
        console.log(value);
        this.status.next(value);
    }

    getStatus(): InputStatus {
        return this.status.value;
    }

    registerShowMissingRequired() {}

    setValue() {}

    getSuggestedValues(): Promise<MdsWidgetValue[]> {
        return Promise.resolve(this.definition.values);
    }
}
