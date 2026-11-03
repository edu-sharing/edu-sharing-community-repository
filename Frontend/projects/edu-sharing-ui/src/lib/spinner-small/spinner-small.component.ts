import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
    selector: 'es-spinner-small',
    templateUrl: 'spinner-small.component.html',
    styleUrls: ['spinner-small.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [MatProgressSpinnerModule],
})
export class SpinnerSmallComponent {
    @Input() diameter = 20;
    constructor() {}
}
