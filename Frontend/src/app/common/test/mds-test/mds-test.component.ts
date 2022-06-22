import { Component } from '@angular/core';
import {
    Collection,
    Node,
    RestCollectionService,
    RestConnectorService,
} from '../../../core-module/core.module';

@Component({
    selector: 'es-mds-test',
    templateUrl: './mds-test.component.html',
})
export class MdsTestComponent {
    public collections: Node[];
    constructor(
        private collectionsService: RestCollectionService,
        private connector: RestConnectorService,
    ) {
        connector.login('admin', 'admin').subscribe(() => {
            collectionsService.search('*').subscribe((list) => {
                this.collections = list.collections;
            });
        });
    }
}
