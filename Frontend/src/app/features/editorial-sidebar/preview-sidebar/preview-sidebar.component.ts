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
import { BehaviorSubject, firstValueFrom, of, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { PreviewContentComponent } from './preview-content/preview-content.component';

import { PreviewSidebarService } from './preview-sidebar.service';
import { CustomOptions, RestHelper } from 'ngx-edu-sharing-ui';
import { CardDialogRef } from '../../dialogs/card-dialog/card-dialog-ref';
import { GenericDialogData } from '../../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../dialogs/dialogs.service';

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
    /**
     * custom options to configure the actionbar
     */
    @Input() customOptions: CustomOptions;
    /** Emits when the user clicked the "close" button. */
    @Output() closed = new EventEmitter<void>();

    readonly isMobileScreen = this.getIsMobileScreen();

    private readonly destroyed = new Subject<void>();
    private modalDialogRef: CardDialogRef<GenericDialogData<string>, string>;
    private modalOpen$ = new BehaviorSubject<boolean>(false);

    constructor(
        private dialogs: DialogsService,
        private previewSidebarService: PreviewSidebarService,
        private breakpointObserver: BreakpointObserver,
    ) {
        this.previewSidebarService.registerInstance(this);
    }

    ngAfterViewInit(): void {
        // Wait for `contentRef` to be populated before calling `registerDialogOnMobile`.
        // this.registerDialogOnMobile();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.previewSidebarService.unregisterInstance(this);
    }

    async updateNode(node: Node): Promise<void> {
        this.node = node;
        const isMobileScreen: boolean = await firstValueFrom(this.getIsMobileScreen());
        if (isMobileScreen && !this.modalDialogRef) {
            // setTimeout is currently necessary to wait for the view being rendered
            setTimeout(async (): Promise<void> => {
                await this.openAsDialog();
            });
        }
    }

    getModalOpenState(): BehaviorSubject<boolean> {
        return this.modalOpen$;
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
        if (!this.node) {
            this.modalDialogRef = null;
            return;
        }
        this.modalDialogRef = await this.dialogs.openGenericDialog({
            title: RestHelper.getTitle(this.node),
            avatar: { kind: 'image', url: this.node.icon?.url },
            contentTemplate: this.modalRef,
            minWidth: '100%',
            minHeight: '100%',
            contentPadding: 0,
        });
        this.modalOpen$.next(true);
        this.modalDialogRef.afterClosed().subscribe(() => {
            this.modalDialogRef = null;
            this.modalOpen$.next(false);
            if (this.isMobileScreen && !this.destroyed.isStopped) {
                this.closed.emit();
            }
        });
    }

    private getIsMobileScreen() {
        // handled in editorial sidebar!
        return of(false);
        /*
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));*/
    }

    async openModal() {
        await this.openAsDialog();
    }
}
