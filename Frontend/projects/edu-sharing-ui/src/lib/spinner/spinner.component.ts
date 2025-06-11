import { Component, HostBinding } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'es-spinner',
    templateUrl: 'spinner.component.html',
    styleUrls: ['spinner.component.scss'],
    imports: [TranslateModule],
})
export class SpinnerComponent {
    @HostBinding('attr.data-test') readonly dataTest = 'loading-spinner';

    constructor() {}
}
