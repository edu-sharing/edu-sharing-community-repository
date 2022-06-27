import { Component, Input, Output, EventEmitter } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ArchiveRestore, DialogButton, Node } from '../../../../core-module/core.module';

@Component({
    selector: 'es-recycle-restore-info',
    templateUrl: 'restore.component.html',
    styleUrls: ['restore.component.css'],
})
export class RecycleRestoreComponent {
    @Input() results: any;
    @Output() onClose = new EventEmitter();
    @Output() onRestoreFolder = new EventEmitter();
    public showFileChooser: Boolean;
    buttons: DialogButton[];
    constructor() {
        this.buttons = DialogButton.getSingleButton('CLOSE', () => this.confirm(), 'standard');
    }
    public confirm(): void {
        this.onClose.emit();
    }
    private cancel(): void {
        this.onClose.emit();
    }
    private chooseDirectory(): void {
        this.showFileChooser = new Boolean(true);
    }
    closeFolder() {
        this.showFileChooser = false;
    }
    folderSelected(event: Node[]) {
        const nodes: any[] = [];
        for (const result of this.results.results) {
            if ((result.restoreStatus as any) === 1) {
                nodes.push({ ref: { nId: result.nodeId } });
            }
        }
        this.showFileChooser = false;
        // this.appComponent.restoreNodes(nodes,event.ref.id);
        this.onRestoreFolder.emit({ nodes, parent: event[0].ref.id });
        this.cancel();
    }
    public static get STATUS_FINE(): string {
        return 'FINE';
    }
    public static get STATUS_DUPLICATENAME(): string {
        return 'DUPLICATENAME';
    }
    public static get STATUS_PARENT_FOLDER_MISSING(): string {
        return 'FALLBACK_PARENT_NOT_EXISTS';
    }
    public static get STATUS_PARENT_FOLDER_NO_PERMISSION(): string {
        return 'FALLBACK_PARENT_NO_PERMISSION';
    }

    public static prepareResults(translate: TranslateService, results: any) {
        for (const result of results.results) {
            if (result.restoreStatus === RecycleRestoreComponent.STATUS_FINE) continue;
            translate
                .get('RECYCLE.RESTORE.' + result.restoreStatus)
                .subscribe((text: any) => (result.message = text));
            if (result.restoreStatus === RecycleRestoreComponent.STATUS_DUPLICATENAME) {
                results.hasDuplicateNames = true;
                result.restoreStatus = 0;
            }
            if (
                result.restoreStatus === RecycleRestoreComponent.STATUS_PARENT_FOLDER_MISSING ||
                result.restoreStatus === RecycleRestoreComponent.STATUS_PARENT_FOLDER_NO_PERMISSION
            ) {
                results.hasParentFolderMissing = true;
                result.restoreStatus = 1;
            }
        }
    }
}
