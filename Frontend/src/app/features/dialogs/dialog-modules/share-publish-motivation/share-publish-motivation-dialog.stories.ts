import { applicationConfig, type Meta, type StoryObj } from '@storybook/angular';
import {
    MotivationConfig,
    SharePublishMotivationDialogComponent,
} from './share-publish-motivation-dialog.component';
import { Injectable } from '@angular/core';
import { ConfigService, IamV1Service, UserStats } from 'ngx-edu-sharing-api';
import { HttpContext } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { mdsStorybookProviders } from '../../../mds/mds-editor/storybook-utils';

const DefaultRange = [1, 10, 25, 42, 64, 100];
@Injectable()
class ConfigServiceMock extends ConfigService {
    async get<T = MotivationConfig>(name: string, defaultValue?: T): Promise<T> {
        return {
            confetti: true,
            range: DefaultRange,
        } as MotivationConfig as any;
    }
}
@Injectable()
class IamV1ServiceMock extends IamV1Service {
    // eslint-disable-next-line @angular-eslint/prefer-inject
    constructor(private count: number) {
        super(null, null);
    }
    getUserStats(
        params: Parameters<IamV1Service['getUserStats']>[0],
        context?: HttpContext,
    ): Observable<UserStats> {
        return of({
            allStats: null,
            publicStats: {
                nodeCountOER: this.count,
                nodeCount: this.count,
                collectionCount: this.count,
            },
        });
    }
}
const dialog: Meta<SharePublishMotivationDialogComponent> = {
    title: 'Dialogs/Share Publish Motivation',
    component: SharePublishMotivationDialogComponent,
    decorators: [
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                {
                    provide: ConfigService,
                    useClass: ConfigServiceMock,
                },
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(1),
                },
            ],
        }),
    ],
    tags: ['autodocs'],
};

export default dialog;
type Story = StoryObj<SharePublishMotivationDialogComponent>;

export const Variant_1: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[0]),
                },
            ],
        }),
    ],
};

export const Variant_2: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[1]),
                },
            ],
        }),
    ],
};

export const Variant_3: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[2]),
                },
            ],
        }),
    ],
};

export const Variant_4: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[3]),
                },
            ],
        }),
    ],
};

export const Variant_5: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[4]),
                },
            ],
        }),
    ],
};

export const Variant_6: Story = {
    decorators: [
        applicationConfig({
            providers: [
                {
                    provide: IamV1Service,
                    useFactory: () => new IamV1ServiceMock(DefaultRange[5]),
                },
            ],
        }),
    ],
};
