import { Component, OnInit } from '@angular/core';
import { DialogButton } from '../../core-module/ui/dialog-button';
import { MainNavService } from '../../main/navigation/main-nav.service';

@Component({
    selector: 'app-lti',
    templateUrl: './lti.component.html',
    styleUrls: ['./lti.component.scss'],
})
export class LtiComponent implements OnInit {
    public dialogButtons: DialogButton[] = [];

    constructor(private mainNav: MainNavService) {
        this.dialogButtons = DialogButton.getOk(() => {
            (window.opener || window.parent).postMessage(
                { subject: 'org.imsglobal.lti.close' },
                '*',
            );
        });
    }

    ngOnInit(): void {
        this.mainNav.setMainNavConfig({ currentScope: 'lti', title: '', show: false });
    }
}
