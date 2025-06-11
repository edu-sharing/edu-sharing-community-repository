import { ChangeDetectorRef, Component, ElementRef, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Node } from 'ngx-edu-sharing-api';
import { RepoUrlService } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, forkJoin, interval, of } from 'rxjs';
import { catchError, map, take, takeWhile } from 'rxjs/operators';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { Toast } from '../../../../../services/toast';
import { Constraints, NativeWidgetComponent } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { AiPreviewImagesOverlayComponent } from './ai-preview-images-overlay/ai-preview-images-overlay.component';

@Component({
    selector: 'es-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetPreviewComponent implements NativeWidgetComponent {
    @ViewChild('overlayRef') overlayRef: ElementRef<HTMLElement>;
    @ViewChild('aiPreviewImagesOverlay') aiPreviewImagesOverlay: AiPreviewImagesOverlayComponent;

    static readonly constraints: Constraints = {
        requiresNode: true,
        supportsBulk: false,
        supportsInlineEditing: true,
        onConstraintFailed: 'hide',
    };

    hasChanges = new BehaviorSubject<boolean>(false);
    src$ = new BehaviorSubject<SafeResourceUrl | string>(null);
    nodeSrc: string;
    file: File;
    node: Node;
    delete = false;
    loading$ = new BehaviorSubject(false);
    overlayVisible$ = new BehaviorSubject(false);

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        public mdsEditorInstance: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private repoUrlService: RepoUrlService,
        private sanitizer: DomSanitizer,
        private toast: Toast,
    ) {
        forkJoin([this.mdsEditorInstance.nodes$.pipe(take(1))])
            .pipe(takeUntilDestroyed())
            .subscribe(([nodes]) => {
                if (nodes?.length === 1) {
                    this.node = nodes[0];
                    this.nodeSrc =
                        this.node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                    void this.updateSrc();
                    // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                    interval(5000)
                        .pipe(
                            takeUntilDestroyed(),
                            takeWhile(() => !this.file && !this.overlayVisible$.getValue()),
                        )
                        .subscribe(() => this.updateSrc());
                }
            });
    }

    async setPreview(event: Event) {
        this.file = (event.target as HTMLInputElement).files[0];
        this.delete = false;
        void this.updateSrc();
    }

    async updateSrc() {
        if (this.file) {
            this.src$.next(
                this.sanitizer.bypassSecurityTrustResourceUrl(
                    window.URL.createObjectURL(this.file),
                ),
            );
        } else {
            const src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
            if (this.node) {
                this.src$.next(await this.repoUrlService.getRepoUrl(src, this.node));
            } else {
                this.src$.next(src);
            }
        }
        this.changeDetectorRef.detectChanges();
        this.hasChanges.next(this.file != null || this.delete);
    }

    async foreSave() {
        this.loading$.next(true);

        try {
            this.node = (await this.onSaveNode(this.mdsEditorInstance.nodes$.value))[0];
            this.file = null;
            this.delete = false;
            void this.updateSrc();
            // loading false is triggered via img load event
            this.toast.toast('DIALOG.SAVED');
        } catch (e) {
            this.toast.error(e);
        }
    }

    showOverlay() {
        if (this.aiPreviewImagesOverlay && this.overlayRef) {
            this.aiPreviewImagesOverlay.open(this.overlayRef.nativeElement);
        }
    }

    onSaveNode(nodes: Node[]) {
        if (this.delete) {
            return forkJoin(nodes.map((n) => this.nodeService.deleteNodePreview(n.ref.id)))
                .pipe(map(() => nodes))
                .toPromise();
        }
        if (this.file == null) {
            return null;
        }
        return forkJoin(
            nodes.map((n) =>
                this.nodeService
                    .uploadNodePreview(n.ref.id, this.file, false)
                    .pipe(map((n) => n.node)),
            ),
        )
            .pipe(
                catchError((e) => {
                    this.toast.error(null, 'MDS.ERROR_PREVIEW');
                    return of(nodes);
                }),
            )
            .toPromise();
    }

    getType() {
        return this.node?.preview?.type;
    }
}
