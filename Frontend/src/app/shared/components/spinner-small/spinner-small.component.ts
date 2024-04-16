import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'es-spinner-small',
    templateUrl: 'spinner-small.component.html',
    styleUrls: ['spinner-small.component.scss'],
})
export class SpinnerSmallComponent {
    @Input() diameter = 20;
    constructor() {}
}
