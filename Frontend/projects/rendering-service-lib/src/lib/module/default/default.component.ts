import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiModule, NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { RenderingConnectorService } from '../../services/abstract/rendering-connector.service';

@Component({
    selector: 'rs-module-default',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule, EduSharingUiModule],
    templateUrl: './default.component.html',
    styleUrl: './default.component.scss',
})
export class DefaultComponent implements RenderModule, OnChanges {
    private nodeHelper = inject(NodeHelperService);
    // only provided by hosts supporting connectors (not by the rendering web component)
    private connectorService = inject(RenderingConnectorService, { optional: true });

    @Input() data: undefined;
    @Input() node: Node | undefined;
    previewUrl: String = '';
    downloadUrl: string = '';
    /** whether the material can be opened with a connector (e.g. ONLYOFFICE) */
    readonly canOpen = signal(false);

    ngOnChanges(changes: SimpleChanges): void {
        if (this.node !== undefined) {
            this.previewUrl = this.node.preview?.url ?? '';
            // The module is the fallback for material the browser cannot display, so the only
            // useful actions are opening it with a connector or downloading it - each offered just
            // to users who may actually do so.
            this.downloadUrl = this.nodeHelper.getNodesRight(
                [this.node],
                'DownloadContent',
                NodesRightMode.Effective,
            )
                ? this.node.downloadUrl ?? ''
                : '';
            void this.updateCanOpen(this.node);
        }
    }

    async openConnector(): Promise<void> {
        if (this.node) {
            await this.connectorService?.openConnector(this.node);
        }
    }

    private async updateCanOpen(node: Node): Promise<void> {
        this.canOpen.set(false);
        let canOpen = false;
        try {
            canOpen = (await this.connectorService?.hasAvailableConnector(node)) ?? false;
        } catch (e) {
            console.warn('Could not resolve the connector of the node', e);
        }
        // ignore results for a node that has been replaced in the meantime
        if (node === this.node) {
            this.canOpen.set(canOpen);
        }
    }
}
