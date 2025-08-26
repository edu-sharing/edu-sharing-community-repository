import { Component, Inject, OnDestroy, OnInit, Optional, signal } from '@angular/core';
import { ConfigService, HOME_REPOSITORY, IamV1Service, Node, UserStats } from 'ngx-edu-sharing-api';
import { firstValueFrom, Subject } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { SharePublishMotivationDialogComponentData } from './share-publish-motivation-dialog-dialog-data';
import * as confetti from 'canvas-confetti';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { TranslateModule } from '@ngx-translate/core';

export type MotivationConfig = {
    confetti: boolean;
    range: number[];
};

@Component({
    selector: 'es-share-publish-motivation-dialog',
    imports: [SharedModule, TranslateModule],
    templateUrl: './share-publish-motivation-dialog.component.html',
    styleUrls: ['./share-publish-motivation-dialog.component.scss'],
})
export class SharePublishMotivationDialogComponent implements OnInit, OnDestroy {
    randomMessage = signal<number>(Math.floor(Math.random() * 31) + 1);
    node = signal<Node>(null);
    img = signal<string>('1');

    stats = signal<UserStats>(null);

    constructor(
        private config: ConfigService,
        private iamV1Service: IamV1Service,
        @Optional()
        @Inject(CARD_DIALOG_DATA)
        public data: SharePublishMotivationDialogComponentData,
    ) {
        this.node.set(data?.node);
    }
    async ngOnInit() {
        const config = await this.config.get<MotivationConfig>('publishing.motivation');
        if (config.confetti) {
            const conf = confetti.create(null, {
                resize: true,
            });
            void conf({
                gravity: 2,
                spread: 125,
                particleCount: 600,
            });
        }
        this.stats.set(
            await firstValueFrom(
                this.iamV1Service.getUserStats({
                    repository: HOME_REPOSITORY,
                    person: RestConstants.ME,
                }),
            ),
        );
        let index = config.range.findIndex((r) => r >= this.stats().nodeCount) + 1;
        if (index === 0) {
            index = config.range.length;
        }
        this.img.set(index.toString());
        console.log(index);
    }
    readonly destroyed$ = new Subject<void>();

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
