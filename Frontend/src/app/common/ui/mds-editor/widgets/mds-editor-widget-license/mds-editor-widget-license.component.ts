import {Component, OnInit, Input, SimpleChanges, OnChanges, NgZone} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidgetComponent} from '../../mds-editor-view/mds-editor-view.component';
import {RestConnectorService} from '../../../../../core-module/rest/services/rest-connector.service';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {MainNavService} from '../../../../services/main-nav.service';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {Node} from '../../../../../core-module/rest/data-object';
import {MdsEditorWidgetBase, ValueType} from '../mds-editor-widget-base';
import {TranslateService} from '@ngx-translate/core';
import {NodeHelperService} from '../../../../../core-ui-module/node-helper.service';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {MdsWidgetValue, Values} from '../../types';
import {UIHelper} from '../../../../../core-ui-module/ui-helper';

@Component({
    selector: 'app-mds-editor-widget-license',
    templateUrl: './mds-editor-widget-license.component.html',
    styleUrls: ['./mds-editor-widget-license.component.scss'],
})
export class MdsEditorWidgetLicenseComponent extends MdsEditorWidgetBase implements OnInit, OnChanges, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: false,
        supportsBulk: true,
    };
    readonly valueType: ValueType = ValueType.MultiValue;
    hasChanges = new BehaviorSubject<boolean>(false);

    isSafe: boolean;
    nodes: Node[];
    licenses: License[];
    // ids of checked licenses
    checked: string[] = [];

    constructor(
        private connector: RestConnectorService,
        private mainnav: MainNavService,
        private sanitizer: DomSanitizer,
        private ngZone: NgZone,
        public translate: TranslateService,
        private nodeHelper: NodeHelperService,
        public mdsEditorValues: MdsEditorInstanceService,
    ) {
        super(mdsEditorValues, translate);
        this.isSafe = this.connector.getCurrentLogin()?.currentScope === RestConstants.SAFE_SCOPE;
    }

    ngOnInit(): void {
        this.nodes = this.mdsEditorValues.nodes$.value;
        this.mdsEditorValues.nodes$.subscribe((n) => this.nodes = n);
        this.licenses = this.widget?.definition?.values.map((v) => {
            const url = this.nodeHelper.getLicenseIconByString(v.id, false);
            const license: License = v;
            if(url) {
                license.imageUrl = this.sanitizer.bypassSecurityTrustUrl(url);
            }
            return license;
        });
        this.checked = this.widget.getInitialValues().jointValues ?? [];
    }
    async getValues(values: Values) {
        // nodes mode is read-only, so do not change anything
        if(this.mdsEditorValues.editorMode === 'nodes') {
            return values;
        }
        if(this.checked.length) {
            values[this.widget.definition.id] = this.checked;
        } else {
            delete values[this.widget.definition.id];
        }
        return values;
    }
    ngOnChanges(changes: SimpleChanges) {
    }
    openLicense(): void {
        this.mainnav.getDialogs().nodeLicense = this.mdsEditorValues.nodes$.value;
        // increase priority to have license in foreground
        UIHelper.waitForComponent(this.ngZone, this.mainnav.getDialogs(), 'licenseComponent').subscribe(() =>
            this.mainnav.getDialogs().licenseComponent.priority = 2
        );
        // @TODO: With the rebuild to individual dialogs, we should handle this with scoped services!
        const oldState = this.mainnav.getDialogs().reopenSimpleEdit;
        if(this.mainnav.getDialogs().reopenSimpleEdit) {
            this.mainnav.getDialogs().reopenSimpleEdit = false;
        }
        this.mainnav.getDialogs().onRefresh.first().subscribe((nodes: Node[]) => {
            this.nodes = nodes;
            this.mdsEditorValues.updateNodes(nodes);
            this.mainnav.getDialogs().reopenSimpleEdit = oldState;
        });
    }

    updateValue(license: License, status: boolean) {
        if(status) {
            this.checked.push(license.id);
        } else {
            this.checked.splice(this.checked.indexOf(license.id), 1);
        }
        this.hasChanges.next(true);
    }
}
type License = MdsWidgetValue & {imageUrl?: SafeUrl}
