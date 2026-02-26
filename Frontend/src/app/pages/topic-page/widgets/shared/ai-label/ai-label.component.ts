import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-ai-label',
    imports: [CommonModule, EduSharingUiCommonModule, TranslateModule],
    templateUrl: './ai-label.component.html',
    styleUrls: ['./ai-label.component.scss'],
})
export class AiLabelComponent {
    @Input() imageContainer: boolean = false;
    @Input() textContainer: boolean = false;
}
