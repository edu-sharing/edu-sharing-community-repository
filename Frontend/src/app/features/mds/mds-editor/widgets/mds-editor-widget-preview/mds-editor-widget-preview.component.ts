import { Component } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Metadata } from 'ngx-edu-sharing-graphql';
import * as rxjs from 'rxjs';
import { BehaviorSubject, forkJoin, forkJoin as observableForkJoin } from 'rxjs';
import { map, take, takeWhile } from 'rxjs/operators';
import { Node } from '../../../../../core-module/rest/data-object';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RepoUrlService } from 'ngx-edu-sharing-ui';
import { NativeWidgetComponent } from '../../../types/types';
@Component({
    selector: 'es-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
})
export class MdsEditorWidgetPreviewComponent implements NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    static readonly graphqlIds = [
        'info.preview.url',
        'info.preview.data',
        'info.preview.mimetype',
        'info.preview.type',
    ];

    hasChanges = new BehaviorSubject<boolean>(false);
    src: SafeResourceUrl | string;
    nodeSrc: string;
    file: File;
    node: Node;
    metadata: Metadata;
    delete = false;

    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private repoUrlService: RepoUrlService,
        private sanitizer: DomSanitizer,
    ) {
        forkJoin([
            this.mdsEditorValues.nodes$.pipe(take(1)),
            this.mdsEditorValues.graphqlMetadata$.pipe(take(1)),
        ])
            .pipe(takeUntilDestroyed())
            .subscribe(([nodes, graphqlMetadata]) => {
                if (nodes?.length === 1) {
                    this.node = nodes[0];
                    this.nodeSrc =
                        this.node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                } else if (graphqlMetadata?.length === 1) {
                    this.metadata = graphqlMetadata[0];
                    this.nodeSrc =
                        this.metadata.info.preview.url +
                        '&crop=true&width=400&height=300&dontcache=:cache';
                }
                if (nodes?.length === 1 || graphqlMetadata?.length === 1) {
                    this.updateSrc();
                    // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                    rxjs.interval(5000)
                        .pipe(
                            takeUntilDestroyed(),
                            takeWhile(() => !this.file),
                        )
                        .subscribe(() => this.updateSrc());
                }
            });
    }

    setPreview(event: Event): void {
        this.file = (event.target as HTMLInputElement).files[0];
        this.delete = false;
        this.updateSrc();
    }

    async updateSrc() {
        if (this.file) {
            this.src = this.sanitizer.bypassSecurityTrustResourceUrl(
                window.URL.createObjectURL(this.file),
            );
        } else {
            const src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
            if (this.node) {
                this.src = await this.repoUrlService.getRepoUrl(src, this.node);
            } else {
                this.src = src;
            }
        }
        this.hasChanges.next(this.file != null || this.delete);
    }

    onSaveNode(nodes: Node[]) {
        if (this.delete) {
            return observableForkJoin(
                nodes.map((n) => this.nodeService.deleteNodePreview(n.ref.id)),
            )
                .pipe(map(() => nodes))
                .toPromise();
        }
        if (this.file == null) {
            return null;
        }
        return observableForkJoin(
            nodes.map((n) => this.nodeService.uploadNodePreview(n.ref.id, this.file, false)),
        )
            .pipe(map(() => nodes))
            .toPromise();
    }

    getType() {
        return this.node?.preview?.type || this.metadata?.info?.preview?.type;
    }
}
