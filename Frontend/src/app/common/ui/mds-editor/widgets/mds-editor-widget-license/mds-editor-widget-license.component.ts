import { Component, OnInit, Input } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidget} from '../../mds-editor-view/mds-editor-view.component';
import {RestConnectorService} from '../../../../../core-module/rest/services/rest-connector.service';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {MainNavService} from '../../../../services/main-nav.service';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {Node} from '../../../../../core-module/rest/data-object';

@Component({
    selector: 'app-mds-editor-widget-license',
    templateUrl: './mds-editor-widget-license.component.html',
    styleUrls: ['./mds-editor-widget-license.component.scss'],
})
export class MdsEditorWidgetLicenseComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: true,
    };
    hasChanges = new BehaviorSubject<boolean>(false);

    isSafe: boolean;
    nodes: Node[];

    constructor(
        private connector: RestConnectorService,
        private mainnav: MainNavService,
        private mdsEditorValues: MdsEditorInstanceService,
    ) {
        this.isSafe = this.connector.getCurrentLogin()?.currentScope === RestConstants.SAFE_SCOPE;
    }

    ngOnInit(): void {
        this.nodes = this.mdsEditorValues.nodes$.value;
        this.mdsEditorValues.nodes$.subscribe((n) => this.nodes = n);
    }

    openLicense(): void {
        this.mainnav.getDialogs().nodeLicense = this.mdsEditorValues.nodes$.value;
        this.mainnav.getDialogs().onRefresh.first().subscribe((nodes: Node[]) =>
            this.nodes = nodes
        );
    }
}
