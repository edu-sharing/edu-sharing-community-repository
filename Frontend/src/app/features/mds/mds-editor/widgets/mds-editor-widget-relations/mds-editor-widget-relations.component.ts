import { Component, inject } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';

@Component({
    selector: 'es-mds-editor-widget-relations',
    templateUrl: './mds-editor-widget-relations.component.html',
    styleUrls: ['./mds-editor-widget-relations.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetRelationsComponent implements NativeWidgetComponent {
    mdsEditorInstanceService = inject(MdsEditorInstanceService);

    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);

    isEmpty = of(false);
}
