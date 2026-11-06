import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ConfigurationService } from '../core-module/rest/services/configuration.service';
import { CordovaService } from '../services/cordova.service';
import { RouterHelper } from '../util/router.helper';
import { StartupComponent } from './startup.component';

function configWith(defaultLocation: string | null | undefined): ConfigurationService {
    return {
        get: (name: string, fallback: unknown) =>
            of(
                name === 'defaultLocation' && defaultLocation !== undefined
                    ? defaultLocation
                    : fallback,
            ),
    } as unknown as ConfigurationService;
}

describe('StartupComponent', () => {
    it('goes to the configured route below components/', async () => {
        expect(await StartupComponent.defaultLocation(configWith('workspace'))).toBe(
            'components/workspace',
        );
        expect(await StartupComponent.defaultLocation(configWith('/search'))).toBe(
            'components/search',
        );
    });

    it('takes an absolute url as is', async () => {
        expect(
            await StartupComponent.defaultLocation(configWith('https://portal.example.org/start')),
        ).toBe('https://portal.example.org/start');
    });

    it('defaults to the login, like the backend and the Frontend-Server', async () => {
        expect(await StartupComponent.defaultLocation(configWith(undefined))).toBe(
            'components/login',
        );
        expect(await StartupComponent.defaultLocation(configWith(null))).toBe('components/login');
        expect(await StartupComponent.defaultLocation(configWith('  '))).toBe('components/login');
    });

    it('navigates there in production builds too, replacing the empty root in the history', async () => {
        const navigate = spyOn(RouterHelper, 'navigateToAbsoluteUrl');
        TestBed.configureTestingModule({
            declarations: [StartupComponent],
            providers: [
                provideRouter([]),
                { provide: ConfigurationService, useValue: configWith('workspace') },
                { provide: CordovaService, useValue: { isRunningCordova: () => false } },
            ],
        });
        const component = TestBed.createComponent(StartupComponent).componentInstance;
        await component.ngOnInit();
        expect(navigate).toHaveBeenCalledOnceWith(
            jasmine.anything(),
            jasmine.anything(),
            'components/workspace',
            true,
        );
    });
});
