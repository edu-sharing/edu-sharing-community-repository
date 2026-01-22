import { Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { IconDirective } from '../directives/icon.directive';

@Component({
    selector: 'es-info-message',
    templateUrl: 'info-message.component.html',
    styleUrls: ['info-message.component.scss'],
    imports: [TranslateModule, IconDirective],
})
export class InfoMessageComponent {
    /**
     * the message to display
     */
    @Input() message: string;
    @Input() mode: 'info' | 'warning' | 'error' = 'info';

    ICONS: any = {
        info: 'info',
        warning: 'warning',
        error: 'error',
    };
    constructor() {}
    getIcon() {
        return this.ICONS[this.mode];
    }
}
