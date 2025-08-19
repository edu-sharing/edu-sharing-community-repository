import { BreakpointObserver } from '@angular/cdk/layout';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';
import { RestHelper } from '../../core-module/core.module';
import { CardDialogRef } from '../dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../dialogs/dialogs.service';
import { PreviewContentComponent } from './preview-content.component';
import { GenericDialogData } from '../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { PreviewSidebarService } from './preview-sidebar.service';

/**
 * Sidebar component that previews an element with preview image and some metadata.
 *
 * The sidebar will collapse and display as overlay dialog instead on small screens.
 */
@Component({
    selector: 'es-preview-sidebar',
    templateUrl: './preview-sidebar.component.html',
    styleUrls: ['./preview-sidebar.component.scss'],
    standalone: false,
})
export class PreviewSidebarComponent implements OnDestroy, AfterViewInit {
    @ViewChild('modal') modalRef: TemplateRef<HTMLElement>;
    @ViewChild('preview') previewRef: PreviewContentComponent;

    /** The node to preview. */
    @Input() node: Node;
    /** Emits when the user clicked the "close" button. */
    @Output() closed = new EventEmitter<void>();

    readonly isMobileScreen = this.getIsMobileScreen();

    private readonly destroyed = new Subject<void>();
    private modalDialogRef: CardDialogRef<GenericDialogData<string>, string>;

    constructor(
        private dialogs: DialogsService,
        private previewSidebarService: PreviewSidebarService,
        private breakpointObserver: BreakpointObserver,
    ) {
        this.previewSidebarService.registerInstance(this);
    }

    ngAfterViewInit(): void {
        // Wait for `contentRef` to be populated before calling `registerDialogOnMobile`.
        this.registerDialogOnMobile();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.previewSidebarService.unregisterInstance(this);
    }

    private registerDialogOnMobile(): void {
        let dialogRefPromise: Promise<CardDialogRef<unknown>>;
        let isMobileScreen: boolean;
        this.isMobileScreen
            .pipe(
                tap((value) => (isMobileScreen = value)),
                takeUntil(this.destroyed),
            )
            .subscribe(async () => {
                if (isMobileScreen && !this.modalDialogRef) {
                    await this.openAsDialog();
                } else if (!isMobileScreen) {
                    void dialogRefPromise?.then((dialogRef) => dialogRef.close());
                }
            });
        this.destroyed.subscribe(() => {
            void dialogRefPromise?.then((dialogRef) => dialogRef.close());
        });
    }

    private async openAsDialog() {
        this.modalDialogRef = await this.dialogs.openGenericDialog({
            title: RestHelper.getTitle(this.node),
            avatar: { kind: 'image', url: this.node.iconURL },
            contentTemplate: this.modalRef,
            minWidth: '100%',
            minHeight: '100%',
            contentPadding: 0,
        });
        this.modalDialogRef.afterClosed().subscribe(() => {
            this.modalDialogRef = null;
            if (this.isMobileScreen && !this.destroyed.isStopped) {
                this.closed.emit();
            }
        });
    }

    private getIsMobileScreen() {
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));
    }

    async openModal() {
        await this.openAsDialog();
    }
}
