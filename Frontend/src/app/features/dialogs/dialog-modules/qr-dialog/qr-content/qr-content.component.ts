import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'es-qr-content',
    templateUrl: './qr-content.component.html',
    styleUrls: ['./qr-content.component.scss'],
})
export class QrContentComponent {
    @Input() url: string;
}
