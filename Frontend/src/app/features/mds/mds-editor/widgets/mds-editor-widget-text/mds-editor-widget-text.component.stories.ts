import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { HttpClient, HttpHandler, HttpXhrBackend } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { MdsEditorWidgetTextComponent } from './mds-editor-widget-text.component';
import { SharedModule } from '../../../../../shared/shared.module';
import {
    InitialValues,
    MdsEditorInstanceService,
    Widget as WidgetInstance,
} from '../../mds-editor-instance.service';
import { CordovaService } from '../../../../../common/services/cordova.service';
import { InputStatus, MdsWidget, RequiredMode } from '../../../types/types';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { ViewInstanceService } from '../../mds-editor-view/view-instance.service';
import { provideAnimations } from '@angular/platform-browser/animations';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { Toast } from '../../../../../core-ui-module/toast';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
    DEFAULT_LANGUAGE,
    FakeMissingTranslationHandler,
    MissingTranslationHandler,
    TranslateCompiler,
    TranslateDefaultParser,
    TranslateFakeCompiler,
    TranslateFakeLoader,
    TranslateLoader,
    TranslateModule,
    TranslateParser,
    TranslateService,
    TranslateStore,
    USE_DEFAULT_LANG,
    USE_EXTEND,
    USE_STORE,
} from '@ngx-translate/core';
import { TranslationsModule } from 'ngx-edu-sharing-ui';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<MdsEditorWidgetTextComponent> = {
    title: 'Mds/Widget/Text',
    component: MdsEditorWidgetTextComponent,
    decorators: [
        moduleMetadata({
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective], // Prevent duplicate declaration of InfoMessageComponent
            imports: [SharedModule, MatSnackBarModule, TranslateModule, TranslationsModule],
        }),
        applicationConfig({
            providers: [
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
            ],
        }),
    ],
    parameters: {
        controls: {
            // exclude:/(type)/g
        },
    },
    argTypes: {
        type: {
            control: false,
        },
        bottomCaption: {
            control: {
                type: 'text',
            },
        },
        isRequired: {
            control: 'radio',
            options: [RequiredMode.Mandatory, RequiredMode.Optional, RequiredMode.Ignore],
        },
    } as any,
    args: {
        bottomCaption: 'Bottom Caption',
        isRequired: RequiredMode.Optional,
    } as any,
    tags: ['autodocs'],
    render: (args) => {
        return {
            props: {
                widget: new Widget({
                    placeholder: 'Placeholder...',
                    type: 'text',
                    ...args,
                }) as unknown as WidgetInstance,
            },
        };
    },
};

class Widget {
    readonly focusTrigger = new Subject<void>();
    readonly status = new BehaviorSubject<InputStatus>(null);
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    readonly defaultDefinition: Partial<MdsWidget> = {
        id: 'test',
        caption: 'Test',
        expandable: 'disabled',
    };

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
}
export default meta;
type Story = StoryObj<MdsEditorWidgetTextComponent>;
export const Empty: Story = {
    args: {
        type: 'text',
    } as any,
};
export const EmptyTextarea: Story = {
    args: {
        type: 'textarea',
    } as any,
};
