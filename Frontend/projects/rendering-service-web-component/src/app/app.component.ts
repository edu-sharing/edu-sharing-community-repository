import { Node } from 'ngx-edu-sharing-api';
import { Component, Input, OnChanges, signal, SimpleChanges } from '@angular/core';
import { RenderHelperService } from 'ngx-edu-sharing-ui';
import { RenderDataRequestWithToken } from 'ngx-rendering-service-api';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnChanges {
    @Input() encoded_node: string;
    @Input() signature: string;
    @Input() jwt: string;
    @Input() render_url: string;
    @Input() encoded_user: string;
    @Input() service_worker_url: string;

    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);
    serviceWorkerUrl = signal<string>(null);

    constructor(private renderHelperService: RenderHelperService) {}

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.encoded_node) {
            const data = await this.renderHelperService.getRenderDataForLms(
                this.encoded_node,
                this.signature,
                this.jwt,
                this.render_url,
                this.encoded_user,
            );
            this.node.set(data.node);
            this.request.set(data.request);
            this.serviceWorkerUrl.set(this.service_worker_url);
        }
    }
}
