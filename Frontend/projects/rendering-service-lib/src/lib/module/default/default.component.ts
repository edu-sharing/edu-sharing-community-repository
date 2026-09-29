import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiModule, NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'rs-module-default',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule, EduSharingUiModule],
    templateUrl: './default.component.html',
    styleUrl: './default.component.scss',
})
export class DefaultComponent implements RenderModule, OnChanges {
    private nodeHelper = inject(NodeHelperService);

    @Input() data: undefined;
    @Input() node: Node | undefined;
    previewUrl: String = '';
    downloadUrl: string = '';

    ngOnChanges(changes: SimpleChanges): void {
        if (this.node !== undefined) {
            this.previewUrl = this.node.preview?.url ?? '';
            // The module is the fallback for material the browser cannot display, so the only
            // useful action is downloading it - offered just to users who may actually do so.
            this.downloadUrl = this.nodeHelper.getNodesRight(
                [this.node],
                'DownloadContent',
                NodesRightMode.Effective,
            )
                ? this.node.downloadUrl ?? ''
                : '';
        }
    }
}
