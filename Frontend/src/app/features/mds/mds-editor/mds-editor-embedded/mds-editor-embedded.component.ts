import { Component, inject } from '@angular/core';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';

@Component({
    selector: 'es-mds-editor-embedded',
    templateUrl: './mds-editor-embedded.component.html',
    styleUrls: ['./mds-editor-embedded.component.scss'],
    standalone: false,
})
export class MdsEditorEmbeddedComponent {
    private mdsEditorInstance = inject(MdsEditorInstanceService);

    constructor() {
        this.mdsEditorInstance.isEmbedded = true;
    }
}
