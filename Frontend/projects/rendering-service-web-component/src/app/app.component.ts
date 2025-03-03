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
    @Input() encodedNode: string;
    @Input() signature: string;
    @Input() jwt: string;
    @Input() renderUrl: string;
    @Input() encodedUser: string;

    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);

    constructor(private renderHelperService: RenderHelperService) {}

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.node_id) {
            const data = await this.renderHelperService.getRenderDataForLms(
                this.encodedNode,
                this.signature,
                this.jwt,
                this.renderUrl,
                this.encodedUser,
            );
            this.node.set(data.node);
            this.request.set(data.request);
        }
    }
}
