import {
    Directive,
    ElementRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { combineLatest, forkJoin, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { TranslationsService } from '../../translations/translations.service';
import { ConfigurationService } from '../../core-module/core.module';

/**
 * Uses the element's text content to update the document title.
 *
 * Use on the page's h1 heading. If the page doesn't have a visible h1 heading, add one at the
 * beginning of the main content and make it invisible.
 *
 * @example <h1 esTitle class="cdk-visually-hidden">{{ 'WORKSPACE.TITLE' | translate }}</h1>
 */
@Directive({
    selector: '[esTitle]',
})
export class TitleDirective implements OnInit, OnChanges, OnDestroy {
    /**
     * Use a title that differs from the h1 heading.
     *
     * Set to `null` to not use a page heading and use the site title as title.
     *
     * @example <h1 esTitle="Home">Trending items</h1>
     * @example <h1 [esTitle]="null">Trending items</h1>
     */
    @Input() esTitle: string;

    private mutationObserver: MutationObserver;
    /** The page heading as by the content of the directive's h1 element. */
    private pageHeading = new Subject<string>();
    /**
     * Overrides pageHeading with the `esTitle` input.
     *
     * Will be set to `null` if the input isn't used.
     */
    private titleOverride = new Subject<string>();
    /** The combined result of pageHeading and titleOverride. */
    private pageTitle = new Subject<string>();

    constructor(
        private elementRef: ElementRef,
        private documentTitle: Title,
        private translations: TranslationsService,
        configuration: ConfigurationService,
    ) {
        this.mutationObserver = new MutationObserver(() => this.updatePageHeading());
        this.mutationObserver.observe(this.elementRef.nativeElement, {
            characterData: true,
            subtree: true,
        });
        combineLatest([this.pageHeading, this.titleOverride])
            .pipe(map(([pageHeading, titleOverride]) => titleOverride ?? pageHeading))
            .subscribe(this.pageTitle);
        combineLatest([
            forkJoin({
                branding: configuration.get('branding'),
                siteTitle: configuration.get('siteTitle', 'edu-sharing'),
            }),
            this.pageTitle,
            this.translations.waitForInit(), // Prevent initial flicker of the untranslated heading
        ]).subscribe(([config, pageTitle]) => this.updateTitle(config, pageTitle));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.esTitle) {
            const override = changes.esTitle.currentValue;
            if (override === '') {
                // Input not set
                this.titleOverride.next(null);
            } else {
                this.titleOverride.next(override ?? '');
            }
        }
    }

    ngOnInit(): void {
        this.updatePageHeading();
    }

    ngOnDestroy(): void {
        this.mutationObserver.disconnect();
    }

    private updatePageHeading(): void {
        this.pageHeading.next(this.elementRef.nativeElement.textContent);
    }

    private updateTitle(config: { branding: boolean; siteTitle: string }, pageTitle: string): void {
        if (!pageTitle) {
            this.documentTitle.setTitle(config.siteTitle);
        } else if (config.branding) {
            this.documentTitle.setTitle(pageTitle + ' - ' + config.siteTitle);
        } else {
            this.documentTitle.setTitle(pageTitle);
        }
    }
}
