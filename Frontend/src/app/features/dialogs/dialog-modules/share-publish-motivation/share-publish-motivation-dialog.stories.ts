import { applicationConfig, type Meta, type StoryObj } from '@storybook/angular';
import {
    MotivationConfig,
    SharePublishMotivationDialogComponent,
} from './share-publish-motivation-dialog.component';
import { Injectable } from '@angular/core';
import { ConfigService, IamV1Service, UserStats } from 'ngx-edu-sharing-api';
import { GetUserStats$Params } from '../../../../../../dist/edu-sharing-api/lib/api/fn/iam-v-1/get-user-stats';
import { HttpContext } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { mdsStorybookProviders } from '../../../mds/mds-editor/storybook-utils';

@Injectable()
class ConfigServiceMock extends ConfigService {
    async get<T = string>(name: string, defaultValue?: T): Promise<T> {
        return {
            confetti: true,
            range: [1, 10, 25, 42, 64, 100],
        } as MotivationConfig as any;
    }
}
@Injectable()
class IamV1ServiceMock extends IamV1Service {
    getUserStats(params: GetUserStats$Params, context?: HttpContext): Observable<UserStats> {
        return of({
            allStats: null,
            publicStats: {
                nodeCount: Math.floor(Math.pow(Math.random(), 2) * 150),
                nodeCountCC: 1,
                collectionCount: 1,
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
                    useClass: IamV1ServiceMock,
                },
            ],
        }),
    ],
    tags: ['autodocs'],
};

export default dialog;
type Story = StoryObj<SharePublishMotivationDialogComponent>;

export const Default: Story = {
    args: {},
};
