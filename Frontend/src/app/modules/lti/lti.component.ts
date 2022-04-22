import { Component, OnInit } from '@angular/core';
import { DialogButton } from '../../core-module/ui/dialog-button';
import {
    GlobalContainerComponent
} from '../../common/ui/global-container/global-container.component';
import {TranslationsService} from '../../translations/translations.service';

@Component({
  selector: 'app-lti',
  templateUrl: './lti.component.html',
  styleUrls: ['./lti.component.scss']
})
export class LtiComponent implements OnInit {
    public dialogButtons: DialogButton[] = [];
  constructor(
      private translations: TranslationsService,
  ) {
      this.translations.waitForInit().subscribe(() => {
          GlobalContainerComponent.finishPreloading();
      });
      this.dialogButtons = DialogButton.getOk(() => {
          (window.opener || window.parent).postMessage({subject: 'org.imsglobal.lti.close'}, '*');
      });
  }

    ngOnInit(): void {
    }

}
