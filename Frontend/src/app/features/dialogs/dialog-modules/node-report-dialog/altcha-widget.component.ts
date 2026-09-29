import { PlatformLocation } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    ElementRef,
    inject,
    input,
    output,
    signal,
    viewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import type { AltchaWidgetElement } from 'altcha/external';

const ASSETS_PATH = 'assets/altcha/';
const ALGORITHM = 'PBKDF2/SHA-256';

let widgetLoaded: Promise<void> | null = null;

/**
 * Load the self-hosted ALTCHA widget. The "external" build is used so that the worker and styles are served
 * as regular files from the assets folder (no blob workers or inline styles, which the default CSP would block).
 */
function loadWidget(baseHref: string): Promise<void> {
    if (!widgetLoaded) {
        widgetLoaded = (async () => {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = baseHref + ASSETS_PATH + 'altcha.css';
            document.head.appendChild(link);
            await import('altcha/external');
            await import('altcha/i18n/de');
            const workerUrl = baseHref + ASSETS_PATH + 'pbkdf2.js';
            globalThis.$altcha.algorithms.set(ALGORITHM, () => new Worker(workerUrl));
        })();
    }
    return widgetLoaded;
}

/**
 * Renders the ALTCHA proof-of-work widget and emits the solved payload (or null if not / no longer solved).
 */
@Component({
    selector: 'es-altcha-widget',
    template: `
        @if (loaded()) {
        <altcha-widget
            #widgetRef
            [attr.challenge]="challengeUrl()"
            [attr.language]="language"
            (statechange)="onStateChange($event)"
        ></altcha-widget>
        }
    `,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AltchaWidgetComponent {
    private platformLocation = inject(PlatformLocation);
    private translate = inject(TranslateService);

    readonly challengeUrl = input.required<string>();
    readonly payloadChange = output<string | null>();

    private readonly widget = viewChild<ElementRef<AltchaWidgetElement>>('widgetRef');

    readonly loaded = signal(false);
    readonly language = this.translate.currentLang?.split('-')[0] === 'de' ? 'de' : 'en';

    constructor() {
        loadWidget(this.platformLocation.getBaseHrefFromDOM()).then(
            () => this.loaded.set(true),
            (error) => console.error('Could not load ALTCHA widget', error),
        );
    }

    /**
     * Reset the widget so that a new challenge has to be solved (a payload can only be used once)
     */
    reset() {
        this.widget()?.nativeElement.reset();
    }

    onStateChange(event: Event) {
        const detail = (event as CustomEvent<{ state: string; payload?: string }>).detail;
        this.payloadChange.emit(detail.state === 'verified' ? detail.payload ?? null : null);
    }
}
