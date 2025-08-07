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
import { EduSharingUiCommonModule, UIConstants } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver } from '@angular/cdk/layout';
import * as rxjs from 'rxjs';
import { map, Subject } from 'rxjs';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { first, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'es-editorial-sidebar',
    templateUrl: 'editorial-sidebar.component.html',
    styleUrls: ['editorial-sidebar.component.scss'],
    imports: [EduSharingUiCommonModule, CommonModule, MatButtonModule, TranslateModule],
})
export class EditorialSidebarComponent implements OnInit, OnDestroy {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    nodes = input<Node[]>();
    isModal = input<boolean>(false);
    title = signal('EDITORIAL.SIDEBAR.TITLE');
    @Output() closeTrigger = new EventEmitter<void>();
    @ViewChild('content', { static: true }) dialogContent: TemplateRef<unknown>;

    private readonly destroyed = new Subject<void>();

    constructor(private dialogs: DialogsService) {}

    ngOnInit(): void {
        if (this.isModal()) {
            void this.openDialog();
        }
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
