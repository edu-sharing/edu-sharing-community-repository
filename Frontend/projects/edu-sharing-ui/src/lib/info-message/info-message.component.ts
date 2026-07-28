import { Component, Input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { IconDirective } from '../directives/icon.directive';

@Component({
    selector: 'es-info-message',
    templateUrl: 'info-message.component.html',
    styleUrls: ['info-message.component.scss'],
    imports: [TranslateModule, IconDirective, MatButtonModule],
})
export class InfoMessageComponent {
    /**
     * the message to display
     */
    @Input() message: string;
    @Input() mode: 'info' | 'warning' | 'error' = 'info';
    /** renders a close button that lets the user hide the message */
    @Input() closable: boolean = false;
    /** set by the close button, hides the whole message */
    protected dismissed = signal(false);

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
