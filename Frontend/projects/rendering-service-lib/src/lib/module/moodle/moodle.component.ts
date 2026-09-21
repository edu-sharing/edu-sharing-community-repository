import { Component, Input, OnInit, inject } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'rs-module-moodle',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule, EduSharingUiModule],
    templateUrl: './moodle.component.html',
    styleUrl: './moodle.component.scss',
    // Without the iframe there is nothing that fills the module box, so the host page can size the
    // module by its preview image (like the url module) instead of the 16/9 frame it reserves for
    // the embedded course.
    host: { '[class.link-only]': '!showIframe' },
})
export class MoodleComponent implements RenderModule, OnInit {
    private sanitizer = inject(DomSanitizer);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() isWebComponent: boolean = false;
    previewUrl: string = '';
    sanitizedUrl: SafeResourceUrl | null = null;
    sanitizedLinkUrl: SafeResourceUrl | null = null;
    showIframe: boolean = false;

    ngOnInit() {
        // Not keyed off items[0].link any more: the backend only builds the forward link the
        // frontend is going to use, so in link-only mode there is no iframe url at all.
        const item = this.data?.items?.[0];
        if (!item) {
            return;
        }
        // Configured per repository as a Moodle module credential and normalized to "true"/"false"
        // by the backend. Compared against 'false' rather than 'true' so that a backend which does
        // not send it at all keeps the previous behaviour: the embedded course.
        this.showIframe =
            !this.isWebComponent && item.additionalData?.['showPreviewIframe'] !== 'false';
        this.sanitizedUrl = this.getSafeUri();
        this.sanitizedLinkUrl = this.getSafeLinkUri();
        this.previewUrl = this.node?.preview?.url ?? '';
        // A frame without a src would leave an empty box - fall back to the preview image instead.
        this.showIframe = this.showIframe && this.sanitizedUrl !== null;
    }

    getSafeUri() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            const uri = new URL(this.data.items[0].link);
            return this.sanitizer.bypassSecurityTrustResourceUrl(uri.toString());
        }
        return null;
    }

    getSafeLinkUri() {
        // additionalData carries the display switch as well, so it can be present without a
        // linkUrl - new URL('') would throw and take the whole component down with it.
        const linkUrl = this.data?.items?.[0].additionalData?.['linkUrl'];
        if (!linkUrl) {
            return null;
        }
        return this.sanitizer.bypassSecurityTrustResourceUrl(new URL(linkUrl).toString());
    }
}
