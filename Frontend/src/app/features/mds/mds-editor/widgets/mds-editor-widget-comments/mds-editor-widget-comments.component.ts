import { Component, inject } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';

@Component({
    selector: 'es-mds-editor-widget-comments',
    templateUrl: './mds-editor-widget-comments.component.html',
    styleUrls: ['./mds-editor-widget-comments.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetCommentsComponent implements NativeWidgetComponent {
    mdsEditorInstanceService = inject(MdsEditorInstanceService);

    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);

    isEmpty = of(false);
}
