import { Component, OnInit, Input } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidgetComponent} from '../../mds-editor-view/mds-editor-view.component';
import {MainNavService} from '../../../../services/main-nav.service';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {MdsEditorWidgetAuthorComponent} from '../mds-editor-widget-author/mds-editor-widget-author.component';
import {NativeWidgetType} from '../../types';

@Component({
    selector: 'app-mds-editor-widget-link',
    templateUrl: './mds-editor-widget-link.component.html',
    styleUrls: ['./mds-editor-widget-link.component.scss'],
})
export class MdsEditorWidgetLinkComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    @Input() widgetName: NativeWidgetType.Maptemplate | NativeWidgetType.Contributor;

    hasChanges = new BehaviorSubject<boolean>(false);

    // caption: string; // Could use as label.
    linkLabel: string;

    constructor(
        private mainnav: MainNavService,
        private mdsEditorInstanceService: MdsEditorInstanceService,
    ) {}

    ngOnInit(): void {
        switch (this.widgetName) {
            case 'maptemplate':
                this.linkLabel = 'MDS.TEMPLATE_LINK';
                break;
            case 'contributor':
                this.linkLabel = 'MDS.CONTRIBUTOR_LINK';
                break;
        }
    }

    async onClick() {
        if (this.widgetName === 'maptemplate') {
            const nodes = await this.mdsEditorInstanceService.save();
            if(Array.isArray(nodes)) {
                this.mainnav.getDialogs().onRefresh.emit(nodes);
            }
            this.mainnav.getDialogs().nodeMetadata = null;

            this.mainnav.getDialogs().nodeMetadata = null;
            this.mainnav.getDialogs().nodeTemplate = this.mdsEditorInstanceService.nodes$.value[0];
        } else if (this.widgetName === 'contributor') {
            await MdsEditorWidgetAuthorComponent.openContributorDialog(
                this.mdsEditorInstanceService,
                this.mainnav
            );
        } else {
            throw new Error('not implemented');
        }
    }
}
