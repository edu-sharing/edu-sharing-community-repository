import { forkJoin as observableForkJoin, BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { FileChangeEvent } from '@angular/compiler-cli/src/perform_watch';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { Node } from '../../../../../core-module/rest/data-object';

@Component({
    selector: 'es-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
})
export class MdsEditorWidgetPreviewComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);
    src: SafeResourceUrl | string;
    nodeSrc: string;
    file: File;
    node: Node;
    delete = false;
    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private sanitizer: DomSanitizer,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$.subscribe((nodes) => {
            if (nodes?.length === 1) {
                this.nodeSrc =
                    nodes[0].preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                this.node = nodes[0];
                this.updateSrc();
                // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                // FIXME: this will run forever!
                setInterval(() => {
                    if (this.file) {
                        return;
                    }
                    this.updateSrc();
                }, 5000);
            }
        });
    }
    setPreview(event: Event): void {
        this.file = (event.target as HTMLInputElement).files[0];
        this.delete = false;
        this.updateSrc();
    }
    updateSrc() {
        if (this.file) {
            this.src = this.sanitizer.bypassSecurityTrustResourceUrl(
                window.URL.createObjectURL(this.file),
            );
        } else {
            this.src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
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
}
