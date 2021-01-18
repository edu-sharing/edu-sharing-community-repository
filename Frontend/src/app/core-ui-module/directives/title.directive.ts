import { Directive, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { combineLatest, forkJoin, Subject } from 'rxjs';
import { ConfigurationService } from '../../core-module/core.module';
import { Translation } from '../translation';

/**
 * Uses the element's text content to update the document title.
 *
 * Use on the page's h1 heading. If the page doesn't have a visible h1 heading, add one at the
 * beginning of the main content and make it invisible.
 *
 * @example <h1 appTitle class="cdk-visually-hidden">{{ 'WORKSPACE.TITLE' | translate }}</h1>
 */
@Directive({
    selector: '[appTitle]',
})
export class TitleDirective implements OnInit, OnDestroy {
    private mutationObserver: MutationObserver;
    private pageHeading = new Subject<string>();

    constructor(
        private elementRef: ElementRef,
        private documentTitle: Title,
        configuration: ConfigurationService,
    ) {
        this.mutationObserver = new MutationObserver(() => this.updatePageHeading());
        this.mutationObserver.observe(this.elementRef.nativeElement, {
            characterData: true,
            subtree: true,
        });
        combineLatest([
            forkJoin({
                branding: configuration.get('branding'),
                siteTitle: configuration.get('siteTitle', 'edu-sharing'),
            }),
            this.pageHeading,
            Translation.waitForInit(), // Prevent initial flicker of the untranslated heading
        ]).subscribe(([config, pageHeading]) => this.updateTitle(config, pageHeading));
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

    private updateTitle(
        config: { branding: boolean; siteTitle: string },
        pageHeading: string,
    ): void {
        if (!pageHeading) {
            this.documentTitle.setTitle(config.siteTitle);
        } else if (config.branding) {
            this.documentTitle.setTitle(pageHeading + ' - ' + config.siteTitle);
        } else {
            this.documentTitle.setTitle(pageHeading);
        }
    }
}
