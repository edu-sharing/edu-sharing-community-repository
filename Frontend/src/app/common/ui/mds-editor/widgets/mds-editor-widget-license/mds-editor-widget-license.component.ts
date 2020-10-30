import {Component, OnInit, Input, SimpleChanges, OnChanges} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidget} from '../../mds-editor-view/mds-editor-view.component';
import {RestConnectorService} from '../../../../../core-module/rest/services/rest-connector.service';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {MainNavService} from '../../../../services/main-nav.service';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {Node} from '../../../../../core-module/rest/data-object';
import {MdsEditorWidgetBase, ValueType} from '../mds-editor-widget-base';
import {TranslateService} from '@ngx-translate/core';
import {NodeHelper} from '../../../../../core-ui-module/node-helper';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {MdsWidgetValue, Values} from '../../types';

@Component({
    selector: 'app-mds-editor-widget-license',
    templateUrl: './mds-editor-widget-license.component.html',
    styleUrls: ['./mds-editor-widget-license.component.scss'],
})
export class MdsEditorWidgetLicenseComponent extends MdsEditorWidgetBase implements OnInit, OnChanges, NativeWidget {
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
        public translate: TranslateService,
        public mdsEditorValues: MdsEditorInstanceService,
    ) {
        super(mdsEditorValues, translate);
        this.isSafe = this.connector.getCurrentLogin()?.currentScope === RestConstants.SAFE_SCOPE;
    }

    ngOnInit(): void {
        this.nodes = this.mdsEditorValues.nodes$.value;
        this.mdsEditorValues.nodes$.subscribe((n) => this.nodes = n);
        this.licenses = this.widget?.definition?.values.map((v) => {
            const url = NodeHelper.getLicenseIconByString(v.id, this.connector, false);
            const license: License = v;
            if(url) {
                license.imageUrl = this.sanitizer.bypassSecurityTrustUrl(url);
            }
            return license;
        });
        this.checked = this.widget.getInitialValues().jointValues ?? [];
    }
    getValues(values: Values) {
        if(this.checked.length) {
            values[this.widget.definition.id] = this.checked;
        }
        return values;
    }
    ngOnChanges(changes: SimpleChanges) {
    }
    openLicense(): void {
        this.mainnav.getDialogs().nodeLicense = this.mdsEditorValues.nodes$.value;
        this.mainnav.getDialogs().onRefresh.first().subscribe((nodes: Node[]) =>
            this.nodes = nodes
        );
    }

    updateValue(license: License, status: boolean) {
        if(status) {
            this.checked.push(license.id);
        } else {
            this.checked.splice(this.checked.indexOf(license.id), 1);
        }
    }
}
type License = MdsWidgetValue & {imageUrl?: SafeUrl}
