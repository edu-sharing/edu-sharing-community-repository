import { Component } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { OPEN_URL_MODE } from '../../../core-module/ui/ui-constants';
import { BridgeService } from '../../../core-bridge-module/bridge.service';

@Component({
    selector: 'es-imprint-privacy-footer',
    templateUrl: './imprint-privacy-footer.component.html',
    styleUrls: ['./imprint-privacy-footer.component.scss'],
})
export class ImprintPrivacyFooterComponent {
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
