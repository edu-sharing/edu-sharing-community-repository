import {
    AfterViewInit,
    Component,
    ElementRef,
    Input,
    OnInit,
    Renderer2,
    ViewChild,
    DOCUMENT,
    inject,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PlatformLocation } from '@angular/common';
import {
    RENDERING_SERVICE_LIB_CONFIG,
    RenderingServiceLibConfiguration,
} from '../../../rendering-service-lib-configuration';

@Component({
    selector: 'rs-module-h5p',
    imports: [RenderingModule, MatButtonModule, MatIconModule],
    templateUrl: './h5p.component.html',
    styleUrl: './h5p.component.scss',
})
export class H5pComponent implements RenderModule, OnInit, AfterViewInit {
    private renderer = inject(Renderer2);
    private sanitizer = inject(DomSanitizer);
    private document = inject<Document>(DOCUMENT);
    private platformLocation = inject(PlatformLocation);
    configuration = inject<RenderingServiceLibConfiguration>(RENDERING_SERVICE_LIB_CONFIG, {
        optional: true,
    });

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @ViewChild('iframeEl', { static: false }) iframeEl?: ElementRef<HTMLIFrameElement>;
    private assetsUrl: string = '';

    sanitizedUrl: SafeResourceUrl = new (class implements SafeResourceUrl {})();

    constructor() {
        const configuration = this.configuration;

        if (configuration && configuration.assetsUrl) {
            this.assetsUrl = (configuration?.assetsUrl || '').replace('assets', '');
        } else {
            this.assetsUrl = this.platformLocation.getBaseHrefFromDOM();
        }
    }

    ngOnInit() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            this.sanitizedUrl = this.getSafeUri();
        }
    }

    ngAfterViewInit(): void {
        const h5pResizerUrl = this.assetsUrl + 'h5p-resizer.js';

        this.addScriptAfterIframe(h5pResizerUrl);
    }

    getSafeUri() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            const uri = new URL(this.data.items[0].link);
            /**
      if (uri.hostname.includes('nip.io')) {
        uri.hostname = 'localhost'
      }
        */
            return this.sanitizer.bypassSecurityTrustResourceUrl(uri.toString());
        }
        return new (class implements SafeResourceUrl {})();
    }

    private addScriptAfterIframe(src: string): void {
        if (!this.iframeEl?.nativeElement?.parentNode) {
            return;
        }

        if (this.document.querySelector(`script[src="${src}"]`)) {
            return;
        }

        const script = this.renderer.createElement('script');
        script.type = 'text/javascript';
        script.src = src;
        script.async = true;

        const parent = this.iframeEl.nativeElement.parentNode;
        this.renderer.insertBefore(parent, script, this.iframeEl.nativeElement.nextSibling);
    }
}
