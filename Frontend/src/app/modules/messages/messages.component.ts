import { TranslationsService } from '../../translations/translations.service';
import {ActivatedRoute, Data, Params, Router} from '@angular/router';
import {Toast} from '../../core-ui-module/toast';
import {TranslateService} from '@ngx-translate/core';
import {RestConnectorService} from '../../core-module/core.module';
import {Component, ViewChild, ElementRef} from '@angular/core';
import {RouterComponent} from '../../router/router.component';
import {UIConstants} from '../../core-module/ui/ui-constants';
import {GlobalContainerComponent} from '../../common/ui/global-container/global-container.component';
@Component({
    selector: 'es-messages-main',
    templateUrl: 'messages.component.html',
    styleUrls: ['messages.component.scss'],
})
export class MessagesComponent {
    public message : string;
    public messageDetail : string;
    public messageText : string;
    public error : string;
    constructor(private toast: Toast,
                private route: ActivatedRoute,
                private router: Router,
                private translate: TranslateService,
                private translations: TranslationsService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.route.params.subscribe(async (data: Params) => {
                this.setMessage(data);
                this.route.data.subscribe((routeData) => {
                    if(routeData && Object.keys(routeData).length) {
                        this.setMessage(routeData);
                    }
                })
                GlobalContainerComponent.finishPreloading();
            })
        });
    }

    private setMessage(data: Params|Data) {
        this.message = 'MESSAGES.' + data.message;
        this.messageDetail = 'MESSAGES.DETAILS.' + data.message;
        this.messageText = data.text;
        this.error = data.message;
        if (this.translate.instant(this.message) === this.message) {
            this.message = 'MESSAGES.INVALID';
            this.messageDetail = 'MESSAGES.DETAILS.INVALID';
        }
    }

    public openSearch() {
        this.router.navigate([UIConstants.ROUTER_PREFIX+'search']);
    }
    public closeWindow() {
        window.close();
    }
}

