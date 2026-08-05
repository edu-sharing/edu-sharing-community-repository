/**
 * Created by Torsten on 13.01.2017.
 */

import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { HttpClient } from '@angular/common/http';
import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest, firstValueFrom } from 'rxjs';
import { filter } from 'rxjs/operators';
import { notNull } from '../util/functions';

type IconsConfig = Array<{
    original: string;
    context?: string;
    replace?: string;
    cssClass?: string;
}>;

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
    /** compiled `context` patterns of the icons config, `null` for invalid expressions */
    private static contextPatterns = new Map<string, RegExp | null>();

    private element = inject<ElementRef<HTMLElement>>(ElementRef);
    private http = inject(HttpClient);
    private translate = inject(TranslateService);
    private renderer = inject(Renderer2);
    private config = inject(ConfigService, { optional: true });

    private originalId$ = new BehaviorSubject<string>(null);
    private iconContext$ = new BehaviorSubject<string>(null);
    private _id: string;
    private _customClass: string;
    private _aria: boolean;
    private altTextSpan: HTMLElement;
    private isReady = false;
    private svg: SVGSVGElement;

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
    @Input() set esIconContext(context: string) {
        this.iconContext$.next(context);
    }
    @Input() path?: string;

    constructor() {
        combineLatest([
            this.originalId$.pipe(filter(notNull)),
            this.iconContext$,
            this.config.get('icons', null).catch((_) => Promise.resolve([])),
        ])
            .pipe(takeUntilDestroyed())
            .subscribe(([originalId, iconContext, iconsConfig]) =>
                this.setIcon(originalId, iconContext, iconsConfig),
            );
    }

    async ngOnInit() {
        this.isReady = true;
        this.element.nativeElement.setAttribute('aria-hidden', 'true');
        // Material styles expect icons to have the class `mat-icon`, e.g.,
        // https://github.com/angular/components/blob/ae0b9e1c1bae5e937d039ea53652fe1656bc4623/src/material/form-field/form-field.scss#L156
        this.element.nativeElement.classList.add('mat-icon');
        this.updateAria();
    }

    ngOnDestroy(): void {
        if (this.altTextSpan) {
            this.altTextSpan.remove();
        }
    }

    /**
     * The `context` of an icons config entry is a regular expression, matched against the whole
     * context of the icon (i.e. it is implicitly anchored, so a plain string still behaves like an
     * exact match).
     */
    private static matchesContext(entry: IconsConfig[0], context: string): boolean {
        if (!IconDirective.contextPatterns.has(entry.context)) {
            let pattern: RegExp | null;
            try {
                pattern = new RegExp(`^(?:${entry.context})$`);
            } catch (error) {
                console.error(`Invalid regex in icons config context "${entry.context}"`, error);
                pattern = null;
            }
            IconDirective.contextPatterns.set(entry.context, pattern);
        }
        return IconDirective.contextPatterns.get(entry.context)?.test(context) ?? false;
    }

    private async setIcon(id: string, context: string, iconsConfig: IconsConfig) {
        this.element.nativeElement.classList.remove('edu-icons', 'custom-icons', 'material-icons');
        if (this._customClass) {
            // the mapped class (e.g. a `fa-*` class) carries the glyph via a CSS custom property,
            // so it must be dropped explicitly — otherwise the previous icon keeps winning the
            // cascade and the icon appears frozen when the input changes.
            this.element.nativeElement.classList.remove(this._customClass);
            this._customClass = null;
        }
        if (this.svg) {
            this.renderer.removeChild(this.element.nativeElement, this.svg);
            this.svg = null;
        }
        this.element.nativeElement.innerText = '';

        // resolve the config override first, so a mapping may also point at an `svg-` icon and an
        // `svg-` icon can itself be overridden
        let customClass: string = null;
        // the context regex is matched against '' when the icon sets no context, so a pattern like
        // `.*` also covers context-less icons (use `.+` to require a context)
        let mapping: IconsConfig = iconsConfig?.filter(
            (i) => i.original === id && i.context && IconDirective.matchesContext(i, context ?? ''),
        );
        if (!mapping?.length) {
            mapping = iconsConfig?.filter((i) => i.original === id && !i.context);
        }
        if (mapping?.length) {
            // the first matching entry wins — with regex contexts, more than one entry may match
            id = mapping[0].replace || '';
            customClass = mapping[0].cssClass;
        }
        this._id = id;

        if (id.startsWith('svg-')) {
            try {
                const iconName = id.slice(4);
                const fileName = iconName.endsWith('.svg') ? iconName : `${iconName}.svg`;
                const path = `${this.path || 'assets/images/icons/'}${fileName}`;

                const svgText = await firstValueFrom(this.http.get(path, { responseType: 'text' }));

                // a newer icon was requested while this one was loading
                if (this._id !== id) return;
                if (!svgText) return;

                const parser = new DOMParser();
                const doc = parser.parseFromString(svgText, 'image/svg+xml');
                const svgElement = doc.querySelector('svg');

                if (!svgElement) {
                    console.error('No SVG element found in the file');
                    return;
                }

                svgElement.classList.add('svg-icons');
                this.svg = svgElement;

                this.renderer.appendChild(this.element.nativeElement, this.svg);

                if (this._aria) {
                    this.updateAria();
                }

                return;
            } catch (error) {
                console.error('Error loading SVG:', error);
                return;
            }
        }
        if (this._aria) {
            this.updateAria();
        }
        let cssClass: string;
        if (id?.startsWith('edu-') && !customClass) {
            cssClass = 'edu-icons';
            id = id.substring(4);
        } else if (id?.startsWith('custom-') || customClass) {
            cssClass = 'custom-icons';
            // only strip the prefix when it is actually present — a mapping may supply just a
            // `cssClass` and leave the id (or an empty replacement) untouched
            if (id?.startsWith('custom-')) {
                id = id.substring(7);
            }
        } else {
            cssClass = 'material-icons';
        }
        this.element.nativeElement.classList.add(cssClass);
        if (customClass) {
            this._customClass = customClass;
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
        if (this.svg) {
            // for SVG elements, add aria-label instead
            this.svg.setAttribute('aria-label', altText);
        }
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
