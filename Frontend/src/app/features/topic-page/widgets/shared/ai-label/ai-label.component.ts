import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-ai-label',
    imports: [EduSharingUiCommonModule, TranslateModule],
    templateUrl: './ai-label.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./ai-label.component.scss'],
})
export class AiLabelComponent {
    @Input() imageContainer: boolean = false;
    @Input() textContainer: boolean = false;
}
