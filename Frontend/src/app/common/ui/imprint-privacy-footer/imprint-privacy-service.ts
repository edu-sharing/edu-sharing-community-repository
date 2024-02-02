import { Injectable } from '@angular/core';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { OPEN_URL_MODE } from '../../../core-module/ui/ui-constants';
import { ConfigService } from 'ngx-edu-sharing-api';

@Injectable({
    providedIn: 'root',
})
export class ImprintPrivacyService {
    imprintUrl: string;
    privacyInformationUrl: string;
    constructor(private configService: ConfigService, private bridge: BridgeService) {
        this.configService.observeConfig().subscribe((data: any) => {
            this.imprintUrl = data.imprintUrl;
            this.privacyInformationUrl = data.privacyInformationUrl;
        });
    }

    openImprint() {
        UIHelper.openUrl(this.imprintUrl, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }

    openPrivacy() {
        UIHelper.openUrl(this.privacyInformationUrl, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }
}
