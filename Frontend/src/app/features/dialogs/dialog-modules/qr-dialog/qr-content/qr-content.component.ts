import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'es-qr-content',
    templateUrl: './qr-content.component.html',
    styleUrls: ['./qr-content.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class QrContentComponent {
    @Input() url: string;
}
