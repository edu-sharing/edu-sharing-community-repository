import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    Inject,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { AuthenticationService, Node, NodeService } from 'ngx-edu-sharing-api';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { first, startWith, take } from 'rxjs/operators';
import { DialogButton, RestHelper } from 'src/app/core-module/core.module';
import { Toast, ToastType } from 'src/app/core-ui-module/toast';
import { UIHelper } from 'src/app/core-ui-module/ui-helper';
import { Node } from '../../../../core-module/rest/data-object';
import { MainNavService } from '../../../../main/navigation/main-nav.service';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';

export interface NodeEmbedDialogData {
    node: Node;
}

/**
 * Dialog to generate an embed snippet for a node.
 */
@Component({
    selector: 'es-node-embed-dialog',
    templateUrl: './node-embed-dialog.component.html',
    styleUrls: ['./node-embed-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEmbedDialogComponent implements OnInit, OnDestroy {
    @HostBinding('hidden') hidden: string | null = null;
    @ViewChild('textarea') textareaRef: ElementRef<HTMLTextAreaElement>;

    readonly buttons = [new DialogButton('OPTIONS.COPY', { color: 'primary' }, () => this.copy())];

    readonly sizeConstraints = {
        width: { min: 300, max: 1200 },
        height: { min: 200, max: 1200 },
    };

    readonly form = new FormGroup({
        width: new FormControl(400, [
            Validators.min(this.sizeConstraints.width.min),
            Validators.max(this.sizeConstraints.width.max),
        ]),
        height: new FormControl(300, [
            Validators.min(this.sizeConstraints.height.min),
            Validators.max(this.sizeConstraints.height.max),
        ]),
        version: new FormControl('fixed'),
    });

    embedCode = '';

    private readonly destroyed$ = new Subject<void>();
    isPublic: boolean;
    canPublish: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeEmbedDialogData,
        private dialogRef: CardDialogRef,
        private changeDetectorRef: ChangeDetectorRef,
        private location: Location,
        private mainNav: MainNavService,
        private nodeService: NodeService,
        private authenticationService: AuthenticationService,
        private ngZone: NgZone,
        private router: Router,
        private toast: Toast,
    ) {}

    ngOnInit(): void {
        this.dialogRef.patchConfig({ buttons: this.buttons });
        this.registerFormChanges();
        this.updateIsPublic(this.config.node);
        this.updateSharingPermissions();
    }
    async updateSharingPermissions() {
        const info = await this.authenticationService.observeLoginInfo().pipe(first()).toPromise();
        this.canPublish =
            info.toolPermissions.includes(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES) &&
            this.config.node.access?.includes(RestConstants.PERMISSION_WRITE);
        this.changeDetectorRef.detectChanges();
    }
    updateIsPublic(node: Node) {
        this.isPublic = node.isPublic;
        this.changeDetectorRef.detectChanges();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    openInviteDialog(): void {
        // We cannot show the invite dialog on top of this dialog, since this dialog is attached via
        // a `cdkOverlay`, so instead, we just hide this dialog until the invite dialog is closed.
        this.hidden = 'true';
        this.mainNav.getDialogs().nodeShare = [this.data.node];
        this.mainNav
            .getDialogs()
            .nodeShareChange.pipe(
                first((value) => !value),
                tap((event) => console.log(this.config.node)),
                // update node to check if "isPublic" has changed
                tap(async () =>
                    this.updateIsPublic(await this.nodeService.getNode(this.config.node.ref.repo, this.config.node.ref.id).toPromise())
                ),
            ).subscribe(() => (this.hidden = null));
    }

    private registerFormChanges(): void {
        this.form.valueChanges.pipe(startWith(this.form.value)).subscribe((values) => {
            if (this.form.valid) {
                this.embedCode = this.getEmbedCode(values);
                // Run a second change detection for `cdkTextareaAutosize` on the embed-code
                // textarea.
                this.ngZone.runOutsideAngular(() =>
                    setTimeout(() => this.changeDetectorRef.detectChanges()),
                );
            }
            this.buttons[0].disabled = !this.form.valid;
        });
        // The dialog is closable by backdrop click until any value has been changed.
        this.form.valueChanges.pipe(take(1)).subscribe(() => {
            this.dialogRef.patchConfig({ closable: Closable.Standard });
        });
    }

    private async copy(): Promise<void> {
        UIHelper.copyElementToClipboard(this.textareaRef.nativeElement);
        this.toast.show({
            message: 'EMBED.COPIED_TO_CLIPBOARD_NOTICE',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
    }

    private getEmbedCode(values: any): string {
        const node = this.data.node;
        // We use `createElement` to have attributes sanitized. Note that occurrences of `&` in the
        // attribute `src` are rightfully escaped to `&amp;`.
        const iFrame = document.createElement('iframe');
        iFrame.src = this.getEmbedLink(node, values.version);
        iFrame.title = RestHelper.getTitle(node);
        iFrame.width = values.width;
        iFrame.height = values.height;
        iFrame.frameBorder = '0';
        iFrame.allow = 'accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture';
        return iFrame.outerHTML;
    }

    private getEmbedLink(node: Node, version: 'fixed' | 'newest'): string {
        const routerLink = 'eduservlet/render';
        const queryParams = {
            node_id: node.ref.id,
            version: version === 'fixed' ? node.content.version : null,
            // Currently, `RenderingServlet` only supports local nodes. Uncomment, when other
            // repositories become supported.
            //
            // repository: node.ref.isHomeRepo ? null : node.ref.repo,
        };
        const urlTree = this.router.createUrlTree([routerLink], { queryParams });
        return location.origin + this.location.prepareExternalUrl(urlTree.toString());
    }
}
