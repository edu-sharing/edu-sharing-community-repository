import { Component, Input, OnInit } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'rs-module-moodle',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule],
    templateUrl: './moodle.component.html',
    styleUrl: './moodle.component.scss',
})
export class MoodleComponent implements RenderModule, OnInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() isWebComponent: boolean = false;
    previewUrl: string = '';
    sanitizedUrl: SafeResourceUrl | null = null;
    sanitizedLinkUrl: SafeResourceUrl | null = null;

    constructor(private sanitizer: DomSanitizer) {}

    ngOnInit() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            this.sanitizedUrl = this.getSafeUri();
            this.sanitizedLinkUrl = this.getSafeLinkUri();
            this.previewUrl = this.node?.preview?.url ?? '';
        }
    }

    getSafeUri() {
        if (this.data?.items !== undefined && this.data.items[0].link) {
            const uri = new URL(this.data.items[0].link);
            return this.sanitizer.bypassSecurityTrustResourceUrl(uri.toString());
        }
        return null;
    }

    getSafeLinkUri() {
        if (this.data?.items !== undefined && this.data.items[0].additionalData) {
            const uri = new URL(this.data.items[0].additionalData['linkUrl'] ?? '');
            return this.sanitizer.bypassSecurityTrustResourceUrl(uri.toString());
        }
        return null;
    }
}
