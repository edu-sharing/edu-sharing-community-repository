import { Component, Input, OnChanges, signal, SimpleChanges, inject } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { FormsModule } from '@angular/forms';
import { RenderData, AssetStateItem } from '../../dto/RenderData';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatAnchor } from '@angular/material/button';

@Component({
    selector: 'rs-module-binder',
    imports: [RenderingModule, FormsModule, MatProgressSpinner, MatAnchor],
    templateUrl: './binder.component.html',
    styleUrl: './binder.component.scss',
})
export class BinderComponent implements RenderModule, OnChanges {
    private sanitizer = inject(DomSanitizer);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    binderItem = signal<AssetStateItem | undefined>(undefined);
    previewUrl = signal<SafeResourceUrl | undefined>(undefined);
    hasPreview = signal<boolean>(false);

    ngOnChanges(changes: SimpleChanges): void {
        const binderJobData = this.data?.items?.find((item) => item.additionalData === null);
        if (binderJobData !== undefined) {
            this.binderItem.set(binderJobData);
        }
        const previewJobData = this.data?.items?.find((item) => item.additionalData !== null);
        if (previewJobData !== undefined) {
            this.hasPreview.set(true);
            if (previewJobData.link !== undefined && previewJobData.link !== '') {
                this.previewUrl.set(this.getSafeUri(previewJobData.link));
            }
        }
    }

    getSafeUri(assetLink: string) {
        const uri = new URL(assetLink);
        /**
      if (uri.hostname.includes("nip.io")) {
        uri.hostname = "localhost"
      }
        */
        return this.sanitizer.bypassSecurityTrustResourceUrl(uri.toString());
    }
}
