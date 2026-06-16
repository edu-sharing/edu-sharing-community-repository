import { Component, Input, OnInit, inject } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'rs-module-edu-html',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule],
    templateUrl: './eduHtml.component.html',
    styleUrl: './eduHtml.component.scss',
})
export class EduHtmlComponent implements RenderModule, OnInit {
    private sanitizer = inject(DomSanitizer);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    sanitizedUrl: SafeResourceUrl = new (class implements SafeResourceUrl {})();
    url: string = '';

    ngOnInit() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            this.sanitizedUrl = this.getSafeUri();
            this.url = this.data.items[0].link;
        }
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
}
