import { Component, OnInit } from '@angular/core';
import { View } from '../../../../core-module/core.module';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';

@Component({
    selector: 'app-mds-editor-core',
    templateUrl: './mds-editor-core.component.html',
    styleUrls: ['./mds-editor-core.component.scss'],
})
export class MdsEditorCoreComponent implements OnInit {
    views: View[];

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.views = this.mdsEditorInstance.views;
    }
}
