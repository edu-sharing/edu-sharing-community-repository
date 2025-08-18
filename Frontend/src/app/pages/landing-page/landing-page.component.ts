import { Component, computed, OnInit, signal } from '@angular/core';
import { AuthenticationService, ConfigService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Router } from '@angular/router';
import { UIService } from 'ngx-edu-sharing-ui';
import { MainNavService } from '../../main/navigation/main-nav.service';

export type SwimlaneTypes = 'featured-media' | 'collections' | 'recent-activities' | 'shares';
export type SwimlaneEntry = {
    id: SwimlaneTypes;
    defaultExpanded: boolean;
};
@Component({
    selector: 'es-landing-page',
    templateUrl: 'landing-page.component.html',
    styleUrls: ['landing-page.component.scss'],
    standalone: false,
})
export class LandingPageComponent implements OnInit {
    /**
     * displayed swimlanes (in order)
     * are be retrieved from the backend client.config
     */
    swimlanes = signal<SwimlaneEntry[]>([]);
    constructor(
        private router: Router,
        private mainNav: MainNavService,
        private configService: ConfigService,
        private ui: UIService,
        private authenticationService: AuthenticationService,
    ) {
        this.mainNav.setMainNavConfig({
            showUser: true,
            showScope: true,
            currentScope: 'LANDING',
            title: 'SIDEBAR.LANDING',
            show: true,
            create: {
                allowed: true,
                allowBinary: true,
            },
            showNavigation: true,
        });
    }

    async ngOnInit(): Promise<void> {
        const login = await firstValueFrom(this.authenticationService.observeLoginInfo());
        if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
            this.ui.goToLogin();
            return;
        }
        this.swimlanes.set(
            await this.configService.get<SwimlaneEntry[]>('frontpage.dashbaord.swimlanes', [
                {
                    id: 'recent-activities',
                    defaultExpanded: true,
                },
                {
                    id: 'collections',
                    defaultExpanded: true,
                },
                {
                    id: 'featured-media',
                    defaultExpanded: true,
                },
                {
                    id: 'shares',
                    defaultExpanded: true,
                },
            ]),
        );
    }
}
