/**
 * Created by Torsten on 13.01.2017.
 */

import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Directive, ElementRef, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '../../../core-module/rest/services/configuration.service';

/**
 * Replaces the element's content with an icon.
 *
 * Example: `<i icon="save"></i>`
 *
 * Optionally, a translated `aria-label` can be attached by setting `aria` to a truthy value: `<i
 * icon="save" aria="true"></i>`. Otherwise, `aria-hidden` will be set.
 *
 * For backwards compatibility, the directive is also activated on elements that set
 * `class="material-icons"`. This is mainly to set the `aria-hidden` attribute. Occurrences should
 * be updated to the syntax above.
 */
@Directive({
    selector: '[icon], .material-icons',
})
export class IconDirective {
    private _id: string;
    private _aria = false;
    private iconsConfig: Array<{ original: string; replace: string }>;

    @Input() set aria(aria: boolean) {
        aria = coerceBooleanProperty(aria);
        if (aria !== this._aria) {
            this._aria = aria;
            this.updateAria();
        }
    }

    @Input() set icon(id: string) {
        this.setIcon(id);
    }

    constructor(
        private element: ElementRef,
        private translate: TranslateService,
        private config: ConfigurationService,
    ) {
        // FIXME: This might resolve after `setIcon` was called and mappings might be ignored.
        this.config.get('icons', null).subscribe((icons) => (this.iconsConfig = icons));
        this.updateAria();
    }

    private setIcon(id: string) {
        if (this._id) {
            this.element.nativeElement.classList.remove(
                'edu-icons',
                'custom-icons',
                'material-icons',
            );
        }
        const mapping = this.iconsConfig?.filter((i) => i.original === id);
        if (mapping?.length === 1) {
            id = mapping[0].replace;
        }
        this._id = id;
        if (this._aria) {
            this.updateAria();
        }
        let cssClass: string;
        if (id.startsWith('edu-')) {
            cssClass = 'edu-icons';
            id = id.substr(4);
        } else if (id.startsWith('custom-')) {
            cssClass = 'custom-icons';
            id = id.substr(7);
        } else {
            cssClass = 'material-icons';
        }
        this.element.nativeElement.classList.add(cssClass);
        this.element.nativeElement.innerText = id;
    }

    private updateAria() {
        this.element.nativeElement.removeAttribute('aria-label');
        this.element.nativeElement.removeAttribute('aria-hidden');
        if (this._aria && this._id) {
            this.translate.get('ICON_LABELS.' + this._id).subscribe((lang) => {
                this.element.nativeElement.setAttribute('aria-label', lang);
            });
        } else {
            this.element.nativeElement.setAttribute('aria-hidden', true);
        }
    }
}
