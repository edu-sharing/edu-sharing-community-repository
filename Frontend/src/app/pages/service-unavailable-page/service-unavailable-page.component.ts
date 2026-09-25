import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { exhaustMap, interval } from 'rxjs';
import { SpinnerSmallComponent } from 'ngx-edu-sharing-ui';
import { returnUrl, SERVICE_UNAVAILABLE_NEXT_PARAM } from '../../util/service-unavailable';

interface Texts {
    readonly title: string;
    readonly detail: string;
    readonly retry: string;
    readonly checking: string;
}

/**
 * Fixed texts: the translations come from the backend, which is exactly what is missing
 * here. German for every `de*` browser language, English otherwise.
 */
const TEXTS: Readonly<Record<'de' | 'en', Texts>> = {
    de: {
        title: 'Der Dienst ist gerade nicht erreichbar',
        detail: 'Wir versuchen es automatisch weiter. Sobald der Dienst wieder verfügbar ist, geht es hier von selbst weiter.',
        retry: 'Jetzt erneut versuchen',
        checking: 'Prüfe Verbindung …',
    },
    en: {
        title: 'The service is currently unavailable',
        detail: 'We keep trying automatically. As soon as the service is available again, you will be taken back.',
        retry: 'Try again now',
        checking: 'Checking connection …',
    },
};

/**
 * `components/error/503`: shown while the backend is unreachable - the Frontend-Server
 * sends browsers here with the original url as `next`.
 *
 * Works without the backend: no TranslationsService, no ConfigService, no HttpClient
 * (whose interceptors would toast every failed probe). a simple page (`SIMPLE_PAGE_ROUTES`), so
 * `AppComponent` skips its global initialization on this route. Polls `rest/_about` and returns to `next` as soon as it
 * answers.
 */
@Component({
    selector: 'es-service-unavailable-page',
    templateUrl: 'service-unavailable-page.component.html',
    styleUrls: ['service-unavailable-page.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    // no module of the app - SpinnerSmallComponent is standalone and needs nothing but material
    imports: [SpinnerSmallComponent],
    standalone: true,
})
export class ServiceUnavailablePageComponent {
    static readonly RETRY_INTERVAL_MS = 10_000;

    readonly texts = TEXTS[navigator.language?.toLowerCase().startsWith('de') ? 'de' : 'en'];
    readonly checking = signal(false);

    constructor() {
        // exhaustMap: no second probe while one is still running
        interval(ServiceUnavailablePageComponent.RETRY_INTERVAL_MS)
            .pipe(
                exhaustMap(() => this.retry()),
                takeUntilDestroyed(),
            )
            .subscribe();
    }

    async retry(): Promise<void> {
        if (this.checking()) {
            return;
        }
        this.checking.set(true);
        try {
            const response = await fetch(new URL('rest/_about', document.baseURI), {
                cache: 'no-store',
                credentials: 'same-origin',
                headers: { accept: 'application/json' },
            });
            if (response.ok) {
                this.navigate(this.target());
                return;
            }
        } catch {
            // still unreachable
        }
        this.checking.set(false);
    }

    /** a full reload - the app initializes normally again, see AppComponent */
    navigate(url: string): void {
        window.location.replace(url);
    }

    /** where to return to, see `returnUrl` */
    target(): string {
        return returnUrl(
            new URLSearchParams(window.location.search).get(SERVICE_UNAVAILABLE_NEXT_PARAM),
            document.baseURI,
        );
    }
}
