import { Component, Input } from '@angular/core';

@Component({
    selector: 'es-qr-content',
    templateUrl: './qr-content.component.html',
    styleUrls: ['./qr-content.component.scss'],
    standalone: false,
})
export class QrContentComponent {
    @Input() url: string;
}
