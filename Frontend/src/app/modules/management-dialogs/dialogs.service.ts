import { Overlay } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { LinkData } from '../../core-ui-module/node-helper.service';
import { Values } from '../../features/mds/types/types';
import { WorkspaceFileUploadSelectComponent } from './file-upload-select/file-upload-select.component';
import { WorkspaceLicenseComponent } from './license/license.component';

export interface DialogRef<R> {
    close: () => void;
    afterClosed: () => Observable<R | undefined>;
}

export type UploadSelectResult =
    | {
          kind: 'file';
          files: FileList;
      }
    | {
          kind: 'link';
          link: LinkData;
      };

/**
 * Provides overlay-based dialogs.
 *
 * Meant to replace the global `ManagementDialogsComponent`.
 */
@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    constructor(private overlay: Overlay) {}

    openLicenseDialog({ properties }: { properties?: Values }): DialogRef<Values> {
        const overlayRef = this.overlay.create();
        const portal = new ComponentPortal(WorkspaceLicenseComponent);
        const componentRef = overlayRef.attach(portal);
        const dialogClosed = new Subject<Values | null>();
        properties && (componentRef.instance.properties = properties);
        componentRef.instance.onDone.subscribe((properties) =>
            dialogClosed.next(properties as Values),
        );
        componentRef.instance.onCancel.subscribe(() => dialogClosed.next(null));
        dialogClosed.subscribe(() => overlayRef.dispose());
        return {
            close: () => dialogClosed.next(null),
            afterClosed: () => dialogClosed.pipe(take(1)),
        };
    }

    openUploadSelectDialog({
        showLti = true,
    }: {
        showLti?: boolean;
    }): DialogRef<UploadSelectResult> {
        const overlayRef = this.overlay.create();
        const portal = new ComponentPortal(WorkspaceFileUploadSelectComponent);
        const componentRef = overlayRef.attach(portal);
        const dialogClosed = new Subject<UploadSelectResult | null>();
        componentRef.instance.showLti = showLti;
        componentRef.instance.onFileSelected.subscribe((files) =>
            dialogClosed.next({ kind: 'file', files }),
        );
        componentRef.instance.onLinkSelected.subscribe((link) =>
            dialogClosed.next({ kind: 'link', link }),
        );
        componentRef.instance.onCancel.subscribe(() => dialogClosed.next(null));
        dialogClosed.subscribe(() => overlayRef.dispose());
        return {
            close: () => dialogClosed.next(null),
            afterClosed: () => dialogClosed.pipe(take(1)),
        };
    }
}
