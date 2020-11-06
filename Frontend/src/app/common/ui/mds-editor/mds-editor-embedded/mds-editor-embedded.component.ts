import { Component, OnInit } from '@angular/core';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';

@Component({
    selector: 'app-mds-editor-embedded',
    templateUrl: './mds-editor-embedded.component.html',
    styleUrls: ['./mds-editor-embedded.component.scss'],
})
export class MdsEditorEmbeddedComponent implements OnInit {
    constructor(private mdsEditorInstance: MdsEditorInstanceService) {
        this.mdsEditorInstance.isEmbedded = true;
    }

    ngOnInit(): void {}
}
