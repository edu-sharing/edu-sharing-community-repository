import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    Inject,
    InjectionToken,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { AuthenticationService, Node, NodeService } from 'ngx-edu-sharing-api';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { pipe, Subject } from 'rxjs';
import { first, startWith, tap } from 'rxjs/operators';
import { DialogButton, RestConstants, RestHelper } from 'src/app/core-module/core.module';
import { Toast, ToastType } from 'src/app/core-ui-module/toast';
import { UIHelper } from 'src/app/core-ui-module/ui-helper';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { I } from '@angular/cdk/keycodes';

export interface NodeEmbedConfig {
    node: Node;
    onClose: () => void;
}

export const NODE_EMBED_CONFIG = new InjectionToken<NodeEmbedConfig>('Node Embed Config');

/**
 * Dialog to generate an embed snippet for a node.
 *
 * Use via `NodeEmbedService`.
 */
@Component({
    selector: 'es-node-embed',
    templateUrl: './node-embed.component.html',
    styleUrls: ['./node-embed.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEmbedComponent implements OnInit, OnDestroy {
    @HostBinding('hidden') hidden: string | null = null;
    @ViewChild('textarea') textareaRef: ElementRef<HTMLTextAreaElement>;

    readonly buttons = [
        new DialogButton('OPTIONS.COPY', DialogButton.TYPE_PRIMARY, () => this.copy()),
        // new DialogButton('CLOSE', DialogButton.TYPE_CANCEL, this.config.onClose),
    ];

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
        @Inject(NODE_EMBED_CONFIG) public config: NodeEmbedConfig,
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
        this.mainNav.getDialogs().nodeShare = [this.config.node];
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
        const node = this.config.node;
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
