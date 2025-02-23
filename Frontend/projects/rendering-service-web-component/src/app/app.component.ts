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
    @Input() node_id: string;
    @Input() version: string;

    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);

    constructor(private renderHelperService: RenderHelperService) {}

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.node_id) {
            const data = await this.renderHelperService.getRenderData(
                changes.node_id.currentValue,
                changes.version?.currentValue,
            );
            this.node.set(data.node);
            this.request.set(data.request);
        }
    }
}
