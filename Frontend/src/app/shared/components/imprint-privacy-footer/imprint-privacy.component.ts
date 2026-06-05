import { Component, Input, inject } from '@angular/core';
import { ImprintPrivacyService } from './imprint-privacy-service';

@Component({
    selector: 'es-imprint-privacy',
    templateUrl: './imprint-privacy.component.html',
    styleUrls: ['./imprint-privacy.component.scss'],
    standalone: false,
})
export class ImprintPrivacyComponent {
    service = inject(ImprintPrivacyService);

    @Input() separator: string = '';
}
