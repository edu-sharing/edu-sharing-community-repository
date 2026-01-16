/**
 * Created by Torsten on 13.01.2017.
 */

import { CommonModule } from '@angular/common';
import { Component, Input, signal, WritableSignal } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { take } from 'rxjs/operators';
import { ConfigurationHelper } from '../../../core-module/core.module';
import { SharedModule } from '../../shared.module';

@Component({
    selector: 'es-footer',
    templateUrl: 'footer.component.html',
    styleUrls: ['footer.component.scss'],
    imports: [CommonModule, SharedModule],
})
export class FooterComponent {
    _scope: string;
    public show: boolean;
    showFooter: WritableSignal<boolean> = signal(false);

    @Input() set scope(scope: string) {
        this._scope = scope;
        this.config
            .observeConfig()
            .pipe(take(1))
            .subscribe(() => {
                const footerScopes: string[] = ConfigurationHelper.getFooter(this.config);
                this.showFooter.set(footerScopes.includes(this._scope));
            });
    }

    constructor(private config: ConfigService) {}
}
