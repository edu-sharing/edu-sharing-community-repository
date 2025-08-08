import {
    Component,
    EventEmitter,
    input,
    OnDestroy,
    OnInit,
    Output,
    signal,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    EduSharingUiCommonModule,
    ElementType,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    Target,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { Subject } from 'rxjs';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-editorial-sidebar',
    templateUrl: 'editorial-sidebar.component.html',
    styleUrls: ['editorial-sidebar.component.scss'],
    imports: [EduSharingUiCommonModule, CommonModule, MatButtonModule, TranslateModule],
    providers: [OptionsHelperDataService],
})
export class EditorialSidebarComponent implements OnInit, OnDestroy {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    nodes = input<Node[]>();
    isModal = input<boolean>(false);
    title = signal('EDITORIAL.SIDEBAR.TITLE');
    @Output() closeTrigger = new EventEmitter<void>();
    @ViewChild('content', { static: true }) dialogContent: TemplateRef<unknown>;

    private readonly destroyed = new Subject<void>();
    options = signal<OptionItem[]>(null);

    constructor(
        private dialogs: DialogsService,
        private optionsHelperDataService: OptionsHelperDataService,
    ) {}

    ngOnInit(): void {
        if (this.isModal()) {
            void this.openDialog();
        }
        void this.initOptions();
    }

    private async initOptions() {
        const options = [];
        const todo = new OptionItem('test', 'home', () => {});
        options.push(todo);

        options.forEach((o) => (o.elementType = [ElementType.Unknown]));
        this.optionsHelperDataService.setData({
            scope: Scope.EditorialSidebar,
            customOptions: {
                useDefaultOptions: false,
                addOptions: options,
            },
        });
        this.options.set(await this.optionsHelperDataService.getAvailableOptions(Target.Actionbar));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private async openDialog(): Promise<CardDialogRef<unknown>> {
        return await this.dialogs.openGenericDialog({
            title: this.title(),
            contentTemplate: this.dialogContent,
            minWidth: 350,
        });
    }
}
