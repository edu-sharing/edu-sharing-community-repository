/**
 * Created by Torsten on 13.01.2017.
 */

import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Directive, ElementRef, Input, OnDestroy, OnInit, Optional } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest } from 'rxjs';

/**
 * Replaces the element's content with an icon.
 *
 * Example: `<i esIcon="save"></i>`
 *
 * Optionally, a translated `aria-label` can be attached by setting `aria` to a truthy value: `<i
 * esIcon="save" aria="true"></i>`. Otherwise, `aria-hidden` will be set.
 *
 * For backwards compatibility, the directive is also activated on elements that set
 * `class="material-icons"`. This is mainly to set the `aria-hidden` attribute. Occurrences should
 * be updated to the syntax above.
 */
@Directive({
    selector: 'i[esIcon], i.material-icons',
})
export class IconDirective implements OnInit, OnDestroy {
    private originalId$ = new BehaviorSubject<string>(null);
    private _id: string;
    private _aria: boolean;
    private iconsConfig: Array<{ original: string; replace?: string; cssClass?: string }>;
    private altTextSpan: HTMLElement;
    private isReady = false;

    /**
     * An alt text to show to screen readers.
     *
     * If omitted, the icon will be invisible to screen readers.
     *
     * @see https://material.angular.io/components/icon/overview#indicator-icons
     */
    @Input() set altText(altText: string) {
        this.setAltText(altText);
    }

    /** If true, an alt text (see above) will be set based on the icon. */
    @Input() set aria(aria: boolean) {
        aria = coerceBooleanProperty(aria);
        if (aria !== this._aria) {
            this._aria = aria;
            if (this.isReady) {
                this.updateAria();
            }
        }
    }

    @Input() set esIcon(id: string) {
        this.originalId$.next(id);
    }

    constructor(
        private element: ElementRef<HTMLElement>,
        private translate: TranslateService,
        @Optional() private config: ConfigService,
    ) {}

    async ngOnInit() {
        this.isReady = true;
        this.element.nativeElement.setAttribute('aria-hidden', 'true');
        this.updateAria();
        combineLatest([this.originalId$, this.config.observeConfig()]).subscribe(() => {
            this.iconsConfig = this.config.instant('icons', null);
            this.setIcon();
        });
    }

    ngOnDestroy(): void {
        if (this.altTextSpan) {
            this.altTextSpan.remove();
        }
    }

    private setIcon() {
        let id = this.originalId$.value;
        if (this._id) {
            this.element.nativeElement.classList.remove(
                'edu-icons',
                'custom-icons',
                'material-icons',
            );
        }
        let customClass: string = null;
        const mapping = this.iconsConfig?.filter((i) => i.original === id);
        if (mapping?.length === 1) {
            id = mapping[0].replace || '';
            customClass = mapping[0].cssClass;
        }
        this._id = id;
        if (this._aria) {
            this.updateAria();
        }
        let cssClass: string;
        if (id?.startsWith('edu-') && !customClass) {
            cssClass = 'edu-icons';
            id = id.substr(4);
        } else if (id?.startsWith('custom-') || customClass) {
            cssClass = 'custom-icons';
            id = id.substr(7);
        } else {
            cssClass = 'material-icons';
        }
        this.element.nativeElement.classList.add(cssClass);
        if (customClass) {
            this.element.nativeElement.classList.add(customClass);
        }
        this.element.nativeElement.innerText = id;
    }

    private updateAria() {
        if (this._aria !== undefined) {
            if (this._aria && this._id) {
                this.translate.get('ICON_LABELS.' + this._id).subscribe((lang) => {
                    this.setAltText(lang);
                });
            } else {
                this.setAltText(null);
            }
        }
    }

    private setAltText(altText: string): void {
        if (altText && !this.altTextSpan) {
            this.insertAltTextSpan();
        }
        if (this.altTextSpan) {
            this.altTextSpan.innerText = altText;
        }
    }

    private insertAltTextSpan(): void {
        this.altTextSpan = document.createElement('span');
        this.altTextSpan.classList.add('cdk-visually-hidden');
        this.element.nativeElement.insertAdjacentElement('afterend', this.altTextSpan);
    }
}
