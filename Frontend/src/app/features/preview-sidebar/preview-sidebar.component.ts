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
    @ViewChild('content') contentRef: TemplateRef<HTMLElement>;

    /** The node to preview. */
    @Input() node: Node;
    /** Emits when the user clicked the "close" button. */
    @Output() closed = new EventEmitter<void>();

    readonly isMobileScreen = this.getIsMobileScreen();

    private readonly destroyed = new Subject<void>();

    constructor(private dialogs: DialogsService, private breakpointObserver: BreakpointObserver) {}

    ngAfterViewInit(): void {
        // Wait for `contentRef` to be populated before calling `registerDialogOnMobile`.
        this.registerDialogOnMobile();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
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
                if (isMobileScreen && !dialogRefPromise) {
                    dialogRefPromise = this.openAsDialog();
                    const dialogRef = await dialogRefPromise;
                    dialogRef.afterClosed().subscribe(() => {
                        dialogRefPromise = null;
                        if (isMobileScreen && !this.destroyed.isStopped) {
                            this.closed.emit();
                        }
                    });
                } else if (!isMobileScreen) {
                    void dialogRefPromise?.then((dialogRef) => dialogRef.close());
                }
            });
        this.destroyed.subscribe(() => {
            void dialogRefPromise?.then((dialogRef) => dialogRef.close());
        });
    }

    private async openAsDialog(): Promise<CardDialogRef<unknown>> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: RestHelper.getTitle(this.node),
            avatar: { kind: 'image', url: this.node.iconURL },
            contentTemplate: this.contentRef,
            minWidth: 400,
            contentPadding: 0,
        });
        return dialogRef;
    }

    private getIsMobileScreen() {
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));
    }
}
