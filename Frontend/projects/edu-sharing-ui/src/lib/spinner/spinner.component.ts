import { Component, HostBinding, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'es-spinner',
    templateUrl: 'spinner.component.html',
    styleUrls: ['spinner.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [TranslatePipe],
})
export class SpinnerComponent {
    @HostBinding('attr.data-test') readonly dataTest = 'loading-spinner';

    constructor() {}
}
