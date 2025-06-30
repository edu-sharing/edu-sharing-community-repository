import { Component, computed, OnInit, signal } from '@angular/core';
import { AuthenticationService, ConfigService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Router } from '@angular/router';
import { UIService } from 'ngx-edu-sharing-ui';

type SwimlaneEntry = {
    id: string;
    defaultVisibility: boolean;
};
@Component({
    selector: 'es-landing-page',
    templateUrl: 'landing-page.component.html',
    styleUrls: ['landing-page.component.scss'],
    standalone: false,
})
export class LandingPageComponent implements OnInit {
    private swimlanes = signal<SwimlaneEntry[]>([]);
    swimlanesVisible = computed(() => this.swimlanes().filter((s) => s.defaultVisibility));
    constructor(
        private router: Router,
        private configService: ConfigService,
        private ui: UIService,
        private authenticationService: AuthenticationService,
    ) {}

    async ngOnInit(): Promise<void> {
        const login = await firstValueFrom(this.authenticationService.observeLoginInfo());
        console.log(login);
        if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
            this.ui.goToLogin();
            return;
        }
        this.swimlanes.set(
            await this.configService.get<SwimlaneEntry[]>('frontpage.dashbaord.swimlanes', [
                {
                    id: 'test',
                    defaultVisibility: true,
                },
            ]),
        );
    }
}
