import { A11yModule } from '@angular/cdk/a11y';
import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { ImprintPrivacyService } from './imprint-privacy-service';

@Component({
    selector: 'es-imprint-privacy',
    templateUrl: './imprint-privacy.component.html',
    styleUrls: ['./imprint-privacy.component.scss'],
    imports: [CommonModule, A11yModule, TranslateModule],
})
export class ImprintPrivacyComponent {
    service = inject(ImprintPrivacyService);

    @Input() separator: string = '';
}
