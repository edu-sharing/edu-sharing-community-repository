import { Injectable } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Node } from 'ngx-edu-sharing-api';
import { Observable, Subject } from 'rxjs';
import { first, map, take, takeUntil } from 'rxjs/operators';
import { WorkspaceManagementDialogsComponent } from './management-dialogs.component';

export interface DialogRef<R> {
    close: () => void;
    afterClosed: () => Observable<R | undefined>;
}

/**
 * Provides access to dialogs via a global component.
 *
 * @deprecated will be replaced by overlay-based dialogs available through `DialogsService`.
 */
@Injectable({
    providedIn: 'root',
})
export class ManagementDialogsService {
    private dialogsComponent: WorkspaceManagementDialogsComponent;

    constructor(private route: ActivatedRoute, private router: Router) {}

    getDialogsComponent(): WorkspaceManagementDialogsComponent {
        return this.dialogsComponent;
    }

    registerDialogsComponent(dialogsComponent: WorkspaceManagementDialogsComponent): void {
        this.dialogsComponent = dialogsComponent;
        this.subscribeChanges();
    }

    openUpload({ parent, files }: { parent: Node; files: FileList }): DialogRef<Node[]> {
        const dialogClosed = new Subject<Node[] | undefined>();
        this.dialogsComponent.parent = parent;
        this.dialogsComponent.filesToUpload = files;
        dialogClosed.subscribe(() => {
            // Reset to default values
            this.dialogsComponent.parent = null;
        });
        // After the upload, a metadata editor will be shown to the user. When that dialog is closed
        // `onUploadFilesProcessed` will be called---even if it was canceled.
        this.dialogsComponent.onUploadFilesProcessed.pipe(take(1)).subscribe(dialogClosed);
        return {
            close: null as () => void, // Not implemented
            afterClosed: () => dialogClosed.asObservable(),
        };
    }

    openUploadSelect({
        parent,
        showPicker = false,
    }: {
        parent: Node;
        showPicker: boolean;
    }): DialogRef<{ files: FileList; parent: Node | undefined }> {
        const dialogClosed = new Subject<
            { files: FileList; parent: Node | undefined } | undefined
        >();
        this.dialogsComponent.parent = parent;
        this.dialogsComponent.uploadShowPicker = showPicker;
        this.dialogsComponent.showUploadSelect = true;
        this.dialogsComponent.showUploadSelectChange
            .pipe(
                first((value) => !value),
                map(() => void 0),
            )
            .subscribe(dialogClosed);
        dialogClosed.subscribe(() => {
            // Reset to default values
            this.dialogsComponent.uploadShowPicker = false;
            this.dialogsComponent.parent = null;
        });
        this.dialogsComponent.onUploadFileSelected
            .pipe(takeUntil(dialogClosed))
            // Do not subscribe to `dialogClosed` directly, since the `takeUntil` pipe will complete
            // the observable before anyone sees its `next` value when the dialog is closed without
            // a result.
            .subscribe((files) => {
                dialogClosed.next({ files, parent: this.dialogsComponent.parent || parent });
                dialogClosed.complete();
            });
        return {
            close: () => dialogClosed.isStopped || (this.dialogsComponent.showUploadSelect = false),
            afterClosed: () => dialogClosed.asObservable(),
        };
    }

    private subscribeChanges() {
        this.dialogsComponent.signupGroupChange.subscribe((value: boolean) => {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParamsHandling: 'merge',
                queryParams: {
                    signupGroup: value || null,
                },
            });
        });
        this.route.queryParams.subscribe((params: Params) => {
            this.dialogsComponent.signupGroup = params.signupGroup;
        });
    }
}
