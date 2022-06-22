import { Component, Input, Output, EventEmitter } from '@angular/core';
import { DialogButton, RestConstants } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { RestSearchService } from '../../../core-module/core.module';
import { DateHelper } from '../../../core-ui-module/DateHelper';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'es-search-save-search',
    templateUrl: 'save-search.component.html',
    styleUrls: ['save-search.component.scss'],
})
export class SearchSaveSearchComponent {
    @Input() set searchQuery(searchQuery: string) {
        this.setName(searchQuery);
    }
    @Input() set name(name: string) {
        if (name) this._name = name;
    }

    @Output() onClose = new EventEmitter();
    @Output() onSave = new EventEmitter();
    public _name: string;
    buttons: DialogButton[];
    constructor(
        private search: RestSearchService,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.setName();
        this.buttons = DialogButton.getSaveCancel(
            () => this.cancel(),
            () => this.save(),
        );
    }
    public cancel() {
        this.onClose.emit();
    }
    public save() {
        this.onSave.emit(this._name);
    }
    private setName(searchQuery: string = null) {
        this._name =
            (searchQuery
                ? searchQuery
                : this.translate.instant('SEARCH.SAVE_SEARCH.UNKNOWN_QUERY')) +
            ' - ' +
            DateHelper.formatDate(this.translate, Date.now(), {
                showAlwaysTime: true,
                useRelativeLabels: false,
            });
    }
}
