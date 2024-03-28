import { Component, HostBinding, OnInit } from '@angular/core';

@Component({
    selector: 'es-spinner',
    templateUrl: 'spinner.component.html',
    styleUrls: ['spinner.component.scss'],
    standalone: true,
})
export class SpinnerComponent {
    @HostBinding('attr.data-test') readonly dataTest = 'loading-spinner';

    constructor() {}
}
