import { Clipboard } from '@angular/cdk/clipboard';
import { inject, Injectable, Signal, signal, TemplateRef, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Closable } from '../../../../features/dialogs/card-dialog/card-dialog-config';
import { DialogsService } from '../../../../features/dialogs/dialogs.service';
import { TopicPageHelperService } from './topic-page-helper.service';

const MAIL_I18N_PREFIX = 'TOPIC_PAGE.SWIMLANE.SHARE_CONTENT.WRITE_MAIL.';

/**
 * Links to sections of the topic page via the URL fragment: scrolling to the fragment of the
 * current route, and sharing a swimlane by link, QR code or mail.
 */
@Injectable()
export class TopicPageShareService {
    private clipboard = inject(Clipboard);
    private dialogs = inject(DialogsService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private topicPageHelperService = inject(TopicPageHelperService);
    private translate = inject(TranslateService);

    private readonly qrCodeUrlValue: WritableSignal<string> = signal('');
    // the URL the QR code dialog shows
    readonly qrCodeUrl: Signal<string> = this.qrCodeUrlValue.asReadonly();

    private fragmentListenerInitialized: boolean = false;
    private initialFragmentScrollPerformed: boolean = false;
    private latestUrlFragment: string;
    private scrollContainer: () => HTMLElement | undefined = () => undefined;
    private readonly destroyed = takeUntilDestroyed<string>();

    /**
     * Sets the element the page scrolls in, which fragments are scrolled into view within.
     */
    setScrollContainer(scrollContainer: () => HTMLElement | undefined): void {
        this.scrollContainer = scrollContainer;
    }

    /**
     * Scrolls the element named by the fragment of the current route into view, now and whenever
     * the fragment changes. Called once per page; later calls are ignored.
     */
    listenToFragment(): void {
        if (this.fragmentListenerInitialized) {
            return;
        }
        this.fragmentListenerInitialized = true;
        this.route.fragment.pipe(this.destroyed).subscribe((urlFragment: string): void => {
            this.latestUrlFragment = urlFragment;
            if (!this.latestUrlFragment) {
                this.initialFragmentScrollPerformed = true;
                return;
            }
            if (this.initialFragmentScrollPerformed) {
                this.scrollElementIntoView();
            } else {
                // TODO: this should be replaced by waiting for all widgets being loaded
                setTimeout((): void => {
                    this.performScrollAttempts();
                }, 3000);
            }
        });
    }

    /**
     * Appends a fragment to the URL and scrolls its element into view.
     */
    navigateToFragment(fragment: string): void {
        // use history over router to avoid directly navigating to wrong fragment position
        history.pushState(null, '', this.retrieveFragmentUrl(fragment));
        this.latestUrlFragment = fragment;
        this.scrollElementIntoView();
    }

    retrieveFragmentUrl(fragment: string): string {
        const url = new URL(window.location.href);
        url.hash = fragment;
        return url.toString();
    }

    /**
     * Opens a dialog with a QR code linking to a fragment of the given page variant.
     */
    async openQrCodeDialog(
        fragment: string,
        variantId: string,
        contentTemplate: TemplateRef<undefined>,
    ): Promise<void> {
        await this.setVariantIdInUrl(variantId);
        this.qrCodeUrlValue.set(this.retrieveFragmentUrl(fragment));
        await this.dialogs.openGenericDialog({
            title: 'TOPIC_PAGE.SWIMLANE.QR_CODE.HEADING',
            contentPadding: 0,
            contentTemplate,
            closable: Closable.Casual,
            buttons: [{ label: 'CANCEL', config: { color: 'standard' } }],
        });
    }

    /**
     * Copies the link to a fragment of the given page variant into the clipboard.
     */
    async copyLink(fragment: string, variantId: string): Promise<void> {
        await this.setVariantIdInUrl(variantId);
        // similar to copying links in GitHub issues, the target is first set to the URL and then copied
        this.navigateToFragment(fragment);
        // the URL is only updated after the scroll started
        setTimeout((): void => {
            this.clipboard.copy(window.location.href);
            this.topicPageHelperService.openSaveConfigToast(
                'TOPIC_PAGE.SWIMLANE.SHARE_CONTENT.COPY_LINK_SUCCESS',
            );
        }, 300);
    }

    /**
     * Opens the mail client with a message linking to a fragment of the page.
     */
    writeMail(fragment: string, title: string): void {
        const subjectText = this.translate.instant(MAIL_I18N_PREFIX + 'SUBJECT_SHARED_CONTENT', {
            title: title || this.translate.instant(MAIL_I18N_PREFIX + 'NO_TITLE'),
        });
        const bodyText = this.translate.instant(MAIL_I18N_PREFIX + 'BODY_SHARED_CONTENT', {
            url: this.retrieveFragmentUrl(fragment),
        });
        const subject = encodeURIComponent(subjectText);
        const body = encodeURIComponent(bodyText);
        window.open(`mailto:?subject=${subject}&body=${body}`, '_self');
    }

    private async setVariantIdInUrl(variantId: string): Promise<void> {
        await this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { variantId },
            queryParamsHandling: 'merge',
        });
    }

    /**
     * Performs up to 10 scroll attempts (1 per second) until scrollElementIntoView returns true.
     */
    private performScrollAttempts(): void {
        let attemptCount = 0;
        const maxAttempts = 10;
        const scrollInterval = setInterval(() => {
            attemptCount++;
            if (this.scrollElementIntoView()) {
                this.initialFragmentScrollPerformed = true;
                clearInterval(scrollInterval);
            } else if (attemptCount >= maxAttempts) {
                clearInterval(scrollInterval);
            }
        }, 1000);
    }

    /**
     * Scrolls the latest URL fragment into view if it exists.
     */
    private scrollElementIntoView(): boolean {
        if (!this.latestUrlFragment) {
            return false;
        }
        const element: HTMLElement = document.getElementById(this.latestUrlFragment);
        const scrollContainer: HTMLElement = this.scrollContainer();
        if (!element || !scrollContainer) {
            return false;
        }
        // scroll only the card's scroll region (not the window), keeping the scroll-margin gap
        const scrollMargin = parseFloat(getComputedStyle(element).scrollMarginTop) || 0;
        const offsetInContainer =
            element.getBoundingClientRect().top - scrollContainer.getBoundingClientRect().top;
        scrollContainer.scrollTo({
            top: scrollContainer.scrollTop + offsetInContainer - scrollMargin,
            behavior: 'smooth',
        });
        // set focus to element that was scrolled into view
        if (!element.hasAttribute('tabindex')) {
            element.setAttribute('tabindex', '-1');
        }
        window.setTimeout(() => {
            element.focus({ preventScroll: true });
        }, 500);
        return true;
    }
}
