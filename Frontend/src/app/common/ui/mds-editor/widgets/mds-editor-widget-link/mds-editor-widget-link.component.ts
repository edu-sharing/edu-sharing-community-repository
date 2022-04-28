import {Component, OnInit, Input, NgZone} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidgetComponent} from '../../mds-editor-view/mds-editor-view.component';
import {MainNavService} from '../../../../services/main-nav.service';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {MdsEditorWidgetAuthorComponent} from '../mds-editor-widget-author/mds-editor-widget-author.component';
import {Constraints, NativeWidgetType} from '../../types';
import {MdsEditorWidgetBase, ValueType} from '../mds-editor-widget-base';
import {
    RestConnectorService
} from '../../../../../core-module/rest/services/rest-connector.service';
import {DomSanitizer} from '@angular/platform-browser';
import {TranslateService} from '@ngx-translate/core';
import {NodeHelperService} from '../../../../../core-ui-module/node-helper.service';

@Component({
    selector: 'es-mds-editor-widget-link',
    templateUrl: './mds-editor-widget-link.component.html',
    styleUrls: ['./mds-editor-widget-link.component.scss'],
})
export class MdsEditorWidgetLinkComponent extends MdsEditorWidgetBase implements OnInit, NativeWidgetComponent {
    static readonly constraints: Constraints = {
        supportsInlineEditing: true,
        requiresNode: true,
        supportsBulk: false,
    };
    readonly valueType: ValueType = ValueType.String;
    @Input() widgetName: NativeWidgetType.Maptemplate | NativeWidgetType.Contributor;

    hasChanges = new BehaviorSubject<boolean>(false);

    // caption: string; // Could use as label.
    linkLabel: string;

    constructor(
        private connector: RestConnectorService,
        private mainnav: MainNavService,
        public translate: TranslateService,
        public mdsEditorValues: MdsEditorInstanceService,
    ) {
        super(mdsEditorValues, translate);
    }
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
            const nodes = await this.mdsEditorValues.save();
            if(Array.isArray(nodes)) {
                this.mainnav.getDialogs().onRefresh.emit(nodes);
            }
            this.mainnav.getDialogs().nodeMetadata = null;

            this.mainnav.getDialogs().nodeMetadata = null;
            this.mainnav.getDialogs().nodeTemplate = this.mdsEditorValues.nodes$.value[0];
        } else if (this.widgetName === 'contributor') {
            await MdsEditorWidgetAuthorComponent.openContributorDialog(
                this.mdsEditorValues,
                this.mainnav
            );
        } else {
            throw new Error('not implemented');
        }
    }
}
