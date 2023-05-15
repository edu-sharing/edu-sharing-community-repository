import { filter } from 'rxjs/operators';
import { Component, OnInit, ViewChild } from '@angular/core';
import { VCard } from '../../../../../core-module/ui/VCard';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { FormControl, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { Toast } from '../../../../../core-ui-module/toast';
import { MatTabGroup } from '@angular/material/tabs';
import { MdsWidget } from '../../../types/types';

export interface AuthorData {
    freetext: string;
    author: VCard;
}

@Component({
    selector: 'es-mds-editor-widget-vcard',
    templateUrl: './mds-editor-widget-vcard.component.html',
    styleUrls: ['./mds-editor-widget-vcard.component.scss'],
})
export class MdsEditorWidgetVCardComponent extends MdsEditorWidgetBase implements OnInit {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    @ViewChild(MatTabGroup) tabGroup: MatTabGroup;
    readonly valueType: ValueType = ValueType.String;

    formControl: FormGroup;
    editType: number;

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private toast: Toast,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnInit(): void {
        let initialValue = this.widget.getInitialValues().jointValues;
        if (!initialValue) {
            initialValue = [''];
        }
        const vcard = new VCard(initialValue[0]);
        this.editType = vcard.getType();
        this.formControl = new FormGroup(
            {
                givenname: new FormControl(vcard.givenname),
                surname: new FormControl(vcard.surname),
                org: new FormControl(vcard.org),
            },
            this.getStandardValidators(),
        );
        this.formControl.valueChanges.pipe(filter((value) => value !== null)).subscribe((value) => {
            vcard.givenname = value.givenname;
            vcard.surname = value.surname;
            vcard.org = value.org;
            let result = initialValue.slice();
            if (vcard.isValid()) {
                result[0] = vcard.toVCardString();
            } else {
                result = initialValue.slice(1);
            }
            this.setValue(result);
        });
        setTimeout(() => this.tabGroup.realignInkBar());
    }

    focus(): void {}

    blur(): void {
        this.onBlur.emit();
    }
    public static mapGraphqlId(definition: MdsWidget) {
        // attach the "Contributor" graphql Attributes
        return MdsEditorWidgetBase.attachGraphqlSelection(definition, ['role', 'content']);
    }
}
