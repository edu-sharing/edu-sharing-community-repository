import { Component, Input, EventEmitter, Output, ViewChild, ElementRef } from '@angular/core';
import { RestMdsService } from '../../../core-module/core.module';
import { MdsMetadatasets, Node, MdsInfo, NodeList } from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { RestHelper } from '../../../core-module/core.module';
import { RestNodeService } from '../../../core-module/core.module';
import { BreadcrumbsService } from '../../../shared/components/breadcrumbs/breadcrumbs.service';

@Component({
    selector: 'es-workspace-create-lti',
    templateUrl: 'create-lti.component.html',
    styleUrls: ['create-lti.component.scss'],
    providers: [BreadcrumbsService],
})
export class WorkspaceCreateLtiComponent {
    @ViewChild('input') input: ElementRef;
    public disabled = true;
    public _name = '';
    public _parent: Node;
    public _tool: Node;
    @Input() set name(name: string) {
        this._name = name;
        this.input.nativeElement.focus();
    }
    @Input() set parent(parent: Node) {
        this._parent = parent;
        this.node.getNodeParents(parent.ref.id).subscribe((data) => {
            this.breadcrumbsService.setNodePath(data.nodes.reverse());
        });
    }
    @Input() set tool(tool: Node) {
        this._tool = tool;
    }
    @Output() onCancel = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    constructor(
        private node: RestNodeService,
        private breadcrumbsService: BreadcrumbsService,
        private translate: TranslateService,
    ) {}
    public cancel() {
        this.onCancel.emit();
    }
    public create() {
        if (this.disabled) return;
        this.onCreate.emit({ name: this._name, parent: this._parent });
    }
    public setState(event: any) {
        this.disabled = !this._name.trim();
    }
}
