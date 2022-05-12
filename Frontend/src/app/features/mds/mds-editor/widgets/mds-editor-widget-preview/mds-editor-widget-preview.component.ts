import {forkJoin as observableForkJoin, BehaviorSubject, Observable, forkJoin} from 'rxjs';
import { map } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {RestNodeService} from '../../../../../core-module/rest/services/rest-node.service';
import {Node} from '../../../../../core-module/rest/data-object';
import {Metadata} from 'ngx-edu-sharing-graphql';
import {NativeWidgetComponent} from '../../../types/mds-types';
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
        private sanitizer: DomSanitizer,
    ) {}

    ngOnInit(): void {
        forkJoin([
            this.mdsEditorValues.nodes$,
            this.mdsEditorValues.graphqlMetadata$,
        ]).subscribe(data => {
            if (data[0]?.length === 1) {
                this.node = data[0][0];
                this.nodeSrc = this.node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
            } else if (data[1]?.length === 1) {
                this.metadata = data[1][0];
                this.nodeSrc = this.metadata.info.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
            }
            if (data[0]?.length === 1 || data[1]?.length === 1) {
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
        this.mdsEditorValues.graphqlMetadata$.subscribe((metadata) => {
            if (metadata?.length === 1) {
                console.log(metadata);
                this.nodeSrc = metadata[0].info.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                this.metadata = metadata[0];
                this.updateSrc();
                // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                // FIXME: this will run forever!
                setInterval(() => {
                    if(this.file) {
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
        if(this.file) {
            this.src = this.sanitizer.bypassSecurityTrustResourceUrl(window.URL.createObjectURL(this.file));
        } else {
            this.src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
        }
        this.hasChanges.next(this.file != null || this.delete);
    }
    onSaveNode(nodes: Node[]) {
        if (this.delete) {
            return observableForkJoin(nodes.map((n) => this.nodeService.deleteNodePreview(n.ref.id))).pipe(
                map(() => nodes)).toPromise();
        }
        if(this.file == null) {
            return null;
        }
        return observableForkJoin(nodes.map((n) => this.nodeService.uploadNodePreview(n.ref.id, this.file))).pipe(
                map(() => nodes)).toPromise();
    }

    getType() {
        return this.node?.preview?.type || this.metadata?.info?.preview?.type;
    }
}
