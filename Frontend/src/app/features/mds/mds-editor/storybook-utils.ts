import { ApplicationConfig, EventEmitter, importProvidersFrom } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import {
    HOME_REPOSITORY,
    MdsWidget,
    Node,
    RestConstants,
    SuggestionResponseDto,
} from 'ngx-edu-sharing-api';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { CordovaService } from '../../../services/cordova.service';
import { Toast } from '../../../services/toast';
import { InputStatus, MdsWidgetValue } from '../types/types';
import { MdsEditorInstanceService } from './mds-editor-instance.service';
import {
    InitialValues,
    MdsValueList,
    MdsViewerService,
    ViewInstanceService,
} from 'ngx-edu-sharing-ui';

export const translateProvider = {
    instant: (v: string) => v,
    get: (v: string) => of(v),
    onTranslationChange: of({ lang: 'none' }),
    onDefaultLangChange: of({ lang: 'none' }),
    onLangChange: of({}),
};
export class MdsEditorInstanceServiceMock extends MdsEditorInstanceService {
    nodes$ = new BehaviorSubject<Node[]>([
        {
            ref: {
                id: 'nodeid',
                repo: HOME_REPOSITORY,
                archived: false,
            },
        },
    ] as Node[]);
    widgets = new BehaviorSubject([(window as any).widget]);
}
export class MdsViewerServiceMock extends MdsViewerService {
    values$ = new BehaviorSubject({
        [RestConstants.CCM_PROP_LICENSE]: ['CC_0'],
    });
}
export const mdsStorybookProviders: ApplicationConfig['providers'] = [
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    { provide: MdsEditorInstanceService, useClass: MdsEditorInstanceServiceMock },
    { provide: MdsViewerService, useClass: MdsViewerServiceMock },
    ViewInstanceService,
    CordovaService,
    Toast,
    {
        provide: TranslateService,
        useValue: translateProvider,
    },
    MatSnackBar,
    provideAnimations(),
    importProvidersFrom(MatSnackBarModule),
];

export class WidgetDummy extends MdsEditorInstanceService.Widget {
    readonly setValueExternal = new Subject<string[]>();
    readonly focusTrigger = new Subject<void>();
    readonly addValue = new EventEmitter<MdsWidgetValue>();
    readonly status = new BehaviorSubject<InputStatus>(null);
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    static readonly defaultDefinition: Partial<MdsWidget> = {
        id: 'id',
        values: [
            { id: 'V1', caption: 'Value 1' },
            { id: 'V2', caption: 'Value 2' },
            { id: 'T1', caption: 'Test 1' },
            { id: 'T2', caption: 'Test 2' },
        ],
    };
    readonly fakeBackendValues = [
        { id: 'V1', caption: 'Value 1' },
        { id: 'V2', caption: 'Value 2' },
        { id: 'T1', caption: 'Test 1' },
        { id: 'T2', caption: 'Test 2' },
    ];

    protected readonly suggestionValuesSubject = new BehaviorSubject([
        {
            value: 'T1',
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
            id: 'idT1',
            nodeId: 'nodeid',
        },
        {
            value: 'V1',
            propertyId: this.definition.id,
            type: 'USER_PROPOSAL',
            confidence: 1,
            description: 'description',
            version: '1',
            createdBy: {
                authorityName: 'use',
            },
            status: 'PENDING',
            created: '',
            id: 'idV2',
            nodeId: 'nodeid',
        },
    ] as SuggestionResponseDto[]);

    public constructor(definition: MdsWidget) {
        super(null, { ...WidgetDummy.defaultDefinition, ...definition }, null, null, null, null);
        console.info(this, this.definition);
    }
    async getSuggestedValues(searchString?: string): Promise<MdsWidgetValue[]> {
        if (searchString) {
            console.log('filter values', searchString);
            const filterString = searchString.toLowerCase();
            return this.fakeBackendValues.filter(
                (value) =>
                    value.caption.toLowerCase().indexOf(filterString) !== -1 ||
                    value.id.toLowerCase().indexOf(filterString) !== -1,
            );
        } else {
            return this.definition.values;
        }
    }
    getInitialDisplayValues(): BehaviorSubject<MdsValueList> {
        return new BehaviorSubject<MdsValueList>({ values: [] });
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
}
