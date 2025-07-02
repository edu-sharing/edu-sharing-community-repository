import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
    applicationConfig,
    componentWrapperDecorator,
    type Meta,
    moduleMetadata,
    type StoryObj,
} from '@storybook/angular';
import {
    ApiRequestConfiguration,
    AuthenticationService,
    ClientConfig,
    ConfigService,
    EduSharingApiModule,
    LoginInfo,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { NodeEntriesCardComponent } from './node-entries-card.component';
import {
    DummyNode,
    mdsStorybookProviders,
    ToastMock,
    DefaultColumns,
    translateProvider,
} from '../../../../../../src/app/features/mds/mds-editor/storybook-utils';
import {
    CustomSelectionModel,
    EduSharingUiCommonModule,
    InteractionType,
    NodeEntriesService,
    OptionItem,
    Target,
    Toast,
} from 'ngx-edu-sharing-ui';
import { Injectable } from '@angular/core';
import { NodeEntriesModule } from '../node-entries.module';
import { ListItem } from '../../types/list-item';
import { ListOptions } from '../entries-model';
import { Observable, of } from 'rxjs';

@Injectable()
export class AuthenticationServiceMock implements Partial<AuthenticationService> {
    async hasToolpermission(tp: string) {
        return true;
    }
    observeLoginInfo(): Observable<LoginInfo> {
        return of();
    }
    observeUserChanges(): Observable<void> {
        return of();
    }
}

@Injectable()
export class NodeEntriesServiceMock implements Partial<NodeEntriesService<any>> {
    elementInteractionType = InteractionType.Emitter;
    checkbox = true;
    selection = new CustomSelectionModel<any>();
    get columns(): ListItem[] {
        return DefaultColumns;
    }
}
@Injectable()
export class NodeEntriesServiceMockApply extends NodeEntriesServiceMock {
    private _options: OptionItem[] = [new OptionItem('APPLY', 'redo', () => {})];
    get options(): ListOptions {
        return {
            [Target.List]: this._options.map((o) => {
                o.showAlways = true;
                o.showCallback = async () => true;
                return o;
            }),
        };
    }
}

@Injectable()
export class ConfigServiceMockRating implements Partial<ConfigService> {
    public instant<T = string>(name: string, defaultValue?: T): T {
        return 'stars' as T;
    }
    observeConfig({ forceUpdate = false } = {}): Observable<ClientConfig | null> {
        return of({});
    }
}
const dummyNode = DummyNode;
const card: Meta<NodeEntriesCardComponent<any>> = {
    title: 'Core UI/Lists/Card',
    component: NodeEntriesCardComponent,
    decorators: [
        componentWrapperDecorator(
            (story) => `<div style="max-width: var(--cardWidth);">${story}</div>`,
        ),
        moduleMetadata({
            imports: [EduSharingApiModule, NodeEntriesModule, EduSharingUiCommonModule],
            declarations: [],
        }),
        applicationConfig({
            providers: [
                provideAnimations(),
                {
                    provide: ApiRequestConfiguration,
                    useValue: {},
                },
                {
                    provide: AuthenticationService,
                    useClass: AuthenticationServiceMock,
                },
                {
                    provide: NodeEntriesService,
                    useClass: NodeEntriesServiceMock,
                },
                {
                    provide: TranslateService,
                    useClass: translateProvider,
                },
                {
                    provide: Toast,
                    useValue: ToastMock,
                },
            ],
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default card;
type Story = StoryObj<NodeEntriesCardComponent<any>>;
export const CardSimple: Story = {
    args: {
        node: dummyNode,
    },
};
export const CardRatings: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: ConfigService,
                    useClass: ConfigServiceMockRating,
                },
            ],
        }),
    ],
    args: {
        node: dummyNode,
    },
};
export const CardSeries: Story = {
    args: {
        node: {
            ...dummyNode,
            properties: {
                ...dummyNode.properties,
                ['virtual:childobjectcount']: ['3'],
            },
        },
    },
};
export const CardApply: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: NodeEntriesService,
                    useClass: NodeEntriesServiceMockApply,
                },
            ],
        }),
    ],
    args: {
        node: dummyNode,
    },
};
