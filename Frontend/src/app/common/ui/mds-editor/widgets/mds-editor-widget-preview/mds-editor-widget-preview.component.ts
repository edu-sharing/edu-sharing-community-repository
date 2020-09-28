import { Component, OnInit } from '@angular/core';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import {NativeWidget} from '../../mds-editor-view/mds-editor-view.component';
import {BehaviorSubject, Observable} from 'rxjs';
import {FileChangeEvent} from '@angular/compiler-cli/src/perform_watch';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {RestNodeService} from '../../../../../core-module/rest/services/rest-node.service';
import {Node} from '../../../../../core-module/rest/data-object';

@Component({
    selector: 'app-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
})
export class MdsEditorWidgetPreviewComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);
    src: SafeResourceUrl | string;
    nodeSrc: string;
    file: File;
    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private sanitizer: DomSanitizer,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$.subscribe((nodes) => {
            if (nodes?.length === 1) {
                this.nodeSrc = nodes[0].preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                this.updateSrc();
                // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
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
        this.updateSrc();
    }
    updateSrc() {
        if(this.file) {
            this.src = this.sanitizer.bypassSecurityTrustResourceUrl(window.URL.createObjectURL(this.file));
        } else {
            this.src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
        }
        this.hasChanges.next(this.file != null);
    }
    onSaveNode(nodes: Node[]) {
        if(this.file == null) {
            return null;
        }
        return Observable.forkJoin(nodes.map((n) => this.nodeService.uploadNodePreview(n.ref.id, this.file))).
                map(() => nodes).toPromise();
    }
}
