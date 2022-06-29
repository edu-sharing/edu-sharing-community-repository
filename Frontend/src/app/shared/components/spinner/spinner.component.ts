import { Component, HostBinding, OnInit } from '@angular/core';

@Component({
    selector: 'es-spinner',
    templateUrl: 'spinner.component.html',
    styleUrls: ['spinner.component.scss'],
})
export class SpinnerComponent {
    @HostBinding('attr.data-test') readonly dataTest = 'loading-spinner';

    constructor() {}
}
